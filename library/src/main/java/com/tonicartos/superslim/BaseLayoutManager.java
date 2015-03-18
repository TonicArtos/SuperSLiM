package com.tonicartos.superslim;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A LayoutManager that lays out mSection headers with optional stickiness and uses a map of
 * sections to view layout managers to layout items.
 */
class BaseLayoutManager extends RecyclerView.LayoutManager {

    static final int SECTION_MANAGER_CUSTOM = -1;

    static final int SECTION_MANAGER_LINEAR = 0x01;

    static final int SECTION_MANAGER_GRID = 0x02;

    static final int SECTION_MANAGER_STAGGERED_GRID = 0x03;

    private static final int NO_POSITION_REQUEST = -1;

    private int mRequestPosition = NO_POSITION_REQUEST;

    private final SectionLayoutManager mLinearSlm;

    private final SectionLayoutManager mGridSlm;

    private Rect mRect = new Rect();

    private int mRequestPositionOffset = 0;

    private HashMap<String, SectionLayoutManager> mSlms;

    private boolean mSmoothScrollEnabled = true;

    private LayoutHelper.Parent mHelperDelegate;

    private ArrayList<SectionData> mSections;

    public BaseLayoutManager(Context context) {
        mLinearSlm = new LinearSLM();
        mGridSlm = new GridSLM(context);
        mSlms = new HashMap<>();
        mHelperDelegate = new LayoutHelperDelegate(this);
    }

    BaseLayoutManager(Builder builder) {
        mLinearSlm = new LinearSLM();
        mGridSlm = new GridSLM(builder.context);
        mSlms = builder.slms;
        SectionAdapter sectionAdapter = (SectionAdapter) builder.adapter;
        mSections = SectionData.processSections(
                builder.adapter.getItemCount(), sectionAdapter.getSectionStartPositions());
        mHelperDelegate = new LayoutHelperDelegate(this);
    }

    /**
     * Add a section layout manager to those that can be used to lay out items.
     *
     * @param key Key to match that to be set in {@link LayoutParams#setSlm(String)}.
     * @param slm SectionLayoutManager to add.
     */
    public void addSlm(String key, SectionLayoutManager slm) {
        mSlms.put(key, slm);
    }

