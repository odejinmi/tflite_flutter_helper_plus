#include "include/tflite_flutter_helper_plus/tflite_flutter_helper_plus_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "tflite_flutter_helper_plus_plugin.h"

void TfliteFlutterHelperPlusPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  tflite_flutter_helper_plus::TfliteFlutterHelperPlusPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
