package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

/**
 * State to track the current top mMarkerLine views are being mMarkerLine relative to.
 */
public class Recycler {

    public final RecyclerView.Recycler inner;

    public final SparseArray<android.view.View> viewCache;

    public Recycler(RecyclerView.Recycler recycler) {
        viewCache = new SparseArray<>();
        inner = recycler;
    }

    public void cacheView(View view) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        viewCache.put(params.getViewPosition(), view);
    }

    public void cacheView(int position, View view) {
        viewCache.put(position, view);
    }

    public void decacheView(int position) {
        viewCache.remove(position);
    }

    public android.view.View getCachedView(int position) {
        return viewCache.get(position);
    }

    public View getView(int position) {
        android.view.View child = getCachedView(position);
        if (child == null) {
            child = inner.getViewForPosition(position);
        }

        return child;
    }

    public void recycleCache() {
        for (int i = 0; i < viewCache.size(); i++) {
            inner.recycleView(viewCache.valueAt(i));
        }
    }
}
