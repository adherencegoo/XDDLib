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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
     * Abbreviation of Log,
     * Log.d(PRIMITIVE_LOG_TAG, MESSAGE);
     * MESSAGE = METHOD_TAG(including CODE_HYPERLINK) + MESSAGE_CONTENT;
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
            V(Log.VERBOSE), D(Log.DEBUG), I(Log.INFO), W(Log.WARN), E(Log.ERROR), X(-1);

            public final int mValue;
            private Type(final int value) {
                mValue = value;
            }
        }

        public static int v(@NonNull final Object... objects) { return _log(Type.V, objects); }
        public static int d(@NonNull final Object... objects) { return _log(Type.D, objects); }
        public static int i(@NonNull final Object... objects) { return _log(Type.I, objects); }
        public static int w(@NonNull final Object... objects) { return _log(Type.W, objects); }
        public static int e(@NonNull final Object... objects) { return _log(Type.E, objects); }
        /** @param objects: must contain Lg.Type*/
        public static int log(@NonNull final Object... objects) { return _log(Type.X, objects); }

        private static class ObjectArrayParser {
            //settings =====================================================
            private final boolean mNeedMethodTag;
            private final String mDelimiter;
            private boolean mInsertFirstDelimiter;
            private final BracketType mBracket;

            //parsed results =====================================================
            private String mMethodTag = null;//cache the first found one
            private ArrayList<Throwable> mTrArray = null;
            private final StringBuilder mStringBuilder = new StringBuilder();
            private Type mLgType = Type.X;//cache the LAST found one

            //others =====================================================
            private boolean mIsParsed = false;

            /** ->[a]->[b]->[c] */
            private static ObjectArrayParser newMethodTagParser() {
                return new ObjectArrayParser(false, METHOD_TAG_DELIMITER, true, BracketType.BRACKET);
            }

            /** a, b, c */
            private static ObjectArrayParser newMessageParser() {
                return new ObjectArrayParser(true, MESSAGE_CONTENT_DELIMITER, false, BracketType.NONE);
            }

            private ObjectArrayParser(final boolean needMethodTag,
                                      @NonNull final String delimiter,
                                      final boolean insertFirstDelimiter,
                                      @NonNull final BracketType bracket) {
                mNeedMethodTag = needMethodTag;
                mDelimiter = delimiter;
                mInsertFirstDelimiter = insertFirstDelimiter;
                mBracket = bracket;
            }

            private ObjectArrayParser reset() {
                mMethodTag = null;
                if (mTrArray != null) mTrArray.clear();
                mStringBuilder.setLength(0);
                mLgType = Type.X;

                mIsParsed = false;
                return this;
            }

            private ObjectArrayParser parse(@NonNull final Object... objects) {
                reset();

                for (final Object obj : objects) {
                    //cache some info--------------------------------------
                    if (mNeedMethodTag
                            && mMethodTag == null && obj instanceof CharSequence
                            && METHOD_TAG_PATTERN.matcher((CharSequence)obj).matches()) {
                        mMethodTag = (String) obj;
                        if (!((String) obj).endsWith(TAG_END))
                            mInsertFirstDelimiter = true;
                    } else if (obj instanceof Throwable) {
                        if (mTrArray == null) mTrArray = new ArrayList<>();
                        mTrArray.add((Throwable) obj);
                    } else if (obj instanceof List<?>
                            && ((List)obj).size() > 0
                            && ((List)obj).get(0) instanceof Throwable) {//List<Throwable>
                        this.parse(((List)obj).toArray());
                    } else if (obj instanceof Type) {
                        mLgType = (Type) obj;
                    } //process the obj---------------------------------------------------------------
                    else if (obj instanceof Object[]) {//recursively parse Object[] in Object[], including native with any class type
                        final Object[] objArray = (Object[]) obj;
                        if (objArray.length != 0) {
                            this.parse(objArray);
                        }
                    } else if (!(obj instanceof CtrlKey)) {
                        //transform obj into string
                        //ArrayList is acceptable
                        //Can't be Object[] and native array
                        boolean removePackageName = true;
                        String objStr;
                        if (obj instanceof String) {
                            objStr = (String) obj;
                        } else if (obj.getClass().isArray()) {//array with primitive type (array with class type has been processed in advance)
                            removePackageName = false;
                            objStr = primitiveTypeArrayToString(obj);
                        } else {
                            objStr = obj.toString();
                        }

                        //remove package name if present
                        int dotPos;
                        if (removePackageName && isToStringFromObjectClass(obj) && (dotPos = objStr.lastIndexOf('.')) != -1) {
                            objStr = objStr.substring(dotPos + 1);//OuterClass$InnerClass
                        }

                        if (objStr.isEmpty()) {
                            continue;
                        }

                        //output the result
                        if (mInsertFirstDelimiter) {
                            mStringBuilder.append(mDelimiter);
                        }
                        mStringBuilder.append(mBracket.mLeft);
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
                if (mTrArray != null && mTrArray.size() != 0) {
                    final String dashSeparator = getSeparator("", '-');
                    mStringBuilder.append('\n');
                    for (final Throwable tr : mTrArray) {
                        mStringBuilder.append(dashSeparator).append('\n');
                        mStringBuilder.append(Log.getStackTraceString(tr));
                    }
                    mStringBuilder.append(getSeparator("Throwable end", '='));
                }

                //tag must be at the beginning
                return mNeedMethodTag ? mMethodTag + mStringBuilder.toString() : mStringBuilder.toString();
            }
        }

        /**@param type: if unknown, use the result parsed from objects; if still unknown, assertion fails */
        private static int _log(@NonNull final Type type, @NonNull final Object... objects) {
            final ObjectArrayParser parser = ObjectArrayParser.newMessageParser().parse(objects);
            final Type finalType = type == Type.X ? parser.mLgType : type;
            switch (finalType){
                case V: return Log.v(PRIMITIVE_LOG_TAG, parser.toString());
                case D: return Log.d(PRIMITIVE_LOG_TAG, parser.toString());
                case I: return Log.i(PRIMITIVE_LOG_TAG, parser.toString());
                case W: return Log.w(PRIMITIVE_LOG_TAG, parser.toString());
                case E: return Log.e(PRIMITIVE_LOG_TAG, parser.toString());
                default:
                    Assert.fail(PRIMITIVE_LOG_TAG + TAG_END + "[UsageError] Unknown Lg.Type");
                    return -1;
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

        private static String primitiveTypeArrayToString(@NonNull final Object obj) {
            Assert.assertTrue(obj.getClass().isArray());
            final Class componentType = obj.getClass().getComponentType();
            final ArrayList<Object> arrayList = new ArrayList<>();//Note: [NotWork] arrayList = new ArrayList(Arrays.asList((int[])obj));
            switch (componentType.toString()) {
                case "byte":    for (byte a: (byte[])obj) arrayList.add(a);         break;
                case "short":   for (short a: (short[])obj) arrayList.add(a);       break;
                case "int":     for (int a: (int[])obj) arrayList.add(a);           break;
                case "long":    for (long a: (long[])obj) arrayList.add(a);         break;
                case "float":   for (float a: (float[])obj) arrayList.add(a);       break;
                case "double":  for (double a: (double[])obj) arrayList.add(a);     break;
                case "char":    for (char a: (char[])obj) arrayList.add(a);         break;
                case "boolean": for (boolean a: (boolean[])obj) arrayList.add(a);   break;
                default:
                    Assert.fail(PRIMITIVE_LOG_TAG + TAG_END
                            + ObjectArrayParser.class.getCanonicalName()
                            + "." + new Object(){}.getClass().getEnclosingMethod().getName()
                            + "(): can't parse native array with primitive type yet: "
                            + componentType + "[]");
            }
            return arrayList.toString();
        }

        public static String getMethodTag(@NonNull final Object... messages){
            //this method is invoked outside the class, and the result is reused, so don't show hyperlink
            return _getMethodTag(false, messages);
        }

        private static String _getMethodTag(final boolean showHyperlink,
                                            @NonNull final Object... messageObjects){
            return _getMethodTag(showHyperlink, findFirstOuterElement(), messageObjects);
        }

        private static String _getMethodTag(final boolean showHyperlink,
                                            @NonNull final StackTraceElement targetElement,
                                            @NonNull final Object... messageObjects){
            String tag = null;

            if (DEBUG_PRINT_ELEMENTS) {
                printStackTraceElements(Thread.currentThread().getStackTrace());
            }

            final StringBuilder resultBuilder = new StringBuilder();

            if (showHyperlink) {//(FileName:LineNumber)   //no other text allowed
                resultBuilder.append("(")
                        .append(targetElement.getFileName())
                        .append(":")
                        .append(targetElement.getLineNumber())
                        .append(")");
            }

            //OuterClass$InnerClass.MethodName
            final String classFullName = targetElement.getClassName();//PACKAGE_NAME.OuterClass$InnerClass
            resultBuilder.append(METHOD_TAG_DELIMITER)
                    .append(classFullName.substring(classFullName.lastIndexOf('.') +1))//remove package name
                    .append(".")
                    .append(targetElement.getMethodName());

            if (messageObjects.length != 0){
                resultBuilder.append(ObjectArrayParser.newMethodTagParser().parse(messageObjects));
            }

            resultBuilder.append(TAG_END);

            if (DEBUG_PRINT_ELEMENTS) {
                d(tag, "result: " + resultBuilder.toString());
                d(tag, getSeparator("end", '^'));
            }
            return resultBuilder.toString();
        }

        public static void printStackTrace(@NonNull final Object... objects){
            final String result = PRIMITIVE_LOG_TAG + TAG_END
//                    + "(" + (new Throwable().getStackTrace()[0].getMethodName()) + ") "
                    + ObjectArrayParser.newMessageParser().parse(objects,
                            "\n\t" + PRIMITIVE_LOG_TAG + TAG_END + "direct invoker: "
                                    + _getMethodTag(true, findOuterElementWithDepth(1)))
                    .toString();
            (new Exception(result)).printStackTrace();
        }

        public static void showToast(@NonNull final Context context, @NonNull final Object... objects) {
            final String message = ObjectArrayParser.newMessageParser().parse(objects).toString();
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

    /** Abbreviation of Time*/
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

        private static void elapsed(@NonNull final Object... objects) {
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
            final Lg.ObjectArrayParser parser = Lg.ObjectArrayParser.newMessageParser().parse(Lg.Type.D, timeStampString, objects);
            final String parsedString = parser.toString();

            Lg.log(parser.mLgType, parsedString, "go to sleep " + ms + "ms~");
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Lg.e(e);
            }
            Lg.log(parser.mLgType, parsedString, "wake up");
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
