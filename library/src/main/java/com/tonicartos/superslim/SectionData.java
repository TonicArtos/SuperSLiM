package com.tonicartos.superslim;

/**
 * Created by tonic on 18/12/14.
 */
public class SectionData {

    private final int mMinimumHeight;

    private LayoutState.View mSectionHeader;

    private int mSection;

    private int mHeaderHeight;

    private int mAnchorPosition;

    private int mMarkerLine;

    private int mFirstPosition;

    private LayoutManager.Direction mFillDirection;

    private int mHeaderStartMargin;

    private int mContentStartMargin;

    private int mContentEndMargin;

    private int mHeaderEndMargin;

    public SectionData(LayoutManager lm, LayoutState state,
            LayoutManager.Direction direction, int anchorPosition, int markerLine) {
        LayoutState.View firstView = state
                .getView(
                        (state.getView(anchorPosition).getLayoutParams()).sectionFirstPosition);
        LayoutManager.LayoutParams params = firstView
                .getLayoutParams();
        mSection = params.section;
        mFirstPosition = params.sectionFirstPosition;
        mHeaderStartMargin = params.headerStartMargin;
        mHeaderEndMargin = params.headerEndMargin;
        mContentStartMargin = mHeaderStartMargin + lm.getPaddingLeft();
        mContentEndMargin = mHeaderEndMargin + lm.getPaddingRight();

        mMarkerLine = markerLine;
        mAnchorPosition = anchorPosition;
        mFillDirection = direction;

        mSectionHeader = firstView;
        if (params.isHeader) {
            lm.measureHeader(mSectionHeader);
            mHeaderHeight = lm.getDecoratedMeasuredHeight(mSectionHeader.view);
            if (params.headerStartMarginIsAuto) {
                if (params.isHeaderStartAligned() && !params.isHeaderOverlay()) {
                    mHeaderStartMargin = lm.getDecoratedMeasuredWidth(mSectionHeader.view);
                } else {
                    mHeaderStartMargin = 0;
                }
            }
            if (params.headerEndMarginIsAuto) {
                if (params.isHeaderEndAligned() && !params.isHeaderOverlay()) {
                    mHeaderEndMargin = lm.getDecoratedMeasuredWidth(mSectionHeader.view);
                } else {
                    mHeaderEndMargin = 0;
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
            mSectionHeader = null;
            mMinimumHeight = 0;
        }

        if (mAnchorPosition == mFirstPosition && mSectionHeader != null) {
            // Bump past header.
            mAnchorPosition += 1;
        }

        mContentStartMargin = mHeaderStartMargin + lm.getPaddingLeft();
        mContentEndMargin = mHeaderEndMargin + lm.getPaddingRight();
    }

    public int getAnchorPosition() {
        return mAnchorPosition;
    }

    public int getContentEndMargin() {
        return mContentEndMargin;
    }

    public int getContentStartMargin() {
        return mContentStartMargin;
    }

    public int getFirstPosition() {
        return mFirstPosition;
    }

    public int getHeaderEndMargin() {
        return mHeaderEndMargin;
    }

    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    public int getHeaderStartMargin() {
        return mHeaderStartMargin;
    }

    public int getMarkerLine() {
        return mMarkerLine;
    }

    public int getMinimumHeight() {
        return mMinimumHeight;
    }

    public int getSection() {
        return mSection;
    }

    public LayoutState.View getSectionHeader() {
        return mSectionHeader;
    }

    public boolean isFillDirectionEnd() {
        return mFillDirection == LayoutManager.Direction.END;
    }

    public boolean isFillDirectionNON() {
        return mFillDirection == LayoutManager.Direction.NONE;
    }

    public boolean isFillDirectionStart() {
        return mFillDirection == LayoutManager.Direction.START;
    }

    public SectionLayoutManager loadManager(LayoutManager lm, LayoutManager.SlmFactory slmFactory) {
        SectionLayoutManager sectionManager = slmFactory.getSectionLayoutManager(lm, mSection);
        if (sectionManager == null) {
            sectionManager = new LinearSectionLayoutManager(lm);
        }

        int startMargin = sectionManager.getHeaderStartMargin();
        int endMargin = sectionManager.getHeaderEndMargin();

        if (startMargin >= 0) {
            mHeaderStartMargin = startMargin;
        }

        if (endMargin >= 0) {
            mHeaderEndMargin = endMargin;
        }

        mContentStartMargin = mHeaderStartMargin + lm.getPaddingLeft();
        mContentEndMargin = mHeaderEndMargin + lm.getPaddingRight();

        return sectionManager;
    }

    @Override
    public String toString() {
        return "SectionData\nSection " + mSection + "\nAnchor Position " + mAnchorPosition
                + "\nFirst Position " + mFirstPosition + "\nMinimum Height " + mMinimumHeight
                + "\nHeader Height " + mHeaderHeight + "\nHeader Margins " + mHeaderStartMargin
                + ":" + mHeaderEndMargin + "\nContent Margins " + mContentStartMargin + ":"
                + mContentEndMargin + "\nMarker Line " + mMarkerLine;
    }
}
