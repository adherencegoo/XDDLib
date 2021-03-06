package com.example.xddlib.analysis

import com.example.xddlib.presentation.Lg

@Suppress("unused")
class XddException(vararg data: Any?) : Exception(
        Lg.getFinalMessage(Lg.VarargParser.Control.KILL_METHOD_TAG, *data).toString())