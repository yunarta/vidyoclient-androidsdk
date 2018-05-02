package com.vidyo.vidyoconnector.util;

import android.databinding.BindingAdapter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

public class DataBindingExtension {

    @BindingAdapter("show")
    public static void show(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @BindingAdapter("showBlock")
    public static void showBlock(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("highlight")
    public static void highlight(Button view, boolean isHighlighted) {
        Drawable background = view.getBackground();
        if (background.isStateful()) {
            if (view.isEnabled()) {
                if (isHighlighted) {
                    background.setState(new int[]{android.R.attr.state_enabled, android.R.attr.state_checked});
                } else {
                    background.setState(new int[]{android.R.attr.state_enabled});
                }
            } else {
                background.setState(new int[]{});
            }
        }
    }
}
