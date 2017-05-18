package com.example.xddlib;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
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
import java.util.regex.Pattern;

/** Created by Owen_Chen on 2017/3/15. */

public class XDD {
    private XDD(){}
    private static final String THIS_FILE_NAME = XDD.class.getSimpleName() + ".java";//immutable
    public static final String PRIMITIVE_LOG_TAG = XDD.class.getSimpleName() + "D";//mutable

    private static final int DEFAULT_REPEAT_COUNT = 30;
    private static final boolean DEBUG_PRINT_ELEMENTS = false;
    private static final Pattern CODE_HYPERLINK_PATTERN_KERNEL
            = Pattern.compile("[(].*[.](java:)[0-9]+[)]");//(ANYTHING.java:NUMBER)

    private static final String TAG_END = ": ";
    private static final String METHOD_TAG_DELIMITER = "->";
    private static final Pattern METHOD_TAG_PATTERN
            = Pattern.compile("^(" + CODE_HYPERLINK_PATTERN_KERNEL.pattern() + ")?" //start with (ANYTHING.java:NUMBER) or nothing
            + "(" + METHOD_TAG_DELIMITER + ").*(" + TAG_END + ").*");//->XXX: XXX

    private static final Pattern CODE_HYPERLINK_PATTERN
            = Pattern.compile("^" + CODE_HYPERLINK_PATTERN_KERNEL.pattern() + ".*");//(ANYTHING.java:NUMBER)ANYTHING

    private static final String MESSAGE_CONTENT_DELIMITER = ", ";

    private enum BracketType {
        NONE("", ""),
        ROUND("(", ")"),
        BRACKET("[", "]"),
        CURLY("{", "}"),
        ANGLE("<", ">");

        public final String mLeft;
        public final String mRight;
        BracketType(final String left, final String right) {
            mLeft = left;
            mRight = right;
        }
    }

    private enum CtrlKey {
        TEST;

        private Object mValue;

        /**@return the enum itself */
        CtrlKey setValue(@Nullable final Object value) {
            mValue = value;
            return this;
        }

        /** Reset the value after invoking getter */
        Object getValue() {
            final Object returned = mValue;
            mValue = null;
            return returned;
        }
    }

    /**
     * Log.d(PRIMITIVE_LOG_TAG, MESSAGE);
     * MESSAGE = METHOD_TAG(including CODE_HYPERLINK) + MESSAGE_CONTENT
     * */
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

        public static int v(@NonNull final Object... objects) { return log(Type.V, objects); }
        public static int d(@NonNull final Object... objects) { return log(Type.D, objects); }
        public static int i(@NonNull final Object... objects) { return log(Type.I, objects); }
        public static int w(@NonNull final Object... objects) { return log(Type.W, objects); }
        public static int e(@NonNull final Object... objects) { return log(Type.E, objects); }

