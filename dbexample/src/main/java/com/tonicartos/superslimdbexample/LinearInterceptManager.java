package com.tonicartos.superslimdbexample;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;

/**
 *
 */
public class LinearInterceptManager extends LinearLayoutManager {

    public LinearInterceptManager(Context context) {
        super(context);
    }

    public LinearInterceptManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public LinearInterceptManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        super.onItemsChanged(recyclerView);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
//        if (state.isPreLayout()) {
//            Log.d("asdf", "Pre-layout children");
//        } else {
//            Log.d("asdf", "Post-layout children");
//        }
        super.onLayoutChildren(recycler, state);
    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
//        Log.d("asdf", "Items added - positionStart: " + positionStart + " itemCount: " + itemCount);
        super.onItemsAdded(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
//        Log.d("asdf", "Items removed - positionStart: " + positionStart + " itemCount: " + itemCount);
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
//        Log.d("asdf", "Items updated - positionStart: " + positionStart + " itemCount: " + itemCount);
        super.onItemsUpdated(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
//        Log.d("asdf", "Items moved - from: " + from + " to:" + to + " itemCount: " + itemCount);
        super.onItemsMoved(recyclerView, from, to, itemCount);
    }
}
