package com.example.xddlib.xddpref.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.xddlib.R;
import com.example.xddlib.XddInternalUtils;
import com.example.xddlib.xddpref.data.XddPrefBinaryData;
import com.example.xddlib.xddpref.data.XddPrefEnumData;

import java.util.Map;

/**
 * Created by adher on 2017/7/20.
 */

public final class XddPrefContainer extends LinearLayout {
    public XddPrefContainer(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public static XddPrefContainer inflate(@NonNull final Context context, @Nullable final ViewGroup parent) {
        return XddInternalUtils.UiUtils.inflate(XddPrefContainer.class, R.layout.xdd_pref_dialog_container_constructor, context, parent);
    }

    public XddPrefContainer init(@NonNull final Map<String, XddPrefEnumData<?>> prefs) {
        for (final Map.Entry<String, XddPrefEnumData<?>> entry : prefs.entrySet()) {
            final XddPrefEnumData<?> aPref = entry.getValue();
            if (aPref instanceof XddPrefBinaryData) {
                XddPrefToggleElement.inflate(this).init(aPref);
            } else {
                XddPrefRadioElement.inflate(this).init(aPref);
            }
        }
        return this;
    }

    public void saveToNative() {
        for (int childIndex=0 ; childIndex<getChildCount() ; childIndex++) {
            final XddPrefAbstractElement element = (XddPrefAbstractElement) getChildAt(childIndex);
            element.saveToNative();
        }
    }
}
