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

    private boolean mSmoothScrollEnabled = false;

    /**
     * Find the position of the first completely visible item.
     *
     * @return Position of first completely visible item.
     */
    public int findFirstCompletelyVisibleItemPosition() {
        final LayoutParams lp = (LayoutParams) getChildAt(0).getLayoutParams();
        final SectionLayoutManager manager = getSectionLayoutManager(lp.layoutId);

        return manager.findFirstCompletelyVisibleItemPosition(lp.getTestedFirstPosition());
    }

    /**
     * Find the position of the first visible item.
     *
     * @return Position of first visible item.
     */
    public int findFirstVisibleItemPosition() {
        final LayoutParams lp = (LayoutParams) getChildAt(0).getLayoutParams();
        final SectionLayoutManager manager = getSectionLayoutManager(lp.layoutId);

        return manager.findFirstVisibleItemPosition(lp.getTestedFirstPosition());
    }

    /**
     * Find the position of the last completely visible item.
     *
     * @return Position of last completely visible item.
     */
    public int findLastCompletelyVisibleItemPosition() {
        final LayoutParams lp = (LayoutParams) getChildAt(getChildCount() - 1).getLayoutParams();
        final SectionLayoutManager manager = getSectionLayoutManager(lp.layoutId);

        return manager.findLastCompletelyVisibleItemPosition(lp.getTestedFirstPosition());
    }

    /**
     * Find the position of the last visible item.
     *
     * @return Position of last visible item.
     */
    public int findLastVisibleItemPosition() {
        final LayoutParams lp = (LayoutParams) getChildAt(getChildCount() - 1).getLayoutParams();
        final SectionLayoutManager manager = getSectionLayoutManager(lp.layoutId);

        return manager.findLastVisibleItemPosition(lp.getTestedFirstPosition());
    }

    public boolean isSmoothScrollEnabled() {
        return mSmoothScrollEnabled;
    }

    public void setSmoothScrollEnabled(boolean smoothScrollEnabled) {
        mSmoothScrollEnabled = smoothScrollEnabled;
    }

    public FillResult layoutAndAddHeader(LayoutState state, SectionData section,
            FillResult fillResult) {
        final LayoutState.View header = section.getSectionHeader(state);
        if (header == null) {
            return fillResult;
        }

        final LayoutParams params = header.getLayoutParams();
        final int width = getDecoratedMeasuredWidth(header.view);
        final int height = getDecoratedMeasuredHeight(header.view);

        // Adjust marker line if needed.
        if (params.isHeaderInline() && !params.isHeaderOverlay()) {
            fillResult.markerStart -= height;
        }

        // Check header if header is stuck.
        final boolean isStuck = params.isHeaderSticky() && fillResult.markerStart < 0
                && !mDisableStickyHeaderDisplay;

        // Attach after section children if overlay, otherwise before.
        final int attachIndex;
        if (isStuck || params.isHeaderOverlay()) {
            attachIndex = fillResult.firstChildIndex + fillResult.addedChildCount;
        } else {
            attachIndex = fillResult.firstChildIndex;
        }

        // Attach header.
        if (header.wasCached) {
            if ((params.isHeaderSticky() && !mDisableStickyHeaderDisplay)
                    || getDecoratedBottom(header.view) >= 0) {
                attachView(header.view, attachIndex);
                state.decacheView(section.getFirstPosition());
                fillResult.positionStart -= 1;
            }
            if (!params.isHeaderSticky() || mDisableStickyHeaderDisplay) {
                // Layout unneeded if the header is not sticky and was cached.
                return fillResult;
            }
        }

        // Do Layout

        Rect rect = setHeaderRectSides(state, section, width, params, mRect);
        rect = setHeaderRectTopAndBottom(state, fillResult, height, params, rect);
        if (rect.bottom < 0) {
            // Header is offscreen.
            return fillResult;
        } else if (!header.wasCached) {
            fillResult.positionStart -= 1;
            addView(header.view, attachIndex);
        }

        layoutDecorated(header.view, rect.left, rect.top, rect.right, rect.bottom);

        return fillResult;
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
            View anchorView = getAnchorChild(itemCount);
            requestedPosition = anchorView == null ? 0 : getPosition(anchorView);
            borderLine = getBorderLine(anchorView, Direction.END);
        }

        detachAndScrapAttachedViews(recycler);

        final int anchorPosition = determineAnchorPosition(
                new LayoutState(this, recycler, state), requestedPosition);

        fill(recycler, state, anchorPosition, borderLine, true);
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
        int numChildren = getChildCount();//
        if (numChildren == 0) {
            return 0;
        }

        final int itemCount = state.getItemCount();

        /*
         * Strategy.
         *
         * The scroll has reached the start if the padded edge of the view is aligned with the top
         * edge of the first section's header, the section's highest edge, and that the section's
         * first view by adapter position is a child view.
         *
         * The end has been reached if the padded edge of the view is aligned with the bottom edge
         * of the last section's header or the section's lowest edge, and that the last adapter
         * position is a child view.
         */

        // Get start views.
        LayoutParams lp = (LayoutParams) getChildAt(0).getLayoutParams();
        SectionLayoutManager manager = getSectionLayoutManager(lp.layoutId);

        int startSectionFirstPosition = lp.getTestedFirstPosition();
        View startSectionFirstView = manager.getFirstVisibleView(startSectionFirstPosition, true);
        View startHeaderView = findAttachedHeaderForSection(
                state.getItemCount(), startSectionFirstPosition, Direction.END);
        int startSectionHighestEdge = manager.getHighestEdge(
                startSectionFirstPosition, getPaddingTop());

        // Get end views.
        lp = (LayoutParams) getChildAt(getChildCount() - 1)
                .getLayoutParams();
        int endSectionFirstPosition = lp.getTestedFirstPosition();
        manager = getSectionLayoutManager(lp.layoutId);

        View endSectionLastView = manager.getLastVisibleView(endSectionFirstPosition);
        View endHeaderView = findAttachedHeaderForSection(
                state.getItemCount(), endSectionFirstPosition, Direction.START);
        int endSectionLowestEdge = manager.getLowestEdge(
                endSectionFirstPosition, getHeight() - getPaddingBottom());

        //Work out if reached start.
        final boolean startDisplayed;
        final int firstEdge;
        final int recyclerViewStartEdge = getPaddingTop();
        if (startHeaderView == null) {
            startDisplayed = getPosition(startSectionFirstView) == 0;
            firstEdge = startSectionHighestEdge;
        } else {
            startDisplayed = getPosition(startSectionFirstView) == 1;
            final int headerStartEdge = getDecoratedTop(startHeaderView);
            firstEdge = startSectionHighestEdge < headerStartEdge ? startSectionHighestEdge
                    : headerStartEdge;
        }
        final boolean reachedStart = startDisplayed && firstEdge >= recyclerViewStartEdge;

        // Work out if reached end.
        final boolean endDisplayed;
        final int lastEdge;
        final int recyclerViewEndEdge = getHeight() - getPaddingBottom();
        if (endHeaderView == null) {
            endDisplayed = getPosition(endSectionLastView) == itemCount - 1;
            lastEdge = endSectionLowestEdge;
        } else {
            endDisplayed = getPosition(endSectionLastView) == itemCount - 1;
            final int headerEndEdge = getDecoratedBottom(endHeaderView);
            lastEdge = endSectionLowestEdge > headerEndEdge ? endSectionLowestEdge : headerEndEdge;
        }
        final boolean reachedEnd = endDisplayed && lastEdge <= recyclerViewEndEdge;

        // Check if scrolling is possible.
        if (reachedEnd && reachedStart) {
            return 0;
        }

        // Work out how far to scroll.
        int delta;
        if (dy > 0) {
            // Scrolling to end.
            if (endDisplayed) {
                delta = Math.max(-dy, recyclerViewEndEdge - lastEdge);
            } else {
                delta = -dy;
            }
        } else {
            // Scrolling to top.
            if (startDisplayed) {
                if (startHeaderView != null) {
                    LayoutParams params = (LayoutParams) startHeaderView.getLayoutParams();
                    if (params.isHeaderInline()) {
                        delta = Math.min(-dy, (recyclerViewStartEdge + getDecoratedMeasuredHeight(
                                startHeaderView)) - startSectionHighestEdge);
                    } else {
                        delta = Math.min(-dy, recyclerViewStartEdge - firstEdge);
                    }
                } else {
                    delta = Math.min(-dy, recyclerViewStartEdge - firstEdge);
                }
            } else {
                delta = -dy;
            }
        }

        offsetChildrenVertical(delta);

        final int anchorPosition;
        LayoutState layoutState = new LayoutState(this, recycler, state);
        if (delta > 0) {
            anchorPosition = determineAnchorPosition(
                    layoutState, getPosition(startSectionFirstView));
        } else {
            anchorPosition = determineAnchorPosition(
                    layoutState, getPosition(endSectionLastView));
        }
        fill(recycler, state, anchorPosition, 0, false);

        return -delta;
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
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        View view = getAnchorChild(getItemCount());
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

