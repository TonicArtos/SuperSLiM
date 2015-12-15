package com.tonicartos.superslim.adapter;

import com.tonicartos.superslim.SectionConfig;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(flag = true, value = {
        SectionConfig.HEADER_INLINE,
        SectionConfig.HEADER_EMBEDDED,
        SectionConfig.HEADER_START,
        SectionConfig.HEADER_END,
        SectionConfig.HEADER_FLOAT,
        SectionConfig.HEADER_TAIL
})
public @interface HeaderStyle {}
