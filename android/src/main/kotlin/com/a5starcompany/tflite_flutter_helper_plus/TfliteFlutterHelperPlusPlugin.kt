package com.a5starcompany.tflite_flutter_helper_plus

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** TfliteFlutterHelperPlusPlugin */
class TfliteFlutterHelperPlusPlugin: FlutterPlugin, ActivityAware {
  private var methodCallHandler: MethodCallHandlerImpl? = null
  private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null


  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    pluginBinding = flutterPluginBinding
  }


  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    pluginBinding = null
  }


  /**
   * It initialises the [methodCallHandler]
   */
  private fun initializeMethodHandler(messenger: BinaryMessenger?, binding: ActivityPluginBinding) {
    methodCallHandler = MethodCallHandlerImpl(messenger,  binding)

  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    initializeMethodHandler(pluginBinding?.binaryMessenger,  binding)


  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivity() {
    methodCallHandler?.dispose()
    methodCallHandler = null

  }
}
