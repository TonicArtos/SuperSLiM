package com.tonic.sectionlayoutmanager;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

/**
 * A LayoutManager that lays out section headers with optional stickiness and uses a map of
 * sections
 * to view layout managers to layout items.
 */
public class LayoutManager extends RecyclerView.LayoutManager {

    public static final int HEADER_ALIGN_START = 0x02;

    public static final int HEADER_ALIGN_END = 0x03;

    public static final int HEADER_OVERLAY_START = 0x04;

    public static final int HEADER_OVERLAY_END = 0x05;

    public static final int HEADER_INLINE = 0x01;

    public static final int HEADER_MARGIN_AUTO = -0x01;

    private static final int NO_STICKIED_POSITION = -0x01;

    private int mStickiedPosition = NO_STICKIED_POSITION;

    private SlmFactory mSlmFactory = new SlmFactory() {
        @Override
        public SectionLayoutManager getSectionLayoutManager(int section) {
            return new LinearSectionLayoutManager();
        }
    };

    private SparseArray<LayoutState.View> mPendingFloatingHeaders
            = new SparseArray<LayoutState.View>();

    /**
     * Marker for lowest edge of stickied header.
     */
    private int mStickyBottomMarker;

    public LayoutManager(Context context) {
    }

    public void setSlmFactory(SlmFactory factory) {
        mSlmFactory = factory;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        final int anchorPosition = getAnchorItemPosition(state);
        detachAndScrapAttachedViews(recycler);
        fill(SectionLayoutManager.Direction.END, anchorPosition, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        int numChildren = getChildCount();//
        if (numChildren == 0) {
            return 0;
        }

        //Take top measurements from the top view as determined by the section layout manager.
        android.view.View topView = null;
        // Look for a section header (which may be floating so can't just look at the first child).
        int sectionSearching = -1;
        for (int i = 0; i < numChildren; i++) {
            View v = getChildAt(i);
            LayoutParams params = (LayoutParams) v.getLayoutParams();
            if (sectionSearching >= 0 && sectionSearching != params.section) {
                break;
            } else if (params.isHeader) {
                topView = v;
                break;
            } else if (sectionSearching < 0) {
                sectionSearching = params.section;
            }
        }

        boolean firstItemReached;
        if (topView == null) {
            // No floating header so ask for the highest view from the section (which might not
            // be the first child).
            SectionLayoutManager sectionLayoutManager = mSlmFactory
                    .getSectionLayoutManager(sectionSearching);
            sectionLayoutManager.setLayoutManager(this);
            topView = sectionLayoutManager.getTopView(sectionSearching);
            firstItemReached = getPosition(sectionLayoutManager.getFirstView(sectionSearching))
                    == 0;
        } else {
            firstItemReached = getPosition(topView) == 0;
        }

        //Take bottom measurements from the bottom view as determined by the section layout
        // manager.
        android.view.View lastVisibleChild = getChildAt(
                numChildren - (mStickiedPosition == NO_STICKIED_POSITION ? 1 : 2));
        android.view.View bottomView;

        // Skip overlay headers which mock being last (except when they have empty sections and
        // really are last).
        LayoutParams lp = (LayoutParams) lastVisibleChild.getLayoutParams();
        if (lp.isHeader) {
            if (lp.headerAlignment == HEADER_OVERLAY_START
                    || lp.headerAlignment == HEADER_OVERLAY_END) {
                // Check the one above. If it belongs to another section then the overlay header
                // is really the last visible view.
                if (numChildren > (mStickiedPosition == NO_STICKIED_POSITION ? 2 : 3)) {
                    android.view.View oneAbove = getChildAt(
                            numChildren - (mStickiedPosition == NO_STICKIED_POSITION ? 2 : 3));
                    LayoutParams oneAboveLp = (LayoutParams) oneAbove.getLayoutParams();
                    if (oneAboveLp.section == lp.section) {
                        lastVisibleChild = oneAbove;
                        lp = oneAboveLp;
                    }
                }
            }
        }

        if (lp.isHeader) {
            // Last visible view is a header so we don't need to refer to the section layout
            // manager as to which view really is bottom most.
            bottomView = lastVisibleChild;
        } else {
            SectionLayoutManager sectionLayoutManager = mSlmFactory
                    .getSectionLayoutManager(lp.section);
            sectionLayoutManager.setLayoutManager(this);
            bottomView = sectionLayoutManager.getBottomView(lp.section);
        }
        boolean lastItemReached = getPosition(lastVisibleChild) == state.getItemCount() - 1;

        // Check we need to scroll.
        int viewSpan = getDecoratedBottom(bottomView) - getDecoratedTop(topView);
        if (firstItemReached && lastItemReached
                && viewSpan <= getHeight() - getPaddingTop() - getPaddingBottom() - dy) {
            //We cannot scroll in either direction
            return 0;
        }

        int delta;
        if (dy > 0) {
            // Scrolling to bottom.
            if (lastItemReached) {
                int offset = getHeight() - getDecoratedBottom(bottomView) - getPaddingBottom();
                delta = Math.max(-dy, offset);
            } else {
                delta = -dy;
            }
        } else {
            // Scrolling to top.
            if (firstItemReached) {
                int offset = -getDecoratedTop(topView) + getPaddingTop();
                delta = Math.min(-dy, offset);
            } else {
                delta = -dy;
            }
        }

        offsetChildrenVertical(delta);

        if (delta < 0) {
            fill(SectionLayoutManager.Direction.START, getPosition(bottomView), recycler, state);
        } else if (0 < delta) {
            fill(SectionLayoutManager.Direction.END, getPosition(topView), recycler, state);
        }

        return -delta;
    }

    private void fill(SectionLayoutManager.Direction direction, final int anchorPosition,
            RecyclerView.Recycler recycler, RecyclerView.State rvs) {

        LayoutState state = new LayoutState(this, recycler, rvs, getChildCount());
        final int itemCount = state.recyclerState.getItemCount();
        final int recyclerViewHeight = getHeight();

        if (anchorPosition >= itemCount || anchorPosition < 0) {
            return;
        }

        state.detachAndCacheAllViews();

        state.direction = direction;
        final int borderline = getBorderLine(state, anchorPosition);
        state.markerLine = borderline;
        int currentPosition = anchorPosition;
        /**
         * When filling to start the first section may be incomplete requiring a pass to end.
         * This only matters for filling in floating headers in the correct place.
         */
        boolean pastIncompleteSection = state.isDirectionEnd()
                && currentPosition == state.sectionFirstPosition;
        boolean fillToEndDone = false;
        boolean fillToStartDone = false;

        Log.d("Sticky", "" + mStickiedPosition);
        mStickyBottomMarker = recyclerViewHeight;

        while (!fillToEndDone || !fillToStartDone) {
            // Look at the current view and find out details.
            state.setSectionData((LayoutParams) state.getView(
                    ((LayoutParams) state.getView(currentPosition).view
                            .getLayoutParams()).sectionFirstPosition).view.getLayoutParams());

            SectionLayoutManager sectionManager = getSectionLayoutManager(state.section);
            sectionManager.setLayoutManager(this);
            state.setSectionData(sectionManager);

//            Log.d("Fill", "Section " + state.section);
//            Log.d("Fill", "Direction " + (state.isDirectionStart() ? " Start" : " End"));

            LayoutState.View sectionHeader = loadSectionHeader(state);

            // Check to see if we are actually going to fill a complete section.
            if (!pastIncompleteSection && state.isDirectionStart()) {
                pastIncompleteSection = currentPosition + 1 >= itemCount ||
                        ((LayoutParams) state.getView(currentPosition + 1).view
                                .getLayoutParams()).section != state.section;
            }

            if (state.isDirectionEnd()) {
                if (currentPosition == state.sectionFirstPosition) {
                    layoutHeader(state, sectionHeader);
                    currentPosition += 1;
                }
            }

            int count = sectionManager.fill(state, currentPosition);
            currentPosition += state.isDirectionStart() ? -count : count;

            if (state.isDirectionStart()) {
                layoutHeader(state, sectionHeader);
                if (currentPosition == state.sectionFirstPosition) {
                    currentPosition -= 1;
                }
            } else if (!pastIncompleteSection && sectionHeader != null) {
                // Make sure to load floating header for an incomplete section.
                LayoutParams headerLp = (LayoutParams) sectionHeader.view.getLayoutParams();
                if (headerLp.headerAlignment == HEADER_OVERLAY_END
                        || headerLp.headerAlignment == HEADER_OVERLAY_START) {
                    // Temporarily switch to borderline so the partial section can correctly
                    // position the floating header.
                    int markerLine = state.markerLine;
                    state.markerLine = borderline;
                    layoutHeader(state, sectionHeader);
                    state.markerLine = markerLine;
                }
            }

            // Make sure floating header is after all views of the current section.
            if (state.isDirectionEnd() || pastIncompleteSection) {
                final int floatingPosition = state.isDirectionStart() ? count : -1;
                final LayoutState.View floatingHeader = mPendingFloatingHeaders.get(state.section);
                if (floatingHeader != null) {
                    if (floatingHeader.wasCached) {
                        mPendingFloatingHeaders.remove(state.section);
                        attachView(floatingHeader.view, floatingPosition);
                        state.decacheView(state.sectionFirstPosition);
                    } else {
                        mPendingFloatingHeaders.remove(state.section);
                        addView(floatingHeader.view, floatingPosition);
                    }
                }
            }

            pastIncompleteSection = true;

            if (state.isDirectionStart() && (state.markerLine <= 0 || currentPosition < 0)) {
                state.direction = SectionLayoutManager.Direction.END;
                state.markerLine = borderline;
                currentPosition = anchorPosition + 1;
                fillToStartDone = true;
                if (currentPosition >= itemCount) {
                    fillToEndDone = true;
                }
            } else if (state.isDirectionEnd() && (state.markerLine >= recyclerViewHeight
                    || currentPosition >= itemCount)) {
                state.direction = SectionLayoutManager.Direction.START;
                pastIncompleteSection = false;
                state.markerLine = borderline;
                currentPosition = anchorPosition - 1;
                fillToEndDone = true;
                if (currentPosition < 0) {
                    fillToStartDone = true;
                }
            }
        }

        Log.d("Sticky", "" + mStickiedPosition);

        // Do stickied header.
        if (mStickiedPosition != NO_STICKIED_POSITION) {
            LayoutState.View stickiedHeader = state.getView(mStickiedPosition);
            measureHeader(stickiedHeader);
            Rect rect = new Rect();
            int height = getDecoratedMeasuredHeight(stickiedHeader.view);
            int width = getDecoratedMeasuredWidth(stickiedHeader.view);

            int offset = mStickyBottomMarker - height;

            LayoutParams params = (LayoutParams) stickiedHeader.view.getLayoutParams();
            updateHeaderRectSides(state, rect, width, params);
            rect.top = offset < 0 ? offset : 0;
            rect.bottom = offset < 0 ? height + offset : height;
            layoutDecorated(stickiedHeader.view, rect.left, rect.top, rect.right, rect.bottom);

            if (!params.isItemRemoved()) {
                if (stickiedHeader.wasCached) {
                    attachView(stickiedHeader.view);
                    state.decacheView(mStickiedPosition);
                } else {
                    addView(stickiedHeader.view);
                }
            }
        }

        mPendingFloatingHeaders.clear();
        state.recycleCache();
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        View topChild = getChildAt(0);
        View bottomChild = getChildAt(getChildCount() - 1);
        if (getPosition(bottomChild) == mStickiedPosition && getChildCount() > 1) {
            bottomChild = getChildAt(getChildCount() - 2);
        }
        if (topChild == null) {
            return 0;
        }

        final int decoratedTop = getDecoratedTop(topChild);
        final int decoratedBottom = getDecoratedBottom(bottomChild);
        final int realTopOffset = decoratedTop > 0 ? decoratedTop : 0;
        final int realBottomOffset = decoratedBottom < getHeight() ? decoratedBottom : 0;
        return getHeight() - realTopOffset - realBottomOffset;
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        View topChild = getChildAt(0);
        View bottomChild = getChildAt(getChildCount() - 1);
        if (getPosition(bottomChild) == mStickiedPosition && getChildCount() > 1) {
            bottomChild = getChildAt(getChildCount() - 2);
        }
        if (topChild == null) {
            return 0;
        }

        final int itemCount = state.getItemCount();
        final int topPosition = getPosition(topChild);
        final int bottomPosition = getPosition(bottomChild);
        final boolean firstItemVisible = topPosition == 0;
        final boolean lastItemVisible = bottomPosition == itemCount;

        // Check for case where the entirety of the adapter contents are displayed.
        final int viewSpan = getDecoratedBottom(bottomChild) - getDecoratedTop(topChild);
        if (firstItemVisible && lastItemVisible
                && viewSpan < getHeight() - getPaddingTop() - getPaddingBottom()) {
            return 0;
        }

        // Estimated scroll range is a multiple of the displayed item span.
        final float numItemsShown = getPosition(bottomChild) - getPosition(topChild);
        return (int) (viewSpan * (itemCount / numItemsShown));
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        View topChild = getChildAt(0);
        View bottomChild = getChildAt(getChildCount() - 1);
        if (getPosition(bottomChild) == mStickiedPosition && getChildCount() > 1) {
            bottomChild = getChildAt(getChildCount() - 2);
        }
        if (topChild == null) {
            return 0;
        }

        final int topPosition = getPosition(topChild);
        final float abovePosition = topPosition == 0 ? 0 : topPosition - 1;
        int decoratedTop = getDecoratedTop(topChild);
        final int offscreenPortion = decoratedTop < 0 ? -decoratedTop : 0;
        final int viewSpan = getDecoratedBottom(bottomChild) - decoratedTop;
        // Estimated scroll range is a multiple of the displayed item span plus the part of the
        // top view which is offscreen.
        final float numItemsShown = getPosition(bottomChild) - getPosition(topChild);
        return (int) (viewSpan * (abovePosition / numItemsShown)) + offscreenPortion;

    }

    private int getBorderLine(LayoutState state, int anchorPosition) {
        int borderline;
        final android.view.View marker = state.getCachedView(anchorPosition);
        if (marker == null) {
            borderline = getPaddingTop();
        } else if (state.isDirectionStart()) {
            LayoutParams lp = (LayoutParams) marker.getLayoutParams();
            if (lp.isHeader && lp.headerAlignment != HEADER_INLINE) {
                borderline = getDecoratedTop(marker);
            } else {
                borderline = getDecoratedBottom(marker);
            }
        } else {
            borderline = getDecoratedTop(marker);
        }
        return borderline;
    }

    private LayoutState.View loadSectionHeader(LayoutState state) {
        LayoutState.View sectionHeader = state.getView(state.sectionFirstPosition);
        LayoutParams headerLp = (LayoutParams) sectionHeader.view.getLayoutParams();
        if (headerLp.isHeader) {
            measureHeader(sectionHeader);
            if (headerLp.headerAlignment != HEADER_INLINE) {
                state.headerOverlap = getDecoratedMeasuredHeight(sectionHeader.view);
                state.headerOffset = 0;
            } else {
                state.headerOverlap = 0;
            }
            state.updateSectionData(sectionHeader);
        } else {
            state.headerOverlap = 0;
        }

        if (!headerLp.isHeader) {
            sectionHeader = null;
        }

        return sectionHeader;
    }


    private SectionLayoutManager getSectionLayoutManager(int section) {
        SectionLayoutManager sectionManager = mSlmFactory
                .getSectionLayoutManager(section);
        if (sectionManager == null) {
            sectionManager = new LinearSectionLayoutManager();
        }
        return sectionManager;
    }

    private void layoutHeader(LayoutState state, LayoutState.View header) {
        if (header == null) {
            return;
        }
        boolean isStickied = mStickiedPosition == state.sectionFirstPosition;
        if (isStickied) {
            Log.d("Layout", "Sticky Header " + state.sectionFirstPosition);
        }

        final int width = getDecoratedMeasuredWidth(header.view);
        final int height = getDecoratedMeasuredHeight(header.view);

        LayoutParams lp = (LayoutParams) header.view.getLayoutParams();

        if (lp.headerAlignment == HEADER_INLINE && !lp.isSticky) {
            if (state.isDirectionStart() && state.markerLine <= 0) {
                return;
            } else if (state.isDirectionEnd() && state.markerLine >= getHeight()) {
                return;
            }
        }

        if (state.headerOverlap > 0 && state.headerOffset == LayoutState.NO_HEADER_OFFSET
                && !lp.isSticky) {
            return;
        }

        // Check cached header has not become sticky with scroll.
        if (!isStickied && header.wasCached) {
            if (getDecoratedTop(header.view) <= 0 && lp.isSticky) {
                isStickied = true;
                mStickiedPosition = state.sectionFirstPosition;
                Log.d("Layout", "Cached became sticky " + state.sectionFirstPosition);
            }
        }

        // Handle cached view but skip stickied headers as they do not have their cached layout
        // preserved.
        if (!isStickied && header.wasCached) {
            attachOrFloatView(state, header, lp);
            if (lp.headerAlignment == HEADER_INLINE) {
                if (state.isDirectionStart()) {
                    state.markerLine = getDecoratedTop(header.view);
                } else {
                    state.markerLine = getDecoratedBottom(header.view);
                }
            }

            if (lp.isSticky && getDecoratedTop(header.view) < mStickyBottomMarker) {
                mStickyBottomMarker = getDecoratedTop(header.view);
            }
            return;
        }

        // Handle non-cached view.
        // TODO: RTL layout stuff
        Rect rect = new Rect();
        updateHeaderRectSides(state, rect, width, lp);
        updateHeaderRectTopAndBottom(state, rect, height, lp);

        if (rect.top > 0) {
            if (isStickied) {
                mStickiedPosition = NO_STICKIED_POSITION;
                isStickied = false;
                Log.d("Layout", "Lost sticky " + state.sectionFirstPosition);
            }
        } else if (!isStickied && lp.isSticky) {
            isStickied = true;
            mStickiedPosition = state.sectionFirstPosition;
            Log.d("Layout", "Become sticky " + state.sectionFirstPosition);
        }

        layoutDecorated(header.view, rect.left, rect.top, rect.right, rect.bottom);
        if (!isStickied) {
            // Attach any header which may not be stickied anymore.
            attachOrFloatView(state, header, lp);
            if (lp.isSticky && getDecoratedTop(header.view) < mStickyBottomMarker) {
                mStickyBottomMarker = getDecoratedTop(header.view);
            }
        }

        if (lp.headerAlignment == HEADER_INLINE) {
            if (state.isDirectionStart()) {
                // Align marker line to align with top of header.
                state.markerLine = rect.top;
            } else {
                // Align marker line to align with bottom of header.
                state.markerLine = rect.bottom;
            }
        }
    }

    private void updateHeaderRectTopAndBottom(LayoutState state, Rect rect, int height,
            LayoutParams lp) {
        if (lp.headerAlignment == HEADER_INLINE
                && state.isDirectionStart()) {
            // Position header above marker line (bottom is aligned to line).
            rect.top = state.markerLine - height;
            rect.bottom = state.markerLine;
        } else {
            // Position header below marker line.
            rect.top = state.markerLine;
            rect.bottom = rect.top + height;
        }

        if (state.headerOffset > 0) {
            rect.top -= state.headerOffset;
            rect.bottom -= state.headerOffset;
            state.headerOffset = LayoutState.NO_HEADER_OFFSET;
        }
    }

    private void updateHeaderRectSides(LayoutState state, Rect rect, int width, LayoutParams lp) {
        if (lp.headerAlignment == HEADER_OVERLAY_START) {
            rect.left = getPaddingLeft();
            rect.right = rect.left + width;
        } else if (lp.headerAlignment == HEADER_OVERLAY_END) {
            rect.right = getWidth() - getPaddingRight();
            rect.left = rect.right - width;
        } else if (lp.headerAlignment == HEADER_ALIGN_END) {
            // Align header with end margin or end edge of recycler view.
            if (!lp.headerEndMarginIsAuto && state.headerEndMargin > 0) {
                rect.left = getWidth() - state.headerEndMargin - getPaddingLeft();
                rect.right = rect.left + width;
            } else {
                rect.right = getWidth() - getPaddingRight();
                rect.left = rect.right - width;
            }
        } else if (lp.headerAlignment == HEADER_ALIGN_START) {
            // Align header with start margin or start edge of recycler view.
            if (!lp.headerStartMarginIsAuto && state.headerStartMargin > 0) {
                rect.right = state.headerStartMargin + getPaddingLeft();
                rect.left = rect.right - width;
            } else {
                rect.left = getPaddingLeft();
                rect.right = rect.left + width;
            }
        } else {
            rect.left = getPaddingLeft();
            rect.right = rect.left + width;
        }
    }

    private void attachOrFloatView(LayoutState state, LayoutState.View view,
            LayoutParams params) {
        if (params.headerAlignment == HEADER_OVERLAY_START
                || params.headerAlignment == HEADER_OVERLAY_END) {
            mPendingFloatingHeaders.put(params.section, view);
        } else if (!params.isItemRemoved()) {
            if (view.wasCached) {
                attachView(view.view, state.isDirectionStart() ? 0 : -1);
                state.decacheView(state.sectionFirstPosition);
            } else {
                addView(view.view, state.isDirectionStart() ? 0 : -1);
            }
        }
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

    private void measureHeader(LayoutState.View header) {
        if (header.wasCached) {
            return;
        }

        // Width to leave for the section to which this header belongs. Only applies if the
        // header is being laid out adjacent to the section.
        int unavailableWidth = 0;
        LayoutParams lp = (LayoutParams) header.view.getLayoutParams();
        if (lp.headerAlignment == HEADER_ALIGN_START && !lp.headerStartMarginIsAuto) {
            unavailableWidth = getWidth() - lp.headerStartMargin;
        } else if (lp.headerAlignment == HEADER_ALIGN_END && !lp.headerEndMarginIsAuto) {
            unavailableWidth = getWidth() - lp.headerEndMargin;
        }
        measureChildWithMargins(header.view, unavailableWidth, 0);
    }

    private int getAnchorItemPosition(RecyclerView.State state) {
        final int itemCount = state.getItemCount();

        if (getChildCount() > 0) {
            return findAnchorChild(itemCount);
        }
        return 0;
    }

    /**
     * Look through view views to find one that can act as an anchor.
     *
     * @param itemCount RecyclerView count of items.
     * @return Anchor markerLine.
     */
    private int findAnchorChild(int itemCount) {
        final int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            final android.view.View view = getChildAt(i);

            // Skip headers that are stickied.
            final int position = getPosition(view);
            if (isStickyPosition(position)) {
                continue;
            }

            if (position >= 0 && position < itemCount) {
                return position;
            }
        }
        return 0;
    }

    private boolean isStickyPosition(int position) {
        return mStickiedPosition == position;
    }

    @Override
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        final LayoutParams newLp = new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        newLp.width = LayoutParams.MATCH_PARENT;
        newLp.height = LayoutParams.MATCH_PARENT;
        if (newLp instanceof LayoutParams) {
            newLp.init((LayoutParams) lp);
        }
        return newLp;
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        private static final boolean DEFAULT_IS_HEADER = false;

        private static final boolean DEFAULT_IS_STICKY = false;

        private static final int HEADER_NONE = -0x01;

        private static final int DEFAULT_HEADER_MARGIN = -0x01;

        private static final int DEFAULT_HEADER_ALIGNMENT = HEADER_INLINE;

        public boolean isHeader;

        public int headerAlignment;

        public int sectionFirstPosition;

        public boolean isSticky;

        public int section;

        public int headerEndMargin;

        public int headerStartMargin;

        public boolean headerStartMarginIsAuto;

        public boolean headerEndMarginIsAuto;

        public LayoutParams(int width, int height) {
            super(width, height);

            isHeader = DEFAULT_IS_HEADER;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs,
                    R.styleable.sectionlayoutmanager_LayoutManager);
            isHeader = a.getBoolean(
                    R.styleable.sectionlayoutmanager_LayoutManager_slm_isHeader,
                    false);
            headerAlignment = a.getInt(
                    R.styleable.sectionlayoutmanager_LayoutManager_slm_alignHeader,
                    HEADER_INLINE);
            sectionFirstPosition = a.getInt(
                    R.styleable.sectionlayoutmanager_LayoutManager_slm_sectionFirstPosition,
                    HEADER_NONE);
            isSticky = a.getBoolean(
                    R.styleable.sectionlayoutmanager_LayoutManager_slm_isSticky,
                    false);
            section = a.getInt(
                    R.styleable.sectionlayoutmanager_LayoutManager_slm_section,
                    0);

            // Header margin types can be dimension or integer (enum).
            if (a.getType(R.styleable.sectionlayoutmanager_LayoutManager_slm_headerStartMargin) ==
                    TypedValue.TYPE_DIMENSION) {
                headerStartMarginIsAuto = false;
                headerStartMargin = a.getDimensionPixelSize(
                        R.styleable.sectionlayoutmanager_LayoutManager_slm_headerStartMargin,
                        0);
            } else {
                headerStartMarginIsAuto = true;
            }
            if (a.getType(R.styleable.sectionlayoutmanager_LayoutManager_slm_headerEndMargin) ==
                    TypedValue.TYPE_DIMENSION) {
                headerEndMarginIsAuto = false;
                headerEndMargin = a.getDimensionPixelSize(
                        R.styleable.sectionlayoutmanager_LayoutManager_slm_headerEndMargin,
                        0);
            } else {
                headerEndMarginIsAuto = true;
            }

            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
            init(other);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams other) {
            super(other);
            init(other);
        }

        private void init(ViewGroup.LayoutParams other) {
            if (other instanceof LayoutParams) {
                final LayoutParams lp = (LayoutParams) other;
                isHeader = lp.isHeader;
                headerAlignment = lp.headerAlignment;
                sectionFirstPosition = lp.sectionFirstPosition;
                isSticky = lp.isSticky;
                section = lp.section;
                headerEndMargin = lp.headerEndMargin;
                headerStartMargin = lp.headerStartMargin;
                headerEndMarginIsAuto = lp.headerEndMarginIsAuto;
                headerStartMarginIsAuto = lp.headerStartMarginIsAuto;
            } else {
                isHeader = DEFAULT_IS_HEADER;
                headerAlignment = DEFAULT_HEADER_ALIGNMENT;
                isSticky = DEFAULT_IS_STICKY;
                headerEndMargin = DEFAULT_HEADER_MARGIN;
                headerStartMargin = DEFAULT_HEADER_MARGIN;
                headerStartMarginIsAuto = true;
                headerEndMarginIsAuto = true;
            }
        }


    }

    public static abstract class SlmFactory {

        abstract public SectionLayoutManager getSectionLayoutManager(int section);
    }

}
