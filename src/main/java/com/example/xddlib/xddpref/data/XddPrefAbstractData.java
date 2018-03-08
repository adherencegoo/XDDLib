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

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "key:%s, %s, SharedValue:%s",
                kKey, kClass, NativePreferenceHelper.get(kClass, kKey, mDefaultValue));
    }

    public @NonNull String getKey() {
        return kKey;
    }

    public @NonNull T get(final boolean showLog) {
        if (showLog) XDD.Lg.printStackTrace(this);
        return NativePreferenceHelper.get(kClass, kKey, mDefaultValue);
    }

    public @NonNull T get() {
        return get(true);
    }

    public boolean sharedValueIsEqualTo(@NonNull final Object valueAsObject) {
        return convertToTemplateType(valueAsObject).equals(get(false));
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
        NativePreferenceHelper.put(kKey, convertToTemplateType(valueAsObject));
    }

    abstract boolean givenValueIsCandidate(@NonNull final T givenValue);
}