//    @Override
//    public int computeVerticalScrollExtent(RecyclerView.State state) {
//        if (!mSmoothScrollEnabled) {
//            return getChildCount();
//        }
//
//        int endSection = ((LayoutParams) getChildAt(getChildCount() - 1).getLayoutParams())
// .section;
//        SectionLayoutManager manager = mSlmFactory.getSectionLayoutManager(this, endSection);
//        View endView = manager.getLastVisibleView(endSection);
//
//        int topOffset = computeVerticalScrollOffset(state);
//
//        int lastContentPosition = getPosition(endView) + 1;
//        int lastBottom = getDecoratedBottom(endView);
//        int lastHeight = getDecoratedMeasuredHeight(endView);
//        int bottomOffset = lastContentPosition * 10 - (lastBottom >= getHeight() ?
//                (lastBottom - getHeight())
//                        / (lastHeight / 10) : 0);
//
//        return bottomOffset - topOffset;
//    }
//
//    @Override
//    public int computeVerticalScrollOffset(RecyclerView.State state) {
//        if (!mSmoothScrollEnabled) {
//            final View v = getChildAt(0);
//            final View v2 = getChildAt(2);
//            final int p = getPosition(v);
//            final int p2 = getPosition(v2);
//            return p2 - p == 1 ? p : p2;
//        }
//
//        int startSection = ((LayoutParams) getChildAt(0).getLayoutParams()).section;
//        SectionLayoutManager manager = mSlmFactory.getSectionLayoutManager(this, startSection);
//        View firstContentView = manager.getFirstVisibleView(startSection);
//        View firstHeaderView = findAttachedHeaderForSection(state.getItemCount(), startSection,
//                Direction.END);
//
//        int firstContentPosition = getPosition(firstContentView);
//        int contentTop = getDecoratedTop(firstContentView);
//        int contentHeight = getDecoratedMeasuredHeight(firstContentView);
//
//        if (firstHeaderView == null) {
//            return (int) (firstContentPosition * 10
//                    - (contentTop < 0 ? contentTop / (contentHeight / 10f) : 0));
//        }
//
//        int headerPosition = getPosition(firstHeaderView);
//        if (firstContentPosition - headerPosition == 1) {
//            int i = 0;
//            for (; i < getItemCount(); i++) {
//                if (getChildAt(i) == firstContentView) {
//                    break;
//                }
//            }
//            if (i + 1 < getItemCount()) {
//                View next = getChildAt(i + 1);
//                LayoutParams nextParams = (LayoutParams) next.getLayoutParams();
//                if (next == firstHeaderView || nextParams.section != startSection) {
//                    int headerTop = getDecoratedTop(firstHeaderView);
//                    int headerHeight = getDecoratedMeasuredHeight(firstHeaderView);
//                    return (int) (headerPosition * 10
//                            - (headerTop < 0 ? headerTop / (headerHeight / 20f) : 0));
//                }
//            }
//            return (int) (headerPosition * 10
//                    - (contentTop < 0 ? contentTop / (contentHeight / 20f) : 0));
//        }
//        return (int) (firstContentPosition * 10
//                - (contentTop < 0 ? contentTop / (contentHeight / 10f) : 0));
//    }
//
//    @Override
//    public int computeVerticalScrollRange(RecyclerView.State state) {
//        if (!mSmoothScrollEnabled) {
//            return state.getItemCount();
//        }
//        return state.getItemCount() * 10;
//    }

    /**
     * Register a SectionLayoutManager.
     *
     * @param layoutId Id of layout. Referenced by first section view.
     * @param manager  SectionLayoutManager to register.
     */
    public void registerSectionLayoutManager(int layoutId, SectionLayoutManager manager) {
        mSectionLayouts.put(layoutId, manager);
    }

    void measureHeader(LayoutState.View header) {
        if (header.wasCached) {
            return;
        }

        // Width to leave for the mSection to which this header belongs. Only applies if the
        // header is being laid out adjacent to the mSection.
        int unavailableWidth = 0;
        LayoutParams lp = (LayoutParams) header.view.getLayoutParams();
        int recyclerWidth = getWidth() - getPaddingStart() - getPaddingEnd();
        if (!lp.isHeaderOverlay()) {
            if (lp.isHeaderStartAligned() && !lp.headerStartMarginIsAuto) {
                unavailableWidth = recyclerWidth - lp.headerMarginStart;
            } else if (lp.isHeaderEndAligned() && !lp.headerEndMarginIsAuto) {
                unavailableWidth = recyclerWidth - lp.headerMarginEnd;
            }
        }
        measureChildWithMargins(header.view, unavailableWidth, 0);
    }

    private int determineAnchorPosition(LayoutState state, int position) {
        SectionData section = new SectionData(this, state, Direction.NONE, position, 0);

        if (section.getFirstPosition() == position && section.getSectionHeader(state) != null &&
                section.getSectionHeader(state).getLayoutParams().isHeaderInline()) {
            // Already know what to do in this case.
            return position;
        }

        return mSectionLayouts.get(section.getLayoutId())
                .getAnchorPosition(state, section, position);
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State rvs,
            final int anchorPosition, int scrappedBorderLine, boolean scrapped) {

        LayoutState state = new LayoutState(this, recycler, rvs);
        final int itemCount = state.recyclerState.getItemCount();
        final int recyclerViewHeight = getHeight();

        if (anchorPosition >= itemCount || anchorPosition < 0) {
            return;
        }

        state.detachAndCacheAllViews();

        // Borderline
        int borderline = scrapped ? scrappedBorderLine
                : getBorderLine(state, anchorPosition, Direction.END);

        // Prepare anchor section.
        SectionData section = new SectionData(this, state, Direction.NONE, anchorPosition,
                borderline);
        SectionLayoutManager sectionManager = getSectionLayoutManager(section.getLayoutId());
        section.loadMargins(this, sectionManager);

        // Fill anchor section.
        FillResult anchorResult = sectionManager.fill(state, section);
        anchorResult = layoutAndAddHeader(state, section, anchorResult);

        // Fill sections before anchor to start.
        FillResult fillResult;
        fillResult = fillSections(state, anchorResult, recyclerViewHeight, Direction.START);
        final int finalStartMarker = fillResult.markerStart;
        final int finalStartPosition = fillResult.positionStart;

        // Fill sections after anchor to end.
        fillResult = fillSections(state, anchorResult, recyclerViewHeight, Direction.END);
        final int finalEndMarker = fillResult.markerEnd;
        final int finalEndPosition = fillResult.positionEnd;

        state.recycleCache();
    }

    private FillResult fillSections(LayoutState layoutState, FillResult fillState,
            int recyclerViewHeight, Direction direction) {
        while (true) {
            final int anchor;
            final SectionData section;
            if (direction == Direction.END) {
                anchor = fillState.positionEnd + 1;
                if (fillState.markerEnd >= recyclerViewHeight
                        || anchor >= layoutState.recyclerState.getItemCount()) {
                    break;
                }
                section = new SectionData(this, layoutState, direction, anchor,
                        fillState.markerEnd);
            } else {
                anchor = fillState.positionStart - 1;
                if (fillState.markerStart <= 0 || anchor < 0) {
                    break;
                }
                section = new SectionData(this, layoutState, direction, anchor,
                        fillState.markerStart);
            }

            SectionLayoutManager sectionManager = getSectionLayoutManager(section.getLayoutId());
            section.loadMargins(this, sectionManager);
            fillState = sectionManager.fill(layoutState, section);
            fillState = layoutAndAddHeader(layoutState, section, fillState);
        }
        return fillState;
    }

    /**
     * Find a view that is the header for the specified section. Looks in direction specified from
     * opposite end.
     *
     * @param itemCount            Current number of items in adapter.
     * @param sectionFirstPosition Section to look for header inside of. Search is expected to start
     *                             inside the section so it must be at the matching end specified by
     *                             the direction.
     * @param direction            Direction to look in. Direction.END means to look from the start
     *                             to the end.
     * @return Null if no header found, otherwise the header view.
     */
    private View findAttachedHeaderForSection(final int itemCount, final int sectionFirstPosition,
            final Direction direction) {
        int position = direction == Direction.END ? 0 : getChildCount() - 1;
        int nextStep = direction == Direction.END ? 1 : -1;
        for (; 0 <= position && position < itemCount; position += nextStep) {
            View child = getChildAt(position);
            if (child == null) {
                continue;
            }
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            if (params.getTestedFirstPosition() != sectionFirstPosition) {
                break;
            } else if (params.isHeader) {
                return child;
            }
        }
        return null;
    }

    /**
     * Find the first view in the hierarchy that can act as an anchor.
     *
     * @param itemCount Number of items currently in the adapter.
     * @return The anchor view, or null if no view is a valid anchor.
     */
    private View getAnchorChild(final int itemCount) {
        if (getChildCount() > 0) {
            final int childCount = getChildCount();

            for (int i = 0; i < childCount; i++) {
                final View view = getChildAt(i);

                // Skip headers.
                if (((LayoutParams) view.getLayoutParams()).isHeader) {
                    //TODO: Handle empty sections with headers.
                    continue;
                }

                final int position = getPosition(view);
                if (position >= 0 && position < itemCount) {
                    return view;
                }
            }
        }
        return null;
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

    private int getBorderLine(LayoutState state, int anchorPosition,
            Direction direction) {
        int borderline;
        final android.view.View marker = state.getCachedView(anchorPosition);
        if (marker == null) {
            if (direction == Direction.START) {
                borderline = getPaddingBottom();
            } else {
                borderline = getPaddingTop();
            }
        } else if (direction == Direction.START) {
            borderline = getDecoratedBottom(marker);
        } else {
            borderline = getDecoratedTop(marker);
        }
        return borderline;
    }

    private int getDirectionToPosition(int targetPosition) {
        LayoutParams lp = (LayoutParams) getChildAt(0).getLayoutParams();
        final View startSectionFirstView = getSectionLayoutManager(lp.layoutId)
                .getFirstVisibleView(lp.getTestedFirstPosition(), true);
        return targetPosition < getPosition(startSectionFirstView) ? -1 : 1;
    }

    private SectionLayoutManager getSectionLayoutManager(int layoutId) {
        SectionLayoutManager manager = mSectionLayouts.get(layoutId);
        if (manager == null) {
            throw new UnknownSectionLayoutException(layoutId);
        }
        return manager;
    }

    private Rect setHeaderRectSides(LayoutState state, SectionData section, int width,
            LayoutParams params, Rect r) {

        if (params.isHeaderEndAligned()) {
            // Position header from end edge.
            if (!params.isHeaderOverlay() && !params.headerEndMarginIsAuto
                    && section.getHeaderMarginEnd() > 0) {
                // Position inside end margin.
                if (state.isLTR) {
                    r.left = getWidth() - section.getHeaderMarginEnd() - getPaddingEnd();
                    r.right = r.left + width;
                } else {
                    r.right = section.getHeaderMarginEnd() + getPaddingEnd();
                    r.left = r.right - width;
                }
            } else if (state.isLTR) {
                r.right = getWidth() - getPaddingEnd();
                r.left = r.right - width;
            } else {
                r.left = getPaddingEnd();
                r.right = r.left + width;
            }
        } else if (params.isHeaderStartAligned()) {
            // Position header from start edge.
            if (!params.isHeaderOverlay() && !params.headerStartMarginIsAuto
                    && section.getHeaderMarginStart() > 0) {
                // Position inside start margin.
                if (state.isLTR) {
                    r.right = section.getHeaderMarginStart() + getPaddingStart();
                    r.left = r.right - width;
                } else {
                    r.left = getWidth() - section.getHeaderMarginStart() - getPaddingStart();
                    r.right = r.left + width;
                }
            } else if (state.isLTR) {
                r.left = getPaddingStart();
                r.right = r.left + width;
            } else {
                r.right = getWidth() - getPaddingStart();
                r.left = r.right - width;
            }
        } else {
            // Header is not aligned to a directed edge and assumed to fill the width available.
            r.left = getPaddingLeft();
            r.right = r.left + width;
        }

        return r;
    }

    private Rect setHeaderRectTopAndBottom(LayoutState state, FillResult fillResult, int height,
            LayoutParams params, Rect r) {
        r.top = fillResult.markerStart;
        if (params.headerDisplay != LayoutParams.HEADER_INLINE && fillResult.headerOffset < 0) {
            r.top += fillResult.headerOffset;
        }
        r.bottom = r.top + height;

        if (params.isHeaderSticky() && !mDisableStickyHeaderDisplay) {
            if (r.top < 0) {
                r.top = 0;
                r.bottom = height;
            }
            if (r.bottom > fillResult.markerEnd) {
                r.bottom = fillResult.markerEnd;
                r.top = r.bottom - height;
            }
        }

        return r;
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

        public int layoutId;

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
            layoutId = a.getInt(
                    R.styleable.superslim_LayoutManager_slm_section_layoutId,
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
                layoutId = lp.layoutId;
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

        public UnknownSectionLayoutException(int layoutId) {
            super("No registered layout for id " + layoutId + ".");
        }
    }
}
