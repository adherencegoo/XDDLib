package com.example.xddlib;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleableRes;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by adher on 2017/7/25.
 */

@SuppressWarnings("unused")
public final class XddInternalUtils {
    private XddInternalUtils() {}

    public final static class UiUtils {
        private UiUtils() {}

        public static @NonNull <T> T inflate(final @NonNull Class<T> uiClass,
                                             final @LayoutRes int layoutId,
                                             final @NonNull ViewGroup parent) {
            return inflate(uiClass, layoutId, parent.getContext(), parent);
        }

        public static @NonNull <T> T inflate(final @NonNull Class<T> uiClass,
                                             final @LayoutRes int layoutId,
                                             final @NonNull Context context,
                                             final @Nullable ViewGroup parent) {
            //Note: if parent is null, inflate() returns inflated view; else, it returns parent
            final View inflateReturn = LayoutInflater.from(context).inflate(layoutId, parent);
            final View inflatedView = parent == null ? inflateReturn : parent.getChildAt(parent.getChildCount()-1);
            return uiClass.cast(inflatedView);
        }
    }

    public final static class StyleUtils {
        private StyleUtils(){}

        /**
         * @param attrId [android.]R.attr.COLOR_ATTRIBUTE_NAME or R.styleable.COLOR_ATTRIBUTE_NAME
         * @param styleablePaths to locate colorAttrId; leave empty if colorAttrId is directly defined in the theme (not nested structure)<br/>
         *                       Example: Theme.Gallery -> actionBarStyle -> titleTextStyle -> textColor<br/>
         *                       Then, styleablePaths = android.R.attr.actionBarStyle, android.R.attr.titleTextStyle<br/>
         * @return color int in argb format
         * */
        private static TypedArray getTypedArrayInHierarchy(final @NonNull Context context,
                                                           final @AttrRes int attrId,
                                                           final @StyleableRes int... styleablePaths) {
            if (styleablePaths.length == 0) {
                return context.obtainStyledAttributes(new int[] {attrId});
            } else {
                final TypedValue outValue = new TypedValue();
                Resources.Theme currentTheme = context.getTheme();
                for (int idx=0 ; idx<styleablePaths.length ; idx++) {
                    final int aStyleable = styleablePaths[idx];
                    currentTheme.resolveAttribute(aStyleable, outValue, true);

                    if (idx != styleablePaths.length-1) {
                        currentTheme = context.getResources().newTheme();
                        currentTheme.applyStyle(outValue.resourceId, true);
                    }
                }

                return context.getTheme().obtainStyledAttributes(outValue.resourceId, new int[]{attrId});
            }
        }

        /** See {@link #getTypedArrayInHierarchy(Context, int, int...)} */
        public static @ColorInt int getColorAttr(final @NonNull Context context,
                                                 final @AttrRes int attrId,
                                                 final @StyleableRes int... styleablePaths) {
            final TypedArray leafTypedArray = getTypedArrayInHierarchy(context, attrId, styleablePaths);
            final int result = leafTypedArray.getColor(0, Color.BLACK);
            leafTypedArray.recycle();
            return result;
        }

        /** See {@link #getTypedArrayInHierarchy(Context, int, int...)} */
        public static Drawable getDrawableAttr(final @NonNull Context context,
                                               final @AttrRes int attrId,
                                               final @StyleableRes int... styleablePaths) {
            final TypedArray leafTypedArray = getTypedArrayInHierarchy(context, attrId, styleablePaths);
            final Drawable result = leafTypedArray.getDrawable(0);
            leafTypedArray.recycle();
            return result;
        }
    }
}
