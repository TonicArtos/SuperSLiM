package com.tonicartos.superslim;

import android.support.annotation.NonNull;

public class SectionData {

    private final int mMinimumHeight;

    private final boolean mHasHeader;

    private int mLayoutId;

    private int mHeaderHeight;

    private int mAnchorPosition;

    private int mMarkerLine;

    private int mFirstPosition;

    private LayoutManager.Direction mFillDirection;

    private int mHeaderMarginStart;

    private int mContentMarginStart;

    private int mContentMarginEnd;

    private int mHeaderMarginEnd;

    public SectionData(LayoutManager lm, LayoutState state,
            LayoutManager.Direction direction, int anchorPosition, int markerLine) {

        mMarkerLine = markerLine;
        mAnchorPosition = anchorPosition;
        mFillDirection = direction;

        final LayoutState.View anchorView = state.getView(anchorPosition);
        mFirstPosition = (anchorView.getLayoutParams()).getTestedFirstPosition();
        final LayoutState.View firstView;
        if (mFirstPosition != anchorPosition) {
            state.recycleView(anchorView);
            firstView = state.getView(mFirstPosition);
        } else {
            firstView = anchorView;
        }

        LayoutManager.LayoutParams params = firstView.getLayoutParams();
        mLayoutId = params.layoutId;
        mHeaderMarginStart = params.headerMarginStart;
        mHeaderMarginEnd = params.headerMarginEnd;
        mContentMarginStart = mHeaderMarginStart + lm.getPaddingStart();
        mContentMarginEnd = mHeaderMarginEnd + lm.getPaddingEnd();

        if (params.isHeader) {
            lm.measureHeader(firstView);
            mHeaderHeight = lm.getDecoratedMeasuredHeight(firstView.view);
            if (params.headerStartMarginIsAuto) {
                if (params.isHeaderStartAligned() && !params.isHeaderOverlay()) {
                    mHeaderMarginStart = lm.getDecoratedMeasuredWidth(firstView.view);
                } else {
                    mHeaderMarginStart = 0;
                }
            }
            if (params.headerEndMarginIsAuto) {
                if (params.isHeaderEndAligned() && !params.isHeaderOverlay()) {
                    mHeaderMarginEnd = lm.getDecoratedMeasuredWidth(firstView.view);
                } else {
                    mHeaderMarginEnd = 0;
                }
            }
            if (params.isHeaderInline() && !params.isHeaderOverlay() && (
                    direction == LayoutManager.Direction.END ||
                            (direction == LayoutManager.Direction.NONE &&
                                    mAnchorPosition == mFirstPosition))) {
                mMarkerLine += mHeaderHeight;
            }

            if (params.isHeaderInline() && !params.isHeaderOverlay()) {
                mMinimumHeight = 0;
            } else {
                mMinimumHeight = mHeaderHeight;
            }
            mHasHeader = true;
        } else {
            mHasHeader = false;
            mMinimumHeight = 0;
        }
        state.recycleView(firstView);

        if (mAnchorPosition == mFirstPosition && mHasHeader) {
            // Bump past header.
            mAnchorPosition += 1;
        }
    }

    public int getAnchorPosition() {
        return mAnchorPosition;
    }

    public int getContentMarginEnd() {
        return mContentMarginEnd;
    }

    public int getContentMarginStart() {
        return mContentMarginStart;
    }

    public int getFirstPosition() {
        return mFirstPosition;
    }

    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    public int getHeaderMarginEnd() {
        return mHeaderMarginEnd;
    }

    public int getHeaderMarginStart() {
        return mHeaderMarginStart;
    }

    public int getLayoutId() {
        return mLayoutId;
    }

    public int getMarkerLine() {
        return mMarkerLine;
    }

    public int getMinimumHeight() {
        return mMinimumHeight;
    }

    public LayoutState.View getSectionHeader(LayoutState state) {
        return mHasHeader ? state.getView(mFirstPosition) : null;
    }

    public boolean isFillDirectionEnd() {
        return mFillDirection == LayoutManager.Direction.END;
    }

    public boolean isFillDirectionNone() {
        return mFillDirection == LayoutManager.Direction.NONE;
    }

    public boolean isFillDirectionStart() {
        return mFillDirection == LayoutManager.Direction.START;
    }

    public void loadMargins(@NonNull LayoutManager lm, @NonNull SectionLayoutManager manager) {
        int startMargin = manager.getHeaderStartMargin();
        int endMargin = manager.getHeaderEndMargin();

        if (startMargin >= 0) {
            mHeaderMarginStart = startMargin;
        }

        if (endMargin >= 0) {
            mHeaderMarginEnd = endMargin;
        }

        mContentMarginStart = mHeaderMarginStart + lm.getPaddingStart();
        mContentMarginEnd = mHeaderMarginEnd + lm.getPaddingEnd();
    }

    @Override
    public String toString() {
        return "SectionData\nLayoutId " + mLayoutId + "\nAnchor Position " + mAnchorPosition
                + "\nFirst Position " + mFirstPosition + "\nMinimum Height " + mMinimumHeight
                + "\nHeader Height " + mHeaderHeight + "\nHeader Margins " + mHeaderMarginStart
                + ":" + mHeaderMarginEnd + "\nContent Margins " + mContentMarginStart + ":"
                + mContentMarginEnd + "\nMarker Line " + mMarkerLine;
    }
}
