package com.tonicartos.superslim;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        SuperSlimLayoutManager.HORIZONTAL,
        SuperSlimLayoutManager.VERTICAL
})
public @interface Orientation {}
