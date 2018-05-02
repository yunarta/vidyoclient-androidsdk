package com.vidyo.vidyoconnector.model

import android.app.Activity

/**
 * Router for determining activity available action.
 */
interface MainActivityRouter {
    fun returnToLaunchingApplication(url: String, callState: Int)
}

class MainActivityRouterDelegate(delegate: MainActivityRouter) : MainActivityRouter by delegate

class MainActivityRouterImpl(private val activity: Activity) : MainActivityRouter {

    override fun returnToLaunchingApplication(url: String, callState: Int) {
        // Provide a callstate of either 0 or 1, depending on whether the call was successful
        activity.packageManager.getLaunchIntentForPackage(url).let {
            it.putExtra("callstate", callState)
            activity.startActivity(it)
        }
    }
}