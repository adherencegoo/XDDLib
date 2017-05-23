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
import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Pattern;

/** Created by Owen_Chen on 2017/3/15. */

public class XDD {
    private XDD(){}
    private static final String THIS_FILE_NAME = XDD.class.getSimpleName() + ".java";//immutable

    private static final int DEFAULT_REPEAT_COUNT = 30;
    private static final boolean DEBUG_PRINT_ELEMENTS = false;

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

    public enum CtrlKey {
        TEST;

        private Object mValue;

        /**@return the enum itself */
        public CtrlKey setValue(@Nullable final Object value) {
            mValue = value;
            return this;
        }

        /** Reset the value after invoking getter */
        private Object getValue() {
            final Object returned = mValue;
            mValue = null;
            return returned;
        }
    }

    /**
     * Log.d(PRIMITIVE_LOG_TAG, MESSAGE);
     * MESSAGE = METHOD_TAG(including CODE_HYPERLINK) + MESSAGE_CONTENT
     * About 0.65ms per Lg.x (20 times slower than primitive Log.x)
     * */
    static public class Lg {
        private Lg(){}
        public static final String PRIMITIVE_LOG_TAG = XDD.class.getSimpleName() + "D";//mutable
        private static final String TAG_END = ": ";

        private static final Pattern CODE_HYPERLINK_PATTERN_KERNEL
                = Pattern.compile("[(].*[.](java:)[0-9]+[)]");//(ANYTHING.java:NUMBER)

        private static final String METHOD_TAG_DELIMITER = "->";
        private static final Pattern METHOD_TAG_PATTERN
                = Pattern.compile("^(" + CODE_HYPERLINK_PATTERN_KERNEL.pattern() + ")?" //start with (ANYTHING.java:NUMBER) or nothing
                + "(" + METHOD_TAG_DELIMITER + ").*(" + TAG_END + ").*");//->XXX: XXX

        private static final Pattern CODE_HYPERLINK_PATTERN
                = Pattern.compile("^" + CODE_HYPERLINK_PATTERN_KERNEL.pattern() + ".*");//(ANYTHING.java:NUMBER)ANYTHING

        private static final String MESSAGE_CONTENT_DELIMITER = ", ";

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

        private static class VarArgParser {
            //settings =====================================================
            private final boolean mNeedMethodTag;
            private final String mDelimiter;
            private boolean mInsertFirstDelimiter;
            private final BracketType mBracket;

            //parsed results =====================================================
            private String mMethodTag = null;
            private Throwable mTr = null;
            private final StringBuilder mStringBuilder = new StringBuilder();

            //others =====================================================
            private boolean mIsParsed = false;

            /** ->[a]->[b]->[c] */
            private static VarArgParser newMethodTagParser() {
                return new VarArgParser(false, METHOD_TAG_DELIMITER, true, BracketType.BRACKET);
            }

            /** a, b, c */
            private static VarArgParser newMessageParser() {
                return new VarArgParser(true, MESSAGE_CONTENT_DELIMITER, false, BracketType.NONE);
            }

            private VarArgParser(final boolean needMethodTag,
                         @NonNull final String delimiter,
                         final boolean insertFirstDelimiter,
                         @NonNull final BracketType bracket) {
                mNeedMethodTag = needMethodTag;
                mDelimiter = delimiter;
                mInsertFirstDelimiter = insertFirstDelimiter;
                mBracket = bracket;
            }

            private VarArgParser reset() {
                mMethodTag = null;
                mTr = null;
                mStringBuilder.setLength(0);
                mIsParsed = false;
                return this;
            }

            private VarArgParser parse(@NonNull final Object... objects) {
                reset();

                for (final Object obj : objects) {
                    if (mNeedMethodTag
                            && mMethodTag == null && obj instanceof CharSequence
                            && METHOD_TAG_PATTERN.matcher((CharSequence)obj).matches()) {
                        mMethodTag = (String) obj;
                        if (!((String) obj).endsWith(TAG_END))
                            mInsertFirstDelimiter = true;
                    } else if (mTr == null && obj instanceof Throwable) {
                        mTr = (Throwable) obj;
                    } else if (obj instanceof Object[]) {//recursively parse Object[] in Object[]
                        final Object[] objArray = (Object[]) obj;
                        if (objArray.length != 0) {
                            final VarArgParser innerParser = new VarArgParser(false, mDelimiter, mInsertFirstDelimiter, mBracket);
                            mStringBuilder.append(innerParser.parse(objArray).toString());
                            mInsertFirstDelimiter = innerParser.mInsertFirstDelimiter;
                        }
                    } else if (obj.getClass().isArray()) {//native array
                        // TODO: 2017/5/22  how to correctly parse native array in Object[]
                        Log.w(PRIMITIVE_LOG_TAG, "XDD.Lg.VarArgParser.parse(): can't parse native array yet");
                    } else if (!(obj instanceof CtrlKey)) {
                        //transform obj into string
                        //ArrayList is acceptable
                        //Can't be Object[] and native array

                        if (mInsertFirstDelimiter) {
                            mStringBuilder.append(mDelimiter);
                        }

                        mStringBuilder.append(mBracket.mLeft);

                        String objStr;
                        if (obj instanceof String) objStr = (String) obj;
                        else objStr = obj.toString();

                        int dotPos;
                        if (Lg.isToStringFromObjectClass(obj) && (dotPos = objStr.lastIndexOf('.')) != -1) {
                            objStr = objStr.substring(dotPos + 1);//OuterClass$InnerClass
                        }
                        mStringBuilder.append(objStr);

                        mStringBuilder.append(mBracket.mRight);
                        mInsertFirstDelimiter = true;
                    }
                }

                mIsParsed = true;
                return this;
            }

