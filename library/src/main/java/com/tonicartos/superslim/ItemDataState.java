package com.tonicartos.superslim;

import com.tonicartos.superslim.LayoutManager;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        LayoutManager.ITEM_ADDED,
        LayoutManager.ITEM_REMOVED,
        LayoutManager.ITEM_UPDATED,
        LayoutManager.ITEM_MOVED,
        LayoutManager.ITEM_UNCHANGED
})
public @interface ItemDataState {}
