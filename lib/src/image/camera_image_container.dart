import 'package:camera/camera.dart';
import 'package:image/image.dart';
import 'package:quiver/check.dart';
import 'package:tflite_flutter_plus/tflite_flutter_plus.dart';
import 'package:tflite_flutter_helper_plus/src/image/color_space_type.dart';
import 'package:tflite_flutter_helper_plus/src/image/base_image_container.dart';
import 'package:tflite_flutter_helper_plus/src/tensorbuffer/tensorbuffer.dart';

class CameraImageContainer extends BaseImageContainer {
  late final CameraImage cameraImage;

  CameraImageContainer._(this.cameraImage) {
    checkArgument(cameraImage.format.group == ImageFormatGroup.yuv420,
        message: "Only supports loading YUV_420_888 Image.");
  }

  static CameraImageContainer create(CameraImage cameraImage) {
    return CameraImageContainer._(cameraImage);
  }

  @override
  BaseImageContainer clone() {
    throw UnsupportedError("CameraImage cannot be cloned");
  }

  @override
  ColorSpaceType get colorSpaceType {
    return ColorSpaceType.yuv_420_888;
  }

  @override
  TensorBuffer getTensorBuffer(TfLiteType dataType) {
    throw UnsupportedError(
        'Converting CameraImage to TensorBuffer is not supported.');
  }

  @override
  int get height => cameraImage.height;

  @override
  Image get image => throw UnsupportedError(
      'Converting CameraImage to Image is not supported.');

  @override
  CameraImage get mediaImage => cameraImage;

  @override
  int get width => cameraImage.width;
}
