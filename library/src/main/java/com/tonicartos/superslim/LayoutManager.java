package com.tonicartos.superslim;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

/**
 * A LayoutManager that lays out mSection headers with optional stickiness and uses a map of
 * sections to view layout managers to layout items.
 */
public class LayoutManager extends RecyclerView.LayoutManager {

    private static final int NO_POSITION_REQUEST = -1;

    private int mRequestPosition = NO_POSITION_REQUEST;

    private Rect mRect = new Rect();

    private int mRequestPositionOffset = 0;

    private boolean mDisableStickyHeaderDisplay = false;

    private SparseArrayCompat<SectionLayoutManager> mSectionLayouts = new SparseArrayCompat<>();

    private boolean mSmoothScrollEnabled = true;

    /**
     * Find the position of the first completely visible item.
     *
     * @return Position of first completely visible item.
     */
    public View findFirstCompletelyVisibleItem() {
        SectionData sd = new SectionData(this, getChildAt(0));
        final SectionLayoutManager slm = getSectionLayoutManager(sd);

        View firstVisibleView = slm.getFirstCompletelyVisibleView(sd.firstPosition, false);

        int firstVisiblePosition = getPosition(firstVisibleView);
        if (firstVisiblePosition == sd.firstPosition ||
                firstVisiblePosition > sd.firstPosition + 1) {
            // Header doesn't matter.
            return firstVisibleView;
        }

        // Maybe the header is completely visible.
        View first = findAttachedHeaderOrFirstViewForSection(sd.firstPosition, 0, Direction.START);
        if (first == null) {
            return firstVisibleView;
        }

        final int topEdge = getClipToPadding() ? getPaddingTop() : 0;
        final int bottomEdge = getClipToPadding() ? getHeight() - getPaddingBottom() : getHeight();

        final int firstTop = getDecoratedTop(first);
        final int firstBottom = getDecoratedBottom(first);

        if (firstTop < topEdge || bottomEdge < firstBottom) {
            return firstVisibleView;
        }

        if (firstBottom <= getDecoratedTop(firstVisibleView)) {
            return first;
        }

        LayoutParams firstParams = (LayoutParams) first.getLayoutParams();
        if ((!firstParams.isHeaderInline() || firstParams.isHeaderOverlay())
                && firstTop == getDecoratedTop(firstVisibleView)) {
            return first;
        }

        return firstVisibleView;
    }

    /**
     * Find the position of the first completely visible item.
     *
     * @return Position of first completely visible item.
     */
    public int findFirstCompletelyVisibleItemPosition() {
        return getPosition(findFirstCompletelyVisibleItem());
    }

    /**
     * Find the position of the first visible item.
     *
     * @return Position of first visible item.
     */
    public View findFirstVisibleItem() {
        SectionData sd = new SectionData(this, getChildAt(0));
        final SectionLayoutManager slm = getSectionLayoutManager(sd);
        View firstVisibleView = slm.getFirstVisibleView(sd.firstPosition, false);
        int position = getPosition(firstVisibleView);
        if (position > sd.firstPosition + 1 || position == sd.firstPosition) {
            return firstVisibleView;
        }
        View first = findAttachedHeaderOrFirstViewForSection(sd.firstPosition, 0, Direction.START);
        if (first == null) {
            return firstVisibleView;
        }

        if (getDecoratedBottom(first) <= getDecoratedTop(firstVisibleView)) {
            return first;
        }

        LayoutParams firstParams = (LayoutParams) first.getLayoutParams();
        if ((!firstParams.isHeaderInline() || firstParams.isHeaderOverlay())
                && getDecoratedTop(first) == getDecoratedTop(firstVisibleView)) {
            return first;
        }

        return firstVisibleView;
    }

    /**
     * Find the position of the first visible item.
     *
     * @return Position of first visible item.
     */
    public int findFirstVisibleItemPosition() {
        return getPosition(findFirstVisibleItem());
    }

    /**
     * Find the position of the last completely visible item.
     *
     * @return Position of last completely visible item.
     */
    public View findLastCompletelyVisibleItem() {
        SectionData sd = new SectionData(this, getChildAt(getChildCount() - 1));
        final SectionLayoutManager slm = getSectionLayoutManager(sd);

        return slm.getLastCompletelyVisibleView(sd.firstPosition);
    }

    /**
     * Find the position of the last completely visible item.
     *
     * @return Position of last completely visible item.
     */
    public int findLastCompletelyVisibleItemPosition() {
        SectionData sd = new SectionData(this, getChildAt(getChildCount() - 1));
        final SectionLayoutManager slm = getSectionLayoutManager(sd);

        return slm.findLastCompletelyVisibleItemPosition(sd.firstPosition);
    }

    /**
     * Find the position of the last visible item.
     *
     * @return Position of last visible item.
     */
    public View findLastVisibleItem() {
        SectionData sd = new SectionData(this, getChildAt(getChildCount() - 1));
        final SectionLayoutManager slm = getSectionLayoutManager(sd);

        return slm.getLastVisibleView(sd.firstPosition);
    }

    /**
     * Find the position of the last visible item.
     *
     * @return Position of last visible item.
     */
    public int findLastVisibleItemPosition() {
        SectionData sd = new SectionData(this, getChildAt(getChildCount() - 1));
        final SectionLayoutManager slm = getSectionLayoutManager(sd);

        return slm.findLastVisibleItemPosition(sd.firstPosition);
    }

    public boolean isSmoothScrollEnabled() {
        return mSmoothScrollEnabled;
    }

    public void setSmoothScrollEnabled(boolean smoothScrollEnabled) {
        mSmoothScrollEnabled = smoothScrollEnabled;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int itemCount = state.getItemCount();
        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        final int requestedPosition;
        final int borderLine;

        if (mRequestPosition != NO_POSITION_REQUEST) {
            requestedPosition = mRequestPosition;
            mRequestPosition = NO_POSITION_REQUEST;
            borderLine = mRequestPositionOffset;
            mRequestPositionOffset = 0;
        } else {
            View anchorView = getAnchorChild();
            requestedPosition = anchorView == null ? 0 : getPosition(anchorView);
            borderLine = getBorderLine(anchorView, Direction.END);
        }

        detachAndScrapAttachedViews(recycler);

        LayoutState layoutState = new LayoutState(this, recycler, state);
        layoutChildren(requestedPosition, borderLine, layoutState);

        // Check
    }

