package com.jaeger.library;

import android.graphics.Color;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.text.style.BackgroundColorSpan;

public class BgColorSpan extends BackgroundColorSpan {
    public int color;

    public BgColorSpan(int color) {
        super(Color.TRANSPARENT);
        this.color = color;
    }

    public BgColorSpan(@NonNull Parcel src) {
        super(src);
    }
}
