package com.tonicartos.superslim;

import android.view.View;

interface LayoutModifierHelper {

    void addView(View view, int index);

    void addView(View view);

    void attachView(View header, int i);

    void attachView(View header);

    void detachAndScrapView(View child, Recycler recycler);

    void detachAndScrapViewAt(int index, Recycler recycler);

    void detachView(View child);

    void detachViewAt(int index);

    void layoutChild(View v, int l, int t, int r, int b);

    void measureChild(View child, int widthUsed, int heightUsed);

    void removeAndRecycleView(View child, Recycler recycler);

    void removeAndRecycleViewAt(int index, Recycler recycler);

    void removeView(View child);

    void removeViewAt(int index);
}
