package com.tonicartos.superslim;

import android.view.View;

public interface LayoutHelperParent {

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

    int getLeft(View child);

    int getMeasuredHeight(View v);

    int getMeasuredWidth(View v);

    int getPosition(View child);

    int getRight(View child);

    int getTop(View child);

    int getWidth();

    void layoutChild(View v, int l, int t, int r, int b);

    void measureChild(View child, int widthUsed, int heightUsed);

    void measureHeader(View header, int widthUsed, int heightUsed);

    void removeAndRecycleView(View child, Recycler recycler);

    void removeAndRecycleViewAt(int index, Recycler recycler);

    void removeView(View child);

    void removeViewAt(int index);
}
