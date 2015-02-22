package com.tonicartos.superslim;

import android.view.View;

public class SectionData2 {

    public final int firstPosition;

    public final boolean hasHeader;

    public final int marginStart;

    public final int marginEnd;

    public final int minimumHeight;

    public final int sectionManager;

    public int headerWidth;

    public int headerHeight;

    LayoutManager.LayoutParams headerParams;

    public SectionData2(LayoutManager lm, View first) {
        headerParams = (LayoutManager.LayoutParams) first.getLayoutParams();

        if (headerParams.isHeader) {
            headerWidth = lm.getDecoratedMeasuredWidth(first);
            headerHeight = lm.getDecoratedMeasuredHeight(first);

            if (headerParams.headerStartMarginIsAuto) {
                marginStart = headerWidth + lm.getPaddingStart();
            } else {
                marginStart = headerParams.headerMarginStart + lm.getPaddingStart();
            }
            if (headerParams.headerEndMarginIsAuto) {
                marginEnd = headerWidth + lm.getPaddingEnd();
            } else {
                marginEnd = headerParams.headerMarginEnd + lm.getPaddingEnd();
            }
        } else {
            marginStart = lm.getPaddingStart();
            marginEnd = lm.getPaddingEnd();
        }

        hasHeader = headerParams.isHeader;

        minimumHeight = hasHeader ? headerHeight : 0;

        firstPosition = headerParams.getTestedFirstPosition();

        sectionManager = headerParams.sectionManager;
    }
}
