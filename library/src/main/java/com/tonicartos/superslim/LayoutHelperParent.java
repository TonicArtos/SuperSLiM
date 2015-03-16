package com.tonicartos.superslim;

import android.view.View;

interface LayoutHelperParent extends LayoutQueryHelper, LayoutModifierHelper {

    void measureHeader(View header, int widthUsed, int heightUsed);
}
