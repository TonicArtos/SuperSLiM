package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public interface LayoutHelper {

    void addView(View view, int index);

    void addView(View view);

    int getHeight();

    int getLayoutDirection();

    int getLeadingEdge();

    int getMeasuredHeight(View v);

    int getMeasuredWidth(View v);

    LayoutHelper getSubsectionLayoutHelper();

    int getWidth();

    void init(SectionData sd, int markerLine, int leadingEdge, int stickyEdge);

    void layoutChild(final View v, int l, int t, int r, int b);

    int layoutHeaderTowardsEnd(View header, int markerLine, RecyclerView.State state);

    int layoutHeaderTowardsStart(View header, int offset, int sectionTop, int sectionBottom,
            RecyclerView.State state);

    void measureChild(View child, int widthUsed, int heightUsed);

    void recycle();
}
