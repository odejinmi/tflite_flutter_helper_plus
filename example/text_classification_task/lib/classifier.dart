import 'package:tflite_flutter_helper_plus/tflite_flutter_helper_plus.dart';

abstract class Classifier {
  List<Category> classify(String text);
}
