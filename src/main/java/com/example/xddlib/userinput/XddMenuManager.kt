package com.example.xddlib.userinput

import android.app.Activity
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import java.lang.ref.WeakReference

@Suppress("unused")
class XddMenuManager(activity: Activity) {
    private class MenuItemBuilder(val mTitle: String, val mAction: Runnable) {
        val mItemId: Int
            get() = mTitle.hashCode()
        fun build(menu: Menu): MenuItem
                = menu.add(Menu.NONE, mItemId, Menu.NONE, mTitle).apply {
            title = SpannableString(title).apply { setSpan(ForegroundColorSpan(Color.RED), 0, title.length, 0) }
        }
    }

    private val mRefActivity = WeakReference<Activity>(activity)
    private val mItemBuilders = ArrayList<MenuItemBuilder>()

    fun addAction(title: String, action: Runnable) {
        mItemBuilders.add(MenuItemBuilder(title, action))
        mRefActivity.get()?.invalidateOptionsMenu()
    }

    fun onCreateOptionsMenu(menu: Menu) = mItemBuilders.let {
        it.forEach {builder -> builder.build(menu)}
        it.size != 0
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        val found = mItemBuilders.find { builder -> item.itemId == builder.mItemId }
        found?.mAction?.run()
        return found != null
    }
}