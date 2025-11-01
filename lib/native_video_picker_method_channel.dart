import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'native_video_picker_platform_interface.dart';

/// An implementation of [NativeVideoPickerPlatform] that uses method channels.
class MethodChannelNativeVideoPicker extends NativeVideoPickerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('native_video_picker');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
