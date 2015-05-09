package com.tonicartos.superslim;

import android.text.TextUtils;
import android.view.View;

public class SectionData {

    public final int firstPosition;

    public final boolean hasHeader;

    public final int minimumHeight;

    public final String sectionManager;

    public final int sectionManagerKind;

    public final int headerWidth;

    public final int headerHeight;

    public final int contentEnd;

    public final int contentStart;

    final int marginStart;

    final int marginEnd;

    LayoutManager.LayoutParams headerParams;

    public SectionData(LayoutManager lm, View first) {
        final int paddingStart = lm.getPaddingStart();
        final int paddingEnd = lm.getPaddingEnd();

        headerParams = (LayoutManager.LayoutParams) first.getLayoutParams();

        if (headerParams.isHeader) {
            headerWidth = lm.getDecoratedMeasuredWidth(first);
            headerHeight = lm.getDecoratedMeasuredHeight(first);

            if (!headerParams.isHeaderInline() || headerParams.isHeaderOverlay()) {
                minimumHeight = headerHeight;
            } else {
                minimumHeight = 0;
            }

            if (headerParams.headerStartMarginIsAuto) {
                if (headerParams.isHeaderStartAligned() && !headerParams.isHeaderOverlay()) {
                    marginStart = headerWidth;
                } else {
                    marginStart = 0;
                }
            } else {
                marginStart = headerParams.headerMarginStart;
            }
            if (headerParams.headerEndMarginIsAuto) {
                if (headerParams.isHeaderEndAligned() && !headerParams.isHeaderOverlay()) {
                    marginEnd = headerWidth;
                } else {
                    marginEnd = 0;
                }
            } else {
                marginEnd = headerParams.headerMarginEnd;
            }
        } else {
            minimumHeight = 0;
            headerHeight = 0;
            headerWidth = 0;
            marginStart = headerParams.headerMarginStart;
            marginEnd = headerParams.headerMarginEnd;
        }

        contentEnd = marginEnd + paddingEnd;
        contentStart = marginStart + paddingStart;

        hasHeader = headerParams.isHeader;

        firstPosition = headerParams.getTestedFirstPosition();

        sectionManager = headerParams.sectionManager;
        sectionManagerKind = headerParams.sectionManagerKind;
    }

    public int getTotalMarginWidth() {
        return marginEnd + marginStart;
    }

    public boolean sameSectionManager(LayoutManager.LayoutParams params) {
        return params.sectionManagerKind == sectionManagerKind ||
                TextUtils.equals(params.sectionManager, sectionManager);
    }
}
