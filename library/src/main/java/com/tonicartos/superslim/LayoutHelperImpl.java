package com.tonicartos.superslim;

import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayDeque;

class LayoutHelperImpl extends LayoutHelper implements LayoutHelperParent, LayoutTrimHelper {

    private static ArrayDeque<LayoutHelperImpl> sPool = new ArrayDeque<>(6);

    private static Rect sRect = new Rect();

    private int mHorizontalOffset;

    private int mVerticalOffset;

    private int mWidth;

    private int mLayoutDirection;

    private LayoutHelperParent mParent;

    private SectionData mSectionData;

    private int mLeadingEdge;

    private int mStickyEdge;

    LayoutHelperImpl(LayoutHelperParent parent) {
        setParent(parent);
    }

    static LayoutHelperImpl getLayoutHelperFromPool(LayoutHelperParent parent) {
        if (sPool.size() == 0) {
            return new LayoutHelperImpl(parent);
        }
        return sPool.pop().setParent(parent);
    }

    private static void returnToPool(LayoutHelperImpl lh) {
        sPool.push(lh);
    }

    @Override
    public void addView(View view, int index) {
        mParent.addView(view, index);
    }

    @Override
    public void addView(View view) {
        mParent.addView(view);
    }

    @Override
    public void detachAndScrapView(View child, Recycler recycler) {
        mParent.detachAndScrapView(child, recycler);
    }

    @Override
    public void detachAndScrapViewAt(int index, Recycler recycler) {
        mParent.detachAndScrapViewAt(index, recycler);
    }

    @Override
    public void detachView(View child) {
        mParent.detachView(child);
    }

    @Override
    public void detachViewAt(int index) {
        mParent.detachViewAt(index);
    }

    @Override
    public int getBottom(View child) {
        return mParent.getBottom(child);
    }

    @Override
    public View getChildAt(int index) {
        return mParent.getChildAt(index);
    }

    @Override
    public int getChildCount() {
        return mParent.getChildCount();
    }

    @Override
    public int getHeight() {
        return mParent.getHeight();
    }

    @Override
    public int getLayoutDirection() {
        return mLayoutDirection;
    }

    @Override
    public int getLeadingEdge() {
        return mLeadingEdge;
    }

    @Override
    public int getLeft(View child) {
        return mParent.getLeft(child);
    }

    @Override
    public int getMeasuredHeight(View v) {
        return mParent.getMeasuredHeight(v);
    }

    @Override
    public int getMeasuredWidth(View v) {
        return mParent.getMeasuredWidth(v);
    }

    @Override
    public int getPosition(View child) {
        return mParent.getPosition(child);
    }

    @Override
    public int getRight(View child) {
        return mParent.getRight(child);
    }

    @Override
    public SectionData getSectionData(int position) {
        if (mSectionData.containsItem(position)) {
            return mSectionData;
        }
        return mParent.getSectionData(position);
    }

    @Override
    public SectionLayoutManager getSlm(SectionData sectionData) {
        return mParent.getSlm(sectionData);
    }

    @Override
    public LayoutHelper getSubsectionLayoutHelper() {
        return getLayoutHelperFromPool(this);
    }

    @Override
    public LayoutTrimHelper getSubsectionLayoutTrimHelper() {
        return getLayoutHelperFromPool(this);
    }

