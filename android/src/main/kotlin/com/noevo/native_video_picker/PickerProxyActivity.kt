package com.noevo.native_video_picker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity

class PickerProxyActivity : ComponentActivity() {

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val resultIntent = Intent()
        resultIntent.data = uri
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly)
            .build()
        picker.launch(request)
    }

    companion object {
        fun newIntent(parent: Activity) =
            Intent(parent, PickerProxyActivity::class.java)
    }
}
