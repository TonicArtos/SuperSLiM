package com.tonicartos.superslim;

import android.view.View;

public interface LayoutQueryHelper {

    int getBottom(View child);

    View getChildAt(int index);

    int getChildCount();

    int getHeight();

    int getLayoutDirection();

    int getLeft(View child);

    int getMeasuredHeight(View v);

    int getMeasuredWidth(View v);

    int getPosition(View child);

    int getRight(View child);

    SectionData getSectionData(int position);

    SectionLayoutManager getSlm(SectionData sectionData, LayoutQueryHelper helper);

    int getStickyEdge();

    int getTop(View child);

    int getWidth();

}
