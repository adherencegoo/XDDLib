package com.example.xddlib.xddpref.data;

import android.support.annotation.NonNull;

/**
 * Created by adher on 2017/7/20.
 */

public final class XddPrefBinaryData extends XddPrefEnumData<Boolean> {
    @NonNull private final String kFalseDescription;
    @NonNull private final String kTrueDescription;

    @SuppressWarnings("unused")
    public XddPrefBinaryData(@NonNull final String key){
        this(key, key);
    }

    @SuppressWarnings("unused")
    public XddPrefBinaryData(@NonNull final String key,
                             @NonNull final String trueDescription){
        this(key, trueDescription, "Not " + trueDescription);
    }

    @SuppressWarnings("unused")
    public XddPrefBinaryData(@NonNull final String key,
                             @NonNull final String trueDescription,
                             @NonNull final String falseDescription ) {
        super(Boolean.class, key, false, true);
        kTrueDescription = trueDescription + " (True)";
        kFalseDescription = falseDescription + " (False)";
    }

    @SuppressWarnings("unused")
    public String getDescription(final boolean wanted) {
        return wanted ? kTrueDescription : kFalseDescription;
    }
}
