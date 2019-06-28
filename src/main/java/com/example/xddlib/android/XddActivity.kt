package com.example.xddlib.android

import com.example.xddlib.userinput.xddpref.data.XddPrefUtils
import android.os.Bundle
import com.example.xddlib.userinput.XddMenuManager
import android.app.Activity
import android.view.Menu
import android.view.MenuItem

open class XddActivity : Activity() {
    private var _xddMenuManager: XddMenuManager? = null

    @Suppress("MemberVisibilityCanBePrivate")
    val xddMenuManager: XddMenuManager
        get() = _xddMenuManager ?: synchronized(this) {
            _xddMenuManager ?: XddMenuManager(this).also {
                _xddMenuManager = it
            }
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Don't use short-circuit: make sure both are executed
        return super.onCreateOptionsMenu(menu) or xddMenuManager.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Use short-circuit: if the item is consumed by the former, don't execute the latter
        return super.onOptionsItemSelected(item) || xddMenuManager.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        xddMenuManager.addAction("XddPref", Runnable { XddPrefUtils.showDialog(this@XddActivity) })
    }
}
