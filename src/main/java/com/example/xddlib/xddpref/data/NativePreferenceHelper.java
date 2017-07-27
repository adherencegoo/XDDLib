package com.example.xddlib.xddpref.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by adher on 2017/7/20.
 */

final class NativePreferenceHelper {
    private static final ArrayList<Class> sValidType = new ArrayList<>(Arrays.asList(new Class[]{
            Boolean.class,
            Float.class,
            Integer.class,
            Long.class,
            String.class}));
    private static @Nullable SharedPreferences sSingleton = null;

    static void init(@NonNull final Context context) {
        sSingleton = context.getSharedPreferences(NativePreferenceHelper.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    static @NonNull SharedPreferences getInstance() {
        Assert.assertNotNull(sSingleton);
        return sSingleton;
    }

    static void checkTypeValid(@NonNull Class klass) {
        if (!sValidType.contains(klass)) {
            throw new IllegalArgumentException("Wrong type: " + klass + ", expected: " + sValidType);
        }
    }

    static <T> T get(@NonNull final Class<T> klass, @NonNull final String key, @NonNull final T defaultValue) {
        Assert.assertNotNull(sSingleton);
        if (klass.equals(Boolean.class))        return klass.cast(sSingleton.getBoolean(key, (Boolean)defaultValue));
        else if (klass.equals(Float.class))     return klass.cast(sSingleton.getFloat(key, (Float)defaultValue));
        else if (klass.equals(Integer.class))   return klass.cast(sSingleton.getInt(key, (Integer)defaultValue));
        else if (klass.equals(Long.class))      return klass.cast(sSingleton.getLong(key, (Long)defaultValue));
        else if (klass.equals(String.class))    return klass.cast(sSingleton.getString(key, (String)defaultValue));
        else throw new IllegalArgumentException("Wrong type: " + klass + ", expected: " + sValidType);
    }

    static <T> void put(@NonNull final String key, @NonNull final T defaultValue) {
        Assert.assertNotNull(sSingleton);
        final Class<?> klass = defaultValue.getClass();
        final SharedPreferences.Editor editor = sSingleton.edit();
        if (klass.equals(Boolean.class))        editor.putBoolean(key, (Boolean)defaultValue);
        else if (klass.equals(Float.class))     editor.putFloat(key, (Float)defaultValue);
        else if (klass.equals(Integer.class))   editor.putInt(key, (Integer)defaultValue);
        else if (klass.equals(Long.class))      editor.putLong(key, (Long)defaultValue);
        else if (klass.equals(String.class))    editor.putString(key, (String)defaultValue);
        else throw new IllegalArgumentException("Wrong type: " + klass + ", expected: " + sValidType);
        editor.apply();
    }
}