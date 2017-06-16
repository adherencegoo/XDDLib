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

//@SuppressWarnings("unused")
@SuppressWarnings("WeakerAccess")
public final class XDD {
    private XDD(){}
    private static final String THIS_FILE_NAME = XDD.class.getSimpleName() + ".java";//immutable
    private static final int DEFAULT_REPEAT_COUNT = 30;

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
     * FinalMsg: (Abc.java:123)->testMethod->[abc]->[def]: ABC, DEF
     *      MethodTag(including CodeHyperlink): (Abc.java:123)->testMethod
     *      PrioritizedMsg(excluding “: ”) : ->[abc]->[def]
     * */
    public static final class Lg {
        private Lg(){}
        public static final String PRIMITIVE_LOG_TAG = XDD.class.getSimpleName() + "D";//mutable
        private static final String TAG_END = ": ";
        private static final Pattern PRIORITIZED_MSG_PATTERN = Pattern.compile("^->\\[.+\\]$");//->[ANYTHING]
        private static final Pattern ACCESS_METHOD_PATTERN = Pattern.compile("^access[$][0-9]+$");//->[ANYTHING]

        @SuppressWarnings("all")
        public enum Type {
            V(Log.VERBOSE), D(Log.DEBUG), I(Log.INFO), W(Log.WARN), E(Log.ERROR), X(-1);

            public final int mValue;
            private Type(final int value) {
                mValue = value;
            }
        }

        public static ObjectArrayParser v(@NonNull final Object... objects) { return _log(Type.V, objects); }
        public static ObjectArrayParser d(@NonNull final Object... objects) { return _log(Type.D, objects); }
        public static ObjectArrayParser i(@NonNull final Object... objects) { return _log(Type.I, objects); }
        public static ObjectArrayParser w(@NonNull final Object... objects) { return _log(Type.W, objects); }
        public static ObjectArrayParser e(@NonNull final Object... objects) { return _log(Type.E, objects); }
        /** @param objects: must contain Lg.Type*/
        public static ObjectArrayParser log(@NonNull final Object... objects) { return _log(Type.X, objects); }

        public static class ObjectArrayParser {
            private enum Settings {
                /** ->[a]->[b]->[c] */
                PrioritizedMsg(false, true, false, "->", BracketType.BRACKET),
                /** ->[a]->[b]->[c]: a, b, c */
                FinalMsg(true, false, true, ", ", BracketType.NONE);

                private final boolean mNeedMethodTag;
                private final boolean mInsertFirstMainMsgDelimiter;
                private final boolean mNeedTagEnd;
                private final String mDelimiter;
                private final BracketType mBracket;

                Settings(final boolean needMethodTag,
                         final boolean insertFirstDelimiter,
                         final boolean needTagEnd,
                         @NonNull final String delimiter,
                         @NonNull final BracketType bracket){
                    mNeedMethodTag = needMethodTag;
                    mInsertFirstMainMsgDelimiter = insertFirstDelimiter;
                    mNeedTagEnd = needTagEnd;

                    mDelimiter = delimiter;
                    mBracket = bracket;
                }
            }


            //settings =====================================================
            private final Settings mSettings;
            private boolean mNeedMethodTag;
            private boolean mInsertMainMsgDelimiter;

            //parsed results =====================================================
            private StackTraceElement mMethodTagSource = null;//cache the first found StackTraceElement
            private ArrayList<Throwable> mTrArray = null;
            private StringBuilder mPrioritizedMsgBuilder = null;
            private StringBuilder mMainMsgBuilder = null;
            private Type mLgType = Type.X;//cache the LAST found one

            //others =====================================================
            private boolean mIsParsed = false;
            private int mPrimitiveLogReturn = -1;

            private ObjectArrayParser(@NonNull final Settings settings) {
                mSettings = settings;
                mNeedMethodTag = mSettings.mNeedMethodTag;
                mInsertMainMsgDelimiter = mSettings.mInsertFirstMainMsgDelimiter;
            }

            private ObjectArrayParser reset() {
                mMethodTagSource = null;
                if (mTrArray != null) mTrArray.clear();
                if (mPrioritizedMsgBuilder != null) mPrioritizedMsgBuilder.setLength(0);
                if (mMainMsgBuilder != null) mMainMsgBuilder.setLength(0);
                mLgType = Type.X;

                mIsParsed = false;
                return this;
            }

            /** Ignore mMethodTagSource of another*/
            private ObjectArrayParser parseAnotherParser(@NonNull final ObjectArrayParser another) {
                final boolean origNeedMethodTag = mNeedMethodTag;
                mNeedMethodTag = false;
                parse(another.mTrArray == null ? null : another.mTrArray.toArray(),
                        another.mPrioritizedMsgBuilder,
                        another.mMainMsgBuilder,
                        another.mLgType);
                mNeedMethodTag = origNeedMethodTag;
                return this;
            }

