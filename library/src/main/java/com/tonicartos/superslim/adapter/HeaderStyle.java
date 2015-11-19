package com.tonicartos.superslim.adapter;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(flag = true, value = {
        Section.Config.HEADER_INLINE,
        Section.Config.HEADER_EMBEDDED,
        Section.Config.HEADER_START,
        Section.Config.HEADER_END,
        Section.Config.HEADER_OVERLAY
})
public @interface HeaderStyle {}
