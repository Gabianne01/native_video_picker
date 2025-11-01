import Flutter
import UIKit
import AVFoundation

public class NativeVideoPickerPlugin: NSObject, FlutterPlugin, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
    
    var result: FlutterResult?
    var controller: UIImagePickerController?
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "native_video_picker", binaryMessenger: registrar.messenger())
        let instance = NativeVideoPickerPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard call.method == "pickVideo" else {
            result(FlutterMethodNotImplemented)
            return
        }
        self.result = result
        DispatchQueue.main.async {
            let picker = UIImagePickerController()
            picker.mediaTypes = ["public.movie"]
            picker.delegate = self
            UIApplication.shared.delegate?.window??.rootViewController?.present(picker, animated: true)
            self.controller = picker
        }
    }
    
    public func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        picker.dismiss(animated: true)
        
        if let url = info[.mediaURL] as? URL {
            let asset = AVURLAsset(url: url)
            let durationSeconds = CMTimeGetSeconds(asset.duration)
            if durationSeconds > 60 {
                result?(FlutterError(code: "too_long", message: "Video longer than 60s", details: nil))
            } else {
                result?(url.absoluteString)
            }
        } else {
            result?(nil)
        }
        cleanup()
    }
    
    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
        result?(nil)
        cleanup()
    }
    
    private func cleanup() {
        result = nil
        controller = nil
    }
}
