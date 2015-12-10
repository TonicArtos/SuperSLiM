package com.tonicartos.superslim;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A child's animation state in one pass of a predictive layout.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        Child.ANIM_APPEARING,
        Child.ANIM_DISAPPEARING,
        Child.ANIM_NONE
})
public @interface AnimationState {}
