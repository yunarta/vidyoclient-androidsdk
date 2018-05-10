package com.vidyo.vidyoconnector.model

import android.app.Activity

/**
 * Router for determining activity available action.
 */
interface VidyoActivityRouter {

    fun returnToLaunchingApplication(url: String, callState: Int)
}

class VidyoActivityRouterDelegate(delegate: VidyoActivityRouter) : VidyoActivityRouter by delegate

class VidyoActivityRouterImpl(private val activity: Activity) : VidyoActivityRouter {

    override fun returnToLaunchingApplication(url: String, callState: Int) {
        // Provide a call state of either 0 or 1, depending on whether the call was successful
        activity.packageManager.getLaunchIntentForPackage(url)?.let {
            it.putExtra("callstate", callState)
            activity.startActivity(it)
        }
    }
}