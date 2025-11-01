import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'native_video_picker_method_channel.dart';

abstract class NativeVideoPickerPlatform extends PlatformInterface {
  /// Constructs a NativeVideoPickerPlatform.
  NativeVideoPickerPlatform() : super(token: _token);

  static final Object _token = Object();

  static NativeVideoPickerPlatform _instance = MethodChannelNativeVideoPicker();

  /// The default instance of [NativeVideoPickerPlatform] to use.
  ///
  /// Defaults to [MethodChannelNativeVideoPicker].
  static NativeVideoPickerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [NativeVideoPickerPlatform] when
  /// they register themselves.
  static set instance(NativeVideoPickerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
