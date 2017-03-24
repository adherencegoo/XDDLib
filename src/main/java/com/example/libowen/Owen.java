package com.example.libowen;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import junit.framework.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;

/**
 * Created by Owen_Chen on 2017/3/15.
 */

public class Owen {
    private static final String TAG = Owen.class.getSimpleName();
    private static final String TAG_DELIMITER = "->";
    private static final String TAG_END = ": ";
    private Owen(){}

    @SuppressWarnings("all")
    public enum LogType { V, D, I, W, E }

    public static int v(final String msg) { return v(getMethodTag(2), msg); }
    public static int d(final String msg) { return d(getMethodTag(2), msg); }
    public static int i(final String msg) { return i(getMethodTag(2), msg); }
    public static int w(final String msg) { return w(getMethodTag(2), msg); }
    public static int e(final String msg) { return e(getMethodTag(2), msg); }

    public static int v(final String tag, final String msg) { return Log.v(TAG, tag + msg); }
    public static int d(final String tag, final String msg) { return Log.d(TAG, tag + msg); }
    public static int i(final String tag, final String msg) { return Log.i(TAG, tag + msg); }
    public static int w(final String tag, final String msg) { return Log.w(TAG, tag + msg); }
    public static int e(final String tag, final String msg) { return Log.e(TAG, tag + msg); }
    public static int log(final LogType type, final String tag, final String msg) {
        switch (type){
            case V: return v(tag, msg);
            case D: return d(tag, msg);
            case I: return i(tag, msg);
            case W: return w(tag, msg);
            case E: return e(tag, msg);
            default: return -1;
        }
    }

    public static Bitmap drawCross(final String outerTag, Bitmap bitmap, final int color, final String msg){
        final String tag = outerTag + (new Object(){}.getClass().getEnclosingMethod().getName()) + TAG_END;
        if (bitmap == null) {
            Log.e(TAG, tag + "null bitmap");
            return null;
        }

        int imageWidth = bitmap.getWidth();
        int imageHeight = bitmap.getHeight();

        //draw a cross
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStrokeWidth(3);
        paint.setColor(color);
        canvas.drawLine(0,0,imageWidth, imageHeight, paint);
        canvas.drawLine(0,imageHeight,imageWidth, 0, paint);
        //draw text
        if (msg != null && !msg.equals("")) {
            paint.setTextSize(22);
            canvas.drawText(msg, imageWidth / 2, imageHeight / 4, paint);
        }
        return bitmap;
    }

    public static void saveBitmap(final String outerTag, final Bitmap bitmap, final String fileName){
        final String tag = outerTag + (new Object(){}.getClass().getEnclosingMethod().getName()) + TAG_END;
        if (bitmap != null) {
            //produce full path for file
            String fileFullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            if (!fileName.startsWith("/")){
                fileFullPath += "/";
            }
            fileFullPath += fileName;
            if (!fileName.endsWith(".jpg")){
                fileFullPath += ".jpg";
            }

            //create folders if needed
            final String folderName = fileFullPath.substring(0, fileFullPath.lastIndexOf('/'));
            File folder = new File(folderName);
            if (!folder.isDirectory()) {//folder not exist
                Assert.assertTrue(tag + "Error in creating folder:[" + folderName + "]", folder.mkdirs());
            }

            OutputStream os = null;
            try {
                os = new FileOutputStream(fileFullPath);
            } catch (FileNotFoundException e) {
                Log.e(TAG, tag + "FileNotFoundException: filePath:" + fileFullPath);
                e.printStackTrace();
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.i(TAG, tag + "bitmap saved:" + fileFullPath);
        } else {
            Log.e(TAG, tag + "bitmap==null");
        }
    }

    public static String getMethodTag(final String... messages){
        return getMethodTag(2, messages);
    }

    @SuppressWarnings("all")
    private static boolean sUseSimpleInstanceName = true;
    private static String getMethodTag(final int depth, final String... messages){
        final String tag = "owen: " + (new Object(){}.getClass().getEnclosingMethod().getName()) + TAG_END;
        Assert.assertTrue(depth >= 1);

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Assert.assertTrue(stackTraceElements != null);
        Assert.assertTrue(stackTraceElements.length != 0);

        StringBuilder resultBuilder = new StringBuilder();
        for (int idx=0 ; idx<stackTraceElements.length ; idx++) {//the former is called earlier
            StackTraceElement anElement = stackTraceElements[idx];

            if (anElement.getFileName().equals(TAG + ".java")){
                Assert.assertTrue(idx + depth < stackTraceElements.length);
                StackTraceElement targetElement = stackTraceElements[idx + depth];

                String classFullName = targetElement.getClassName();//PACKAGE_NAME.OuterClass$InnerClass
                resultBuilder.append(TAG_DELIMITER);
                resultBuilder.append(classFullName.substring(classFullName.lastIndexOf('.') +1));//OuterClass$InnerClass
                resultBuilder.append(".");

                resultBuilder.append(targetElement.getMethodName());
                break;
            }
        }

        // ->[msg]->[msg]->[msg]->[msg]
        if (messages != null && messages.length != 0){
            for (final String msg : messages) {
                resultBuilder.append(TAG_DELIMITER);
                resultBuilder.append('[');
                int dotPos;
                if (sUseSimpleInstanceName && msg.indexOf('@') != -1 && (dotPos = msg.lastIndexOf('.')) != -1) {
                    resultBuilder.append(msg.substring(dotPos + 1));//OuterClass$InnerClass
                } else {
                    resultBuilder.append(msg);//PACKAGE_NAME.OuterClass$InnerClass
                }
                resultBuilder.append(']');
            }
        }

        resultBuilder.append(TAG_END);
        return resultBuilder.toString();
    }

    public static void printStackTrace(final String message){ printStackTrace(getMethodTag(2), message); }
    public static void printStackTrace(final String tag, final String message){
        final String result = TAG + TAG_END + tag + TAG_DELIMITER + message;
        (new Exception(result)).printStackTrace();
    }

    public static final int DEFAULT_REPEAT_COUNT = 50;
    public static String stringRepeat(final String str){
        return stringRepeat(DEFAULT_REPEAT_COUNT, str);
    }

    public static String stringRepeat(final int count, final String str){
        return TextUtils.join("", Collections.nCopies(count, str));
    }
}
