package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;

/**
 * State to track the current top mMarkerLine views are being mMarkerLine relative to.
 */
public class LayoutState {

    public static final int NO_HEADER_OFFSET = -1;

    private static final int MARGIN_AUTO = -1;

    public final RecyclerView.Recycler recycler;

    public final RecyclerView.State recyclerState;

    public final SparseArray<android.view.View> viewCache;

    private final RecyclerView.LayoutManager mLayoutManager;

    public LayoutState(RecyclerView.LayoutManager layoutManager, RecyclerView.Recycler recycler,
            RecyclerView.State recyclerState) {
        viewCache = new SparseArray<android.view.View>(layoutManager.getChildCount());
        this.recyclerState = recyclerState;
        this.recycler = recycler;
        mLayoutManager = layoutManager;
    }

    public void cacheView(int position, android.view.View view) {
        viewCache.put(position, view);
    }

    public void decacheView(int position) {
        viewCache.remove(position);
    }

    public void recycleCache() {
        for (int i = 0; i < viewCache.size(); i++) {
            recycler.recycleView(viewCache.valueAt(i));
        }
    }

    public void detachCachedViews() {
        for (int i = 0; i < viewCache.size(); i++) {
            mLayoutManager.detachView(viewCache.valueAt(i));
        }
    }

    public void cacheAllViews() {
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            final android.view.View child = mLayoutManager.getChildAt(i);
            final int childPosition = mLayoutManager.getPosition(child);
            cacheView(childPosition, child);
        }
    }

    public void detachAndCacheAllViews() {
        cacheAllViews();
        detachCachedViews();
    }

    public android.view.View getCachedView(int position) {
        return viewCache.get(position);
    }

    public View getView(int position) {
        android.view.View child = getCachedView(position);
        boolean wasCached = child != null;
        if (child == null) {
            child = recycler.getViewForPosition(position);
        }

        return new View(child, wasCached);
    }

    public static class View {

        public final android.view.View view;

        public final boolean wasCached;

        public View(android.view.View child, boolean wasCached) {
            this.view = child;
            this.wasCached = wasCached;
        }

        public LayoutManager.LayoutParams getLayoutParams() {
            return (LayoutManager.LayoutParams) view.getLayoutParams();
        }
    }
}
