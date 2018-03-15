package com.example.xddlib

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.annotation.LayoutRes
import android.support.annotation.StyleableRes
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup

/**
 * Created by adher on 2017/7/25.
 */

internal object XddInternalUtils {
    fun <T> inflate(uiClass: Class<T>,
                    @LayoutRes layoutId: Int,
                    parent: ViewGroup): T {
        return inflate(uiClass, layoutId, parent.context, parent)
    }

    fun <T> inflate(uiClass: Class<T>,
                    @LayoutRes layoutId: Int,
                    context: Context,
                    parent: ViewGroup?): T {
        //Note: if parent is null, inflate() returns inflated view; else, it returns parent
        val inflateReturn = LayoutInflater.from(context).inflate(layoutId, parent)
        val inflatedView = if (parent == null) inflateReturn else parent.getChildAt(parent.childCount - 1)
        return uiClass.cast(inflatedView)
    }

    /**
     * @param attrId android.R.attr.COLOR_ATTRIBUTE_NAME or R.styleable.COLOR_ATTRIBUTE_NAME
     * @param styleablePaths to locate colorAttrId; leave empty if colorAttrId is directly defined in the theme (not nested structure)<br></br>
     * Example: Theme.Gallery -> actionBarStyle -> titleTextStyle -> textColor<br></br>
     * Then, styleablePaths = android.R.attr.actionBarStyle, android.R.attr.titleTextStyle<br></br>
     * @return color int in argb format
     */
    private fun getTypedArrayInHierarchy(context: Context,
                                         @AttrRes attrId: Int,
                                         @StyleableRes vararg styleablePaths: Int): TypedArray {
        if (styleablePaths.isEmpty()) {
            return context.obtainStyledAttributes(intArrayOf(attrId))
        } else {
            val outValue = TypedValue()
            var currentTheme = context.theme
            for (idx in styleablePaths.indices) {
                val aStyleable = styleablePaths[idx]
                currentTheme.resolveAttribute(aStyleable, outValue, true)

                if (idx != styleablePaths.size - 1) {
                    currentTheme = context.resources.newTheme()
                    currentTheme.applyStyle(outValue.resourceId, true)
                }
            }

            return context.theme.obtainStyledAttributes(outValue.resourceId, intArrayOf(attrId))
        }
    }

    /** See [.getTypedArrayInHierarchy]  */
    @ColorInt
    fun getColorAttr(context: Context,
                     @AttrRes attrId: Int,
                     @StyleableRes vararg styleablePaths: Int): Int {
        val leafTypedArray = getTypedArrayInHierarchy(context, attrId, *styleablePaths)
        val result = leafTypedArray.getColor(0, Color.BLACK)
        leafTypedArray.recycle()
        return result
    }

    /** See [.getTypedArrayInHierarchy]  */
    fun getDrawableAttr(context: Context,
                        @AttrRes attrId: Int,
                        @StyleableRes vararg styleablePaths: Int): Drawable? {
        val leafTypedArray = getTypedArrayInHierarchy(context, attrId, *styleablePaths)
        val result = leafTypedArray.getDrawable(0)
        leafTypedArray.recycle()
        return result
    }
}
