package com.tonicartos.superslim;

import android.view.View;

public interface LayoutHelperParent {

    void addView(View view, int index);

    void addView(View view);

    int getHeight();

    int getLayoutDirection();

    int getMeasuredHeight(View v);

    int getMeasuredWidth(View v);

    int getWidth();

    void layoutChild(View v, int l, int t, int r, int b);

    void measureChild(View child, int widthUsed, int heightUsed);

    void measureHeader(View header, int widthUsed, int heightUsed);
}
