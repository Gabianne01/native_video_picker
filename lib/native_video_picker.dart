
import 'package:flutter/services.dart';

class NativeVideoPicker {
  static const _channel = MethodChannel('native_video_picker');

  static Future<String?> pickVideo() async {
    return await _channel.invokeMethod<String>('pickVideo');
  }
}