            private ObjectArrayParser parse(@NonNull final Object... objects) {

                for (final Object obj : objects) {
                    if (obj == null) continue;
                    //cache some info--------------------------------------
                    if (mNeedMethodTag && mMethodTagSource == null && obj instanceof StackTraceElement) {
                        mMethodTagSource = (StackTraceElement) obj;
                    } else if (obj instanceof Throwable) {
                        if (mTrArray == null) mTrArray = new ArrayList<>();
                        mTrArray.add((Throwable) obj);
                    } else if (obj instanceof List<?>
                            && ((List)obj).size() > 0
                            && ((List)obj).get(0) instanceof Throwable) {//List<Throwable>
                        this.parse(((List)obj).toArray());
                    } else if (obj instanceof Type) {
                        mLgType = (Type) obj;

                        //process the data======================================================
                    } else if (obj instanceof ObjectArrayParser) {
                        parseAnotherParser((ObjectArrayParser) obj);
                    } else if (obj instanceof Object[]) {//recursively parse Object[] in Object[], including native with any class type
                        final Object[] objArray = (Object[]) obj;
                        if (objArray.length != 0) this.parse(objArray);
                    } else if (!(obj instanceof CtrlKey)) {
                        //transform obj into string
                        //ArrayList is acceptable
                        boolean removePackageName = true;
                        String objStr;
                        if (obj instanceof String) {
                            objStr = (String) obj;
                        } else if (obj.getClass().isArray()) {//array with primitive type (array with class type has been processed in advance)
                            removePackageName = false;
                            objStr = primitiveTypeArrayToString(obj);
                        } else {//Can't be Object[] or array with native type
                            objStr = obj.toString();
                        }

                        if (objStr.isEmpty()) continue;

                        //remove package name if present
                        int dotPos;
                        if (removePackageName && isToStringFromObjectClass(obj) && (dotPos = objStr.lastIndexOf('.')) != -1) {
                            objStr = objStr.substring(dotPos + 1);//OuterClass$InnerClass
                        }

                        if (PRIORITIZED_MSG_PATTERN.matcher(objStr).matches()) {
                            if (mPrioritizedMsgBuilder == null) mPrioritizedMsgBuilder = new StringBuilder();
                            mPrioritizedMsgBuilder.append(objStr);
                        } else {//normal string
                            if (mMainMsgBuilder == null) mMainMsgBuilder = new StringBuilder(50);

                            //output the result
                            if (mInsertMainMsgDelimiter) {
                                mMainMsgBuilder.append(mSettings.mDelimiter);
                            }
                            mMainMsgBuilder.append(mSettings.mBracket.mLeft);
                            mMainMsgBuilder.append(objStr);
                            mMainMsgBuilder.append(mSettings.mBracket.mRight);

                            mInsertMainMsgDelimiter = true;
                        }
                    }
                }

                mIsParsed = true;
                return this;
            }

            @Override
            public String toString() {
                Assert.assertTrue(mIsParsed);
                final StringBuilder resultBuilder = new StringBuilder(200);

                //MethodTag
                if (mNeedMethodTag) {
                    if (mMethodTagSource == null) {
                        mMethodTagSource = findInvokerOfDeepestInnerElement();
                    }
                    resultBuilder.append(getMethodTag(mMethodTagSource));
                }

                if (mPrioritizedMsgBuilder != null) resultBuilder.append(mPrioritizedMsgBuilder);
                if (mSettings.mNeedTagEnd) resultBuilder.append(TAG_END);
                if (mMainMsgBuilder != null) resultBuilder.append(mMainMsgBuilder);

                //tr must be at the end
                if (mTrArray != null && mTrArray.size() != 0) {
                    final String dashSeparator = getSeparator("", '-');
                    resultBuilder.append('\n');
                    for (final Throwable tr : mTrArray) {
                        resultBuilder.append(dashSeparator).append('\n');
                        resultBuilder.append(Log.getStackTraceString(tr));
                    }
                    resultBuilder.append(getSeparator("Throwable end", '='));
                }

                //tag must be at the beginning
                return resultBuilder.toString();
            }
        }

