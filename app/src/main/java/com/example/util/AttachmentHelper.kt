package com.example.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

object AttachmentHelper {

    /**
     * Copies a source Uri (from photo picker, scanner, or file explorer) into 
     * the app secure local file system so it remains persistently accessible offline.
     */
    fun saveUriToAppStorage(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            
            // Extract the extension or default to bin
            val mimeType = contentResolver.getType(uri)
            val extension = if (mimeType != null) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            } else {
                "jpg"
            }
            
            // Create a dedicated directory
            val directory = File(context.filesDir, "attachments")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val filename = "attach_${System.currentTimeMillis()}.$extension"
            val targetFile = File(directory, filename)
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
