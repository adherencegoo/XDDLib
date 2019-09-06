package com.example.xddlib.userinput.xddpref.ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import com.example.xddlib.R
import com.example.xddlib.XddInternalUtils
import com.example.xddlib.presentation.Lg
import com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData
import com.example.xddlib.userinput.xddpref.data.XddPrefPrimitiveRangeData

internal class XddPrefSeekBarElement(context: Context, attributeSet: AttributeSet?) : XddPrefAbstractElement(context, attributeSet), SeekBar.OnSeekBarChangeListener {
    private lateinit var mSeekBar: SeekBar

    companion object {

        fun inflate(parent: XddPrefContainer): XddPrefSeekBarElement {
            return XddInternalUtils.inflate(XddPrefSeekBarElement::class.java, R.layout.xdd_pref_dialog_seekbar_constructor, parent)
        }

    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        onUiValueChanged(p1)
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
        Lg.e()//xdd
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        Lg.e()//xdd
    }

    override val uiValue: Any
        get() = mSeekBar.progress

    @RequiresApi(Build.VERSION_CODES.O)
    override fun init(prefData: XddPrefAbstractData<*>) {
        assert(prefData is XddPrefPrimitiveRangeData)
        super.init(prefData)

        val rangeData = prefData as XddPrefPrimitiveRangeData<*>
        mSeekBar = findViewById(R.id.seekBar)
        mSeekBar.setOnSeekBarChangeListener(this)

        mSeekBar.min = rangeData.min.toInt()
        mSeekBar.max = rangeData.max.toInt()
        mSeekBar.incrementProgressBy(rangeData.step.toInt())
    }

    override fun resetToSharedValue() {
        mSeekBar.progress = (mPrefData as XddPrefPrimitiveRangeData<*>).get().toInt()
    }
}