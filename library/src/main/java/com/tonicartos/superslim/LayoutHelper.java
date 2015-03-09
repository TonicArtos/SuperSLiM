package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public interface LayoutHelper {

    void addView(View view, int index);

    void addView(View view);

    void detachAndScrapView(View child, Recycler recycler);

    void detachAndScrapViewAt(int index, Recycler recycler);

    void detachView(View child);

    void detachViewAt(int index);

    int getBottom(View child);

    View getChildAt(int index);

    int getChildCount();

    int getHeight();

    int getLayoutDirection();

    int getLeadingEdge();

    int getLeft(View child);

    int getMeasuredHeight(View v);

    int getMeasuredWidth(View v);

    int getPosition(View child);

    int getRight(View child);

    LayoutHelper getSubsectionLayoutHelper();

    int getTop(View child);

    int getWidth();

    void init(SectionData sd, int markerLine, int leadingEdge, int stickyEdge);

    void layoutChild(final View v, int l, int t, int r, int b);

    int layoutHeaderTowardsEnd(View header, int markerLine, RecyclerView.State state);

    int layoutHeaderTowardsStart(View header, int offset, int sectionTop, int sectionBottom,
            RecyclerView.State state);

    void measureChild(View child, int widthUsed, int heightUsed);

    void recycle();

    void removeAndRecycleView(View child, Recycler recycler);

    void removeAndRecycleViewAt(int index, Recycler recycler);

    void removeView(View child);

    void removeViewAt(int index);

    int translateFillResult(int markerLine);
}