    /**
     * Layout views from the top.
     *
     * @param anchorPosition Position to start laying out from.
     * @param state          Layout state.  @return Line to which content has been filled. If the
     *                       line is before the leading edge then the end of the data set has been
     */
    private int layoutChildren(int anchorPosition, int borderLine, LayoutState state) {
        final int height = getHeight();

        final LayoutState.View anchor = state.getView(anchorPosition);
        state.cacheView(anchorPosition, anchor.view);

        final int sfp = anchor.getLayoutParams().getTestedFirstPosition();
        final LayoutState.View first = state.getView(sfp);
        measureHeader(first.view);
        state.cacheView(sfp, first.view);

        final SectionData sd = new SectionData(this, first.view);

        final SectionLayoutManager slm = getSectionLayoutManager(sd);
        // Layout header
        int markerLine = borderLine;
        int contentPosition = anchorPosition;
        if (sd.hasHeader && anchorPosition == sd.firstPosition) {
            markerLine = layoutHeaderTowardsEnd(first.view, borderLine, sd, state);
            contentPosition += 1;
        }

        // Layout first section to end.
        markerLine = slm.fillToEnd(height, markerLine, contentPosition, sd, state);

        if (sd.hasHeader && anchorPosition != sd.firstPosition) {
            int offset = slm.computeHeaderOffset(contentPosition, sd, state);
            layoutHeaderTowardsStart(first.view, 0, borderLine, offset, markerLine, sd, state);
        } else {
            markerLine = Math.max(markerLine, getDecoratedBottom(first.view));
        }

        if (sd.hasHeader && getDecoratedBottom(first.view) > 0) {
            addView(first.view);
            state.decacheView(sd.firstPosition);
        }

        // Layout the rest.
        markerLine = fillNextSectionToEnd(height, markerLine, state);

        return markerLine;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        final LayoutParams newLp = new LayoutParams(lp);
        newLp.width = LayoutParams.MATCH_PARENT;
        newLp.height = LayoutParams.MATCH_PARENT;
        newLp.init(lp);
        return newLp;
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        int numChildren = getChildCount();
        if (numChildren == 0) {
            return 0;
        }

        LayoutState layoutState = new LayoutState(this, recycler, state);

        final Direction direction = dy > 0 ? Direction.END : Direction.START;
        final boolean isDirectionEnd = direction == Direction.END;

        final int leadingEdge = isDirectionEnd ? getHeight() + dy : dy;
        final int fillEdge = fillUntil(leadingEdge, direction, layoutState);

        final int delta;
        if (isDirectionEnd) {
            // Add padding so we scroll to inset area at scroll end.
            int fillDelta = fillEdge - getHeight() + getPaddingBottom();
            delta = fillDelta < dy ? fillDelta : dy;
        } else {
            int fillDelta = fillEdge - getPaddingTop();
            delta = fillDelta > dy ? fillDelta : dy;
        }

        if (delta != 0) {
            offsetChildrenVertical(-delta);

            trimTail(isDirectionEnd ? Direction.START : Direction.END, layoutState);
        }

        layoutState.recycleCache();

        return delta;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public void scrollToPosition(int position) {
        if (position < 0 || getItemCount() <= position) {
            Log.e("SuperSLiM.LayoutManager", "Ignored scroll to " + position +
                    " as it is not within the item range 0 - " + getItemCount());
            return;
        }

        mRequestPosition = position;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(final RecyclerView recyclerView, RecyclerView.State state,
            final int position) {
        if (position < 0 || getItemCount() <= position) {
            Log.e("SuperSLiM.LayoutManager", "Ignored smooth scroll to " + position +
                    " as it is not within the item range 0 - " + getItemCount());
            return;
        }

        // Temporarily disable sticky headers.
        mDisableStickyHeaderDisplay = true;
        requestLayout();

        recyclerView.getHandler().post(new Runnable() {
            @Override
            public void run() {
                LinearSmoothScroller smoothScroller = new LinearSmoothScroller(
                        recyclerView.getContext()) {
                    @Override
                    protected void onChildAttachedToWindow(View child) {
                        super.onChildAttachedToWindow(child);
                    }

                    @Override
                    protected void onStop() {
                        super.onStop();
                        // Turn sticky headers back on.
                        mDisableStickyHeaderDisplay = false;
                    }

                    @Override
                    protected int getVerticalSnapPreference() {
                        return LinearSmoothScroller.SNAP_TO_START;
                    }

                    @Override
                    public int calculateDyToMakeVisible(View view, int snapPreference) {
                        final RecyclerView.LayoutManager layoutManager = getLayoutManager();
                        if (!layoutManager.canScrollVertically()) {
                            return 0;
                        }
                        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                                view.getLayoutParams();
                        final int top = layoutManager.getDecoratedTop(view) - params.topMargin;
                        final int bottom = layoutManager.getDecoratedBottom(view)
                                + params.bottomMargin;
                        final int start = getPosition(view) == 0 ? layoutManager.getPaddingTop()
                                : 0;
                        final int end = layoutManager.getHeight() - layoutManager
                                .getPaddingBottom();
                        int dy = calculateDtToFit(top, bottom, start, end, snapPreference);
                        return dy == 0 ? 1 : dy;
                    }

                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        if (getChildCount() == 0) {
                            return null;
                        }

                        return new PointF(0, getDirectionToPosition(targetPosition));
                    }
                };
                smoothScroller.setTargetPosition(position);
                startSmoothScroll(smoothScroller);
            }
        });
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount);

        View first = getChildAt(0);
        View last = getChildAt(getChildCount() - 1);
        if (positionStart + itemCount <= getPosition(first)) {
            return;
        }

        if (positionStart <= getPosition(last)) {
            requestLayout();
        }
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            return 0;
        }

        if (!mSmoothScrollEnabled) {
            return getChildCount();
        }

        float contentInView = getChildCount();