            @Override
            public String toString() {
                Assert.assertTrue(mIsParsed);

                if (mNeedMethodTag) {
                    //modify tag if needed
                    if (mMethodTag == null || mMethodTag.isEmpty()) mMethodTag = _getMethodTag(true);
                    else mMethodTag = insertCodeHyperlinkIfNeeded(mMethodTag);//create hyperlink at the place where Lg.x is called if needed
                }

                //tr must be at the end
                if (mTr != null) {
                    mStringBuilder.append('\n');
                    mStringBuilder.append(Log.getStackTraceString(mTr));
                }

                //tag must be at the beginning
                return mNeedMethodTag ? mMethodTag + mStringBuilder.toString() : mStringBuilder.toString();
            }
        }

        //my fundamental log
        public static int log(final Type type, @NonNull final Object... objects) {
            final String message = VarArgParser.newMessageParser().parse(objects).toString();
            switch (type){
                case V: return Log.v(PRIMITIVE_LOG_TAG, message);
                case D: return Log.d(PRIMITIVE_LOG_TAG, message);
                case I: return Log.i(PRIMITIVE_LOG_TAG, message);
                case W: return Log.w(PRIMITIVE_LOG_TAG, message);
                case E: return Log.e(PRIMITIVE_LOG_TAG, message);
                default: return -1;
            }
        }

        /** @return true if toString method of the object is not ever overridden */
        private static boolean isToStringFromObjectClass(@NonNull final Object object) {
            try {
                return //add some common cases to avoid the last condition
                        !(object instanceof CharSequence) //including String
                                && !(object instanceof Throwable) //including Exception
                                && !(object instanceof AbstractCollection) //including ArrayList, LinkedList ...
                                && object.getClass().getMethod("toString").getDeclaringClass()
                                .getCanonicalName().equals(Object.class.getCanonicalName());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return true;
        }


        public static String getMethodTag(@NonNull final Object... messages){
            //this method is invoked outside the class, and the result is reused, so don't show hyperlink
            return _getMethodTag(false, messages);
        }

        private static String _getMethodTag(final boolean showHyperlink, @NonNull final Object... messageObjects){
            String tag = null;

            if (DEBUG_PRINT_ELEMENTS) {
                printStackTraceElements(Thread.currentThread().getStackTrace());
            }

            final StringBuilder resultBuilder = new StringBuilder();
            final StackTraceElement targetElement = findFirstOuterElement();

            if (showHyperlink) {//(FileName:LineNumber)   //no other text allowed
                resultBuilder.append("(")
                        .append(targetElement.getFileName())
                        .append(":")
                        .append(targetElement.getLineNumber())
                        .append(")");
            }
            //OuterClass$InnerClass.MethodName
            resultBuilder.append(METHOD_TAG_DELIMITER)
                    .append(getMethodNameWithClassWithoutPackage(targetElement));

            if (messageObjects.length != 0){
                resultBuilder.append(VarArgParser.newMethodTagParser().parse(messageObjects));
            }

            resultBuilder.append(TAG_END);

            if (DEBUG_PRINT_ELEMENTS) {
                d(tag, "result: " + resultBuilder.toString());
                d(tag, getSeparator("end", '^'));
            }
            return resultBuilder.toString();
        }

        private static String getMethodNameWithClassWithoutPackage(@NonNull final StackTraceElement element) {
            final String classFullName = element.getClassName();//PACKAGE_NAME.OuterClass$InnerClass
            return classFullName.substring(classFullName.lastIndexOf('.') +1) + "." + element.getMethodName();
        }

        public static void printStackTrace(@NonNull final Object... objects){
            final String result = PRIMITIVE_LOG_TAG + TAG_END
                    + "(" + (new Throwable().getStackTrace()[0].getMethodName()) + ") "
                    + VarArgParser.newMessageParser().parse("immediate invoker: "
                    + getMethodNameWithClassWithoutPackage(findOuterElementWithDepth(1)), objects).toString();
            (new Exception(result)).printStackTrace();
        }

        public static void showToast(@NonNull final Context context, @NonNull final Object... objects) {
            final String message = VarArgParser.newMessageParser().parse(objects).toString();
            Toast.makeText(context, PRIMITIVE_LOG_TAG + TAG_END + message, Toast.LENGTH_LONG).show();
            d("(" + (new Throwable().getStackTrace()[0].getMethodName()) + ") " + message);
        }

        private static StackTraceElement findFirstOuterElement() {
            return findOuterElementWithDepth(0);
        }

        private static StackTraceElement findOuterElementWithDepth(final int offset) {
            final StackTraceElement[] elements = Thread.currentThread().getStackTrace();

            boolean previousIsInnerElement = false;//inner: XDD.xxx
            for (int idx=0 ; idx<elements.length ; idx++){//the former is called earlier
                boolean currentIsInnerElement = elements[idx].getFileName().equals(THIS_FILE_NAME);
                if (previousIsInnerElement && !currentIsInnerElement) {
                    return elements[idx + offset];
                }
                previousIsInnerElement = currentIsInnerElement;
            }
            printStackTraceElements(elements);
            Assert.fail(PRIMITIVE_LOG_TAG + TAG_END + (new Throwable().getStackTrace()[0].getMethodName()) + " fails !!! firstOuterElement not found");
            return elements[0];//unreachable
        }

        private static String insertCodeHyperlinkIfNeeded(@NonNull final String origTag) {
            if (CODE_HYPERLINK_PATTERN.matcher(origTag).matches()) {
                return origTag;
            } else {
                final StackTraceElement targetElement = findFirstOuterElement();
                return "(" + targetElement.getFileName() + ":" + targetElement.getLineNumber() + ")" + origTag;
            }
        }

        private static void printStackTraceElements(@NonNull final StackTraceElement[] elements) {
            final String tag = PRIMITIVE_LOG_TAG + (new Throwable().getStackTrace()[0].getMethodName()) + TAG_END;
            d(tag, getSeparator("start", 'v'));
            for (int idx=0 ; idx<elements.length ; idx++) {
                StackTraceElement element = elements[idx];
                d(tag, String.format(Locale.getDefault(), "element[%d]: %s.%s (%s line:%d)",
                        idx, element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber()));
            }
        }
    }

