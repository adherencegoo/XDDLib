package com.example.xddlib.xddpref.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.example.xddlib.XDD;

import junit.framework.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * Created by adher on 2017/7/20.
 */

public class XddPrefEnumData<T>  {
    private @NonNull final Class<T> kClass;
    private @NonNull final String kKey;
    private @NonNull T mCachedValue;//should be the same as the valued stored in SharedPreference
    private final @NonNull LinkedHashSet<T> mValues;//LinkedHashSet: no duplicated values, persist original order
    private Method mMethodValueOf;

    // TODO: 2017/7/25 see: https://stackoverflow.com/questions/12462079/potential-heap-pollution-via-varargs-parameter
    /**
     * "value1", "value2", "otherValues": duplicated values will be ignored
     * @param klass See {@link com.example.xddlib.xddpref.data.NativePreferenceHelper#sValidType}
     * */
    public XddPrefEnumData(@NonNull final Class<T> klass,
                           @NonNull final String key,
                           @NonNull final T value1,
                           @NonNull final T value2,
                           @NonNull final T... otherValues) {

        NativePreferenceHelper.checkTypeValid(klass);
        if (klass.equals(Boolean.class)) {
            Assert.assertEquals(0, otherValues.length);
            Assert.assertTrue(!value1.equals(value2));
        }

        kClass = klass;
        kKey = key;
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

        //prepare list of values
        mValues = new LinkedHashSet<>(otherValues.length +2);
        mValues.add(value1);
        mValues.add(value2);
        if (otherValues.length != 0) mValues.addAll(Arrays.asList(otherValues));

        XddPrefUtils.addPref(this);
    }

    /** Must be called after {@link com.example.xddlib.xddpref.data.NativePreferenceHelper#init(Context)}*/
    void init() {
        boolean putToNativePreference = true;
        final T firstValue = mValues.iterator().next();
        if (NativePreferenceHelper.getInstance().contains(kKey)) {
            mCachedValue = NativePreferenceHelper.get(kClass, kKey, firstValue);
            //if the value in NativePreference doesn't exist in mValues, abandon NativePreference
            putToNativePreference = !mValues.contains(mCachedValue);
        }
        if (putToNativePreference) {
            saveToNativePreference(firstValue);
        }
    }

    public boolean cachedValueIsEqualTo(@NonNull final Object valueAsObject) {
        return convertToTemplateType(valueAsObject).equals(mCachedValue);
    }

    public void saveToNativePreference(@NonNull final Object valueAsObject) {
        final T valueAsTemplateType = convertToTemplateType(valueAsObject);

        if (!valueAsTemplateType.equals(mCachedValue)) {
            //do save
            Assert.assertTrue("Value to be saved must exist in candidate value list", mValues.contains(valueAsTemplateType));
            mCachedValue = valueAsTemplateType;
            NativePreferenceHelper.put(kKey, mCachedValue);
        }
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

    @SuppressWarnings("WeakerAccess")
    public @NonNull T getCachedValue() {
        return mCachedValue;
    }

    public @NonNull LinkedHashSet<T> getValues() {
        return mValues;
    }

    public @NonNull String getKey() {
        return kKey;
    }
}