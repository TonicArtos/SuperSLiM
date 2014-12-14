package com.tonic.sectionlayoutmanager;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;

/**
 * State to track the current top markerLine views are being markerLine relative to.
 */
public class LayoutState {

    public static final int NO_HEADER_OFFSET = -1;

    private static final int MARGIN_AUTO = -1;

    public final RecyclerView.Recycler recycler;

    public final RecyclerView.State recyclerState;

    public final SparseArray<android.view.View> viewCache;

    private final RecyclerView.LayoutManager mLayoutManager;

    public int headerOverlap;

    public int markerLine;

    public int sectionFirstPosition;

    public int section;

    public int headerOffset = NO_HEADER_OFFSET;

    public SectionLayoutManager.Direction direction;

    public int headerStartMargin;

    public int headerEndMargin;

    public LayoutState(RecyclerView.LayoutManager layoutManager, RecyclerView.Recycler recycler,
            RecyclerView.State recyclerState, int cacheSize) {
        viewCache = new SparseArray<android.view.View>(cacheSize);
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
            Log.d("Cache views", "Child " + i);
            Log.d("Cache views", "Position " + childPosition);
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

    public void setSectionData(LayoutManager.LayoutParams lp) {
        section = lp.section;
        sectionFirstPosition = lp.sectionFirstPosition;
        headerStartMargin = lp.headerStartMargin;
        headerEndMargin = lp.headerEndMargin;
    }

    public void setSectionData(SectionLayoutManager sectionManager) {
        int startMargin = sectionManager.getHeaderStartMargin();
        int endMargin = sectionManager.getHeaderEndMargin();

        if (startMargin >= 0) {
            headerStartMargin = startMargin;
        }

        if (endMargin >= 0) {
            headerEndMargin = endMargin;
        }
    }

    public boolean isDirectionStart() {
        return direction == SectionLayoutManager.Direction.START;
    }

    public boolean isDirectionEnd() {
        return direction == SectionLayoutManager.Direction.END;
    }

    public void updateSectionData(View sectionHeader) {
        LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) sectionHeader.view.getLayoutParams();

        if (lp.headerStartMarginIsAuto) {
            if (lp.headerAlignment == LayoutManager.HEADER_ALIGN_START) {
                headerStartMargin = mLayoutManager.getDecoratedMeasuredWidth(sectionHeader.view);
            } else {
                headerStartMargin = 0;
            }
        }
        if (lp.headerEndMarginIsAuto) {
            if (lp.headerAlignment == LayoutManager.HEADER_ALIGN_END) {
                headerEndMargin = mLayoutManager.getDecoratedMeasuredWidth(sectionHeader.view);
            } else {
                headerEndMargin = 0;
            }
        }
    }

    public static class View {

        public final android.view.View view;

        public final boolean wasCached;

        public View(android.view.View child, boolean wasCached) {
            this.view = child;
            this.wasCached = wasCached;
        }

    }
}
