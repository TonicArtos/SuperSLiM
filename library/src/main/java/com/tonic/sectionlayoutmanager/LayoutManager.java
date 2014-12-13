package com.tonic.sectionlayoutmanager;

import android.content.Context;
import android.content.res.TypedArray;
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

    private static final int DEFAULT_STICKIED_POSITION = -0x01;

    private int mStickiedPosition = DEFAULT_STICKIED_POSITION;

    private SlmFactory mSlmFactory = new SlmFactory() {
        @Override
        public SectionLayoutManager getSectionLayoutManager(int section) {
            return new LinearSectionLayoutManager();
        }
    };

    private SparseArray<CachedView> mPendingFloatingHeaders = new SparseArray<CachedView>();

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
        final int anchorItemPosition = getAnchorItemPosition(state);
        detachAndScrapAttachedViews(recycler);
        fillFrom(anchorItemPosition, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        //Take top measurements from the top view as determined by the section layout manager.
        final View firstVisibleChild = getChildAt(0);
        View topView;

        LayoutParams lp = (LayoutParams) firstVisibleChild.getLayoutParams();
        if (lp.isHeader) {
            // First visible view is a header so we don't need to refer to the section layout
            // manager as to which view really is top most.
            topView = firstVisibleChild;
        } else {
            SectionLayoutManager sectionLayoutManager = mSlmFactory
                    .getSectionLayoutManager(lp.section);
            sectionLayoutManager.setLayoutManager(this);
            topView = sectionLayoutManager.getTopView(lp.section);
        }
        boolean firstItemReached = getPosition(firstVisibleChild) == 0;

        //Take bottom measurements from the bottom view as determined by the section layout
        // manager.
        View lastVisibleChild = getChildAt(getChildCount() - 1);
        View bottomView;

        // Skip overlay headers which mock being last (except when they have empty sections and
        // really are last).
        lp = (LayoutParams) lastVisibleChild.getLayoutParams();
        if (lp.isHeader) {
            if (lp.headerAlignment == HEADER_OVERLAY_START
                    || lp.headerAlignment == HEADER_OVERLAY_END) {
                // Check the one above. If it belongs to another section then the overlay header
                // is really the last visible view.
                View oneAbove = getChildAt(getChildCount() - 2);
                LayoutParams oneAboveLp = (LayoutParams) oneAbove.getLayoutParams();
                if (oneAboveLp.section == lp.section) {
                    lastVisibleChild = oneAbove;
                    lp = oneAboveLp;
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
        if (lastItemReached && viewSpan <= getHeight()) {
            //We cannot scroll in either direction
            return 0;
        }

        int delta;
        if (dy > 0) {
            // Scrolling to bottom.
            if (lastItemReached) {
                int offset = getHeight() - getDecoratedBottom(bottomView) + getPaddingBottom();
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

        if (dy > 0) {
            if (!lastItemReached) {
                fillFrom(getPosition(bottomView), recycler, state);
            }
        } else {
            if (!firstItemReached) {
                fillFrom(getPosition(topView), recycler, state);
            }
        }

        Log.d("Scroll", "Child count " + getChildCount());

        return -delta;
    }

    private void fillFrom(int position, RecyclerView.Recycler recycler, RecyclerView.State rvs) {
        LayoutState state = new LayoutState(this, recycler, rvs, getChildCount());

        final int itemCount = state.recyclerState.getItemCount();
        if (itemCount == 0) {
            // Nothing to do.
            return;
        }

        state.detachAndCacheAllViews();

        View marker = state.getCachedView(position);
        int borderline;
        if (marker == null) {
            borderline = 0;
        } else {
            borderline = getDecoratedTop(marker);
        }
        // Fill above the border line to the start.
        state.markerLine = borderline;
        fillToStart(state, position - 1);

        // Fill below the border line to the end.
        state.markerLine = borderline;
        fillToEnd(state, position);

        state.recycleCache();
    }

    private void fillToEnd(LayoutState state, int position) {
        final int itemCount = state.recyclerState.getItemCount();
        final int height = getHeight();
        int currentPosition = position;

        if (currentPosition >= itemCount) {
            return;
        }

        Log.d("Fill to end position", "" + position);

        while (true) {
            View peek = state.recycler.getViewForPosition(currentPosition);
            LayoutParams lp = (LayoutParams) peek.getLayoutParams();
            state.section = lp.section;
            state.sectionFirstPosition = lp.sectionFirstPosition;
            state.headerOverlap = 0;

            View header = makeAndAddHeaderView(state, currentPosition, Direction.END);
            int headerBottom = state.markerLine;
            if (header != null) {
                currentPosition += 1;
                headerBottom = getDecoratedBottom(header);
            }

            if (currentPosition >= itemCount) {
                break;
            }

            SectionLayoutManager sectionManager = getSectionLayoutManager(state.section);

            sectionManager.setLayoutManager(this);
            int count = sectionManager
                    .fill(state, currentPosition);

            // Make sure the next section starts after this section or the header if the header
            // drops below it.
            state.markerLine = Math.max(state.markerLine, headerBottom);
            int nextPosition = count + currentPosition;
            boolean finishing = nextPosition >= itemCount || state.markerLine >= height;
            boolean sectionFinished = finishing;

            if (!finishing) {
                peek = state.recycler.getViewForPosition(nextPosition);
                lp = (LayoutParams) peek.getLayoutParams();
                sectionFinished = state.section != lp.section;
            }

            if (sectionFinished) {
                View floatingHeader = mPendingFloatingHeaders.get(state.section);
                if (floatingHeader != null) {
                    addView(floatingHeader, -1);
                }
            }

            currentPosition = nextPosition;

            if (finishing) {
                break;
            }
        }
    }

    private void fill(SectionLayoutManager.Direction direction, final int anchorPosition,
            RecyclerView.Recycler recycler, RecyclerView.State rvs) {
        LayoutState state = new LayoutState(this, recycler, rvs, getChildCount());
        final int itemCount = state.recyclerState.getItemCount();
        final int recyclerViewHeight = getHeight();

        if (anchorPosition >= itemCount || anchorPosition < 0) {
            return;
        }

        state.direction = direction;
        final int borderline = getBorderLine(state, anchorPosition);
        state.markerLine = borderline;
        int currentPosition = anchorPosition;
        /**
         * When filling to start the first section may be incomplete requiring a pass to end.
         * This only matters for filling in floating headers in the correct place.
         */
        boolean pastIncompleteSection = false;
        boolean fillToEndDone = false;
        boolean fillToStartDone = false;

        while (true) {
            // Look at the current view and find out details.
            state.setSectionData(
                    (LayoutParams) state.getView(currentPosition).view.getLayoutParams());

            SectionLayoutManager sectionManager = getSectionLayoutManager(state.section);
            sectionManager.setLayoutManager(this);
            state.setSectionData(sectionManager);

            CachedView sectionHeader = loadSectionHeader(state);

            // Check to see if we are actually going to fill a complete section.
            if (!pastIncompleteSection && state.isDirectionStart()) {
                pastIncompleteSection = ((LayoutParams) state.getView(currentPosition + 1).view
                        .getLayoutParams()).section != state.section;
            }

            if (state.isDirectionEnd()) {
                layoutHeader(state, sectionHeader);
            }
            int count = sectionManager.fill(state, currentPosition);
            if (state.isDirectionStart()) {
                layoutHeader(state, sectionHeader);
            }

            // Make sure floating header is after all views of the current section.
            if (state.isDirectionEnd() || pastIncompleteSection) {
                final int floatingPosition = state.isDirectionStart() ? count : -1;
                final CachedView floatingHeader = mPendingFloatingHeaders.get(state.section);
                if (floatingHeader != null) {
                    if (floatingHeader.wasCached) {
                        attachView(floatingHeader.view, floatingPosition);
                        state.decacheView(state.sectionFirstPosition);
                    } else {
                        addView(floatingHeader.view, floatingPosition);
                    }
                }
            }

            pastIncompleteSection = true;

            if (!fillToEndDone && state.isDirectionStart() && (state.markerLine <= 0 || currentPosition < 0)) {
                state.direction = SectionLayoutManager.Direction.END;
                state.markerLine = borderline;
                currentPosition = anchorPosition + 1;
                fillToStartDone = true;
                if (currentPosition >= itemCount || state.markerLine >= recyclerViewHeight) {
                    fillToStartDone = true;
                    break;
                }
            } else if (!fillToStartDone && state.isDirectionEnd() && (state.markerLine >= recyclerViewHeight || currentPosition >= itemCount)) {
                state.direction = SectionLayoutManager.Direction.START;
                state.markerLine = borderline;
                currentPosition = anchorPosition - 1;
                fillToEndDone = true;
                if (currentPosition < 0 || state.markerLine <= 0) {
                    fillToStartDone = true;
                    break;
                }
            }
        }
    }

    private int getBorderLine(LayoutState state, int anchorPosition) {
        int borderline;
        final View marker = state.getCachedView(anchorPosition);
        if (marker == null) {
            borderline = 0;
        } else if (state.isDirectionStart()) {
            borderline = getDecoratedBottom(marker);
        } else {
            borderline = getDecoratedTop(marker);
        }
        return borderline;
    }

    private CachedView loadSectionHeader(LayoutState state) {
        CachedView sectionHeader = state.getView(state.sectionFirstPosition);
        LayoutParams headerLp = (LayoutParams) sectionHeader.view.getLayoutParams();
        if (headerLp.isHeader && headerLp.headerAlignment != HEADER_INLINE) {
            measureHeader(sectionHeader);
            state.headerOverlap = getDecoratedMeasuredHeight(sectionHeader.view);
        } else {
            state.headerOverlap = 0;
        }

        if (!headerLp.isHeader) {
            sectionHeader = null;
        }

        return sectionHeader;
    }

    private void fillToStart(LayoutState state, int position) {
        final int itemCount = state.recyclerState.getItemCount();

        int currentPosition = position;
        if (currentPosition >= itemCount || currentPosition < 0) {
            return;
        }

        boolean completeSection = false;

        Log.d("Fill to start position", "" + position);

        while (true) {
            final View peek = state.recycler.getViewForPosition(currentPosition);
            final LayoutParams lp = (LayoutParams) peek.getLayoutParams();
            state.sectionFirstPosition = lp.sectionFirstPosition;
            state.section = lp.section;

            SectionLayoutManager sectionManager = getSectionLayoutManager(state.section);

            sectionManager.setLayoutManager(this);

            CachedView view = state.getView(state.sectionFirstPosition);
            LayoutParams viewLp = (LayoutParams) view.view.getLayoutParams();
            if (viewLp.isHeader && viewLp.headerAlignment != HEADER_INLINE) {
                if (!view.wasCached) {
                    measureHeader(view.view);
                }
                state.headerOverlap = getDecoratedMeasuredHeight(view.view);
            } else {
                state.headerOverlap = 0;
            }

            int count = sectionManager
                    .fill(state, currentPosition);

            // Skip over the items already handled by the section manager.
            currentPosition = currentPosition - count;
            if (currentPosition < 0) {
                break;
            }

            // Attempt to add the first item of section as a header,
            // if it is a header we will get it as a result to check later.
            View headerView = makeAndAddHeaderView(state, lp.sectionFirstPosition,
                    Direction.START);

            if (completeSection) {
                // Attach floating header for this section after views from the section so it
                // will be drawn over them.
                View floatingHeader = mPendingFloatingHeaders.get(lp.section);
                if (floatingHeader != null) {
                    addView(floatingHeader, getChildCount() - count);
                }
            }

            // Skip over the current item if it was a header.
            if (headerView != null) {
                currentPosition -= 1;
                if (currentPosition < 0) {
                    break;
                }
            }

            if (state.markerLine <= 0) {
                break;
            }

            completeSection = true;
        }
    }

    private SectionLayoutManager getSectionLayoutManager(int section) {
        SectionLayoutManager sectionManager = mSlmFactory
                .getSectionLayoutManager(section);
        if (sectionManager == null) {
            sectionManager = new LinearSectionLayoutManager();
        }
        return sectionManager;
    }

    private View makeAndAddHeaderView(LayoutState state, int position, Direction direction) {
        CachedView r = state.getView(position);

        LayoutParams lp = (LayoutParams) r.view.getLayoutParams();

        if (!lp.isHeader) {
            return null;
        }

        if (r.wasCached) {
            attachView(r.view, direction == Direction.START ? 0 : -1);
            int height = getDecoratedMeasuredHeight(r.view);
            if (lp.headerAlignment == HEADER_INLINE) {
                state.markerLine += direction == Direction.START ? -height : +height;
            }
            state.decacheView(position);
        } else {
            setupHeader(state, r.view, direction);
        }

        return r.view;
    }


    private void layoutHeader(LayoutState state, CachedView header) {
        if (header == null) {
            return;
        }

        final int width = getDecoratedMeasuredWidth(header.view);
        final int height = getDecoratedMeasuredHeight(header.view);

        LayoutParams lp = (LayoutParams) header.view.getLayoutParams();

        if (lp.headerAlignment == HEADER_OVERLAY_START
                || lp.headerAlignment == HEADER_OVERLAY_END) {
            mPendingFloatingHeaders.put(lp.section, header);
        } else if (!lp.isItemRemoved()) {
            if (header.wasCached) {
                attachView(header.view,
                        state.isDirectionStart() ? 0 : -1);
                state.decacheView(state.sectionFirstPosition);
            } else {
                addView(header.view,
                        state.isDirectionStart() ? 0 : -1);
            }
        }

        if (header.wasCached) {
            return;
        }

        // TODO: RTL layout stuff
        int top;
        int bottom;
        int right;
        int left;
        if (lp.headerAlignment == HEADER_ALIGN_END) {
            // Align header with end margin or end edge of recycler view.
            if (lp.headerEndMargin >= 0) {
                left = getWidth() - lp.headerEndMargin;
                right = left + width;
            } else {
                right = getWidth();
                left = right - width;
            }
        } else if (lp.headerAlignment == HEADER_ALIGN_START) {
            // Align header with start margin or start edge of recycler view.
            if (lp.headerStartMargin >= 0) {
                right = lp.headerStartMargin;
                left = right - width;
            } else {
                left = 0;
                right = width;
            }
        } else {
            left = 0;
            right = getWidth();
        }

        if (lp.headerAlignment == HEADER_INLINE
                && state.isDirectionStart()) {
            // Position header above marker line (bottom is aligned to line).
            top = state.markerLine - height;
            bottom = state.markerLine;
        } else {
            // Position header below marker line.
            top = state.markerLine;
            bottom = state.markerLine + height;
        }

        if (state.headerOffset > 0) {
            top -= state.headerOffset;
            state.headerOffset = LayoutState.NO_HEADER_OFFSET;
        }

        layoutDecorated(header.view, left, top, right, bottom);

        if (lp.headerAlignment == HEADER_INLINE) {
            if (state.isDirectionStart()) {
                // Align marker line to align with top of header.
                state.markerLine = top;
            } else {
                // Align marker line to align with bottom of header.
                state.markerLine = bottom;
            }
        }
    }

    private void measureHeader(CachedView header) {
        if (header.wasCached) {
            return;
        }

        // Width to leave for the section to which this header belongs. Only applies if the
        // header is being laid out adjacent to the section.
        int unavailableWidth = 0;
        LayoutParams lp = (LayoutParams) header.view.getLayoutParams();
        if (lp.headerAlignment == HEADER_ALIGN_START && lp.headerStartMargin >= 0) {
            unavailableWidth = getWidth() - lp.headerStartMargin;
        } else if (lp.headerAlignment == HEADER_ALIGN_END && lp.headerEndMargin >= 0) {
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
            final View view = getChildAt(i);

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
                headerStartMargin = a.getDimensionPixelSize(
                        R.styleable.sectionlayoutmanager_LayoutManager_slm_headerStartMargin,
                        0);
            } else {
                headerStartMargin = a
                        .getInt(R.styleable
                                        .sectionlayoutmanager_LayoutManager_slm_headerStartMargin,
                                0);
            }
            if (a.getType(R.styleable.sectionlayoutmanager_LayoutManager_slm_headerEndMargin) ==
                    TypedValue.TYPE_DIMENSION) {
                headerEndMargin = a.getDimensionPixelSize(
                        R.styleable.sectionlayoutmanager_LayoutManager_slm_headerEndMargin,
                        0);
            } else {
                headerEndMargin = a
                        .getInt(R.styleable.sectionlayoutmanager_LayoutManager_slm_headerEndMargin,
                                0);
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
            } else {
                isHeader = DEFAULT_IS_HEADER;
                headerAlignment = DEFAULT_HEADER_ALIGNMENT;
                isSticky = DEFAULT_IS_STICKY;
                headerEndMargin = DEFAULT_HEADER_MARGIN;
                headerStartMargin = DEFAULT_HEADER_MARGIN;
            }
        }


    }

    public static abstract class SlmFactory {

        abstract public SectionLayoutManager getSectionLayoutManager(int section);
    }

    /**
     * State to track the current top markerLine views are being markerLine relative to.
     */
    public static class LayoutState {

        public static final int NO_HEADER_OFFSET = -1;

        public final RecyclerView.Recycler recycler;

        public final RecyclerView.State recyclerState;

        public final SparseArray<View> viewCache;

        private final RecyclerView.LayoutManager mLayoutManager;

        public int headerOverlap;

        public int markerLine;

        public int sectionFirstPosition;

        public int section;

        public int headerOffset = NO_HEADER_OFFSET;

        public SectionLayoutManager.Direction direction;

        private int headerStartMargin;

        private int headerEndMargin;

        public LayoutState(RecyclerView.LayoutManager layoutManager, RecyclerView.Recycler recycler,
                RecyclerView.State recyclerState, int cacheSize) {
            viewCache = new SparseArray<View>(cacheSize);
            this.recyclerState = recyclerState;
            this.recycler = recycler;
            mLayoutManager = layoutManager;
        }

        public void cacheView(int position, View view) {
            viewCache.put(position, view);
        }

        public void decacheView(int position) {
            viewCache.remove(position);
        }

        public void recycleCache() {
            for (int i = 0; i < viewCache.size(); i++) {
                recycler.recycleView(viewCache.valueAt(i));
            }
        }

        public void detachCachedViews() {
            for (int i = 0; i < viewCache.size(); i++) {
                mLayoutManager.detachView(viewCache.valueAt(i));
            }
        }

        public void cacheAllViews() {
            for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
                final View child = mLayoutManager.getChildAt(i);
                final int childPosition = mLayoutManager.getPosition(child);
                cacheView(childPosition, child);
            }
        }

        public void detachAndCacheAllViews() {
            cacheAllViews();
            detachCachedViews();
        }

        public View getCachedView(int position) {
            return viewCache.get(position);
        }

        public CachedView getView(int position) {
            View child = getCachedView(position);
            boolean wasCached = child != null;
            if (child == null) {
                child = recycler.getViewForPosition(position);
            }

            return new CachedView(child, wasCached);
        }

        public void setSectionData(LayoutParams lp) {
            section = lp.section;
            sectionFirstPosition = lp.sectionFirstPosition;
            headerStartMargin = lp.headerStartMargin;
            headerEndMargin = lp.headerEndMargin;
        }

        public void setSectionData(SectionLayoutManager sectionManager) {
            int startMargin = sectionManager.getHeaderStartMargin();
            int endMargin = sectionManager.getHeaderEndMargin();

            if (startMargin >= 0) {
                headerStartMargin = startMargin;
            }

            if (endMargin >= 0) {
                headerEndMargin = endMargin;
            }
        }

        private boolean isDirectionStart() {
            return direction == SectionLayoutManager.Direction.START;
        }

        private boolean isDirectionEnd() {
            return direction == SectionLayoutManager.Direction.END;
        }
    }

    public static class CachedView {

        public final View view;

        public final boolean wasCached;

        public CachedView(View child, boolean wasCached) {
            this.view = child;
            this.wasCached = wasCached;
        }

    }
}
