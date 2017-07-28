package com.example.xddlib.xddpref.ui;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.xddlib.R;
import com.example.xddlib.XddInternalUtils;
import com.example.xddlib.xddpref.data.XddPrefAbstractData;

/**
 * Created by adher on 2017/7/20.
 */

abstract class XddPrefAbstractElement extends LinearLayout {
    static @ColorInt int sColorPrimary = -1;

    XddPrefAbstractData<?> mPrefData;
    private Button mResetButton;

    public XddPrefAbstractElement(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        if (sColorPrimary == -1) {
            sColorPrimary = XddInternalUtils.StyleUtils.getColorAttr(context, android.R.attr.colorAccent);
        }
    }

    void init(final @NonNull XddPrefAbstractData<?> prefData) {
        mPrefData = prefData;

        mResetButton = (Button) findViewById(R.id.key);
        mResetButton.setText(prefData.getKey());
        mResetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToCachedValue();
            }
        });
    }

    /**@return true if UiValue == CachedValue */
    boolean onUiValueChanged(final @NonNull Object uiValue) {
        final boolean equalToCached = mPrefData.cachedValueIsEqualTo(uiValue);
        mResetButton.setEnabled(!equalToCached);
        return equalToCached;
    }

    abstract @NonNull Object getUiValue();

    abstract void resetToCachedValue();

    void saveToNative() {
        mPrefData.saveToNativePreference(getUiValue());
    }
}
