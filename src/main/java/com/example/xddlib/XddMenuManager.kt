package com.example.xddlib

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import java.lang.ref.WeakReference

@Suppress("unused")
class XddMenuManager(activity: Activity) {
    private class MenuItemBuilder(val mTitle: String, val mAction: Runnable) {
        val mItemId: Int
            get() = mTitle.hashCode()
        fun build(menu: Menu): MenuItem
                = menu.add(Menu.NONE, mItemId, Menu.NONE, mTitle)
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