        //my fundamental log
        public static int log(final Type type, @NonNull final Object... objects) {
            final String message = parseObjects(objects);
            switch (type){
                case V: return Log.v(PRIMITIVE_LOG_TAG, message);
                case D: return Log.d(PRIMITIVE_LOG_TAG, message);
                case I: return Log.i(PRIMITIVE_LOG_TAG, message);
                case W: return Log.w(PRIMITIVE_LOG_TAG, message);
                case E: return Log.e(PRIMITIVE_LOG_TAG, message);
                default: return -1;
            }
        }
    }

    //stale: not apply getMethodTag and Lg
    public static Bitmap drawCross(@Nullable final String outerTag, @Nullable Bitmap bitmap, final int color, @Nullable final String msg){
        final String tag = outerTag + (new Object(){}.getClass().getEnclosingMethod().getName()) + TAG_END;
        if (bitmap == null) {
            Log.e(PRIMITIVE_LOG_TAG, tag + "null bitmap");
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
                Log.e(PRIMITIVE_LOG_TAG, tag + "FileNotFoundException: filePath:" + fileFullPath);
                e.printStackTrace();
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.i(PRIMITIVE_LOG_TAG, tag + "bitmap saved:" + fileFullPath);
        } else {
            Log.e(PRIMITIVE_LOG_TAG, tag + "bitmap==null");
        }
    }

    public static String getMethodTag(@NonNull final Object... messages){
        //this method is invoked outside the class, and the result is reused, so don't show hyperlink
        return _getMethodTag(false, messages);
    }

    private static String parseObjects(@NonNull final Object... objects) {
        final StringBuilder messageBuilder = new StringBuilder();
        String tag = null;
        Throwable tr = null;
        boolean needStringDelimiter = false;
        if (objects.length != 0) {
            for (final Object obj : objects) {
                if (needStringDelimiter) {
                    messageBuilder.append(MESSAGE_CONTENT_DELIMITER);
                    needStringDelimiter = false;
                }

                if (tag == null && obj instanceof CharSequence
                        && METHOD_TAG_PATTERN.matcher((CharSequence)obj).matches()) {
                    tag = (String) obj;
                } else if (tr == null && obj instanceof Throwable) {
                    tr = (Throwable) obj;
                } else {
                    messageBuilder.append(obj);
                    needStringDelimiter = true;
                }
            }
        }

        //modify tag if needed
        if (tag == null || tag.isEmpty()) tag = _getMethodTag(true);
        else tag = insertCodeHyperlinkIfNeeded(tag);//create hyperlink at the place where Lg.x is called if needed

        //tr must be at the end
        if (tr != null) {
            messageBuilder.append('\n');
            messageBuilder.append(Log.getStackTraceString(tr));
        }

        //tag must be at the beginning
        return tag + messageBuilder.toString();
    }


    private static StackTraceElement findFirstOuterElement() {
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        boolean previousIsInnerElement = false;//inner: XDD.xxx
        for (StackTraceElement element : elements) {//the former is called earlier
            boolean currentIsInnerElement = element.getFileName().equals(THIS_FILE_NAME);
            if (previousIsInnerElement && !currentIsInnerElement) {
                return element;
            }
            previousIsInnerElement = currentIsInnerElement;
        }
        printStackTraceElements(elements);
        Assert.fail(PRIMITIVE_LOG_TAG + TAG_END + (new Throwable().getStackTrace()[0].getMethodName()) + " fails !!! firstOuterElement not found");
        return null;
    }

    private static String insertCodeHyperlinkIfNeeded(@NonNull final String origTag) {
        if (CODE_HYPERLINK_PATTERN.matcher(origTag).matches()) {
            return origTag;
        } else {
            final StackTraceElement targetElement = findFirstOuterElement();
            Assert.assertNotNull(targetElement);
            return "(" + targetElement.getFileName() + ":" + targetElement.getLineNumber() + ")" + origTag;
        }
    }

    private static String _getMethodTag(final boolean showHyperlink, @NonNull final Object... messageObjects){
        String tag = null;

        if (DEBUG_PRINT_ELEMENTS) {
            printStackTraceElements(Thread.currentThread().getStackTrace());
        }

        final StringBuilder resultBuilder = new StringBuilder();
        final StackTraceElement targetElement = findFirstOuterElement();
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
        resultBuilder.append(METHOD_TAG_DELIMITER)
                .append(classFullName.substring(classFullName.lastIndexOf('.') +1))//OuterClass$InnerClass
                .append(".");
        //method name
        resultBuilder.append(targetElement.getMethodName());

        // ->[msg]->[msg]->[msg]->[msg]
        if (messageObjects.length != 0){
            for (final Object msgObj : messageObjects) {

                String targetMessage;
                if (msgObj instanceof String) targetMessage = (String)msgObj;
                else targetMessage = msgObj.toString();

                int dotPos;
                if (targetMessage.indexOf('@') != -1 && (dotPos = targetMessage.lastIndexOf('.')) != -1) {
                    targetMessage = targetMessage.substring(dotPos + 1);//OuterClass$InnerClass
                }

                resultBuilder.append(METHOD_TAG_DELIMITER)
                        .append('[')
                        .append(targetMessage)
                        .append(']');
            }
        }

        resultBuilder.append(TAG_END);

        if (DEBUG_PRINT_ELEMENTS) {
            Lg.d(tag, "result: " + resultBuilder.toString());
            Lg.d(tag, getSeparator("end", '^'));
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

    public static void printStackTrace(@NonNull final Object... objects){
        final String result = PRIMITIVE_LOG_TAG + TAG_END
                + "(" + (new Throwable().getStackTrace()[0].getMethodName()) + ") "
                + parseObjects(objects);
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
        XDD.Lg.d("updated row count:" + rowCount);
    }

    public static void showToast(@NonNull final Context context, @NonNull final Object... objects) {
        final String message = parseObjects(objects);
        Toast.makeText(context, PRIMITIVE_LOG_TAG + TAG_END + message, Toast.LENGTH_LONG).show();
        Lg.d("(" + (new Throwable().getStackTrace()[0].getMethodName()) + ") " + message);
    }

    public static boolean isMainThread(){
        if (Build.VERSION.SDK_INT >= 23) {
            return Looper.getMainLooper().isCurrentThread();
        } else {
            return Looper.getMainLooper() == Looper.myLooper();
        }
    }

    private static void printStackTraceElements(@NonNull final StackTraceElement[] elements) {
        final String tag = PRIMITIVE_LOG_TAG + (new Throwable().getStackTrace()[0].getMethodName()) + TAG_END;
        Lg.d(tag, getSeparator("start", 'v'));
        for (int idx=0 ; idx<elements.length ; idx++) {
            StackTraceElement element = elements[idx];
            Lg.d(tag, String.format(Locale.getDefault(), "element[%d]: %s.%s (%s line:%d)",
                    idx, element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber()));
        }
    }

    public static void sleep(final long ms, @NonNull final Object... objects) {
        final long timestamp = System.currentTimeMillis();

        final String timeStampString = "timestamp:"+ timestamp;
        final String commonMessage = objects.length == 0 ?
                timeStampString : parseObjects(timeStampString, objects);

        Lg.d(commonMessage, "go to sleep " + ms + "ms~");
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Lg.e(e);
        }
        Lg.d(commonMessage, "wake up");
    }
}
