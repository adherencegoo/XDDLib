package com.example.xddlib.analysis

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.xddlib.presentation.Lg
import com.example.xddlib.presentation.XddToast
import org.junit.Assert
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
                null) { path, _ ->
            XddToast.show(context, "file scanned: $path")
        }

        return newFilename
    }

    @JvmStatic
    fun <T> readFromFile(filenameWithExt: String, javaClass: Class<T>): T? {
        val finalFilename = if (filenameWithExt.startsWith(File.separator)) filenameWithExt else rootDirSlash + filenameWithExt

        var input: InputStream? = null
        var ret: T? = null
        try {
            input = ObjectInputStream(FileInputStream(finalFilename))
            ret = javaClass.cast(input.readObject())
        } catch (e: FileNotFoundException) {
            Lg.e(e)
        } finally {
            input?.close()
        }

        return ret

    }

    /** xdd todo: refactor using [rootDirSlash]*/
    @JvmStatic
    fun saveBitmap(context: Context, bitmap: Bitmap?, fileName: String,
                   vararg objects: Any?) {
        val tag = "saveBitmap"

        //produce full path for file
        var fileFullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
        if (!fileFullPath.endsWith("/") && !fileName.startsWith("/")) {
            fileFullPath += "/"
        }
        fileFullPath += fileName
        if (!fileName.endsWith(".jpg")) {
            fileFullPath += ".jpg"
        }

        if (bitmap != null) {
            //create folders if needed
            val folderName = fileFullPath.substring(0, fileFullPath.lastIndexOf('/'))
            val folder = File(folderName)
            if (!folder.isDirectory) {//folder not exist
                Assert.assertTrue(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END
                        + "Error in creating folder:[" + folderName + "]", folder.mkdirs())
            }

            var os: OutputStream? = null
            try {
                os = FileOutputStream(fileFullPath)
            } catch (e: FileNotFoundException) {
                Lg.e("FileNotFoundException: filePath:$fileFullPath", e)
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)

            val stackTraceElement = Lg.findDisplayedStackTraceElement()
            //Note: Must scan file instead of folder
            MediaScannerConnection.scanFile(context, arrayOf(fileFullPath), null) { path, uri ->
                Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, stackTraceElement, tag, "onScanCompleted",
                        "Bitmap saved", "path:$path", "Uri:$uri", objects)
            }
        } else {
            Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, tag, "bitmap==null", fileFullPath, objects)
        }
    }
}