        // Work out fraction of content lost off top and bottom.
        contentInView -= getFractionOfContentAbove(state, true);
        contentInView -= getFractionOfContentBelow(state, true);

        return (int) (contentInView / state.getItemCount() * getHeight());
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            return 0;
        }

        final View child = getChildAt(0);
        if (!mSmoothScrollEnabled) {
            return getPosition(child);
        }

        float contentAbove = getPosition(child);
        contentAbove += getFractionOfContentAbove(state, false);
        return (int) (contentAbove / state.getItemCount() * getHeight());
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        if (!mSmoothScrollEnabled) {
            return state.getItemCount();
        }

        return getHeight();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        View view = getAnchorChild();
        if (view == null) {
            state.anchorPosition = 0;
            state.anchorOffset = 0;
        } else {
            state.anchorPosition = getPosition(view);
            state.anchorOffset = getDecoratedTop(view);
        }
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        mRequestPosition = ((SavedState) state).anchorPosition;
        mRequestPositionOffset = ((SavedState) state).anchorOffset;
        requestLayout();
    }

    /**
     * Register a SectionLayoutManager.
     *
     * @param id      Id of layout. Referenced by first section view.
     * @param manager SectionLayoutManager to register.
     */
    public void registerSectionLayoutManager(int id, SectionLayoutManager manager) {
        mSectionLayouts.put(id, manager);
    }

    void measureHeader(View header) {
        // Width to leave for the mSection to which this header belongs. Only applies if the
        // header is being laid out adjacent to the mSection.
        int unavailableWidth = 0;
        LayoutParams lp = (LayoutParams) header.getLayoutParams();
        int recyclerWidth = getWidth() - getPaddingStart() - getPaddingEnd();
        if (!lp.isHeaderOverlay()) {
            if (lp.isHeaderStartAligned() && !lp.headerStartMarginIsAuto) {
                unavailableWidth = recyclerWidth - lp.headerMarginStart;
            } else if (lp.isHeaderEndAligned() && !lp.headerEndMarginIsAuto) {
                unavailableWidth = recyclerWidth - lp.headerMarginEnd;
            }
        }
        measureChildWithMargins(header, unavailableWidth, 0);
    }

    private void attachHeaderForStart(View header, SectionData sd, LayoutState state) {
        if (state.getCachedView(sd.firstPosition) != null && getDecoratedBottom(header) > 0) {
            addView(header, findLastIndexForSection(sd.firstPosition) + 1);
            state.decacheView(sd.firstPosition);
//        } else {
//            detachView(header);
//            attachView(header, findLastIndexForSection(sd.firstPosition) + 1);
        }
    }

    private int binarySearchForLastPosition(int min, int max, int sfp) {
        if (max < min) {
            return -1;
        }

        int mid = min + (max - min) / 2;

        View candidate = getChildAt(mid);
        LayoutParams params = (LayoutParams) candidate.getLayoutParams();
        if (params.getTestedFirstPosition() < sfp) {
            return binarySearchForLastPosition(mid + 1, max, sfp);
        }

        if (params.getTestedFirstPosition() > sfp || params.isHeader) {
            return binarySearchForLastPosition(min, mid - 1, sfp);
        }

        if (mid == getChildCount() - 1) {
            return mid;
        }

        View next = getChildAt(mid + 1);
        LayoutParams lp = (LayoutParams) next.getLayoutParams();
        if (lp.getTestedFirstPosition() != sfp) {
            return mid;
        }

        if (lp.isHeader) {
            if (mid + 1 == getChildCount() - 1) {
                return mid;
            }

            next = getChildAt(mid + 2);
            lp = (LayoutParams) next.getLayoutParams();
            if (lp.getTestedFirstPosition() != sfp) {
                return mid;
            }
        }

        return binarySearchForLastPosition(mid + 1, max, sfp);
    }

    /**
     * Fill out the next section as far as possible. The marker line is used as a start line to
     * position content from. If necessary, room for headers is given before laying out the section
     * content. However, headers are always added to an index after the section content.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param markerLine  Start line to begin placing content at.
     * @param state       Layout state.
     * @return Line to which content has been filled.
     */
    private int fillNextSectionToEnd(int leadingEdge, int markerLine, LayoutState state) {
        if (markerLine >= leadingEdge) {
            return markerLine;
        }

        View last = getAnchorAtEnd();
        int anchorPosition = getPosition(last) + 1;

        if (anchorPosition >= state.recyclerState.getItemCount()) {
            return markerLine;
        }

        LayoutState.View header = state.getView(anchorPosition);
        SectionData sd = new SectionData(this, header.view);
        if (sd.hasHeader) {
            measureHeader(header.view);
            sd = new SectionData(this, header.view);
            markerLine = layoutHeaderTowardsEnd(header.view, markerLine, sd, state);
            anchorPosition += 1;
        } else {
            state.cacheView(anchorPosition, header.view);
        }

        if (anchorPosition < state.recyclerState.getItemCount()) {
            SectionLayoutManager slm = getSectionLayoutManager(sd);
            markerLine = slm.fillToEnd(leadingEdge, markerLine, anchorPosition, sd, state);
        }

        if (sd.hasHeader) {
            addView(header.view);
            if (header.wasCached) {
                state.decacheView(sd.firstPosition);
            }
            markerLine = Math.max(getDecoratedBottom(header.view), markerLine);
        }

        return fillNextSectionToEnd(leadingEdge, markerLine, state);
    }

    /**
     * Fill the next section towards the start edge.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param markerLine  Start line to begin placing content at.
     * @param state       Layout state.
     * @return Line content was filled up to.
     */
    private int fillNextSectionToStart(int leadingEdge, int markerLine, LayoutState state) {
        if (markerLine < leadingEdge) {
            return markerLine;
        }

        View first = getAnchorAtStart();
        int anchorPosition = getPosition(first) - 1;

        if (anchorPosition < 0) {
            return markerLine;
        }

        LayoutState.View anchor = state.getView(anchorPosition);
        LayoutParams anchorParams = anchor.getLayoutParams();
        if (anchorParams.isHeader) {
            state.cacheView(anchorPosition, anchor.view);
            anchorPosition = anchorPosition - 1;
            if (anchorPosition < 0) {
                return markerLine;
            }
            anchor = state.getView(anchorPosition);
            anchorParams = anchor.getLayoutParams();
        }

        int sfp = anchorParams.getTestedFirstPosition();

        // Setup section data.
        View header = getHeaderOrFirstViewForSection(sfp, Direction.START, state);
        SectionData sd = new SectionData(this, header);
        if (sd.hasHeader) {
            measureHeader(header);
            sd = new SectionData(this, header);
        }

        // Fill out section.
        SectionLayoutManager slm = getSectionLayoutManager(sd);
        int sectionBottom = markerLine;
        if (anchorPosition >= 0) {
            markerLine = slm.fillToStart(leadingEdge, markerLine, anchorPosition, sd, state);
        }

        // Lay out and attach header.
        if (sd.hasHeader) {
            int headerOffset = 0;
            if (!sd.headerParams.isHeaderInline() || sd.headerParams.isHeaderOverlay()) {
                View firstVisibleView = slm.getFirstVisibleView(sd.firstPosition, true);
                if (firstVisibleView == null) {
                    headerOffset = 0;
                } else {
                    headerOffset = slm
                            .computeHeaderOffset(getPosition(firstVisibleView), sd, state);
                }
            }
            markerLine = layoutHeaderTowardsStart(header, leadingEdge, markerLine, headerOffset,
                    sectionBottom, sd, state);

            attachHeaderForStart(header, sd, state);
        }

        return fillNextSectionToStart(leadingEdge, markerLine, state);
    }

    /**
     * Fill the space between the last content item and the leadingEdge.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param state       Layout state.  @return Line to which content has been filled. If the line
     *                    is before the leading edge then the end of the data set has been reached.
     */
    private int fillToEnd(int leadingEdge, LayoutState state) {
        final View anchor = getAnchorAtEnd();

        LayoutParams anchorParams = (LayoutParams) anchor.getLayoutParams();
        final int sfp = anchorParams.getTestedFirstPosition();
        final View first = getHeaderOrFirstViewForSection(sfp, Direction.END, state);
        final SectionData sd = new SectionData(this, first);

        final SectionLayoutManager slm = getSectionLayoutManager(sd);
        int markerLine = slm.finishFillToEnd(leadingEdge, anchor, sd, state);

        View header = findAttachedHeaderForSectionFromEnd(sd.firstPosition);
        markerLine = updateHeaderForEnd(header, markerLine);

        markerLine = fillNextSectionToEnd(leadingEdge, markerLine, state);

        return markerLine;
    }

    /**
     * Fill towards the start edge.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param state       Layout state.
     * @return Line content was filled up to.
     */
    private int fillToStart(int leadingEdge, LayoutState state) {
        View anchor = getAnchorAtStart();

        LayoutParams anchorParams = (LayoutParams) anchor.getLayoutParams();
        final int sfp = anchorParams.getTestedFirstPosition();
        final View first = getHeaderOrFirstViewForSection(sfp, Direction.START, state);
        final SectionData sd = new SectionData(this, first);

        final SectionLayoutManager slm = getSectionLayoutManager(sd);
        int markerLine = slm.finishFillToStart(leadingEdge, anchor, sd, state);

        markerLine = updateHeaderForStart(first, leadingEdge, markerLine, sd, state);

        markerLine = fillNextSectionToStart(leadingEdge, markerLine, state);

        return markerLine;
    }

    /**
     * Fill up to a line in a given direction.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param direction   Direction fill will be taken towards.
     * @param layoutState Layout state.
     * @return Line to which content has been filled. If the line is before the leading edge then
     * the end of the data set has been reached.
     */
    private int fillUntil(int leadingEdge, Direction direction, LayoutState layoutState) {
        if (direction == Direction.START) {
            return fillToStart(leadingEdge, layoutState);
        } else {
            return fillToEnd(leadingEdge, layoutState);
        }
    }

    /**
     * Find a view that is the header for the specified section. Looks in direction specified from
     * opposite end.
     *
     * @param sfp  Section to look for header inside of. Search is expected to start inside the
     *             section so it must be at the matching end specified by the direction.
     * @param from Edge to start looking from.
     * @return Null if no header found, otherwise the header view.
     */
    private View findAttachedHeaderForSection(final int sfp, final Direction from) {
        if (from == Direction.END) {
            return findAttachedHeaderForSectionFromEnd(sfp);
        } else {
            return findAttachedHeaderForSectionFromStart(0, getChildCount() - 1, sfp);
        }
    }

    /**
     * The header is almost guaranteed to be at the end so just use look there.
     *
     * @param sfp Section identifier.
     * @return Header, or null if not found.
     */
    private View findAttachedHeaderForSectionFromEnd(int sfp) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            if (params.getTestedFirstPosition() != sfp) {
                break;
            } else if (params.isHeader) {
                return child;
            }
        }

        return null;
    }

    /**
     * The header is most likely at the end of the section but we don't know where that is so use
     * binary search.
     *
     * @param min min
     * @param max max
     * @param sfp Section identifier.
     * @return Header, or null if not found.
     */
    private View findAttachedHeaderForSectionFromStart(int min, int max, int sfp) {
        if (max < min) {
            return null;
        }

        int mid = min + (max - min) / 2;

        View candidate = getChildAt(mid);
        LayoutParams params = (LayoutParams) candidate.getLayoutParams();
        if (params.getTestedFirstPosition() != sfp) {
            return findAttachedHeaderForSectionFromStart(min, mid - 1, sfp);
        }

        if (params.isHeader) {
            return candidate;
        }

        return findAttachedHeaderForSectionFromStart(mid + 1, max, sfp);
    }

    /**
     * Find header or, if it cannot be found, the first view for a section.
     *
     * @param sfp        Section to look for header inside of. Search is expected to start inside
     *                   the section so it must be at the matching end specified by the direction.
     * @param startIndex Index to start looking from.
     * @param from       Edge to start looking from.
     * @return Null if no header or first item found, otherwise the found view.
     */
    private View findAttachedHeaderOrFirstViewForSection(final int sfp, int startIndex,
            final Direction from) {
        int childIndex = startIndex;
        int step = from == Direction.START ? 1 : -1;
        for (; 0 <= childIndex && childIndex < getChildCount(); childIndex += step) {
            View child = getChildAt(childIndex);

            if (getPosition(child) == sfp) {
                return child;
            }
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            if (params.getTestedFirstPosition() != sfp) {
                break;
            }
        }

        return null;
    }

    private int findLastIndexForSection(int sfp) {
        return binarySearchForLastPosition(0, getChildCount() - 1, sfp);
    }

    /**
     * Find an anchor to fill to end from.
     *
     * @return Non-header view closest to the end edge.
     */
    private View getAnchorAtEnd() {
        if (getChildCount() == 1) {
            return getChildAt(0);
        }
        View candidate = getChildAt(getChildCount() - 1);
        LayoutParams candidateParams = (LayoutParams) candidate.getLayoutParams();
        if (candidateParams.isHeader) {
            // Try one above.
            View check = getChildAt(getChildCount() - 2);
            LayoutParams checkParams = (LayoutParams) check.getLayoutParams();
            if (checkParams.getTestedFirstPosition() == candidateParams.getTestedFirstPosition()) {
                candidate = check;
            }
        }
        return candidate;
    }

    /**
     * Get the first view in the section that intersects the start edge. Only returns the header if
     * it is the last one displayed.
     *
     * @return View in section at start edge.
     */
    private View getAnchorAtStart() {
        View child = getChildAt(0);
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        int sfp = params.sectionManager;

        if (!params.isHeader) {
            return child;
        }

        int i = 1;
        if (i < getChildCount()) {
            View candidate = getChildAt(i);
            LayoutParams candidateParams = (LayoutParams) candidate.getLayoutParams();
            if (candidateParams.getTestedFirstPosition() == sfp) {
                return candidate;
            }
        }

        return child;
    }

    /**
     * Find the first view in the hierarchy that can act as an anchor.
     *
     * @return The anchor view, or null if no view is a valid anchor.
     */
    private View getAnchorChild() {
        if (getChildCount() == 0) {
            return null;
        }

        final View child = getChildAt(0);
        final LayoutParams params = (LayoutParams) child.getLayoutParams();
        final int sfp = params.getTestedFirstPosition();

        final View first = findAttachedHeaderOrFirstViewForSection(sfp, 0, Direction.START);
        if (first == null) {
            return child;
        }

        final LayoutParams firstParams = (LayoutParams) first.getLayoutParams();
        if (!firstParams.isHeader) {
            return child;
        }

        if (firstParams.isHeaderInline() && !firstParams.isHeaderOverlay()) {
            if (getDecoratedBottom(first) <= getDecoratedTop(child)) {
                return first;
            } else {
                return child;
            }
        }

        if (getDecoratedTop(child) < getDecoratedTop(first)) {
            return child;
        }

        if (sfp + 1 == getPosition(child)) {
            return first;
        }

        return child;

    }

    /**
     * Work out the borderline from the given anchor view and the intended direction to fill the
     * view hierarchy.
     *
     * @param anchorView Anchor view to determine borderline from.
     * @param direction  Direction fill will be taken towards.
     * @return Borderline.
     */
    private int getBorderLine(View anchorView, Direction direction) {
        int borderline;
        if (anchorView == null) {
            if (direction == Direction.START) {
                borderline = getPaddingBottom();
            } else {
                borderline = getPaddingTop();
            }
        } else if (direction == Direction.START) {
            borderline = getDecoratedBottom(anchorView);
        } else {
            borderline = getDecoratedTop(anchorView);
        }
        return borderline;
    }

    private int getDirectionToPosition(int targetPosition) {
        SectionData sd = new SectionData(this, getChildAt(0));
        final View startSectionFirstView = getSectionLayoutManager(sd)
                .getFirstVisibleView(sd.firstPosition, true);
        return targetPosition < getPosition(startSectionFirstView) ? -1 : 1;
    }

    private float getFractionOfContentAbove(RecyclerView.State state, boolean ignorePosition) {
        float fractionOffscreen = 0;

        View child = getChildAt(0);

        final int anchorPosition = getPosition(child);
        int numBeforeAnchor = 0;

        float top = getDecoratedTop(child);
        float bottom = getDecoratedBottom(child);
        if (bottom < 0) {
            fractionOffscreen = 1;
        } else if (0 <= top) {
            fractionOffscreen = 0;
        } else {
            float height = getDecoratedMeasuredHeight(child);
            fractionOffscreen = -top / height;
        }
        SectionData sd = new SectionData(this, child);
        if (sd.headerParams.isHeader && sd.headerParams.isHeaderInline()) {
            // Header must not be stickied as it is not attached after section items.
            return fractionOffscreen;
        }

        // Run through all views in the section and add up values offscreen.
        int firstPosition = -1;
        SparseArray<Boolean> positionsOffscreen = new SparseArray<>();
        for (int i = 1; i < getChildCount(); i++) {
            child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.sectionManager != sd.sectionManager) {
                break;
            }

            final int position = getPosition(child);
            if (!ignorePosition && position < anchorPosition) {
                numBeforeAnchor += 1;
            }

            top = getDecoratedTop(child);
            bottom = getDecoratedBottom(child);
            if (bottom < 0) {
                fractionOffscreen += 1;
            } else if (0 <= top) {
                continue;
            } else {
                float height = getDecoratedMeasuredHeight(child);
                fractionOffscreen += -top / height;
            }

            if (!lp.isHeader) {
                if (firstPosition == -1) {
                    firstPosition = position;
                }
                positionsOffscreen.put(position, true);
            }
        }

        return fractionOffscreen - numBeforeAnchor - getSectionLayoutManager(sd)
                .howManyMissingAbove(firstPosition, positionsOffscreen);
    }

    private float getFractionOfContentBelow(RecyclerView.State state, boolean ignorePosition) {
        final float parentHeight = getHeight();
        View child = getChildAt(getChildCount() - 1);

        final int anchorPosition = getPosition(child);
        int countAfter = 0;

        SectionData sd = new SectionData(this, child);

        float fractionOffscreen = 0;
        int lastPosition = -1;
        SparseArray<Boolean> positionsOffscreen = new SparseArray<>();
        // Run through all views in the section and add up values offscreen.
        for (int i = 1; i <= getChildCount(); i++) {
            child = getChildAt(getChildCount() - i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.sectionManager != sd.sectionManager) {
                break;
            }

            int position = getPosition(child);
            if (!lp.isHeader && !ignorePosition && position > anchorPosition) {
                countAfter += 1;
            }

            float bottom = getDecoratedBottom(child);
            float top = getDecoratedTop(child);
            if (bottom <= parentHeight) {
                continue;
            } else if (parentHeight < top) {
                fractionOffscreen += 1;
            } else {
                float height = getDecoratedMeasuredHeight(child);
                fractionOffscreen += (bottom - parentHeight) / height;
            }

            if (!lp.isHeader) {
                if (lastPosition == -1) {
                    lastPosition = position;
                }
                positionsOffscreen.put(position, true);
            }
        }

        return fractionOffscreen - countAfter - getSectionLayoutManager(sd)
                .howManyMissingBelow(lastPosition, positionsOffscreen);
    }

    private View getHeaderOrFirstViewForSection(int sfp, Direction direction, LayoutState state) {
        View view = findAttachedHeaderOrFirstViewForSection(sfp,
                direction == Direction.START ? 0 : getChildCount() - 1, direction);
        if (view == null) {
            LayoutState.View stateView = state.getView(sfp);
            view = stateView.view;
            if (stateView.getLayoutParams().isHeader) {
                measureHeader(stateView.view);
            }
            state.cacheView(sfp, view);
        }
        return view;
    }

    private SectionLayoutManager getSectionLayoutManager(SectionData sd) {
        SectionLayoutManager manager = mSectionLayouts.get(sd.sectionManager);
        if (manager == null) {
            throw new UnknownSectionLayoutException(sd.sectionManager);
        }
        return manager.init(sd);
    }

    /**
     * Layout header for fill to end.
     *
     * @param header     Header to be laid out.
     * @param markerLine Start of section.
     * @param sd         Section data.
     * @param state      Layout state.
     * @return Line at which to start filling out the section's content.
     */
    private int layoutHeaderTowardsEnd(View header, int markerLine, SectionData sd,
            LayoutState state) {
        Rect r = setHeaderRectSides(mRect, sd, state);

        r.top = markerLine;
        r.bottom = r.top + sd.headerHeight;

        if (sd.headerParams.isHeaderInline() && !sd.headerParams.isHeaderOverlay()) {
            markerLine = r.bottom;
        }

        if (sd.headerParams.isHeaderSticky() && r.top < 0) {
            r.top = 0;
            r.bottom = r.top + sd.headerHeight;
        }

        layoutDecorated(header, r.left, r.top, r.right, r.bottom);

        return markerLine;
    }

    /**
     * Layout header towards start edge.
     *
     * @param header      Header to be laid out.
     * @param leadingEdge Leading edge to align sticky headers against.
     * @param markerLine  Bottom edge of the header.
     * @param sd          Section data.
     * @param state       Layout state.
     * @return Top of the section including the header.
     */
    private int layoutHeaderTowardsStart(View header, int leadingEdge, int markerLine, int offset,
            int sectionBottom, SectionData sd, LayoutState state) {
        Rect r = setHeaderRectSides(mRect, sd, state);

        if (sd.headerParams.isHeaderInline() && !sd.headerParams.isHeaderOverlay()) {
            r.bottom = markerLine;
            r.top = r.bottom - sd.headerHeight;
        } else if (offset <= 0) {
            r.top = markerLine + offset;
            r.bottom = r.top + sd.headerHeight;
        } else {
            r.bottom = leadingEdge;
            r.top = r.bottom - sd.headerHeight;
        }

        if (sd.headerParams.isHeaderSticky() && r.top < leadingEdge) {
            r.top = leadingEdge;
            r.bottom = r.top + sd.headerHeight;
            if (sd.headerParams.isHeaderInline() && !sd.headerParams.isHeaderOverlay()) {
                markerLine -= sd.headerHeight;
            }
        }

        if (r.bottom > sectionBottom) {
            r.bottom = sectionBottom;
            r.top = r.bottom - sd.headerHeight;
        }

        layoutDecorated(header, r.left, r.top, r.right, r.bottom);

        return Math.min(r.top, markerLine);
    }

    private Rect setHeaderRectSides(Rect r, SectionData sd, LayoutState state) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();

        if (sd.headerParams.isHeaderEndAligned()) {
            // Position header from end edge.
            if (!sd.headerParams.isHeaderOverlay() && !sd.headerParams.headerEndMarginIsAuto
                    && sd.marginEnd > 0) {
                // Position inside end margin.
                if (state.isLTR) {
                    r.left = getWidth() - sd.marginEnd - paddingRight;
                    r.right = r.left + sd.headerWidth;
                } else {
                    r.right = sd.marginEnd + paddingLeft;
                    r.left = r.right - sd.headerWidth;
                }
            } else if (state.isLTR) {
                r.right = getWidth() - paddingRight;
                r.left = r.right - sd.headerWidth;
            } else {
                r.left = paddingLeft;
                r.right = r.left + sd.headerWidth;
            }
        } else if (sd.headerParams.isHeaderStartAligned()) {
            // Position header from start edge.
            if (!sd.headerParams.isHeaderOverlay() && !sd.headerParams.headerStartMarginIsAuto
                    && sd.marginStart > 0) {
                // Position inside start margin.
                if (state.isLTR) {
                    r.right = sd.marginStart + paddingLeft;
                    r.left = r.right - sd.headerWidth;
                } else {
                    r.left = getWidth() - sd.marginStart - paddingRight;
                    r.right = r.left + sd.headerWidth;
                }
            } else if (state.isLTR) {
                r.left = paddingLeft;
                r.right = r.left + sd.headerWidth;
            } else {
                r.right = getWidth() - paddingRight;
                r.left = r.right - sd.headerWidth;
            }
        } else {
            // Header is not aligned to a directed edge and assumed to fill the width available.
            r.left = paddingLeft;
            r.right = r.left + sd.headerWidth;
        }

        return r;
    }

    /**
     * Trim content wholly beyond the end edge.
     *
     * @param state Layout state.
     */
    private void trimEnd(LayoutState state) {
        int height = getHeight();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View view = getChildAt(i);
            if (getDecoratedTop(view) >= height) {
                removeAndRecycleView(view, state.recycler);
            } else {
                break;
            }
        }
    }

    /**
     * Trim content wholly beyond the start edge.
     *
     * @param state Layout state.
     */
    private void trimStart(LayoutState state) {
        // Find the first view visible on the screen.
        View anchor = null;
        int anchorIndex = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View look = getChildAt(i);
            if (getDecoratedBottom(look) > 0) {
                anchor = look;
                anchorIndex = i;
                break;
            }
        }

        if (anchor == null) {
            detachAndScrapAttachedViews(state.recycler);
            return;
        }

        LayoutParams anchorParams = (LayoutParams) anchor.getLayoutParams();
        if (anchorParams.isHeader) {
            for (int i = anchorIndex - 1; i >= 0; i--) {
                View look = getChildAt(i);
                LayoutParams lookParams = (LayoutParams) look.getLayoutParams();
                if (lookParams.getTestedFirstPosition() == anchorParams.getTestedFirstPosition()) {
                    anchor = look;
                    anchorParams = lookParams;
                    anchorIndex = i;
                    break;
                }
            }
        }

        for (int i = 0; i < anchorIndex; i++) {
            removeAndRecycleViewAt(0, state.recycler);
        }

        int sfp = anchorParams.getTestedFirstPosition();

        View header = findAttachedHeaderForSection(sfp, Direction.START);
        if (header != null) {
            if (getDecoratedTop(header) < 0) {
                updateHeaderForTrimFromStart(header);
            }

            if (getDecoratedBottom(header) < 0) {
                removeAndRecycleView(header, state.recycler);
            }
        }
    }

    /**
     * Trim all content wholly beyond the direction edge. If the direction is START, then update the
     * header of the section intersecting the top edge.
     *
     * @param direction Direction of edge to trim against.
     * @param state     Layout state.
     */
    private void trimTail(Direction direction, LayoutState state) {
        if (direction == Direction.START) {
            trimStart(state);
        } else {
            trimEnd(state);
        }
    }

    /**
     * Find the header for this section, if any, and move it to be attached after the section's
     * content items. Updates the line showing the end of the section.
     *
     * @param header     Header to update.
     * @param markerLine End of the section as given by the SLM.
     * @return The end of the section including the header.
     */
    private int updateHeaderForEnd(View header, int markerLine) {
        if (header == null) {
            return markerLine;
        }

        // Just keep headers at the end.
        detachView(header);
        attachView(header, -1);

        return Math.max(markerLine, getDecoratedBottom(header));
    }

    /**
     * Update header for an already existing section when filling towards the start.
     *
     * @param header      Header to update.
     * @param leadingEdge Leading edge to align sticky headers against.
     * @param markerLine  Start of section.
     * @param sd          Section data.
     * @param state       Layout state.
     * @return Updated line for the start of the section content including the header.
     */
    private int updateHeaderForStart(View header, int leadingEdge, int markerLine, SectionData sd,
            LayoutState state) {
        if (!sd.hasHeader) {
            return markerLine;
        }

        SectionLayoutManager slm = getSectionLayoutManager(sd);
        int sli = findLastIndexForSection(sd.firstPosition);
        int sectionBottom = getHeight();
        for (int i = sli == -1 ? 0 : sli; i < getChildCount(); i++) {
            View view = getChildAt(i);
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            if (params.getTestedFirstPosition() != sd.firstPosition) {
                view = findAttachedHeaderOrFirstViewForSection(params.getTestedFirstPosition(), i,
                        Direction.START);
                sectionBottom = getDecoratedTop(view);
                break;
            }
        }
        int offset = 0;
        if (!sd.headerParams.isHeaderInline() || sd.headerParams.isHeaderOverlay()) {
            View firstVisibleView = slm.getFirstVisibleView(sd.firstPosition, true);
            if (firstVisibleView == null) {
                offset = 0;
            } else {
                offset = slm.computeHeaderOffset(getPosition(firstVisibleView), sd, state);
            }
        }

        markerLine = layoutHeaderTowardsStart(header, leadingEdge, markerLine, offset,
                sectionBottom, sd, state);

        attachHeaderForStart(header, sd, state);

        return markerLine;
    }

    private void updateHeaderForTrimFromStart(View header) {
        SectionData sd = new SectionData(this, header);
        if (!sd.headerParams.isHeaderSticky() || mDisableStickyHeaderDisplay) {
            return;
        }

        final int slp = findLastIndexForSection(sd.firstPosition);
        if (slp == -1) {
            return;
        }

        SectionLayoutManager slm = getSectionLayoutManager(sd);
        final int sectionBottom = slm.getLowestEdge(sd.firstPosition, slp, getHeight());
        final int sectionTop = slm.getHighestEdge(sd.firstPosition, 0, 0);

        final int height = getDecoratedMeasuredHeight(header);
        if ((sd.headerParams.isHeaderInline() && !sd.headerParams.isHeaderOverlay())
                || (sectionBottom - sectionTop) > height) {
            final int left = getDecoratedLeft(header);
            final int right = getDecoratedRight(header);

            int top = 0;
            int bottom = top + height;

            if (bottom > sectionBottom) {
                bottom = sectionBottom;
                top = bottom - height;
            }

            layoutDecorated(header, left, top, right, bottom);
        }
    }

    public enum Direction {
        START,
        END,
        NONE
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        public static final int HEADER_INLINE = 0x01;

        public static final int HEADER_ALIGN_START = 0x02;

        public static final int HEADER_ALIGN_END = 0x04;

        public static final int HEADER_OVERLAY = 0x08;

        public static final int HEADER_STICKY = 0x10;

        private static final boolean DEFAULT_IS_HEADER = false;

        private static final int NO_FIRST_POSITION = -0x01;

        private static final int DEFAULT_HEADER_MARGIN = -0x01;

        private static final int DEFAULT_HEADER_DISPLAY = HEADER_INLINE | HEADER_STICKY;

        public boolean isHeader;

        public int headerDisplay;

        public int sectionManager;

        public int headerMarginEnd;

        public int headerMarginStart;

        public boolean headerStartMarginIsAuto;

        public boolean headerEndMarginIsAuto;

        private int mFirstPosition;

        public LayoutParams(int width, int height) {
            super(width, height);

            isHeader = DEFAULT_IS_HEADER;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs,
                    R.styleable.superslim_LayoutManager);
            isHeader = a.getBoolean(
                    R.styleable.superslim_LayoutManager_slm_isHeader,
                    false);
            headerDisplay = a.getInt(
                    R.styleable.superslim_LayoutManager_slm_headerDisplay,
                    DEFAULT_HEADER_DISPLAY);
            mFirstPosition = a.getInt(
                    R.styleable.superslim_LayoutManager_slm_section_firstPosition,
                    NO_FIRST_POSITION);
            sectionManager = a.getInt(
                    R.styleable.superslim_LayoutManager_slm_section_sectionManager,
                    0);

            // Header margin types can be dimension or integer (enum).
            boolean isDimension;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                TypedValue value = new TypedValue();
                a.getValue(R.styleable.superslim_LayoutManager_slm_section_headerMarginStart,
                        value);
                isDimension = value.type == TypedValue.TYPE_DIMENSION;
            } else {
                isDimension =
                        a.getType(R.styleable.superslim_LayoutManager_slm_section_headerMarginStart)
                                == TypedValue.TYPE_DIMENSION;
            }
            if (isDimension) {
                headerStartMarginIsAuto = false;
                headerMarginStart = a.getDimensionPixelSize(
                        R.styleable.superslim_LayoutManager_slm_section_headerMarginStart, 0);
            } else {
                headerStartMarginIsAuto = true;
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                TypedValue value = new TypedValue();
                a.getValue(R.styleable.superslim_LayoutManager_slm_section_headerMarginEnd, value);
                isDimension = value.type == TypedValue.TYPE_DIMENSION;
            } else {
                isDimension =
                        a.getType(R.styleable.superslim_LayoutManager_slm_section_headerMarginEnd)
                                == TypedValue.TYPE_DIMENSION;
            }
            if (isDimension) {
                headerEndMarginIsAuto = false;
                headerMarginEnd = a.getDimensionPixelSize(
                        R.styleable.superslim_LayoutManager_slm_section_headerMarginEnd, 0);
            } else {
                headerEndMarginIsAuto = true;
            }

            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
            init(other);
        }

        public boolean areHeaderFlagsSet(int flags) {
            return (headerDisplay & flags) == flags;
        }

        /**
         * Get the first position for the section to which this param's item belongs.
         *
         * @return A value {@literal <=} 0.
         */
        public int getFirstPosition() {
            return mFirstPosition;
        }

        /**
         * Set the first position for the section to which this param's item belongs.
         *
         * @param firstPosition First position of section for this param's item. Must be {@literal
         *                      <=} 0 or an InvalidFirstPositionException runtime exception will be
         *                      thrown.
         */
        public void setFirstPosition(int firstPosition) {
            if (firstPosition < 0) {
                throw new InvalidFirstPositionException();
            }
            mFirstPosition = firstPosition;
        }

        /**
         * Get the first position for the section to which this param's item belongs. Will throw a
         * MissingFirstPositionException runtime exception if the value is {@literal <} 0.
         *
         * @return A value {@literal >=} 0.
         */
        public int getTestedFirstPosition() {
            if (mFirstPosition == NO_FIRST_POSITION) {
                throw new MissingFirstPositionException();
            }
            return mFirstPosition;
        }

        public boolean isHeaderEndAligned() {
            return (headerDisplay & HEADER_ALIGN_END) != 0;
        }

        public boolean isHeaderInline() {
            return (headerDisplay & HEADER_INLINE) != 0;
        }

        public boolean isHeaderOverlay() {
            return (headerDisplay & HEADER_OVERLAY) != 0;
        }

        public boolean isHeaderStartAligned() {
            return (headerDisplay & HEADER_ALIGN_START) != 0;
        }

        public boolean isHeaderSticky() {
            return (headerDisplay & HEADER_STICKY) != 0;
        }

        private void init(ViewGroup.LayoutParams other) {
            if (other instanceof LayoutParams) {
                final LayoutParams lp = (LayoutParams) other;
                isHeader = lp.isHeader;
                headerDisplay = lp.headerDisplay;
                mFirstPosition = lp.mFirstPosition;
                sectionManager = lp.sectionManager;
                headerMarginEnd = lp.headerMarginEnd;
                headerMarginStart = lp.headerMarginStart;
                headerEndMarginIsAuto = lp.headerEndMarginIsAuto;
                headerStartMarginIsAuto = lp.headerStartMarginIsAuto;
            } else {
                isHeader = DEFAULT_IS_HEADER;
                headerDisplay = DEFAULT_HEADER_DISPLAY;
                headerMarginEnd = DEFAULT_HEADER_MARGIN;
                headerMarginStart = DEFAULT_HEADER_MARGIN;
                headerStartMarginIsAuto = true;
                headerEndMarginIsAuto = true;
            }
        }

        private class MissingFirstPositionException extends RuntimeException {

            MissingFirstPositionException() {
                super("Missing section first position.");
            }
        }

        private class InvalidFirstPositionException extends RuntimeException {

            InvalidFirstPositionException() {
                super("Invalid section first position given.");
            }
        }
    }

    protected static class SavedState implements Parcelable {

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public int anchorPosition;

        public int anchorOffset;

        protected SavedState() {
        }

        protected SavedState(Parcel in) {
            anchorPosition = in.readInt();
            anchorOffset = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(anchorPosition);
            out.writeInt(anchorOffset);
        }
    }

    private class UnknownSectionLayoutException extends RuntimeException {

        public UnknownSectionLayoutException(int id) {
            super("No registered layout for id " + id + ".");
        }
    }
}