    @Override
    public int getTop(View child) {
        return mParent.getTop(child);
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public void init(SectionData sd) {
        mSectionData = sd;
    }

    @Override
    public void init(SectionData sd, int markerLine, int leadingEdge, int stickyEdge) {
        init(sd, 0, 0, markerLine, leadingEdge, stickyEdge);
    }

    @Override
    public void init(SectionData sd, int horizontalOffset, int unavailableWidth, int markerLine,
            int leadingEdge,
            int stickyEdge) {
        mWidth = mParent.getWidth() - sd.startMarginWidth - sd.endMarginWidth - unavailableWidth;
        mSectionData = sd;
        mHorizontalOffset = mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR ?
                sd.startMarginWidth : sd.endMarginWidth;
        mHorizontalOffset += horizontalOffset;
        mVerticalOffset = markerLine;
        mLeadingEdge = leadingEdge - markerLine;
        mStickyEdge = stickyEdge - markerLine;
    }

    @Override
    public void layoutChild(final View v, int l, int t, int r, int b) {
        l += mHorizontalOffset;
        t += mVerticalOffset;
        r += mHorizontalOffset;
        b += mVerticalOffset;
        mParent.layoutChild(v, l, t, r, b);
    }

    @Override
    public void measureChild(View child, int widthUsed, int heightUsed) {
        widthUsed += mSectionData.startMarginWidth + mSectionData.endMarginWidth;
        mParent.measureChild(child, widthUsed, heightUsed);
    }

    @Override
    public void measureHeader(View header, int widthUsed, int heightUsed) {
        widthUsed += mSectionData.startMarginWidth + mSectionData.endMarginWidth;
        mParent.measureHeader(header, widthUsed, heightUsed);
    }

    @Override
    public void recycle() {
        reset();
        returnToPool(this);
    }

    @Override
    public void removeAndRecycleView(View child, Recycler recycler) {
        mParent.removeAndRecycleView(child, recycler);
    }

    @Override
    public void removeAndRecycleViewAt(int index, Recycler recycler) {
        mParent.removeAndRecycleViewAt(index, recycler);
    }

    @Override
    public void removeView(View child) {
        mParent.removeView(child);
    }

    @Override
    public void removeViewAt(int index) {
        mParent.removeViewAt(index);
    }

    @Override
    public int translateFillResult(int markerLine) {
        return markerLine + mVerticalOffset;
    }

    @Override
    int layoutHeaderTowardsEnd(final View header, int markerLine,
            final RecyclerView.State state) {
        LayoutManager.LayoutParams headerParams =
                (LayoutManager.LayoutParams) header.getLayoutParams();
        Rect r = setHeaderRectSides(sRect, headerParams, state);

        r.top = markerLine;
        r.bottom = r.top + mSectionData.headerHeight;

        if (headerParams.isHeaderInline() && !headerParams.isHeaderOverlay()) {
            markerLine = r.bottom;
        }

        if (headerParams.isHeaderSticky() && r.top < mStickyEdge) {
            r.top = mStickyEdge;
            r.bottom = r.top + mSectionData.headerHeight;
        }

        mParent.layoutChild(header, r.left, r.top, r.right, r.bottom);

        return markerLine;
    }

    @Override
    int layoutHeaderTowardsStart(final View header, final int offset, int sectionTop,
            final int sectionBottom, final RecyclerView.State state) {
        LayoutManager.LayoutParams headerParams =
                (LayoutManager.LayoutParams) header.getLayoutParams();
        Rect r = setHeaderRectSides(sRect, headerParams, state);

        if (headerParams.isHeaderInline() && !headerParams.isHeaderOverlay()) {
            r.bottom = sectionTop;
            r.top = r.bottom - mSectionData.headerHeight;
        } else if (offset <= 0) {
            r.top = sectionTop + offset;
            r.bottom = r.top + mSectionData.headerHeight;
        } else {
            r.bottom = mStickyEdge;
            r.top = r.bottom - mSectionData.headerHeight;
        }

        if (headerParams.isHeaderSticky() && r.top < mStickyEdge &&
                mSectionData.firstPosition != state.getTargetScrollPosition()) {
            r.top = mStickyEdge;
            r.bottom = r.top + mSectionData.headerHeight;
            if (headerParams.isHeaderInline() &&
                    !headerParams.isHeaderOverlay()) {
                // Push section top up to make room for when the sticky header sides into its final
                // place.
                sectionTop -= mSectionData.headerHeight;
            }
        }

        if (r.bottom > sectionBottom) {
            r.bottom = sectionBottom;
            r.top = r.bottom - mSectionData.headerHeight;
        }

        mParent.layoutChild(header, r.left, r.top, r.right, r.bottom);

        return Math.min(r.top, sectionTop);
    }

    void measureHeader(View header) {
        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) header.getLayoutParams();
        int widthUsed = 0; // Depends on header display param.
        if (!params.isHeaderInline() && !params.isHeaderOverlay()) {
            if (params.isHeaderStartAligned() &&
                    params.marginStart != LayoutManager.LayoutParams.MARGIN_AUTO) {
                widthUsed = mWidth - params.marginStart;
            } else if (params.isHeaderEndAligned() &&
                    params.marginEnd != LayoutManager.LayoutParams.MARGIN_AUTO) {
                widthUsed = mWidth - params.marginEnd;
            }
        }
        mParent.measureHeader(header, widthUsed, 0);
    }

    @Override
    void updateVerticalOffset(int additionalOffset) {
        mVerticalOffset += additionalOffset;
    }

    private void reset() {
        mWidth = 0;
        mSectionData = null;
        mHorizontalOffset = 0;
        mVerticalOffset = 0;
        mLeadingEdge = 0;
        mStickyEdge = 0;
    }

    private Rect setHeaderRectSides(Rect r, LayoutManager.LayoutParams headerParams,
            RecyclerView.State state) {
        if (headerParams.isHeaderEndAligned()) {
            // Position header from end edge.
            if (!headerParams.isHeaderOverlay() &&
                    headerParams.marginEnd != LayoutManager.LayoutParams.MARGIN_AUTO &&
                    mSectionData.endMarginWidth > 0) {
                // Position inside end margin.
                if (mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                    r.left = mWidth - mSectionData.endMarginWidth;
                    r.right = mWidth;
                } else {
                    r.right = mSectionData.endMarginWidth;
                    r.left = 0;
                }
            } else if (mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                r.right = getWidth();
                r.left = r.right - mSectionData.headerWidth;
            } else {
                r.left = 0;
                r.right = r.left + mSectionData.headerWidth;
            }
        } else if (headerParams.isHeaderStartAligned()) {
            // Position header from start edge.
            if (!headerParams.isHeaderOverlay() &&
                    headerParams.marginStart != LayoutManager.LayoutParams.MARGIN_AUTO &&
                    mSectionData.startMarginWidth > 0) {
                // Position inside start margin.
                if (mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                    r.right = mSectionData.startMarginWidth;
                    r.left = 0;
                } else {
                    r.left = mWidth - mSectionData.startMarginWidth;
                    r.right = mWidth;
                }
            } else if (mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                r.left = 0;
                r.right = r.left + mSectionData.headerWidth;
            } else {
                r.right = mWidth;
                r.left = r.right - mSectionData.headerWidth;
            }
        } else {
            // Header is not aligned to a directed edge and assumed to fill the width available.
            r.left = 0;
            r.right = mSectionData.headerWidth;
        }

        return r;
    }

    private LayoutHelperImpl setParent(LayoutHelperParent parent) {
        mParent = parent;
        mLayoutDirection = mParent.getLayoutDirection();
        return this;
    }
}
