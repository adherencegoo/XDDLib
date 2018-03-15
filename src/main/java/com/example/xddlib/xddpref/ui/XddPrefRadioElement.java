package com.example.xddlib.xddpref.ui;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.xddlib.R;
import com.example.xddlib.XddInternalUtils;
import com.example.xddlib.xddpref.data.XddPrefAbstractData;
import com.example.xddlib.xddpref.data.XddPrefEnumData;

import junit.framework.Assert;

/**
 * Created by adher on 2017/7/21.
 */

public class XddPrefRadioElement extends XddPrefAbstractElement {
    private RadioGroup mRadioGroup;

    public XddPrefRadioElement(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public static XddPrefRadioElement inflate(@NonNull final XddPrefContainer parent) {
        return XddInternalUtils.INSTANCE.inflate(XddPrefRadioElement.class, R.layout.xdd_pref_dialog_radio_element_constructor, parent);
    }

    @Override
    void init(@NonNull final XddPrefAbstractData<?> prefData) {
        Assert.assertEquals(XddPrefEnumData.class, prefData.getClass());
        super.init(prefData);

        final XddPrefEnumData<?> enumData = (XddPrefEnumData<?>) prefData;

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.removeAllViews();
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                onUiValueChanged(((RadioButton) group.findViewById(checkedId)).getText());
            }
        });

        //create radio buttons based on values in pref
        final String prefValueAsString = String.valueOf(mPrefData.get(false));
        for (final Object object : enumData.getValues()) {
            final RadioButton radioButton  = new RadioButton(this.getContext());
            mRadioGroup.addView(radioButton); //the RadioButtons are added to the radioGroup instead of the layout
            radioButton.setText(object.toString());

            if (prefValueAsString.equals(radioButton.getText())) {
                radioButton.setChecked(true);
                radioButton.setTextColor(sColorPrimary);
            }
        }
    }

    @Override
    @NonNull Object getUiValue() {
        return ((RadioButton) findViewById(mRadioGroup.getCheckedRadioButtonId())).getText();
    }

    @Override
    void resetToSharedValue() {
        for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
            final RadioButton button = (RadioButton) mRadioGroup.getChildAt(i);
            if (mPrefData.sharedValueIsEqualTo(button.getText())) {
                button.setChecked(true);
                break;
            }
        }
    }
}
