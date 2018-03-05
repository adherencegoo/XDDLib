package com.example.xddlib.xddpref.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.example.xddlib.R;
import com.example.xddlib.XddInternalUtils;
import com.example.xddlib.xddpref.data.XddPrefAbstractData;
import com.example.xddlib.xddpref.data.XddPrefBinaryData;

import junit.framework.Assert;

/**
 * Created by adher on 2017/7/21.
 */

public class XddPrefToggleElement extends XddPrefAbstractElement {
    private ToggleButton mToggleButton;

    public XddPrefToggleElement(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /**@param prefData must be {@link com.example.xddlib.xddpref.data.XddPrefBinaryData}*/
    @Override
    void init(@NonNull final XddPrefAbstractData<?> prefData) {
        Assert.assertEquals(XddPrefBinaryData.class, prefData.getClass());
        super.init(prefData);

        final XddPrefBinaryData binaryData = (XddPrefBinaryData) prefData;
        mToggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        mToggleButton.setTextOn(binaryData.getDescription(true));
        mToggleButton.setTextOff(binaryData.getDescription(false));
        mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onUiValueChanged(isChecked);
            }
        });

        final boolean binary = binaryData.get(false);
        mToggleButton.setTextColor(new ColorStateList(
                new int[][] {
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked/*unchecked*/}},
                new int[]{
                        binary ? sColorPrimary : mToggleButton.getTextColors().getDefaultColor(),
                        binary ? mToggleButton.getTextColors().getDefaultColor() : sColorPrimary}));

        resetToSharedValue();
        //manually call listener, because when ToggleButton is just created, it's false for default; setChecked(false) won't invoke listener
        onUiValueChanged(getUiValue());
    }

    @Override
    @NonNull Object getUiValue() {
        return mToggleButton.isChecked();
    }

    @Override
    void resetToSharedValue() {
        mToggleButton.setChecked(((XddPrefBinaryData)mPrefData).get(false));
    }

    public static XddPrefToggleElement inflate(@NonNull final XddPrefContainer parent) {
        return XddInternalUtils.UiUtils.inflate(XddPrefToggleElement.class, R.layout.xdd_pref_dialog_toggle_element_constructor, parent);
    }

}