        /**@param type: if unknown, use the result parsed from objects; if still unknown, assertion fails */
        private static ObjectArrayParser _log(@NonNull final Type type, @NonNull final Object... objects) {
            final ObjectArrayParser parser = new ObjectArrayParser(ObjectArrayParser.Settings.FinalMsg).parse(objects);
            parser.mLgType = type == Type.X ? parser.mLgType : type;
            switch (parser.mLgType){
                case V: parser.mPrimitiveLogReturn = Log.v(PRIMITIVE_LOG_TAG, parser.toString());   break;
                case D: parser.mPrimitiveLogReturn = Log.d(PRIMITIVE_LOG_TAG, parser.toString());   break;
                case I: parser.mPrimitiveLogReturn = Log.i(PRIMITIVE_LOG_TAG, parser.toString());    break;
                case W: parser.mPrimitiveLogReturn = Log.w(PRIMITIVE_LOG_TAG, parser.toString());  break;
                case E: parser.mPrimitiveLogReturn = Log.e(PRIMITIVE_LOG_TAG, parser.toString());   break;
                default:
                    Assert.fail(PRIMITIVE_LOG_TAG + TAG_END + "[UsageError] Unknown Lg.Type");
                    parser.mPrimitiveLogReturn = -1;
            }
            return parser;
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

        public static ObjectArrayParser getPrioritizedMessage(@NonNull final Object... messages){
            return new ObjectArrayParser(ObjectArrayParser.Settings.PrioritizedMsg).parse(messages);
        }

        public static void printStackTrace(@NonNull final Object... objects){
            final String result = PRIMITIVE_LOG_TAG + TAG_END
                    + new ObjectArrayParser(ObjectArrayParser.Settings.FinalMsg).parse(objects,
                            "\n\t" + PRIMITIVE_LOG_TAG + TAG_END
                                    + "direct invoker: " + getMethodTag(findInvokerOfDeepestInnerElement(1)));
            (new Exception(result)).printStackTrace();
        }

        public static void showToast(@NonNull final Context context, @NonNull final Object... objects) {
            ObjectArrayParser parser = d("(" + (new Throwable().getStackTrace()[0].getMethodName()) + ")", objects);
            Toast.makeText(context, PRIMITIVE_LOG_TAG + TAG_END + parser, Toast.LENGTH_LONG).show();
        }

        private static StackTraceElement findInvokerOfDeepestInnerElement() {
            return findInvokerOfDeepestInnerElement(0);
        }

        private static StackTraceElement findInvokerOfDeepestInnerElement(final int offset) {
            final StackTraceElement[] elements = Thread.currentThread().getStackTrace();//smaller index, called more recently

            for (int idx=elements.length-1 ; idx>=0; idx--) {//search from the farthest to the recent
                if (elements[idx].getFileName().equals(THIS_FILE_NAME)) {
                    for (int jdx=idx+1+offset ; jdx < elements.length ; jdx++) {
                        if (!ACCESS_METHOD_PATTERN.matcher(elements[jdx].getMethodName()).matches()) {//skip access method like "access$000"
                            return elements[jdx];
                        }
                    }
                }
            }
            //Should not enter here!!!
            printStackTraceElements(elements);
            Assert.fail(PRIMITIVE_LOG_TAG + TAG_END + (new Throwable().getStackTrace()[0].getMethodName()) + " fails !!! firstOuterElement not found");
            return elements[0];//unreachable
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

        /** (FileName.java:LineNumber)->OuterClass$InnerClass.MethodName */
        private static String getMethodTag(@NonNull final StackTraceElement targetElement) {
            final StringBuilder methodTagBuilder = new StringBuilder(50);
            methodTagBuilder.append("(")
                    .append(targetElement.getFileName())
                    .append(":")
                    .append(targetElement.getLineNumber())
                    .append(")");

            //OuterClass$InnerClass.MethodName
            final String classFullName = targetElement.getClassName();//PACKAGE_NAME.OuterClass$InnerClass
            methodTagBuilder.append(ObjectArrayParser.Settings.PrioritizedMsg.mDelimiter)
                    .append(classFullName.substring(classFullName.lastIndexOf('.') +1))//remove package name
                    .append(".")
                    .append(targetElement.getMethodName());

            return methodTagBuilder.toString();
        }
    }

    /** Abbreviation of Time*/
    public static final class Tm {
        private Tm() {}

        private static long sStartTime = 0;
        private static long sEndTime = 0;
        private static Lg.ObjectArrayParser sStartMessage = null;
        private static Lg.ObjectArrayParser sEndMessage = null;

        public static void start(@NonNull final Object... objects) {
            sStartMessage = Lg.getPrioritizedMessage(objects);
            Lg.d(sStartMessage, "start timer~");

            sStartTime = System.currentTimeMillis();
        }

        public static void end(@NonNull final Object... objects) {
            sEndTime = System.currentTimeMillis();

            sEndMessage = Lg.getPrioritizedMessage(objects);
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

            final Lg.ObjectArrayParser parser =
                    new Lg.ObjectArrayParser(Lg.ObjectArrayParser.Settings.FinalMsg).parse(Lg.Type.D, "timestamp:"+ timestamp, objects);

            Lg.log(parser, "go to sleep " + ms + "ms~");
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Lg.e(e);
            }
            Lg.log(parser, "wake up");
        }
    }


    // TODO: 2017/5/19 stale: not apply getPrioritizedMessage and Lg
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

    // TODO: 2017/5/19 stale: not apply getPrioritizedMessage and Lg
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
        final StringBuilder stringBuilder = new StringBuilder(count * 2 + 4 + (message == null ? 0 : message.length()));
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
