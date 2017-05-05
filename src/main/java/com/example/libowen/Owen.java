package com.example.libowen;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import junit.framework.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Owen_Chen on 2017/3/15.
 */

public class Owen {
    public static final String TAG = Owen.class.getSimpleName();
    private static final String TAG_DELIMITER = "->";
    private static final String TAG_END = ": ";
    private static final int DEFAULT_REPEAT_COUNT = 30;
    private Owen(){}

    static public class Lg {
        private Lg(){}

        @SuppressWarnings("all")
        public enum Type {
            V(Log.VERBOSE), D(Log.DEBUG), I(Log.INFO), W(Log.WARN),  E(Log.ERROR);

            public final int mValue;
            private Type(final int value) {
                mValue = value;
            }
        }

        public static int v(final String msg) { return log(Type.V, null, msg, null); }
        public static int d(final String msg) { return log(Type.D, null, msg, null); }
        public static int i(final String msg) { return log(Type.I, null, msg, null); }
        public static int w(final String msg) { return log(Type.W, null, msg, null); }
        public static int e(final String msg) { return log(Type.E, null, msg, null); }

        public static int v(final String tag, final String msg) { return log(Type.V, tag, msg, null); }
        public static int d(final String tag, final String msg) { return log(Type.D, tag, msg, null); }
        public static int i(final String tag, final String msg) { return log(Type.I, tag, msg, null); }
        public static int w(final String tag, final String msg) { return log(Type.W, tag, msg, null); }
        public static int e(final String tag, final String msg) { return log(Type.E, tag, msg, null); }

        public static int v(@Nullable final Throwable tr) { return log(Type.V, null, "", tr); }
        public static int d(@Nullable final Throwable tr) { return log(Type.D, null, "", tr); }
        public static int i(@Nullable final Throwable tr) { return log(Type.I, null, "", tr); }
        public static int w(@Nullable final Throwable tr) { return log(Type.W, null, "", tr); }
        public static int e(@Nullable final Throwable tr) { return log(Type.E, null, "", tr); }

        // TODO: 2017/3/29 public static int v(final String tag/msg???, @Nullable final Throwable tr)

        public static int v(final String tag, final String msg, @Nullable final Throwable tr) { return log(Type.V, tag, msg, tr); }
        public static int d(final String tag, final String msg, @Nullable final Throwable tr) { return log(Type.D, tag, msg, tr); }
        public static int i(final String tag, final String msg, @Nullable final Throwable tr) { return log(Type.I, tag, msg, tr); }
        public static int w(final String tag, final String msg, @Nullable final Throwable tr) { return log(Type.W, tag, msg, tr); }
        public static int e(final String tag, final String msg, @Nullable final Throwable tr) { return log(Type.E, tag, msg, tr); }

        //my fundamental log
        public static int log(final Type type, @Nullable String tag, @Nullable String msg, @Nullable final Throwable tr) {
            final String throwableString = (tr == null ? "" : "\n") + Log.getStackTraceString(tr);
            
            if (tag == null || tag.isEmpty()) tag = _getMethodTag(true);
            else tag = getTagWithCodeHyperlink(tag);//create hyperlink at the place where Lg.x is called if needed
            if (msg == null) msg = "";
            switch (type){
                case V: return Log.v(TAG, tag + msg + throwableString);
                case D: return Log.d(TAG, tag + msg + throwableString);
                case I: return Log.i(TAG, tag + msg + throwableString);
                case W: return Log.w(TAG, tag + msg + throwableString);
                case E: return Log.e(TAG, tag + msg + throwableString);
                default: return -1;
            }
        }
    }

    //stale: not apply getMethodTag and Lg
    public static Bitmap drawCross(@Nullable final String outerTag, @Nullable Bitmap bitmap, final int color, @Nullable final String msg){
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
        if (msg != null && !msg.isEmpty()) {
            paint.setTextSize(22);
            canvas.drawText(msg, imageWidth / 2, imageHeight / 4, paint);
        }
        return bitmap;
    }

