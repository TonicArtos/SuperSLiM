package com.tonicartos.superslim;

import android.view.View;

public interface LayoutQueryHelper {

    int getStickyEdge();

    SectionData getSectionData(int position);

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

    SectionLayoutManager getSlm(SectionData sectionData);

    int getTop(View child);

    int getWidth();
}
