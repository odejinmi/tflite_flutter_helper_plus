package com.a5starcompany.tflite_flutter_helper_plus

import androidx.annotation.NonNull

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class SoundStreamErrors {
    FailedToRecord,
    FailedToPlay,
    FailedToStop,
    FailedToWriteBuffer,
    Unknown,
}

enum class SoundStreamStatus {
    Unset,
    Initialized,
    Playing,
    Stopped,
}

const val METHOD_CHANNEL_NAME = "tflite_flutter_helper_plus"

private val LOG_TAG = "TfLiteFlutterHelperPlugin"
private val AUDIO_RECORD_PERMISSION_CODE = 14887
private val DEFAULT_SAMPLE_RATE = 16000
private val DEFAULT_BUFFER_SIZE = 8192
private val DEFAULT_PERIOD_FRAMES = 8192


private lateinit var methodChannel: MethodChannel
private var permissionToRecordAudio: Boolean = false
private var activeResult: MethodChannel.Result? = null
private var debugLogging: Boolean = false

private val mRecordFormat = AudioFormat.ENCODING_PCM_16BIT
private var mRecordSampleRate = DEFAULT_SAMPLE_RATE
private var mRecorderBufferSize = DEFAULT_BUFFER_SIZE
private var mPeriodFrames = DEFAULT_PERIOD_FRAMES
private var audioData: ShortArray? = null
private var mRecorder: AudioRecord? = null
private var mListener: AudioRecord.OnRecordPositionUpdateListener? = null

