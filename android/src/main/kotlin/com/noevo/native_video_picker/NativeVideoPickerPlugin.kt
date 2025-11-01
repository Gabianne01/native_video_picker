package com.noevo.native_video_picker

import java.io.File
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class NativeVideoPickerPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var resultCallback: MethodChannel.Result? = null
    private val REQUEST_CODE = 9911

    // ───────────────────────────────────────────────────────────────
    // Flutter engine connection
    // ───────────────────────────────────────────────────────────────
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "native_video_picker")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    // ───────────────────────────────────────────────────────────────
    // Method calls from Dart
    // ───────────────────────────────────────────────────────────────
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {

            // Launch native video picker
            "pickVideo" -> {
                val act = activity ?: run {
                    result.error("no_activity", "No foreground activity", null)
                    return
                }
                resultCallback = result

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Android 13+ → use new Photo Picker
                        val proxyIntent = PickerProxyActivity.newIntent(act)
                        act.startActivityForResult(proxyIntent, REQUEST_CODE)
                    } else {
                        // Older Android → legacy gallery intent
                        val legacy = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        legacy.type = "video/*"
                        act.startActivityForResult(legacy, REQUEST_CODE)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    resultCallback?.error("intent_fail", "Could not launch picker: ${e.message}", null)
                    resultCallback = null
                }
            }

            // Manual copy (used in fallback or testing)
            "copyVideoToPath" -> {
                val uriStr = call.argument<String>("uri")
                val destDir = call.argument<String>("destDir")
                val destName = call.argument<String>("fileName") ?: "copied_${System.currentTimeMillis()}.mp4"

                if (uriStr == null || destDir == null) {
                    result.error("bad_args", "Missing uri or destDir", null)
                    return
                }

                try {
                    val uri = Uri.parse(uriStr)
                    val resolver = activity?.contentResolver ?: run {
                        result.error("no_resolver", "No content resolver", null)
                        return
                    }

                    // ensure read permission
                    activity?.grantUriPermission(
                        activity?.packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val destFolder = File(destDir)
                    if (!destFolder.exists()) destFolder.mkdirs()
                    val destFile = File(destFolder, destName)

                    resolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }

                    result.success(destFile.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    result.error("copy_fail", e.toString(), null)
                }
            }

            else -> result.notImplemented()
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Handle activity result (picker outcome)
    // ───────────────────────────────────────────────────────────────
    private fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE) return

        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            resultCallback?.success(null)
            resultCallback = null
            return
        }

        val uri = data.data!!
        val duration = getVideoDuration(uri) ?: 0L
        val isTooLong = duration > 60000L

        // Early return if too long — no copying
        if (isTooLong) {
            resultCallback?.success(
                mapOf(
                    "uri" to uri.toString(),
                    "tooLong" to true,
                    "durationMs" to duration
                )
            )
            resultCallback = null
            return
        }

        // Otherwise, copy to cache and return full info
        try {
            val resolver = activity?.contentResolver ?: throw Exception("No resolver")
            val cacheDir = activity!!.cacheDir
            val destFile = File(cacheDir, "picked_${System.currentTimeMillis()}.mp4")

            activity?.grantUriPermission(
                activity?.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            resultCallback?.success(
                mapOf(
                    "uri" to uri.toString(),
                    "tooLong" to false,
                    "durationMs" to duration,
                    "localPath" to destFile.absolutePath
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            resultCallback?.error("copy_fail", e.toString(), null)
        } finally {
            resultCallback = null
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Helper: read duration from content:// URI
    // ───────────────────────────────────────────────────────────────
    private fun getVideoDuration(uri: Uri): Long? {
        val projection = arrayOf(MediaStore.Video.Media.DURATION)
        var cursor: Cursor? = null
        return try {
            cursor = activity?.contentResolver?.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                cursor.getLong(durationColumn)
            } else null
        } finally {
            cursor?.close()
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Lifecycle management
    // ───────────────────────────────────────────────────────────────
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener { requestCode, resultCode, data ->
            onActivityResult(requestCode, resultCode, data)
            true
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