    public static class Tm {
        private Tm() {}

        private static long sStartTime = 0;
        private static long sEndTime = 0;
        private static String sStartMessage = null;
        private static String sEndMessage = null;

        public static void start(@NonNull final Object... objects) {
            sStartMessage = Lg.getMethodTag(objects);
            Lg.d(sStartMessage, "start timer~");

            sStartTime = System.currentTimeMillis();
        }

        public static void end(@NonNull final Object... objects) {
            sEndTime = System.currentTimeMillis();

            sEndMessage = Lg.getMethodTag(objects);
            Lg.d(sEndMessage, "end timer~");

            elapsed();
        }

        public static void elapsed(@NonNull final Object... objects) {
            Lg.d("Elapsed time:" + (sEndTime - sStartTime) + "ms",
                    "from {" + sStartMessage + "} to {" + sEndMessage + "}",
                    objects);

            sStartTime = 0;
            sEndTime = 0;
            sStartMessage = null;
            sEndMessage = null;
        }

        public static void sleep(final long ms, @NonNull final Object... objects) {
            final long timestamp = System.currentTimeMillis();

            final String timeStampString = "timestamp:"+ timestamp;
            final String commonMessage = Lg.VarArgParser.newMessageParser().parse(timeStampString, objects).toString();

            Lg.d(commonMessage, "go to sleep " + ms + "ms~");
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Lg.e(e);
            }
            Lg.d(commonMessage, "wake up");
        }
    }


    // TODO: 2017/5/19 stale: not apply getMethodTag and Lg
    public static Bitmap drawCross(@Nullable final String outerTag, @Nullable Bitmap bitmap, final int color, @Nullable final String msg){
        final String tag = outerTag + (new Object(){}.getClass().getEnclosingMethod().getName()) + Lg.TAG_END;
        if (bitmap == null) {
            Log.e(Lg.PRIMITIVE_LOG_TAG, tag + "null bitmap");
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

    // TODO: 2017/5/19 stale: not apply getMethodTag and Lg
    public static void saveBitmap(@Nullable final String outerTag, @Nullable final Bitmap bitmap, @NonNull final String fileName){
        final String tag = outerTag + (new Object(){}.getClass().getEnclosingMethod().getName()) + Lg.TAG_END;
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
                Log.e(Lg.PRIMITIVE_LOG_TAG, tag + "FileNotFoundException: filePath:" + fileFullPath);
                e.printStackTrace();
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.i(Lg.PRIMITIVE_LOG_TAG, tag + "bitmap saved:" + fileFullPath);
        } else {
            Log.e(Lg.PRIMITIVE_LOG_TAG, tag + "bitmap==null");
        }
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
        Lg.d("updated row count:" + rowCount);
    }

    public static boolean isMainThread(){
        if (Build.VERSION.SDK_INT >= 23) {
            return Looper.getMainLooper().isCurrentThread();
        } else {
            return Looper.getMainLooper() == Looper.myLooper();
        }
    }
}