class MethodCallHandlerImpl(
    messenger: BinaryMessenger?,
    private val binding: ActivityPluginBinding
) :
    MethodChannel.MethodCallHandler, PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {

    private var channel: MethodChannel? = null
    private var result: MethodChannel.Result? = null


    init {
        channel = MethodChannel(messenger!!, METHOD_CHANNEL_NAME)

        channel?.setMethodCallHandler(this)

        binding.addActivityResultListener(this)
        binding.addRequestPermissionsResultListener(this)

    }

    @SuppressLint("LongLogTag")
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        this.result = result

        try {
            when (call.method) {
                "hasPermission" -> hasPermission(result)
                "initializeRecorder" -> initializeRecorder(call, result)
                "startRecording" -> startRecording(result)
                "stopRecording" -> stopRecording(result)
                "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")
                else -> result.notImplemented()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Unexpected exception", e)
            // TODO: implement result.error
        }

    }

    /**
     * It is invoked when making transaction
     * @param arg is the data that was passed in from the flutter side to make payment
     */


    private fun hasRecordPermission(): Boolean {
        if (permissionToRecordAudio) return true

        permissionToRecordAudio = binding.activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return permissionToRecordAudio

    }

    private fun hasPermission(result: MethodChannel.Result) {
        result.success(hasRecordPermission())
    }

    private fun requestRecordPermission() {
        if (!hasRecordPermission()) {
            debugLog("requesting RECORD_AUDIO permission")
            binding.activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_RECORD_PERMISSION_CODE)
        }
    }

    /**
     * this is the call back that is invoked when the activity result returns a value after calling
     * startActivityForResult().
     * @param data is the intent that has the bundle where we can get our result [MonnifyTransactionResponse]
     * @param requestCode if it matches with our [REQUEST_CODE] it means the result if the one we
     * asked for.
     * @param resultCode, it is okay if it equals [RESULT_OK]
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        // Your code to handle permission results
        when (requestCode) {
            AUDIO_RECORD_PERMISSION_CODE -> {
                permissionToRecordAudio = grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                completeInitializeRecorder()
                return true
            }
        }
        return false
    }
    /**
     * dispose the channel when this handler detaches from the activity
     */
    fun dispose() {
        channel?.setMethodCallHandler(null)
        channel = null
    }


    private fun initializeRecorder(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        mRecordSampleRate = call.argument<Int>("sampleRate") ?: mRecordSampleRate
        debugLogging = call.argument<Boolean>("showLogs") ?: false
        mPeriodFrames = AudioRecord.getMinBufferSize(mRecordSampleRate, AudioFormat.CHANNEL_IN_MONO, mRecordFormat)
        mRecorderBufferSize = mPeriodFrames * 2
        audioData = ShortArray(mPeriodFrames)
        activeResult = result

//        if (null == localContext) {
//            completeInitializeRecorder()
//            return
//        }
        permissionToRecordAudio = binding.activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!permissionToRecordAudio) {
            requestRecordPermission()
        } else {
            debugLog("has permission, completing")
            completeInitializeRecorder()
        }
        debugLog("leaving initializeIfPermitted")
    }

    private fun initRecorder() {
        if (mRecorder?.state == AudioRecord.STATE_INITIALIZED) {
            return
        }
        mRecorder = AudioRecord(MediaRecorder.AudioSource.MIC, mRecordSampleRate, AudioFormat.CHANNEL_IN_MONO, mRecordFormat, mRecorderBufferSize)
        if (mRecorder != null) {
            mListener = createRecordListener()
            mRecorder?.positionNotificationPeriod = mPeriodFrames
            mRecorder?.setRecordPositionUpdateListener(mListener)
        }
    }

    private fun completeInitializeRecorder() {

        debugLog("completeInitialize")
        val initResult: HashMap<String, Any> = HashMap()

        if (permissionToRecordAudio) {
            mRecorder?.release()
            initRecorder()
            initResult["isMeteringEnabled"] = true
            sendRecorderStatus(SoundStreamStatus.Initialized)
        }

        initResult["success"] = permissionToRecordAudio
        debugLog("sending result")
        activeResult?.success(initResult)
        debugLog("leaving complete")
        activeResult = null
    }

    private fun sendEventMethod(name: String, data: Any) {
        val eventData: HashMap<String, Any> = HashMap()
        eventData["name"] = name
        eventData["data"] = data
        methodChannel.invokeMethod("platformEvent", eventData)
    }

    private fun debugLog(msg: String) {
        if (debugLogging) {
            Log.d(LOG_TAG, msg)
        }
    }

    private fun startRecording(result: MethodChannel.Result) {
        try {
            if (mRecorder!!.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                result.success(true)
                return
            }
            initRecorder()
            mRecorder!!.startRecording()
            sendRecorderStatus(SoundStreamStatus.Playing)
            result.success(true)
        } catch (e: IllegalStateException) {
            debugLog("record() failed")
            result.error(SoundStreamErrors.FailedToRecord.name, "Failed to start recording", e.localizedMessage)
        }
    }

    private fun stopRecording(result: MethodChannel.Result) {
        try {
            if (mRecorder!!.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                result.success(true)
                return
            }
            mRecorder!!.stop()
            sendRecorderStatus(SoundStreamStatus.Stopped)
            result.success(true)
        } catch (e: IllegalStateException) {
            debugLog("record() failed")
            result.error(SoundStreamErrors.FailedToRecord.name, "Failed to start recording", e.localizedMessage)
        }
    }

    private fun sendRecorderStatus(status: SoundStreamStatus) {
        sendEventMethod("recorderStatus", status.name)
    }

    private fun createRecordListener(): AudioRecord.OnRecordPositionUpdateListener? {
        return object : AudioRecord.OnRecordPositionUpdateListener {
            override fun onMarkerReached(recorder: AudioRecord) {
                recorder.read(audioData!!, 0, mRecorderBufferSize)
            }

            override fun onPeriodicNotification(recorder: AudioRecord) {
                val data = audioData!!
                val shortOut = recorder.read(data, 0, mPeriodFrames)
                // https://flutter.io/platform-channels/#codec
                // convert short to int because of platform-channel's limitation
                val byteBuffer = ByteBuffer.allocate(shortOut * 2)
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(data)

                sendEventMethod("dataPeriod", byteBuffer.array())
            }
        }
    }
}