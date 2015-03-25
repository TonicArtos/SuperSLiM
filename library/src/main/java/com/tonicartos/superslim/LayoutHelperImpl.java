package com.tonicartos.superslim;

import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayDeque;

class LayoutHelperImpl extends LayoutHelper implements LayoutHelper.Parent, LayoutTrimHelper {

    private static ArrayDeque<LayoutHelperImpl> sPool = new ArrayDeque<>(6);

    private static Rect sRect = new Rect();

    private int mChildHorizontalOffset;

    private int mVerticalOffset;

    private int mWidth;

    private int mLayoutDirection;

    private Parent mParent;

    private SectionData mSectionData;

    private int mLeadingEdge;

    private int mStickyEdge;

    private int mTrimEdge;

    private int mHeaderHorizontalOffset;

    private int mUnavailableWidth;

    LayoutHelperImpl(Parent parent) {
        setParent(parent);
    }

    static LayoutHelperImpl getLayoutHelperFromPool(Parent parent) {
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
//        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) view.getLayoutParams();
//        Log.d("add view", "position " + params.getViewPosition() + " index " + index);
        mParent.addView(view, index);
    }

    @Override
    public void addView(View view) {
//        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) view.getLayoutParams();
//        Log.d("add view", "position " + params.getViewPosition());
        mParent.addView(view);
    }

    @Override
    public void attachView(View view, int index) {
//        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) view.getLayoutParams();
//        Log.d("attach view", "position " + params.getViewPosition() + " index " + index);
        mParent.attachView(view, index);
    }

    @Override
    public void attachView(View view) {
//        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) view.getLayoutParams();
//        Log.d("attach view", "position " + params.getViewPosition());
        mParent.attachView(view);
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
        return mParent.getBottom(child) - mVerticalOffset;
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
        return mParent.getLeft(child) - mChildHorizontalOffset;
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
        return mParent.getRight(child) - mChildHorizontalOffset;
    }

    @Override
    public SectionData getSectionData(int position) {
        if (mSectionData.containsItem(position)) {
            return mSectionData;
        }
        return mParent.getSectionData(position);
    }

    @Override
    public SectionLayoutManager getSlm(SectionData sectionData, LayoutQueryHelper helper) {
        return mParent.getSlm(sectionData, helper);
    }

    @Override
    public int getStickyEdge() {
        return mStickyEdge;
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
        return mParent.getTop(child) - mVerticalOffset;
    }

    @Override
    public int getTrimEdge() {
        return mTrimEdge;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public void init(SectionData sd, int trimEdge, int newStickyEdge) {
        mWidth = mParent.getWidth() - sd.startMarginWidth - sd.endMarginWidth;
        mSectionData = sd;
        mStickyEdge = newStickyEdge;
        mTrimEdge = trimEdge;
    }

    @Override
    public void init(SectionData sd, int markerLine, int leadingEdge, int stickyEdge) {
        init(sd, 0, 0, markerLine, leadingEdge, stickyEdge);
    }

    @Override
    public void init(SectionData sd, int horizontalOffset, int unavailableWidth, int markerLine,
            int leadingEdge, int stickyEdge) {
        mWidth = mParent.getWidth() - sd.startMarginWidth - sd.endMarginWidth - unavailableWidth;
        mUnavailableWidth = unavailableWidth;

        mSectionData = sd;
        mChildHorizontalOffset = mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR ?
                sd.startMarginWidth : sd.endMarginWidth;
        mChildHorizontalOffset += horizontalOffset;
        mHeaderHorizontalOffset = horizontalOffset;
        mVerticalOffset = markerLine;
        mLeadingEdge = leadingEdge - markerLine;
        mStickyEdge = stickyEdge - markerLine;
    }

    @Override
    public void layoutChild(final View v, int l, int t, int r, int b) {
        l += mChildHorizontalOffset;
        t += mVerticalOffset;
        r += mChildHorizontalOffset;
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

    public void updateMarkerLine(int oldMarkerLine, int newMarkerLine) {
        mVerticalOffset = newMarkerLine;
        mLeadingEdge += oldMarkerLine - newMarkerLine;
        mStickyEdge += oldMarkerLine - newMarkerLine;
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

        layoutHeader(header, r.left, r.top, r.right, r.bottom);

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

        layoutHeader(header, r.left, r.top, r.right, r.bottom);

        return Math.min(r.top, sectionTop);
    }

    void measureHeader(View header) {
        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) header.getLayoutParams();
        int widthUsed = 0; // Depends on header display param.
        if (!params.isHeaderInline() && !params.isHeaderOverlay()) {
            if (params.isHeaderStartAligned() &&
                    params.marginStart != LayoutManager.LayoutParams.MARGIN_AUTO) {
                widthUsed = mParent.getWidth() - params.marginStart;
            } else if (params.isHeaderEndAligned() &&
                    params.marginEnd != LayoutManager.LayoutParams.MARGIN_AUTO) {
                widthUsed = mParent.getWidth() - params.marginEnd;
            }
        }
        mParent.measureHeader(header, widthUsed, 0);
    }

    @Override
    void updateVerticalOffset(int additionalOffset) {
        mVerticalOffset += additionalOffset;
    }

    private void layoutHeader(View header, int l, int t, int r, int b) {
        l += mHeaderHorizontalOffset;
        t += mVerticalOffset;
        r += mHeaderHorizontalOffset;
        b += mVerticalOffset;
        mParent.layoutChild(header, l, t, r, b);
    }

    private void reset() {
        mWidth = 0;
        mSectionData = null;
        mHeaderHorizontalOffset = 0;
        mChildHorizontalOffset = 0;
        mVerticalOffset = 0;
        mLeadingEdge = 0;
        mStickyEdge = 0;
        mTrimEdge = 0;
    }

    private Rect setHeaderRectSides(Rect r, LayoutManager.LayoutParams headerParams,
            RecyclerView.State state) {
        int width = mParent.getWidth();
        if (headerParams.isHeaderEndAligned()) {
            // Position header from end edge.
            if (!headerParams.isHeaderOverlay() &&
                    headerParams.marginEnd != LayoutManager.LayoutParams.MARGIN_AUTO &&
                    mSectionData.endMarginWidth > 0) {
                // Position inside end margin.
                if (mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                    r.left = width - mSectionData.endMarginWidth;
                    r.right = width;
                } else {
                    r.right = mSectionData.endMarginWidth;
                    r.left = 0;
                }
            } else if (mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                r.right = width;
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
                    r.left = width - mSectionData.startMarginWidth;
                    r.right = width;
                }
            } else if (mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                r.left = 0;
                r.right = r.left + mSectionData.headerWidth;
            } else {
                r.right = width;
                r.left = r.right - mSectionData.headerWidth;
            }
        } else {
            // Header is not aligned to a directed edge and assumed to fill the width available.
            r.left = 0;
            r.right = mSectionData.headerWidth;
        }

        return r;
    }

    private LayoutHelperImpl setParent(Parent parent) {
        mParent = parent;
        mLayoutDirection = mParent.getLayoutDirection();
        return this;
    }
}
