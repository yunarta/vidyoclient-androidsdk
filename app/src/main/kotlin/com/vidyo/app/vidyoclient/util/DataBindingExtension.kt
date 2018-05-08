package com.vidyo.app.vidyoclient.util

import android.databinding.BindingAdapter
import android.view.View
import android.widget.Button
import android.widget.TextView

object DataBindingExtension {

    @JvmStatic
    @BindingAdapter("textRes")
    fun textRes(view: TextView, textResource: Int) {
        if (textResource != 0) {
            view.setText(textResource)
        } else {
            view.text = ""
        }
    }

    @JvmStatic
    @BindingAdapter("show")
    fun show(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    @JvmStatic
    @BindingAdapter("showBlock")
    fun showBlock(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    @JvmStatic
    @BindingAdapter("highlight")
    fun highlight(view: Button, isHighlighted: Boolean) {
        val background = view.background
        if (background.isStateful) {
            if (view.isEnabled) {
                if (isHighlighted) {
                    background.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked)
                } else {
                    background.state = intArrayOf(android.R.attr.state_enabled)
                }
            } else {
                background.state = intArrayOf()
            }
        }
    }
}