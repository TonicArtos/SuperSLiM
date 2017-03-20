package com.tonicartos.superslim.adapter;

import com.tonicartos.superslim.SectionConfig;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({SectionConfig.FOOTER_END,
        SectionConfig.FOOTER_STICKY,
        SectionConfig.FOOTER_INLINE,
        SectionConfig.FOOTER_START})
@Retention(RetentionPolicy.SOURCE)
public @interface FooterStyle {

}