    //stale: not apply getMethodTag and Lg
    public static void saveBitmap(@Nullable final String outerTag, @Nullable final Bitmap bitmap, @NonNull final String fileName){
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

    public static String getMethodTag(final Object... messages){
        //this method is invoked outside the class, and the result is reused, so don't show hyperlink
        return _getMethodTag(false, messages);
    }

    private static StackTraceElement findFirstOuterElement(final StackTraceElement[] elements) {
        Assert.assertTrue(elements != null && elements.length > 0);

        boolean previousIsInnerElement = false;//inner: Owen.xxx
        for (StackTraceElement element : elements) {//the former is called earlier
            boolean currentIsInnerElement = element.getFileName().equals(TAG + ".java");
            if (previousIsInnerElement && !currentIsInnerElement) {
                return element;
            }
            previousIsInnerElement = currentIsInnerElement;
        }
        Assert.fail(TAG + TAG_END + (new Throwable().getStackTrace()[0].getMethodName()) + " fails !!!");
        return null;
    }

    private static String getTagWithCodeHyperlink(@NonNull final String origTag) {
        if (origTag.startsWith("(")) {
            return origTag;
        } else {
            final StackTraceElement targetElement = findFirstOuterElement(Thread.currentThread().getStackTrace());
            Assert.assertNotNull(targetElement);
            return "(" + targetElement.getFileName() + ":" + targetElement.getLineNumber() + ")" + origTag;
        }
    }

    @SuppressWarnings("all")
    private static final boolean REMOVE_PACKAGE_NAME = true;
    private static final boolean PRINT_ELEMENTS = false;
    private static final boolean SHOW_ELAPSED_TIME = false;
    private static String _getMethodTag(final boolean showHyperlink, final Object... messageObjects){
        long t1;
        if (SHOW_ELAPSED_TIME) t1 = System.currentTimeMillis();
        final String tag = TAG + (new Throwable().getStackTrace()[0].getMethodName()) + TAG_END;

        final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        if (PRINT_ELEMENTS) {
            Lg.d(tag, getSeparator("start", 'v'));
            for (int idx=0 ; idx<stackTraceElements.length ; idx++) {
                StackTraceElement element = stackTraceElements[idx];
                Lg.d(tag, String.format(Locale.getDefault(), "element[%d]: %s.%s (%s line:%d)",
                        idx, element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber()));
            }
        }

        final StringBuilder resultBuilder = new StringBuilder();
        final StackTraceElement targetElement = findFirstOuterElement(stackTraceElements);
        Assert.assertNotNull(targetElement);

        if (showHyperlink) {//(FileName:LineNumber)   //no other text allowed
            resultBuilder.append("(")
                    .append(targetElement.getFileName())
                    .append(":")
                    .append(targetElement.getLineNumber())
                    .append(")");
        }
        //class name
        final String classFullName = targetElement.getClassName();//PACKAGE_NAME.OuterClass$InnerClass
        resultBuilder.append(TAG_DELIMITER)
                .append(classFullName.substring(classFullName.lastIndexOf('.') +1))//OuterClass$InnerClass
                .append(".");
        //method name
        resultBuilder.append(targetElement.getMethodName());

        // ->[msg]->[msg]->[msg]->[msg]
        if (messageObjects != null && messageObjects.length != 0){
            for (final Object msgObj : messageObjects) {

                String targetMessage;
                if (msgObj instanceof String) targetMessage = (String)msgObj;
                else targetMessage = msgObj.toString();

                int dotPos;
                if (REMOVE_PACKAGE_NAME && targetMessage.indexOf('@') != -1 && (dotPos = targetMessage.lastIndexOf('.')) != -1) {
                    targetMessage = targetMessage.substring(dotPos + 1);//OuterClass$InnerClass
                }

                resultBuilder.append(TAG_DELIMITER)
                        .append('[')
                        .append(targetMessage)
                        .append(']');
            }
        }

        resultBuilder.append(TAG_END);

        if (PRINT_ELEMENTS) {
            Lg.d(tag, "result: " + resultBuilder.toString());
            Lg.d(tag, getSeparator("end", '^'));
        }
        if (SHOW_ELAPSED_TIME) {
            long t2 = System.currentTimeMillis();
            Lg.i(tag, "elapsed time:" + (t2 - t1) + "ms");//about 0~1ms if PRINT_ELEMENTS is false
        }
        return resultBuilder.toString();
    }


    public static String getSeparator(@Nullable final String message, final char separator) {
        return getSeparator(message, separator, DEFAULT_REPEAT_COUNT);
    }
    public static String getSeparator(@Nullable final String message, final char separator, final int count) {
        final StringBuilder stringBuilder = new StringBuilder();
        final String halfSeparator = stringRepeat(count, String.valueOf(separator));
        stringBuilder.append(halfSeparator);
        if (message != null && !message.isEmpty()) {
            stringBuilder.append(' ');
            stringBuilder.append(message);
            stringBuilder.append(' ');
        }
        stringBuilder.append(halfSeparator);
        return stringBuilder.toString();
    }

    public static void printStackTrace(){ printStackTrace(_getMethodTag(true), "~~~~~~~~~"); }
    public static void printStackTrace(@NonNull final String message){ printStackTrace(_getMethodTag(true), message); }
    public static void printStackTrace(@NonNull final String tag, @NonNull final String message){
        final String result = TAG + TAG_END + getTagWithCodeHyperlink(tag) + TAG_DELIMITER + message;
        (new Exception(result)).printStackTrace();
    }

    public static String stringRepeat(@NonNull final String str) { return stringRepeat(DEFAULT_REPEAT_COUNT, str); }
    public static String stringRepeat(final int count, @NonNull final String str) {
        return TextUtils.join("", Collections.nCopies(count, str));
    }

    public static void dbUpdate(@NonNull final Context context,
                                @NonNull final Uri uri,
                                @NonNull final ContentValues values,
                                final long id){
        String whereClause = id > 0 ? MediaStore.Video.Media._ID + " = ?" : null;
        String[] whereContent = id > 0 ? new String[]{Long.toString(id)} : null;
        int rowCount = context.getContentResolver().update(uri, values, whereClause, whereContent);
        Owen.Lg.d("updated row count:" + rowCount);
    }

    public static void showToast(@NonNull final Context context, @NonNull final String message) {
        showToast(context, _getMethodTag(true), message);
    }
    public static void showToast(@NonNull final Context context, @NonNull String tag, @NonNull final String message) {
        tag = getTagWithCodeHyperlink(tag);
        Toast.makeText(context, TAG + TAG_END + tag + message, Toast.LENGTH_LONG).show();
        Lg.d("(" + (new Throwable().getStackTrace()[0].getMethodName()) + ") " + tag, message);
    }
}
