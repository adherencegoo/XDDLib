package com.example.xddlib;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
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
            V(Log.VERBOSE), D(Log.DEBUG), I(Log.INFO), W(Log.WARN), E(Log.ERROR),
            NONE(0), UNKNOWN(-1);

            public final int mValue;
            private Type(final int value) {
                mValue = value;
            }
        }
        private static final Type DEFAULT_INTERNAL_LG_TYPE = Type.V;

        private static final Type[] TYPES = {Type.V, Type.D, Type.I, Type.W, Type.E};
        public static Type types(final int idx) {
            return TYPES[idx % TYPES.length];
        }
        private static final int[] COLORS = {Color.WHITE, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED};
        public static int colors(final int idx) {
            return COLORS[idx % COLORS.length];
        }

        public static ObjectArrayParser v(@NonNull final Object... objects) { return _log(Type.V, objects); }
        public static ObjectArrayParser d(@NonNull final Object... objects) { return _log(Type.D, objects); }
        public static ObjectArrayParser i(@NonNull final Object... objects) { return _log(Type.I, objects); }
        public static ObjectArrayParser w(@NonNull final Object... objects) { return _log(Type.W, objects); }
        public static ObjectArrayParser e(@NonNull final Object... objects) { return _log(Type.E, objects); }
        /** @param objects: must contain Lg.Type*/
        public static ObjectArrayParser log(@NonNull final Object... objects) { return _log(Type.UNKNOWN, objects); }

        public static class ObjectArrayParser {
            private enum Settings {
                /** ->[a]->[b]->[c] */
                PrioritizedMsg(false, true, "->", BracketType.BRACKET),
                /** ->[a]->[b]->[c]: a, b, c */
                FinalMsg(true, false, ", ", BracketType.NONE);

                private final boolean mNeedMethodTag;
                private final boolean mInsertFirstMainMsgDelimiter;
                private final String mDelimiter;
                private final BracketType mBracket;

                Settings(final boolean needMethodTag,
                         final boolean insertFirstDelimiter,
                         @NonNull final String delimiter,
                         @NonNull final BracketType bracket){
                    mNeedMethodTag = needMethodTag;
                    mInsertFirstMainMsgDelimiter = insertFirstDelimiter;

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
            private Type mLgType = Type.UNKNOWN;//cache the LAST found one

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
                mLgType = Type.UNKNOWN;

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
                        another.mLgType == Type.UNKNOWN ? null : another.mLgType);
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
                        if (obj != Type.UNKNOWN) {
                            mLgType = (Type) obj;
                        }

                        //process the data======================================================
                    } else if (obj instanceof ObjectArrayParser) {
                        parseAnotherParser((ObjectArrayParser) obj);
                    } else if (obj instanceof Object[]) {//recursively parse Object[] in Object[], including native with any class type
                        final Object[] objArray = (Object[]) obj;
                        if (objArray.length != 0) this.parse(objArray);
                    } else if (!(obj instanceof CtrlKey)) {
                        //transform obj into string
                        //ArrayList is acceptable
                        String objStr;
                        if (obj instanceof String) {
                            objStr = (String) obj;
                        } else if (obj.getClass().isArray()) {//array with primitive type (array with class type has been processed in advance)
                            objStr = primitiveTypeArrayToString(obj);
                        } else {//Can't be Object[] or array with native type
                            objStr = toSimpleString(obj);
                        }

                        if (objStr.isEmpty()) continue;

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

                if (mNeedMethodTag && mMethodTagSource == null) {
                    mMethodTagSource = findInvokerOfDeepestInnerElementWithOffset(0);
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
                    resultBuilder.append(getMethodTag(mMethodTagSource));
                }

                if (mPrioritizedMsgBuilder != null) resultBuilder.append(mPrioritizedMsgBuilder);
                if (mNeedMethodTag) resultBuilder.append(TAG_END);
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
            parser.mLgType = type == Type.UNKNOWN ? parser.mLgType : type;
            switch (parser.mLgType){
                case V: parser.mPrimitiveLogReturn = Log.v(PRIMITIVE_LOG_TAG, parser.toString());   break;
                case D: parser.mPrimitiveLogReturn = Log.d(PRIMITIVE_LOG_TAG, parser.toString());   break;
                case I: parser.mPrimitiveLogReturn = Log.i(PRIMITIVE_LOG_TAG, parser.toString());    break;
                case W: parser.mPrimitiveLogReturn = Log.w(PRIMITIVE_LOG_TAG, parser.toString());  break;
                case E: parser.mPrimitiveLogReturn = Log.e(PRIMITIVE_LOG_TAG, parser.toString());   break;
                case NONE: parser.mPrimitiveLogReturn = -1;   break;
                default:
                    Assert.fail(PRIMITIVE_LOG_TAG + TAG_END + "[UsageError] Unknown Lg.Type: " + parser.mLgType);
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

        /** @return toString without package name if toString is not overridden */
        public static String toSimpleString(@Nullable final Object object) {
            if (object == null) {
                return "null";
            }

            int dotPos;
            String objStr = object.toString();
            if (isToStringFromObjectClass(object) && (dotPos = objStr.lastIndexOf('.')) != -1) {
                objStr = objStr.substring(dotPos + 1);//OuterClass$InnerClass
            }
            return objStr;
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

        /**
         * @param objects if containing Lg.Type and it's not UNKNOWN, print stack trace according to that Lg.Type using Lg.log; else print stack trace normally
         * */
        public static void printStackTrace(@NonNull final Object... objects){
            final String self = new Object(){}.getClass().getEnclosingMethod().getName();
            final ObjectArrayParser parser = new ObjectArrayParser(ObjectArrayParser.Settings.FinalMsg)
                    .parse(objects, "\n\tdirect invoker: " + getMethodTag(findInvokerOfDeepestInnerElementWithOffset(1)));

            if (parser.mLgType == Type.UNKNOWN) {
                (new Exception(PRIMITIVE_LOG_TAG + TAG_END + parser)).printStackTrace();
            } else if (parser.mLgType != Type.NONE) {//avoid redundant process
                Lg.log(parser, new Exception(self));
            }
        }

        public static void showToast(@NonNull final Context context, @NonNull final Object... objects) {
            ObjectArrayParser parser = log(DEFAULT_INTERNAL_LG_TYPE, "(" + (new Throwable().getStackTrace()[0].getMethodName()) + ")", objects);
            Toast.makeText(context, PRIMITIVE_LOG_TAG + TAG_END + parser, Toast.LENGTH_LONG).show();
        }

        private static StackTraceElement findInvokerOfDeepestInnerElementWithOffset(final int offset) {
            Assert.assertTrue(offset >= 0);
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
            w(tag, getSeparator("start", 'v'));
            for (int idx=0 ; idx<elements.length ; idx++) {
                StackTraceElement element = elements[idx];
                w(tag, String.format(Locale.getDefault(), "element[%d]: %s.%s (%s line:%d)",
                        idx, element.getClassName(), element.getMethodName(), element.getFileName(), element.getLineNumber()));
            }
        }

        /** (FileName.java:LineNumber)->OuterClass$InnerClass.MethodName */
        private static String getMethodTag(@NonNull final StackTraceElement targetElement) {
            /*final StringBuilder methodTagBuilder = new StringBuilder(50);
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

            return methodTagBuilder.toString();*/

            final String string = targetElement.toString();
            //remove package name
            //ex: packageName.ClassName.methodName(FileName.java:LineNumber)
            int dotIdx = string.length();
            for (int idx=0 ; idx<3 ; idx++) {//find the 3rd dot starting from the end
                dotIdx = string.lastIndexOf('.', dotIdx-1);
            }
            return string.substring(dotIdx+1);
        }
    }

    /** Abbreviation of Time*/
    public static final class Tm {
        private Tm() {}

        private static class Timing {
            @NonNull private final Object mId;
            @NonNull private final Lg.ObjectArrayParser mInfo;
            private long t1 = Long.MAX_VALUE, t2 = Long.MIN_VALUE;

            private Timing(@NonNull final Object id, @NonNull final Lg.ObjectArrayParser info) {
                mId = id;
                mInfo = info;
            }

            @Override
            public String toString() {
                return "internal: " + getInternalElapsedTime() + "ms, " + mInfo;
            }

            private long subtract(@NonNull final Timing past) {
                final long result = t1 - past.t2;
                Assert.assertTrue(result >= 0);
                return result;
            }

            private long getInternalElapsedTime() {
                final long result = t2 - t1;
                Assert.assertTrue(result >= 0);
                return result;
            }
        }

        private static class Timeline {
            @NonNull private final Object mId;
            @NonNull private final ArrayList<Timing> mTimings = new ArrayList<>();

            private Timeline(@NonNull final Object id) {
                mId = id;
            }

            @NonNull private Timing tick(@NonNull final Lg.ObjectArrayParser info) {
                final Timing timing = new Timing(mId, info);
                mTimings.add(timing);
                return timing;
            }

            /**Excluding the time of my own procedure*/
            private long calculateInterestingElapsedTime() {
                Assert.assertTrue(mTimings.size() >= 2);

                long result = 0;
                Timing previous = null;
                for (final Timing current: mTimings){
                    if (previous != null) result += current.subtract(previous);
                    previous = current;
                }

                Assert.assertTrue(result >= 0);
                return result;
            }

            /**Including the time of my own procedure*/
            private long calculateRealElapsedTime() {
                Assert.assertTrue(mTimings.size() >= 2);
                final long result = mTimings.get(mTimings.size()-1).t2 - mTimings.get(0).t1;
                Assert.assertTrue(result >= 0);
                return result;
            }

            private long calculateInternalElapsedTime() {
                final long result = calculateRealElapsedTime() - calculateInterestingElapsedTime();
                Assert.assertTrue(result >= 0);
                return result;
            }

            @Override
            public String toString() {
                Assert.assertTrue(mTimings.size() >= 2);
                final StringBuilder builder = new StringBuilder(mTimings.size() * 50);

                Timing previous = null;
                int idx = 0;
                for (final Timing current: mTimings){
                    if (previous != null) {
                        builder.append("\n\t\t↓ ").append(current.subtract(previous)).append("ms");
                    }
                    builder.append("\n[").append(idx).append("] ").append(current);

                    previous = current;
                    idx++;
                }

                final long interestingElapsed = calculateInterestingElapsedTime();
                final long realElapsed = calculateRealElapsedTime();
                final long internalElapsed = realElapsed - interestingElapsed;
                builder.append("\nTotal elapsed time: ").append(interestingElapsed).append("ms");

                //test
                builder.append("; [TEST] Real total elapsed time: ").append(realElapsed)
                        .append("ms, internal elapsed time: ").append(internalElapsed).append("ms");

                return builder.toString();
            }

            private void clear() {
                mTimings.clear();
            }

            private int size() {
                return mTimings.size();
            }
        }

        private static class TimelineManager {
            @NonNull private final HashMap<Object, Timeline> mTimelines = new HashMap<>(3);

            /**
                     * If id==null:<br/>
                     *      If number of timelines is 1, return it;<br/>
                     *      else: assert fail<br/>
                     * else:<br/>
                     *      If target found: return it<br/>
                     *      else: create a new timeline using given id*/
            @NonNull private Timeline getTargetTimeline(@Nullable final Object id, final boolean asNew) {
                if (asNew) Assert.assertTrue(id != null);

                Timeline target = null;
                if (id == null) {
                    if (mTimelines.size() == 1) {//get the only timeline
                        target = mTimelines.values().iterator().next();
                    } else {
                        Assert.fail(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END + "There are " + mTimelines.size() + " Timelines, but no id is given");
                    }
                } else {
                    target = mTimelines.get(id);
                    if (target == null) {//Create a timeline using given id
                        target = new Timeline(id);
                        mTimelines.put(id, target);
                    }
                }

                if (asNew) {
                    target.clear();
                } else {
                    Assert.assertTrue(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END + "Should not be empty, but it is actually", target.size() != 0);
                }

                return target;
            }

            private void remove(@NonNull final Object id) {
                final Timeline removed = mTimelines.remove(id);
                if (removed != null) {
                    removed.clear();
                }
            }
        }

        @NonNull private static final TimelineManager sManager = new TimelineManager();

        //======================================================================

        /**
         * @param id used as the identifier of new timeline, leave null to use current timestamp for default
         * @return id
         * */
        @NonNull public static Object begin(@Nullable final Object id, @NonNull final Object... objects) {
            final long t1 = System.currentTimeMillis();

            final Timeline timeline = sManager.getTargetTimeline(id == null ? t1 : id, true);
            final Timing timing = timeline.tick(Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, Lg.getPrioritizedMessage("id:" + timeline.mId), "start timer~", objects));

            timing.t1 = t1;
            timing.t2 = System.currentTimeMillis();
            return timing.mId;
        }

        /**
         * @param id used to identify which timeline to use, leave null to<br/>
         *           if there is exactly only one, use it<br/>
         *           else, throw exception
         * @return id
         * */
        @NonNull public static Object tick(@Nullable final Object id, @NonNull final Object... objects) {
            long t1 = System.currentTimeMillis();

            final Timeline timeline = sManager.getTargetTimeline(id, false);
            final Timing timing = timeline.tick(Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, Lg.getPrioritizedMessage("id:" + timeline.mId), "timer ticks", objects));

            timing.t1 = t1;
            timing.t2 = System.currentTimeMillis();
            return timing.mId;
        }

        /**
         * @param id see {@link #tick}
         * */
        public static void end(@Nullable final Object id, @NonNull final Object... objects) {
            final long t1 = System.currentTimeMillis();

            final Timeline timeline = sManager.getTargetTimeline(id, false);
            final Timing timing = timeline.tick(//need not output log at this moment
                    new Lg.ObjectArrayParser(Lg.ObjectArrayParser.Settings.FinalMsg)
                            .parse(Lg.DEFAULT_INTERNAL_LG_TYPE, Lg.getPrioritizedMessage("id:" + timeline.mId), "end timer!", objects));

            timing.t1 = t1;
            timing.t2 = System.currentTimeMillis();

            //about 1ms for the following actions
            Lg.log(timing.mInfo.mLgType/*reuse*/, timing.mInfo.mMethodTagSource/*reuse*/, timeline);//output the elapsed time
            sManager.remove(timeline.mId);
        }


        public static void sleep(final long ms, @NonNull final Object... objects) {
            final long timestamp = System.currentTimeMillis();

            final Lg.ObjectArrayParser parser =
                    new Lg.ObjectArrayParser(Lg.ObjectArrayParser.Settings.FinalMsg).parse(Lg.DEFAULT_INTERNAL_LG_TYPE, "timestamp:"+ timestamp, objects);

            Lg.log(parser, "go to sleep " + ms + "ms~");
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Lg.e(e);
            }
            Lg.log(parser, "wake up");
        }
    }

    public static Bitmap drawCross(@NonNull final Bitmap bitmap, final int color, @Nullable final String msg){

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

    public static void saveBitmap(@NonNull final Context context, @Nullable final Bitmap bitmap, @NonNull final String fileName,
                                  @NonNull final Object... objects) {
        final String tag = (new Object(){}.getClass().getEnclosingMethod().getName());

        //produce full path for file
        String fileFullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        if (!fileFullPath.endsWith("/") && !fileName.startsWith("/")){
            fileFullPath += "/";
        }
        fileFullPath += fileName;
        if (!fileName.endsWith(".jpg")){
            fileFullPath += ".jpg";
        }

        if (bitmap != null) {
            //create folders if needed
            final String folderName = fileFullPath.substring(0, fileFullPath.lastIndexOf('/'));
            final File folder = new File(folderName);
            if (!folder.isDirectory()) {//folder not exist
                Assert.assertTrue(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END
                        + "Error in creating folder:[" + folderName + "]", folder.mkdirs());
            }

            OutputStream os = null;
            try {
                os = new FileOutputStream(fileFullPath);
            } catch (FileNotFoundException e) {
                Lg.e("FileNotFoundException: filePath:" + fileFullPath, e);
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);

            final StackTraceElement stackTraceElement = Lg.findInvokerOfDeepestInnerElementWithOffset(0);
            //Note: Must scan file instead of folder
            MediaScannerConnection.scanFile(context, new String[]{fileFullPath}, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, stackTraceElement, tag, "onScanCompleted",
                            "Bitmap saved", "path:" + path, "Uri:" + uri, objects);
                }
            });
        } else {
            Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, tag, "bitmap==null", fileFullPath, objects);
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

    public static String argbIntToHexString(final int color) {
        return String.format("#%08x", color);
    }

    private static final AtomicBoolean sIsActionDialogShowing = new AtomicBoolean(false);
    public static void showActionDialog(@NonNull final Activity activity,//can't be ApplicationContext
                                        @NonNull final Runnable action,
                                        @NonNull final Object... objects) {
        final Lg.ObjectArrayParser kInnerMethodTag = Lg.getPrioritizedMessage(
                new Object(){}.getClass().getEnclosingMethod().getName(),
                "timestamp:" + System.currentTimeMillis());

        try {
            //if sIsActionDialogShowing is false originally: return true and update it to true
            if (sIsActionDialogShowing.compareAndSet(false, true)) {
                //parse objects
                final Lg.ObjectArrayParser kParsedObjects = new Lg.ObjectArrayParser(Lg.ObjectArrayParser.Settings.FinalMsg).parse(objects);
                final StackTraceElement kOuterMethodTagSource = kParsedObjects.mMethodTagSource;
                kParsedObjects.mNeedMethodTag = false;
                final String kDialogMessage = kParsedObjects.toString();

                //log for starting
                Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, kOuterMethodTagSource, kInnerMethodTag, kParsedObjects);

                //build dialog
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                dialogBuilder.setTitle(Lg.PRIMITIVE_LOG_TAG)
                        .setMessage(kDialogMessage)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                action.run();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setCancelable(true)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                //log for ending
                                Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, kInnerMethodTag,
                                        kOuterMethodTagSource,
                                        "sIsActionDialogShowing=false due to dialog dismissed");
                                sIsActionDialogShowing.set(false);
                            }
                        });

                //show dialog
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialogBuilder.show();
                    }
                });
            } else {//sIsActionDialogShowing is true
                Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, kInnerMethodTag, "Confirm dialog is showing, skip this request");
            }
        } catch (Exception e) {
            //log for ending
            Lg.e(kInnerMethodTag, "sIsActionDialogShowing=false due to exception", e);
            sIsActionDialogShowing.set(false);
        }
    }

    /** {@link #isInvokedFrom(String, String, String, int)} */
    public static boolean isInvokedFrom(@Nullable String fileName,
                                        @Nullable final String partialClassName,
                                        @Nullable final String methodName) {
        return isInvokedFrom(fileName, partialClassName, methodName, -1);
    }

    /** All parameters should not be null(illegal) at the same time
     * @param fileName will auto add postfix ".java" if missing
     * @param partialClassName true if StackTraceElement.getClassName CONTAINS it
     * @return true if those NonNull names are matched
     * */
    public static boolean isInvokedFrom(@Nullable String fileName,
                                        @Nullable final String partialClassName,
                                        @Nullable final String methodName,
                                        final int lineNumber) {
        Assert.assertFalse(fileName == null && partialClassName == null && methodName == null && lineNumber <= 0);
        if (fileName != null && !fileName.endsWith(".java")) fileName += ".java";

        final StackTraceElement[] kElements = Thread.currentThread().getStackTrace();
        for (final StackTraceElement kElement : kElements) {
            boolean found = true;
            if (/*found && */fileName != null) found = kElement.getFileName().equals(fileName);
            if (found && partialClassName != null) found = kElement.getClassName().contains(partialClassName) ;
            if (found && methodName != null) found = kElement.getMethodName().equals(methodName);
            if (found && lineNumber > 0) found = kElement.getLineNumber() == lineNumber;

            if (found) return true;
        }
        return false;
    }
}
