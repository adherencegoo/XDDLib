package com.example.xddlib.analysis

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.xddlib.presentation.XddToast
import java.io.*
import java.util.*

@Suppress("unused")
object XddFile {
    private val rootDirSlash by lazy {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .apply { mkdir() }.absolutePath + File.separator
    }

    /**@return full filename*/
    @JvmStatic
    fun saveToFile(context: Context,
                   pureFilename: String,
                   addTimeString: Boolean = false,
                   vararg data: Any): String {
        val timeString = if (addTimeString) "-${Calendar.getInstance().time}" else ""
        val newFilename = "$rootDirSlash$pureFilename$timeString.txt"

        ObjectOutputStream(FileOutputStream(newFilename)).apply {
            data.forEach { writeObject(it) }
            close()
        }

        MediaScannerConnection.scanFile(context,
                arrayOf(newFilename),
                null,
                {path, _ -> XddToast.show(context, "file scanned: $path")})

        return newFilename
    }

    @JvmStatic
    fun <T> readFromFile(filename: String, javaClass: Class<T>): T {
        val finalFilename = if (filename.startsWith(File.separator)) filename else rootDirSlash + filename

        val input = ObjectInputStream(FileInputStream(finalFilename))
        val ret = javaClass.cast(input.readObject())
        input.close()

        return ret

    }
}