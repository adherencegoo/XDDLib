package com.example.xddlib.xddpref.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.xddlib.XDD;

import junit.framework.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Created by adher on 2017/7/28.
 */

public abstract class XddPrefAbstractData<T> {
    private @NonNull final Class<T> kClass;
    private @NonNull final String kKey;
    /**Should be the same as the valued stored in {@link android.content.SharedPreferences}*/
    private @Nullable T mCachedValue = null;
    /**Value to be used if {@link #mCachedValue} is not ready or not candidate*/
    private @NonNull final T mDefaultValue;
    private Method mMethodValueOf;

    /**
     * @param klass See {@link com.example.xddlib.xddpref.data.NativePreferenceHelper#sValidType}
     * @param defaultValue See {@link #mDefaultValue}
     * */
    XddPrefAbstractData(@NonNull final Class<T> klass,
                        @NonNull final String key,
                        @NonNull final T defaultValue) {
        NativePreferenceHelper.checkTypeValid(klass);
        kClass = klass;
        kKey = key;
        mDefaultValue = defaultValue;

        try {
            if (kClass.equals(String.class)) {
                mMethodValueOf = kClass.getMethod("valueOf", Object.class);
            } else {
                mMethodValueOf = kClass.getMethod("valueOf", String.class);
            }
        } catch (NoSuchMethodException e) {
            XDD.Lg.e(e);
            Assert.fail("[valueOf] not found in " + kClass);
        }

        XddPrefUtils.addPref(this);
    }

    /** Must be called after {@link com.example.xddlib.xddpref.data.NativePreferenceHelper#init(Context)}*/
    void init() {
        boolean putToNativePreference = true;
        if (NativePreferenceHelper.getInstance().contains(kKey)) {
            mCachedValue = NativePreferenceHelper.get(kClass, kKey, mDefaultValue);
            //if the value in NativePreference doesn't exist in mValues, abandon NativePreference
            putToNativePreference = !givenValueIsCandidate(mCachedValue);
        }
        if (putToNativePreference) {
            saveToNativePreference(mDefaultValue);
        }
        Assert.assertTrue(givenValueIsCandidate(mCachedValue));
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "key:%s, %s, cachedValue:%s", kKey, kClass, mCachedValue);
    }

    public @NonNull String getKey() {
        return kKey;
    }

    public @NonNull T getCachedValue(final boolean showLog) {
        Assert.assertNotNull(mCachedValue);
        if (showLog) XDD.Lg.printStackTrace(this);
        return mCachedValue;
    }

    public @NonNull T getCachedValue() {
        return getCachedValue(true);
    }

    public boolean cachedValueIsEqualTo(@NonNull final Object valueAsObject) {
        return convertToTemplateType(valueAsObject).equals(mCachedValue);
    }

    private @NonNull T convertToTemplateType(@NonNull Object value) {
        /* Ex:
        * value(before):    String instance ("0") pointed by Object pointer
        * value(after):     Integer instance (0) pointed by Object pointer
        * return:           Integer instance (0) pointed by Integer pointer
        */

        //convert input Object to Boolean/Integer/Float/Long/String via method: valueOf
        if (!kClass.equals(value.getClass())) {
            final Object tag = XDD.Lg.getPrioritizedMessage("Expected class:" + kClass, "Original input:" + value);
            try {
                value = mMethodValueOf.invoke(null, value);
            } catch (InvocationTargetException e) {
                XDD.Lg.e(tag, e);
            } catch (IllegalAccessException e) {
                XDD.Lg.e(tag, e);
            }
        }
        Assert.assertEquals(kClass, value.getClass());

        return kClass.cast(value);
    }

    public void saveToNativePreference(@NonNull final Object valueAsObject) {
        final T valueAsTemplateType = convertToTemplateType(valueAsObject);

        if (!valueAsTemplateType.equals(mCachedValue)) {
            //do save
            Assert.assertTrue("Value to be saved must be candidate", givenValueIsCandidate(valueAsTemplateType));
            mCachedValue = valueAsTemplateType;
            NativePreferenceHelper.put(kKey, mCachedValue);
        }
    }

    abstract boolean givenValueIsCandidate(@NonNull final T givenValue);
}
