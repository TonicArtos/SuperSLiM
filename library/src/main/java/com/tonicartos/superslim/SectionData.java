package com.tonicartos.superslim;

import android.support.annotation.NonNull;

public class SectionData {

    private final int mMinimumHeight;

    private LayoutState.View mSectionHeader;

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
        mFirstPosition = (state.getView(anchorPosition).getLayoutParams()).getTestedFirstPosition();
        LayoutState.View firstView = state.getView(mFirstPosition);
        LayoutManager.LayoutParams params = firstView.getLayoutParams();
        mLayoutId = params.layoutId;
        mHeaderMarginStart = params.headerMarginStart;
        mHeaderMarginEnd = params.headerMarginEnd;
        mContentMarginStart = mHeaderMarginStart + lm.getPaddingStart();
        mContentMarginEnd = mHeaderMarginEnd + lm.getPaddingEnd();

        mMarkerLine = markerLine;
        mAnchorPosition = anchorPosition;
        mFillDirection = direction;

        mSectionHeader = firstView;
        if (params.isHeader) {
            lm.measureHeader(mSectionHeader);
            mHeaderHeight = lm.getDecoratedMeasuredHeight(mSectionHeader.view);
            if (params.headerStartMarginIsAuto) {
                if (params.isHeaderStartAligned() && !params.isHeaderOverlay()) {
                    mHeaderMarginStart = lm.getDecoratedMeasuredWidth(mSectionHeader.view);
                } else {
                    mHeaderMarginStart = 0;
                }
            }
            if (params.headerEndMarginIsAuto) {
                if (params.isHeaderEndAligned() && !params.isHeaderOverlay()) {
                    mHeaderMarginEnd = lm.getDecoratedMeasuredWidth(mSectionHeader.view);
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
        } else {
            state.recycleView(mSectionHeader);
            mSectionHeader = null;
            mMinimumHeight = 0;
        }

        if (mAnchorPosition == mFirstPosition && mSectionHeader != null) {
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

    public LayoutState.View getSectionHeader() {
        return mSectionHeader;
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
