package com.example.faceswap.storage

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareHelper {
    /**
     * Dispatches native Android Share sheet using content Uri without server uploads.
     */
    fun shareImage(context: Context, imageUri: Uri, title: String = "Share Swapped Photo") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title)
        context.startActivity(chooser)
    }
}
