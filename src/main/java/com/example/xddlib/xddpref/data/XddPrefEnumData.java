package com.example.xddlib.xddpref.data;

import android.support.annotation.NonNull;

import junit.framework.Assert;

import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * Created by adher on 2017/7/20.
 */

public class XddPrefEnumData<T> extends XddPrefAbstractData<T> {
    private final @NonNull LinkedHashSet<T> mValues;//LinkedHashSet: no duplicated values, persist original order

    // TODO: 2017/7/25 see: https://stackoverflow.com/questions/12462079/potential-heap-pollution-via-varargs-parameter
    /**"defaultValue", "value2", "otherValues": duplicated values will be ignored*/
    public XddPrefEnumData(@NonNull final Class<T> klass,
                           @NonNull final String key,
                           @NonNull final T defaultValue,
                           @NonNull final T value2,
                           @NonNull final T... otherValues) {
        super(klass, key, defaultValue);
        if (klass.equals(Boolean.class)) {
            Assert.assertEquals(0, otherValues.length);
            Assert.assertTrue(!defaultValue.equals(value2));
        }

        //prepare list of values
        mValues = new LinkedHashSet<>(otherValues.length +2);
        mValues.add(defaultValue);
        mValues.add(value2);
        if (otherValues.length != 0) mValues.addAll(Arrays.asList(otherValues));
    }

    public @NonNull LinkedHashSet<T> getValues() {
        return mValues;
    }

    @Override
    boolean givenValueIsCandidate(@NonNull final T givenValue) {
        return mValues.contains(givenValue);
    }
}