    public void getEdgeStates(Rect outRect, View child, RecyclerView.State state) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        if (params.isHeader()) {
            if (getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_LTR) {
                outRect.left = params.isHeaderStartAligned() ?
                        ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
                outRect.right = params.isHeaderStartAligned() ?
                        ItemDecorator.INTERNAL : ItemDecorator.EXTERNAL;
            } else {
                outRect.right = params.isHeaderStartAligned() ?
                        ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
                outRect.left = params.isHeaderStartAligned() ?
                        ItemDecorator.INTERNAL : ItemDecorator.EXTERNAL;
            }
            outRect.top = params.getViewPosition() == 0 ?
                    ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
            outRect.bottom = params.getViewPosition() == state.getItemCount() - 1 ?
                    ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
            return;
        }
        SectionData sd = getSectionData(params.getViewPosition());
        SectionLayoutManager slm = getSlm(sd);
        slm.getEdgeStates(outRect, child, sd, getLayoutDirection());
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
            requestedPosition = Math.min(mRequestPosition, itemCount - 1);
            mRequestPosition = NO_POSITION_REQUEST;
            borderLine = mRequestPositionOffset;
            mRequestPositionOffset = 0;
        } else {
            View anchorView = getAnchorChild();
            requestedPosition = anchorView == null ? 0 : getPosition(anchorView);
            borderLine = getBorderLine(anchorView, Direction.END);
        }

        detachAndScrapAttachedViews(recycler);

        Recycler layoutState = new Recycler(recycler);
        int bottomLine = layoutChildren(requestedPosition, borderLine, layoutState);

        fixOverscroll(bottomLine, layoutState);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        LayoutParams params = new LayoutParams(lp);
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;

        return getSlm(params).generateLayoutParams(params);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        boolean isString;
        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.superslim_LayoutManager);
        if (!a.hasValue(R.styleable.superslim_LayoutManager_slm_section_layoutManager)) {
            return new LayoutManager.LayoutParams(c, attrs);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            TypedValue value = new TypedValue();
            a.getValue(R.styleable.superslim_LayoutManager_slm_section_layoutManager, value);
            isString = value.type == TypedValue.TYPE_STRING;
        } else {
            isString =
                    a.getType(R.styleable.superslim_LayoutManager_slm_section_layoutManager)
                            == TypedValue.TYPE_STRING;
        }
        String sectionManager = null;
        int sectionManagerKind;
        if (isString) {
            sectionManager = a
                    .getString(R.styleable.superslim_LayoutManager_slm_section_layoutManager);
            if (TextUtils.isEmpty(sectionManager)) {
                sectionManagerKind = SECTION_MANAGER_LINEAR;
            } else {
                sectionManagerKind = SECTION_MANAGER_CUSTOM;
            }
        } else {
            sectionManagerKind = a
                    .getInt(R.styleable.superslim_LayoutManager_slm_section_layoutManager,
                            SECTION_MANAGER_LINEAR);
        }
        a.recycle();

        return getSlm(sectionManagerKind, sectionManager).generateLayoutParams(c, attrs);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        int numChildren = getChildCount();
        if (numChildren == 0) {
            return 0;
        }

        Recycler layoutState = new Recycler(recycler);

        final Direction direction = dy > 0 ? Direction.END : Direction.START;
        final boolean isDirectionEnd = direction == Direction.END;
        final int height = getHeight();

        final int leadingEdge = isDirectionEnd ? height + dy : dy;

        // Handle situation where total content height is less than the view height. We only
        // have to handle the end direction because we never over scroll the top or lay out
        // from the bottom up.
        if (isDirectionEnd) {
            final View end = getAnchorAtEnd();
            LayoutParams params = (LayoutParams) end.getLayoutParams();
            SectionData sd = getSectionData(params.getViewPosition());
            SectionLayoutManager slm = getSlm(sd);
            LayoutHelperImpl helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
            final int endEdge = slm.getLowestEdge(getChildCount() - 1, leadingEdge, sd, helper);
            helper.recycle();
            if (endEdge < height - getPaddingBottom() &&
                    getPosition(end) == (state.getItemCount() - 1)) {
                return 0;
            }
        }

        final int fillEdge = fillUntil(leadingEdge, direction, layoutState);

        final int delta;
        if (isDirectionEnd) {
            // Add padding so we scroll to inset area at scroll end.
            int fillDelta = fillEdge - height + getPaddingBottom();
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
                        requestLayout();
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
    public int getDecoratedMeasuredWidth(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedMeasuredWidth(child) + lp.leftMargin + lp.rightMargin;
    }

    @Override
    public int getDecoratedMeasuredHeight(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedMeasuredHeight(child) + lp.topMargin + lp.bottomMargin;
    }

    @Override
    public void layoutDecorated(View child, int left, int top, int right, int bottom) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        super.layoutDecorated(child, left + lp.leftMargin, top + lp.topMargin,
                right - lp.rightMargin, bottom - lp.bottomMargin);
    }

    @Override
    public int getDecoratedLeft(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedLeft(child) - lp.leftMargin;
    }

    @Override
    public int getDecoratedTop(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedTop(child) - lp.topMargin;
    }

    @Override
    public int getDecoratedRight(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedRight(child) + lp.rightMargin;
    }

    @Override
    public int getDecoratedBottom(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedBottom(child) + lp.bottomMargin;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();

        if (!(newAdapter instanceof SectionAdapter)) {
            throw new SectionAdapterNotImplementedRuntimeException();
        }
        SectionAdapter sectionAdapter = (SectionAdapter) newAdapter;
        mSections = SectionData.processSections(
                newAdapter.getItemCount(), sectionAdapter.getSectionStartPositions());
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

    private void attachHeaderForStart(View header, int leadingEdge, SectionData sd,
            Recycler state) {
        if (state.getCachedView(sd.firstPosition) != null
                && getDecoratedBottom(header) > leadingEdge) {
            addView(header, findLastIndexForSection(sd.firstPosition) + 1);
            state.decacheView(sd.firstPosition);
        }
    }

    /**
     * Fill out the next section as far as possible. The marker line is used as a start line to
     * position content from. If necessary, room for headers is given before laying out the section
     * content. However, headers are always added to an index after the section content.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param markerLine  Start line to begin placing content at.
     * @param recycler    Layout recycler.
     * @return Line to which content has been filled.
     */
    private int fillNextSectionToEnd(int leadingEdge, int markerLine, Recycler recycler,
            RecyclerView.State state) {
        if (markerLine >= leadingEdge) {
            return markerLine;
        }

        final View last = getAnchorAtEnd();
        int anchorPosition = getPosition(last) + 1;

        if (anchorPosition >= state.getItemCount()) {
            return markerLine;
        }

        final View header = recycler.getView(anchorPosition);
        final LayoutParams headerParams = (LayoutParams) header.getLayoutParams();
        final SectionData sd;
        if (headerParams.isHeader()) {
            measureHeader(header.view);
            sd = getSectionData(anchorPosition, header.view);
            markerLine = layoutHeaderTowardsEnd(header.view, markerLine, sd, recycler);
            anchorPosition += 1;
        } else {
            recycler.cacheView(anchorPosition, header.view);
            sd = getSectionData(anchorPosition, header.view);
        }

        if (anchorPosition < recycler.recyclerState.getItemCount()) {
            SectionLayoutManager slm = getSlm(sd);
            markerLine = slm.onFillToEnd(leadingEdge, markerLine, anchorPosition, sd, recycler);
            updateSectionDataAfterFillToEnd(sd, recycler);
        }

        if (sd.hasHeader) {
            addView(header.view);
            if (header.wasCached) {
                recycler.decacheView(sd.firstPosition);
            }
            markerLine = Math.max(getDecoratedBottom(header.view), markerLine);
        }

        return fillNextSectionToEnd(leadingEdge, markerLine, recycler);
    }

    /**
     * Fill the next section towards the start edge.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param markerLine  Start line to begin placing content at.
     * @param state       Layout state.
     * @return Line content was filled up to.
     */
    private int fillNextSectionToStart(int leadingEdge, int markerLine, Recycler state) {
        if (markerLine < leadingEdge) {
            return markerLine;
        }

        View preAnchor = getAnchorAtStart();
        LayoutParams preAnchorParams = (LayoutParams) preAnchor.getLayoutParams();
        View first = findAttachedHeaderOrFirstViewForSection(preAnchorParams.getFirstPosition(), 0,
                Direction.START);
        int anchorPosition;
        if (first != null) {
            anchorPosition = getPosition(first) - 1;
        } else {
            anchorPosition = getPosition(preAnchor) - 1;
        }

        if (anchorPosition < 0) {
            return markerLine;
        }

        Recycler.View anchor = state.getView(anchorPosition);
        LayoutParams anchorParams = anchor.getLayoutParams();

        // Now we are in our intended section to fill.
        int sfp = anchorParams.getFirstPosition();

        // Setup section data.
        View header = getHeaderOrFirstViewForSection(sfp, Direction.START, state);
        LayoutParams headerParams = (LayoutParams) header.getLayoutParams();
        if (headerParams.isHeader()) {
            measureHeader(header);
        }
        SectionData sd = getSectionData(sfp, header);
        sd.lastContentPosition = anchorPosition;

        // Fill out section.
        SectionLayoutManager slm = getSlm(sd);
        int sectionBottom = markerLine;
        if (anchorPosition >= 0) {
            markerLine = slm.onFillToStart(leadingEdge, markerLine, anchorPosition, sd, state);
        }

        // Lay out and attach header.
        if (sd.hasHeader) {
            int headerOffset = 0;
            if (!sd.headerParams.isHeaderInline() || sd.headerParams.isHeaderOverlay()) {
                View firstVisibleView = slm.findFirstVisibleView(sd.firstPosition, true);
                if (firstVisibleView == null) {
                    headerOffset = 0;
                } else {
                    headerOffset = slm
                            .computeHeaderOffset(getPosition(firstVisibleView), sd, state);
                }
            }
            markerLine = layoutHeaderTowardsStart(header, leadingEdge, markerLine, headerOffset,
                    sectionBottom, sd, state);

            attachHeaderForStart(header, leadingEdge, sd, state);
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
    private int fillToEnd(int leadingEdge, Recycler state) {
        final View anchor = getAnchorAtEnd();

        LayoutParams anchorParams = (LayoutParams) anchor.getLayoutParams();
        final int sfp = anchorParams.getFirstPosition();
        final View first = getHeaderOrFirstViewForSection(sfp, Direction.END, state);
        final SectionData sd = getSectionData(sfp, first);

        final SectionLayoutManager slm = getSlm(sd);
        int markerLine = slm.finishFillToEnd(leadingEdge, anchor, sd, state);
        updateSectionDataAfterFillToEnd(sd, state);

        View header = findAttachedHeaderForSectionFromEnd(sd.firstPosition);
        markerLine = updateHeaderForEnd(header, markerLine);

        if (markerLine <= leadingEdge) {
            markerLine = fillNextSectionToEnd(leadingEdge, markerLine, state);
        }

        return markerLine;
    }

    /**
     * Fill towards the start edge.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param state       Layout state.
     * @return Line content was filled up to.
     */
    private int fillToStart(int leadingEdge, Recycler state) {
        View anchor = getAnchorAtStart();

        LayoutParams anchorParams = (LayoutParams) anchor.getLayoutParams();
        final int sfp = anchorParams.getFirstPosition();
        final View first = getHeaderOrFirstViewForSection(sfp, Direction.START, state);
        final SectionData sd = getSectionData(sfp, first);

        final SectionLayoutManager slm = getSlm(sd);

        int markerLine;
        int anchorPosition = getPosition(anchor);
        if (anchorPosition == sd.firstPosition) {
            markerLine = getDecoratedBottom(anchor);
        } else {
            if (anchorPosition - 1 == sd.firstPosition && sd.hasHeader) {
                // Already at first content position, so no more to do.
                markerLine = getDecoratedTop(anchor);
            } else {
                markerLine = slm.finishFillToStart(leadingEdge, anchor, sd, state);
            }
        }

        markerLine = updateHeaderForStart(first, leadingEdge, markerLine, sd, state);

        if (markerLine >= leadingEdge) {
            markerLine = fillNextSectionToStart(leadingEdge, markerLine, state);
        }

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
    private int fillUntil(int leadingEdge, Direction direction, Recycler layoutState) {
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
            if (params.getFirstPosition() != sfp) {
                break;
            } else if (params.isHeader()) {
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
        if (params.getFirstPosition() != sfp) {
            return findAttachedHeaderForSectionFromStart(min, mid - 1, sfp);
        }

        if (params.isHeader()) {
            return candidate;
        }

        return findAttachedHeaderForSectionFromStart(mid + 1, max, sfp);
    }


    private int findLastIndexForSection(int sfp) {
        return binarySearchForLastPosition(0, getChildCount() - 1, sfp);
    }

    private void fixOverscroll(int bottomLine, Recycler state) {
        if (!isOverscrolled(state)) {
            return;
        }

        // Shunt content down to the bottom of the screen.
        int delta = getHeight() - getPaddingBottom() - bottomLine;
        offsetChildrenVertical(delta);

        // Fill back towards the top.
        int topLine = fillToStart(0, state);

        if (topLine > getPaddingTop()) {
            // Not enough content to fill all the way back up so we shunt it back up.
            offsetChildrenVertical(getPaddingTop() - topLine);
        }
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
        if (candidateParams.isHeader()) {
            // Try one above.
            View check = getChildAt(getChildCount() - 2);
            LayoutParams checkParams = (LayoutParams) check.getLayoutParams();
            if (checkParams.getFirstPosition() == candidateParams.getFirstPosition()) {
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
        int sfp = params.getFirstPosition();

        if (!params.isHeader()) {
            return child;
        }

        int i = 1;
        if (i < getChildCount()) {
            View candidate = getChildAt(i);
            LayoutParams candidateParams = (LayoutParams) candidate.getLayoutParams();
            if (candidateParams.getFirstPosition() == sfp) {
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
        final int sfp = params.getFirstPosition();

        final View first = findAttachedHeaderOrFirstViewForSection(sfp, 0, Direction.START);
        if (first == null) {
            return child;
        }

        final LayoutParams firstParams = (LayoutParams) first.getLayoutParams();
        if (!firstParams.isHeader()) {
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

    private SectionData getCachedSectionData(View child) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        return mSectionDataCache.get(params.getFirstPosition());
    }

    private int getDirectionToPosition(int targetPosition) {
        LayoutParams params = (LayoutParams) getChildAt(0).getLayoutParams();
        final View startSectionFirstView = getSlm(params)
                .findFirstVisibleView(params.getFirstPosition(), true);
        return targetPosition < getPosition(startSectionFirstView) ? -1 : 1;
    }


    private View getHeaderOrFirstViewForSection(int sfp, Direction direction, Recycler state) {
        View view = findAttachedHeaderOrFirstViewForSection(sfp,
                direction == Direction.START ? 0 : getChildCount() - 1, direction);
        if (view == null) {
            Recycler.View stateView = state.getView(sfp);
            view = stateView.view;
            if (stateView.getLayoutParams().isHeader()) {
                measureHeader(stateView.view);
            }
            state.cacheView(sfp, view);
        }
        return view;
    }

    public SectionData getSectionData(int position) {
        for (SectionData sd : mSections) {
            if (sd.containsItem(position)) {
                return sd;
            }
        }
        throw new MissingSectionDataRuntimeException(position);
    }

    private SectionLayoutManager getSlm(int kind, String key) {
        if (kind == SECTION_MANAGER_CUSTOM) {
            return mSlms.get(key);
        } else if (kind == SECTION_MANAGER_LINEAR) {
            return mLinearSlm;
        } else if (kind == SECTION_MANAGER_GRID) {
            return mGridSlm;
        } else if (kind == SECTION_MANAGER_STAGGERED_GRID) {
            throw new NotYetImplementedSlmException(kind);
        } else {
            throw new UnknownSectionLayoutException(kind);
        }
    }

    private SectionLayoutManager getSlm(LayoutParams params) {
        if (params.sectionManagerKind == SECTION_MANAGER_CUSTOM) {
            return mSlms.get(params.sectionManager);
        } else if (params.sectionManagerKind == SECTION_MANAGER_LINEAR) {
            return mLinearSlm;
        } else if (params.sectionManagerKind == SECTION_MANAGER_GRID) {
            return mGridSlm;
        } else if (params.sectionManagerKind == SECTION_MANAGER_STAGGERED_GRID) {
            throw new NotYetImplementedSlmException(params.sectionManagerKind);
        } else {
            throw new UnknownSectionLayoutException(params.sectionManagerKind);
        }
    }

    private SectionLayoutManager getSlm(SectionData sd) {
        SectionLayoutManager slm;
        if (sd.sectionManagerKind == SECTION_MANAGER_CUSTOM) {
            slm = mSlms.get(sd.sectionManager);
            if (slm == null) {
                throw new UnknownSectionLayoutException(sd.sectionManager);
            }
        } else if (sd.sectionManagerKind == SECTION_MANAGER_LINEAR) {
            slm = mLinearSlm;
        } else if (sd.sectionManagerKind == SECTION_MANAGER_GRID) {
            slm = mGridSlm;
        } else if (sd.sectionManagerKind == SECTION_MANAGER_STAGGERED_GRID) {
            throw new NotYetImplementedSlmException(sd.sectionManagerKind);
        } else {
            throw new UnknownSectionLayoutException(sd.sectionManagerKind);
        }

        return slm.init(sd, mHelperDelegate);
    }

    private boolean isOverscrolled(Recycler state) {
        final int itemCount = state.recyclerState.getItemCount();

        if (getChildCount() == 0) {
            return false;
        }

        View lastVisibleView = findLastCompletelyVisibleItem();
        if (lastVisibleView == null) {
            lastVisibleView = getChildAt(getChildCount() - 1);
        }

        boolean reachedBottom = getPosition(lastVisibleView) == itemCount - 1;
        if (!reachedBottom ||
                getDecoratedBottom(lastVisibleView) >= getHeight() - getPaddingBottom()) {
            return false;
        }

        View firstVisibleView = findFirstCompletelyVisibleItem();
        if (firstVisibleView == null) {
            firstVisibleView = getChildAt(0);
        }

        boolean reachedTop = getPosition(firstVisibleView) == 0
                && getDecoratedTop(firstVisibleView) == getPaddingTop();
        return !reachedTop;
    }

    /**
     * Layout views from the top.
     *
     * @param anchorPosition Position to start laying out from.
     * @param state          Layout state.  @return Line to which content has been filled. If the
     *                       line is before the leading edge then the end of the data set has been
     */
    private int layoutChildren(int anchorPosition, int borderLine, Recycler state) {
        final int height = getHeight();

        final Recycler.View anchor = state.getView(anchorPosition);
        state.cacheView(anchorPosition, anchor.view);

        final int sfp = anchor.getLayoutParams().getFirstPosition();
        final Recycler.View first = state.getView(sfp);
        measureHeader(first.view);
        state.cacheView(sfp, first.view);

        final SectionData sd = getSectionData(sfp, first.view);

        final SectionLayoutManager slm = getSlm(sd);
        // Layout header
        int markerLine = borderLine;
        int contentPosition = anchorPosition;
        if (sd.hasHeader && anchorPosition == sd.firstPosition) {
            markerLine = layoutHeaderTowardsEnd(first.view, borderLine, sd, state);
            contentPosition += 1;
        }

        // Layout first section to end.
        markerLine = slm.onFillToEnd(height, markerLine, contentPosition, sd, state);

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
            Recycler state) {
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
            int sectionBottom, SectionData sd, Recycler state) {
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

        if (sd.headerParams.isHeaderSticky() && r.top < leadingEdge &&
                sd.firstPosition != state.recyclerState.getTargetScrollPosition()) {
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

    private Rect setHeaderRectSides(Rect r, SectionData sd, Recycler state) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();

        if (sd.headerParams.isHeaderEndAligned()) {
            // Position header from end edge.
            if (!sd.headerParams.isHeaderOverlay() && !sd.headerParams.marginEndIsAuto
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
            if (!sd.headerParams.isHeaderOverlay() && !sd.headerParams.marginStartIsAuto
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
    private void trimEnd(Recycler state) {
        int height = getHeight();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (getDecoratedTop(child) >= height) {
                removeAndRecycleView(child, state.inner);
            } else {
                if (!((LayoutParams) child.getLayoutParams()).isHeader()) {
                    break;
                }
            }
        }
    }

    /**
     * Looks for any content wholly beyond the start edge. Trim is constrained items belonging to
     * sections intersecting the start edge.
     *
     * @param recycler Layout state.
     */
    private void trimStart(Recycler recycler) {
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

        // No visible views so we trim everything.
        if (anchor == null) {
            detachAndScrapAttachedViews(recycler.inner);
            return;
        }

        // Perform tasks prior to trim.
        final SectionData sd = getSectionData(getPosition(anchor));
        final LayoutHelperImpl helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
        helper.init(sd, 0, 0);
        getSlm(sd).onPreTrimAtStartEdge(anchorIndex, sd, helper);
        helper.recycle();

        // Now trim views before the first visible item.
        for (int i = 0; i < anchorIndex; i++) {
            removeAndRecycleViewAt(0, recycler.inner);
        }

        // Then trim any non-visible views remaining in the same section.
    }

    /**
     * Trim all content wholly beyond the direction edge. If the direction is START, then update the
     * header of the section intersecting the top edge.
     *
     * @param direction Direction of edge to trim against.
     * @param state     Layout state.
     */
    private void trimTail(Direction direction, Recycler state) {
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
            Recycler state) {
        if (!sd.hasHeader) {
            return markerLine;
        }

        SectionLayoutManager slm = getSlm(sd);
        int sli = findLastIndexForSection(sd.firstPosition);
        int sectionBottom = getHeight();
        for (int i = sli == -1 ? 0 : sli; i < getChildCount(); i++) {
            View view = getChildAt(i);
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            if (params.getFirstPosition() != sd.firstPosition) {
                View first = findAttachedHeaderOrFirstViewForSection(
                        params.getFirstPosition(), i,
                        Direction.START);
                if (first == null) {
                    sectionBottom = getDecoratedTop(view);
                } else {
                    sectionBottom = getDecoratedTop(first);
                }
                break;
            }
        }
        int offset = 0;
        if (!sd.headerParams.isHeaderInline() || sd.headerParams.isHeaderOverlay()) {
            View firstVisibleView = slm.findFirstVisibleView(sd.firstPosition, true);
            if (firstVisibleView == null) {
                offset = 0;
            } else {
                offset = slm.computeHeaderOffset(getPosition(firstVisibleView), sd, state);
            }
        }

        markerLine = layoutHeaderTowardsStart(header, leadingEdge, markerLine, offset,
                sectionBottom, sd, state);

        attachHeaderForStart(header, leadingEdge, sd, state);

        return markerLine;
    }

    private void updateHeaderForTrimFromStart(View header) {
        LayoutParams params = (LayoutParams) header.getLayoutParams();
        if (!params.isHeaderSticky()) {
            return;
        }

        SectionData sd = getSectionData(params.getViewPosition());
        final int sli = findLastIndexForSection(sd.firstPosition);
        if (sli == -1) {
            return;
        }

        final int sfi = findFirstIndexForSection(sd.firstPosition);

        SectionLayoutManager slm = getSlm(params);
        final int sectionBottom = slm.getLowestEdge(sli, getHeight(), sd, mHelperDelegate);
        final int sectionTop = slm.getHighestEdge(sd.firstPosition, 0, 0);

        final int height = getDecoratedMeasuredHeight(header);
        if ((params.isHeaderInline() && !params.isHeaderOverlay())
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

    public static class Builder {

        final Context context;

        HashMap<String, SectionLayoutManager> slms = new HashMap<>();

        RecyclerView.Adapter adapter;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder addSlm(String key, SectionLayoutManager slm) {
            slms.put(key, slm);
            return this;
        }

        public Builder addAdapter(RecyclerView.Adapter adapter) {
            if (!(adapter instanceof SectionAdapter)) {
                throw new SectionAdapterNotImplementedRuntimeException();
            }
            this.adapter = adapter;
            return this;
        }

        public BaseLayoutManager build() {
            return new BaseLayoutManager(this);
        }
    }

    private static class SectionAdapterNotImplementedRuntimeException extends RuntimeException {

        SectionAdapterNotImplementedRuntimeException() {
            super("Adapter must implement SectionAdapter.");
        }
    }

    public static interface SectionAdapter {

        List<Integer> getSectionStartPositions();
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        public static final int HEADER_ALIGN_START = 0x01;

        public static final int HEADER_ALIGN_END = 0x02;

        public static final int HEADER_INLINE = 0x03;

        public static final int HEADER_OVERLAY = 0x04;

        public static final int HEADER_STICKY = 0x08;

        public static final int MARGIN_AUTO = -0x01;

        private static final int NO_FIRST_POSITION = -0x01;

        private static final int DEFAULT_HEADER_MARGIN = -0x01;

        private static final int DEFAULT_HEADER_DISPLAY = 0;

        public int headerDisplay;

        public int marginEnd;

        public int marginStart;

        String sectionManager;

        int sectionManagerKind;

        private List<Integer> mSubsections;

        public LayoutParams(int width, int height) {
            super(width, height);

            sectionManagerKind = SECTION_MANAGER_LINEAR;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.superslim_LayoutManager);
            headerDisplay = a.getInt(
                    R.styleable.superslim_LayoutManager_slm_headerDisplay,
                    DEFAULT_HEADER_DISPLAY);

            // Header margin types can be dimension or integer (enum).
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                TypedValue value = new TypedValue();
                a.getValue(R.styleable.superslim_LayoutManager_slm_section_marginStart,
                        value);
                loadHeaderStartMargin(a, value.type == TypedValue.TYPE_DIMENSION);

                a.getValue(R.styleable.superslim_LayoutManager_slm_section_marginEnd, value);
                loadHeaderEndMargin(a, value.type == TypedValue.TYPE_DIMENSION);

                a.getValue(R.styleable.superslim_LayoutManager_slm_section_layoutManager, value);
                loadSlm(a, value.type == TypedValue.TYPE_STRING);
            } else {
                boolean isDimension;
                isDimension =
                        a.getType(R.styleable.superslim_LayoutManager_slm_section_marginStart)
                                == TypedValue.TYPE_DIMENSION;
                loadHeaderStartMargin(a, isDimension);

                isDimension =
                        a.getType(R.styleable.superslim_LayoutManager_slm_section_marginEnd)
                                == TypedValue.TYPE_DIMENSION;
                loadHeaderEndMargin(a, isDimension);

                boolean isString =
                        a.getType(R.styleable.superslim_LayoutManager_slm_section_layoutManager)
                                == TypedValue.TYPE_STRING;
                loadSlm(a, isString);
            }

            a.recycle();
        }

        public LayoutParams(ViewGroup.MarginLayoutParams other) {
            super(other);
            init(other);
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
            init(other);
        }

        public boolean areHeaderFlagsSet(int flags) {
            return (headerDisplay & flags) == flags;
        }

        public List<Integer> getSubsections() {
            return mSubsections;
        }

        public void setSubsections(List<Integer> sectionFirstPositions) {
            mSubsections = sectionFirstPositions;
        }

        public boolean isHeader() {
            return (headerDisplay & HEADER_INLINE) != 0;
        }

        public boolean isHeaderEndAligned() {
            return (headerDisplay & HEADER_ALIGN_END) != 0;
        }

        public boolean isHeaderInline() {
            return (headerDisplay & HEADER_INLINE) == HEADER_INLINE;
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

        /**
         * Set the layout manager for this section to a custom implementation. This custom SLM must
         * be registered via {@link #addSlm(String, com.tonicartos.superslim.SectionLayoutManager)}.
         *
         * @param key Identifier for a registered custom SLM to layout this section out with.
         */
        public void setSlm(String key) {
            sectionManagerKind = SECTION_MANAGER_CUSTOM;
            sectionManager = key;
        }

        /**
         * Set the layout manager for this section to one of the default implementations.
         *
         * @param id Kind of SLM to use.
         */
        public void setSlm(int id) {
            sectionManagerKind = id;
        }

        private void init(ViewGroup.LayoutParams other) {
            if (other instanceof LayoutParams) {
                final LayoutParams lp = (LayoutParams) other;
                headerDisplay = lp.headerDisplay;
                sectionManager = lp.sectionManager;
                sectionManagerKind = lp.sectionManagerKind;
                marginEnd = lp.marginEnd;
                marginStart = lp.marginStart;
            } else {
                headerDisplay = DEFAULT_HEADER_DISPLAY;
                marginEnd = DEFAULT_HEADER_MARGIN;
                marginStart = DEFAULT_HEADER_MARGIN;
                sectionManagerKind = SECTION_MANAGER_LINEAR;
            }
        }

        private void loadHeaderEndMargin(TypedArray a, boolean isDimension) {
            if (isDimension) {
                marginEnd = a.getDimensionPixelSize(
                        R.styleable.superslim_LayoutManager_slm_section_marginEnd, 0);
            } else {
                marginEnd = MARGIN_AUTO;
            }
        }

        private void loadHeaderStartMargin(TypedArray a, boolean isDimension) {
            if (isDimension) {
                marginStart = a.getDimensionPixelSize(
                        R.styleable.superslim_LayoutManager_slm_section_marginStart, 0);
            } else {
                marginStart = MARGIN_AUTO;
            }
        }

        private void loadSlm(TypedArray a, boolean isString) {
            if (isString) {
                sectionManager = a
                        .getString(R.styleable.superslim_LayoutManager_slm_section_layoutManager);
                if (TextUtils.isEmpty(sectionManager)) {
                    sectionManagerKind = SECTION_MANAGER_LINEAR;
                } else {
                    sectionManagerKind = SECTION_MANAGER_CUSTOM;
                }
            } else {
                sectionManagerKind = a
                        .getInt(R.styleable.superslim_LayoutManager_slm_section_layoutManager,
                                SECTION_MANAGER_LINEAR);
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

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
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

    private class NotYetImplementedSlmException extends RuntimeException {

        public NotYetImplementedSlmException(int id) {
            super("SLM not yet implemented " + id + ".");
        }
    }

    private class UnknownSectionLayoutException extends RuntimeException {

        public UnknownSectionLayoutException(String key) {
            super("No registered layout for id " + key + ".");
        }

        public UnknownSectionLayoutException(int id) {
            super("No built-in layout known by id " + id + ".");
        }
    }

    private class LayoutHelperDelegate implements LayoutHelper.Parent {

        private final BaseLayoutManager mLayoutManager;

        private int mLayoutDirection;

        LayoutHelperDelegate(BaseLayoutManager lm) {
            mLayoutManager = lm;
        }

        @Override
        public void addView(View view, int index) {
            mLayoutManager.addView(view, index);
        }

        @Override
        public void addView(View view) {
            mLayoutManager.addView(view);
        }

        @Override
        public void attachView(View header, int i) {
            mLayoutManager.attachView(header, i);
        }

        @Override
        public void attachView(View header) {
            mLayoutManager.attachView(header);
        }

        @Override
        public void detachAndScrapViewAt(int index, Recycler recycler) {
            mLayoutManager.detachAndScrapViewAt(index, recycler.inner);
        }

        @Override
        public SectionData getSectionData(int position) {
            return mLayoutManager.getSectionData(position);
        }

        @Override
        public int getBottom(View child) {
            return mLayoutManager.getDecoratedBottom(child);
        }

        @Override
        public View getChildAt(int index) {
            return mLayoutManager.getChildAt(index);
        }

        @Override
        public int getChildCount() {
            return mLayoutManager.getChildCount();
        }

        @Override
        public int getHeight() {
            return mLayoutManager.getHeight();
        }

        @Override
        public int getLayoutDirection() {
            return mLayoutDirection;
        }

        @Override
        public int getLeft(View child) {
            return mLayoutManager.getDecoratedLeft(child);
        }

        @Override
        public int getMeasuredHeight(View v) {
            return mLayoutManager.getDecoratedMeasuredHeight(v);
        }

        @Override
        public int getMeasuredWidth(View v) {
            return mLayoutManager.getDecoratedMeasuredWidth(v);
        }

        @Override
        public int getPosition(View child) {
            return mLayoutManager.getPosition(child);
        }

        @Override
        public int getRight(View child) {
            return mLayoutManager.getDecoratedRight(child);
        }

        @Override
        public SectionLayoutManager getSlm(SectionData sectionData) {
            return mLayoutManager.getSlm(sectionData);
        }

        @Override
        public int getTop(View child) {
            return mLayoutManager.getDecoratedTop(child);
        }

        @Override
        public int getWidth() {
            return mLayoutManager.getWidth() - mLayoutManager.getPaddingLeft()
                    - mLayoutManager.getPaddingRight();
        }

        @Override
        public void layoutChild(View v, int l, int t, int r, int b) {
            l += mLayoutManager.getPaddingLeft();
            r += mLayoutManager.getPaddingLeft();
            mLayoutManager.layoutDecorated(v, l, t, r, b);
        }

        @Override
        public void measureChild(View child, int widthUsed, int heightUsed) {
            mLayoutManager.measureChild(child, widthUsed, heightUsed);
        }

        @Override
        public void measureHeader(View header, int widthUsed, int heightUsed) {
            mLayoutManager.measureChild(header, widthUsed, heightUsed);
        }

        @Override
        public void detachAndScrapView(View child, Recycler recycler) {
            mLayoutManager.detachAndScrapView(child, recycler.inner);
        }

        @Override
        public void detachView(View child) {
            mLayoutManager.detachView(child);
        }

        @Override
        public void detachViewAt(int index) {
            mLayoutManager.detachViewAt(index);
        }

        @Override
        public void removeAndRecycleViewAt(int index, Recycler recycler) {
            mLayoutManager.removeAndRecycleViewAt(index, recycler.inner);
        }

        @Override
        public void removeAndRecycleView(View child, Recycler recycler) {
            mLayoutManager.removeAndRecycleView(child, recycler.inner);
        }

        @Override
        public void removeView(View child) {
            mLayoutManager.removeView(child);
        }

        @Override
        public void removeViewAt(int index) {
            mLayoutManager.removeViewAt(index);
        }

        void init(View recyclerView) {
            mLayoutDirection = ViewCompat.getLayoutDirection(recyclerView);
        }
    }

    private class MissingSectionDataRuntimeException extends RuntimeException {

        public MissingSectionDataRuntimeException(int position) {
            super("Missing section data for item at position " + position + ".");
        }
    }
}
