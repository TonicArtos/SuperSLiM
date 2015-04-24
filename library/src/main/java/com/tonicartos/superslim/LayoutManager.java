package com.tonicartos.superslim;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A LayoutManager that lays out mSection headers with optional stickiness and uses a map of
 * sections to view layout managers to layout items.
 */
public class LayoutManager extends RecyclerView.LayoutManager {

    final static int DIRECTION_END = 0;

    final static int DIRECTION_START = 1;

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

    public LayoutManager(Context context) {
        mLinearSlm = new LinearSLM();
        mGridSlm = new GridSLM(context);
        mSlms = new HashMap<>();
        mHelperDelegate = new LayoutHelperDelegate(this);
    }

    // Suppress unchecked list assignment warning.
    @SuppressWarnings("unchecked")
    LayoutManager(Builder builder) {
        mLinearSlm = new LinearSLM();
        mGridSlm = new GridSLM(builder.context);
        mSlms = builder.slms;
        SectionAdapter sectionAdapter = (SectionAdapter) builder.adapter;
        mSections = SectionData.processSectionGraph(
                builder.adapter.getItemCount(), sectionAdapter.getSections());
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

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        if (getChildCount() == 0 || state.getItemCount() == 0) {
            return 0;
        }

        if (!mSmoothScrollEnabled) {
            return getChildCount();
        }

        float contentInView = getChildCount();

        // Work out fraction of content lost off top and bottom.
        contentInView -= getFractionOfContentAbove(true);
        contentInView -= getFractionOfContentBelow(true);

        return (int) (contentInView / state.getItemCount() * getHeight());
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        if (getChildCount() == 0 || state.getItemCount() == 0) {
            return 0;
        }

        final View child = getChildAt(0);
        if (!mSmoothScrollEnabled) {
            return getPosition(child);
        }

        float contentAbove = getPosition(child);
        contentAbove += getFractionOfContentAbove(false);
        return (int) (contentAbove / state.getItemCount() * getHeight());
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        if (!mSmoothScrollEnabled) {
            return state.getItemCount();
        }

        return getHeight();
    }

    public int findFirstCompletelyVisibleItemPosition() {
        View view = findOneVisibleChild(0, getChildCount(), true);
        return view == null ? RecyclerView.NO_POSITION : getPosition(view);
    }

    public int findFirstVisibleItemPosition() {
        View view = findOneVisibleChild(0, getChildCount(), false);
        return view == null ? RecyclerView.NO_POSITION : getPosition(view);
    }

    public int findLastCompletelyVisibleItemPosition() {
        View view = findOneVisibleChild(getChildCount() - 1, -1, true);
        return view == null ? RecyclerView.NO_POSITION : getPosition(view);
    }

    public int findLastVisibleItemPosition() {
        View view = findOneVisibleChild(getChildCount() - 1, -1, false);
        return view == null ? RecyclerView.NO_POSITION : getPosition(view);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        LayoutParams params = LayoutParams.from(lp);
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
    public int getDecoratedBottom(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedBottom(child) + lp.bottomMargin;
    }

    @Override
    public int getDecoratedLeft(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedLeft(child) - lp.leftMargin;
    }

    @Override
    public int getDecoratedMeasuredHeight(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedMeasuredHeight(child) + lp.topMargin + lp.bottomMargin;
    }

    @Override
    public int getDecoratedMeasuredWidth(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedMeasuredWidth(child) + lp.leftMargin + lp.rightMargin;
    }

    @Override
    public int getDecoratedRight(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedRight(child) + lp.rightMargin;
    }

    @Override
    public int getDecoratedTop(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        return super.getDecoratedTop(child) - lp.topMargin;
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
        SectionLayoutManager slm = getSlm(sd, mHelperDelegate);
        slm.getEdgeStates(outRect, child, sd, getLayoutDirection());
    }

    public SectionData getSectionData(int position) {
        for (SectionData sd : mSections) {
            if (sd.containsItem(position)) {
                return sd;
            }
        }
        throw new MissingSectionDataRuntimeException(position);
    }

    @Override
    public void layoutDecorated(View child, int left, int top, int right, int bottom) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child
                .getLayoutParams();
        super.layoutDecorated(child, left + lp.leftMargin, top + lp.topMargin,
                right - lp.rightMargin, bottom - lp.bottomMargin);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();

        if (!(newAdapter instanceof SectionAdapter)) {
            throw new SectionAdapterNotImplementedRuntimeException();
        }
        SectionAdapter sectionAdapter = (SectionAdapter) newAdapter;
        //noinspection unchecked
        mSections = SectionData.processSectionGraph(
                newAdapter.getItemCount(), sectionAdapter.getSections());
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        super.onItemsChanged(recyclerView);

        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (!(adapter instanceof SectionAdapter)) {
            throw new SectionAdapterNotImplementedRuntimeException();
        }
        SectionAdapter sectionAdapter = (SectionAdapter) adapter;
        //noinspection unchecked
        mSections = SectionData.processSectionGraph(
                adapter.getItemCount(), sectionAdapter.getSections());
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount);

        for (SectionData sd : mSections) {
            sd.updateInitStatus(positionStart, itemCount);
            mLinearSlm.clearConfigurationForSection(sd);
            mGridSlm.clearConfigurationForSection(sd);
            for (SectionLayoutManager slm : mSlms.values()) {
                slm.clearConfigurationForSection(sd);
            }
        }

        View first = getChildAt(0);
        int count = getChildCount();
        View last = getChildAt(count - 1);
        if (positionStart + itemCount <= getPosition(first)) {
            return;
        }

        if (positionStart <= getPosition(last)) {
            requestLayout();
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler r, RecyclerView.State state) {
        final int itemCount = state.getItemCount();
        if (itemCount == 0) {
            detachAndScrapAttachedViews(r);
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
            View anchorView = findAnchorChild();
            requestedPosition = anchorView == null ?
                    0 : Math.min(getPosition(anchorView), itemCount - 1);
            borderLine = getBorderLine(anchorView, DIRECTION_END);
        }

        detachAndScrapAttachedViews(r);

        Recycler recycler = new Recycler(r);
        int bottomLine = layoutChildren(requestedPosition, borderLine, recycler, state);

        fixOverscroll(bottomLine, recycler, state);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        mRequestPosition = ((SavedState) state).anchorPosition;
        mRequestPositionOffset = ((SavedState) state).anchorOffset;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        View view = findAnchorChild();
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
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        int numChildren = getChildCount();
        if (numChildren == 0) {
            return 0;
        }

        Recycler layoutState = new Recycler(recycler);

        final int direction = dy > 0 ? DIRECTION_END : DIRECTION_START;
        final boolean isDirectionEnd = direction == DIRECTION_END;
        final int height = getHeight();
        final int leadingEdge = isDirectionEnd ? height + dy : dy;

        // Handle situation where total content height is less than the view height. We only
        // have to handle the end direction because we never over scroll the top or lay out
        // from the bottom up.
        final int fillEdge;
        if (isDirectionEnd) {
            if (scrollToEndCompleted(leadingEdge, state)) {
                return 0;
            }
            fillEdge = fillToEnd(leadingEdge, layoutState, state);
        } else {

            fillEdge = fillToStart(leadingEdge, layoutState, state);
        }

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

            trimTail(isDirectionEnd ? DIRECTION_START : DIRECTION_END, layoutState);
        }

        layoutState.recycleCache();

        return delta;
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

                    @Override
                    protected int getVerticalSnapPreference() {
                        return LinearSmoothScroller.SNAP_TO_START;
                    }

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
                };
                smoothScroller.setTargetPosition(position);
                startSmoothScroll(smoothScroller);
            }
        });
    }

    /**
     * Fill out the next section as far as possible. If it wasn't far enough, do the next section…
     * The marker line is used as a start line to position content from. If necessary, room for
     * headers is given before laying out the section content. However, headers are always added to
     * an index after the section content.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param markerLine  Start line to begin placing content at.
     * @param recycler    Layout recycler.
     * @return Line to which content has been filled.
     */
    private int fillNextSectionsToEnd(int leadingEdge, int markerLine, Recycler recycler,
            RecyclerView.State state) {
        if (markerLine >= leadingEdge) {
            return markerLine;
        }

        // Find anchor.
        int position = getPosition(getChildAt(getChildCount() - 1));
        SectionData priorSd = getSectionData(position);
        final int anchorPosition = priorSd.lastPosition + 1;

        if (anchorPosition >= state.getItemCount()) {
            return markerLine;
        }

        // Setup section.
        final SectionData sd = getSectionData(anchorPosition);
        final LayoutHelper helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
        sd.init(helper, recycler.getView(sd.firstPosition));
        helper.init(sd, markerLine, leadingEdge, markerLine);
        final SectionLayoutManager slm = getSlm(sd, helper);

        // Layout section.
        markerLine = slm.beginFillToEnd(anchorPosition, sd, helper, recycler, state);
        helper.recycle();

        // Layout next section.
        return fillNextSectionsToEnd(leadingEdge, markerLine, recycler, state);
    }

    /**
     * Fill the next section towards the start edge.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param markerLine  Start line to begin placing content at.
     * @param recycler    Layout state.
     * @return Line content was filled up to.
     */
    private int fillNextSectionsToStart(int leadingEdge, int markerLine, Recycler recycler,
            RecyclerView.State state) {
        if (markerLine <= leadingEdge) {
            return markerLine;
        }

        // Find anchor.
        int position = getPosition(getChildAt(0));
        SectionData priorSd = getSectionData(position);
        final int anchorPosition = priorSd.firstPosition - 1;

        if (anchorPosition < 0) {
            return markerLine;
        }

        // Fill section to start.
        final SectionData sd = getSectionData(anchorPosition);
        final LayoutHelper helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
        sd.init(helper, recycler.getView(sd.firstPosition));
        helper.init(sd, markerLine, leadingEdge, leadingEdge);

        final SectionLayoutManager slm = getSlm(sd, helper);

        // Layout section.
        markerLine = slm.beginFillToStart(anchorPosition, sd, helper, recycler, state);
        helper.recycle();

        // Layout next section.
        return fillNextSectionsToStart(leadingEdge, markerLine, recycler, state);
    }

    /**
     * Fill the space between the last content item and the leadingEdge.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param state       Layout state.  @return Line to which content has been filled. If the line
     *                    is before the leading edge then the end of the data set has been reached.
     */
    private int fillToEnd(int leadingEdge, Recycler recycler, RecyclerView.State state) {
        final int lastIndex = getChildCount() - 1;
        final View lastChild = getChildAt(lastIndex);
        final SectionData sd = getSectionData(getPosition(lastChild));
        final LayoutHelperImpl helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
        final int tempMarkerLine = 0;
        helper.init(sd, tempMarkerLine, leadingEdge, leadingEdge);

        // Get a marker line from the position that content has been filled up to. We don't know
        // how the section lays itself out so we have to ask it for the lowest edge.
        final SectionLayoutManager slm = getSlm(sd, helper);
        int markerLine = slm.getLowestEdge(lastIndex, 0, sd, helper);

        int lastPosition = getPosition(lastChild);
        if (sd.hasHeader && lastPosition == sd.firstPosition && lastIndex > 0) {
            final int candidatePosition = getPosition(getChildAt(lastIndex - 1));
            if (sd.containsItem(candidatePosition)) {
                lastPosition = candidatePosition;
            }
        }

        helper.updateMarkerLine(tempMarkerLine, markerLine);
        helper.init(sd, markerLine, leadingEdge, leadingEdge);
        markerLine = slm.finishFillToEnd(lastPosition + 1, sd, helper, recycler, state);
        helper.recycle();

        return fillNextSectionsToEnd(leadingEdge, markerLine, recycler, state);
    }

    /**
     * Fill towards the start edge.
     *
     * @param leadingEdge Line to fill up to. Content will not be wholly beyond this line.
     * @param recycler    Layout state.
     * @return Line content was filled up to.
     */
    private int fillToStart(int leadingEdge, Recycler recycler, RecyclerView.State state) {
        final int firstIndex = 0;
        int firstPosition = getPosition(getChildAt(firstIndex));
        SectionData sd = getSectionData(firstPosition);
        final LayoutHelperImpl helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
        final int tempMarkerLine = 0;
        helper.init(sd, tempMarkerLine, leadingEdge, leadingEdge);

        // Get a marker line from the position that content has been filled up to. We don't know
        // how the section lays itself out so we have to ask it for the highest edge.
        final SectionLayoutManager slm = getSlm(sd, helper);
        int markerLine = slm.getHighestEdge(firstIndex, tempMarkerLine, sd, helper);

        helper.updateMarkerLine(tempMarkerLine, markerLine);
        helper.init(sd, markerLine, leadingEdge, leadingEdge);
        markerLine = slm.finishFillToStart(firstPosition - 1, sd, helper, recycler, state);
        helper.recycle();

        return fillNextSectionsToStart(leadingEdge, markerLine, recycler, state);
    }

    /**
     * Find anchor view at end.
     *
     * @return View at end, only header if it is the only one in the last section.
     */
    private View findAnchorAtEnd() {
        if (getChildCount() == 1) {
            return getChildAt(0);
        }
        View candidate = getChildAt(getChildCount() - 1);
        LayoutParams candidateParams = (LayoutParams) candidate.getLayoutParams();
        SectionData sd = getSectionData(candidateParams.getViewPosition());
        if (candidateParams.isHeader()) {
            // Try one above.
            View check = getChildAt(getChildCount() - 2);
            if (sd.containsItem(check)) {
                candidate = check;
            }
        }
        return candidate;
    }

    /**
     * Find anchor at start.
     *
     * @return View at start, only header if it is the only one in the first section.
     */
    private View findAnchorAtStart() {
        View candidate = getChildAt(0);
        LayoutParams params = (LayoutParams) candidate.getLayoutParams();
        SectionData sd = getSectionData(params.getViewPosition());

        if (!params.isHeader()) {
            return candidate;
        }

        int i = 1;
        if (i < getChildCount()) {
            candidate = getChildAt(i);
            if (sd.containsItem(candidate)) {
                return candidate;
            }
        }

        return candidate;
    }

    /**
     * Find the first view in the hierarchy that can act as an anchor.
     *
     * @return The anchor view, or null if no view is a valid anchor.
     */
    private View findAnchorChild() {
        if (getChildCount() == 0) {
            return null;
        }

        final View child = getChildAt(0);
        SectionData sd = getSectionData(getPosition(child));
        if (!sd.hasHeader) {
            return child;
        }

        final int headerIndex = Utils.findHeaderIndexFromFirstIndex(0, sd, mHelperDelegate);
        if (headerIndex == Utils.INVALID_INDEX) {
            return child;
        }

        final View header = getChildAt(headerIndex);

        LayoutParams headerParams = (LayoutParams) header.getLayoutParams();
        if (headerParams.isHeaderInline() && !headerParams.isHeaderOverlay()) {
            if (getDecoratedBottom(header) <= getDecoratedTop(child)) {
                return header;
            } else {
                return child;
            }
        }

        if (getDecoratedTop(child) < getDecoratedTop(header)) {
            return child;
        }

        if (sd.firstPosition + 1 == getPosition(child)) {
            return header;
        }

        return child;
    }

    private View findOneVisibleChild(int startIndex, int lastIndex, boolean completelyVisible) {
        final int top = 0;
        final int bottom = getHeight();

        View visibleAnchor = null;
        final int step = startIndex < lastIndex ? 1 : -1;
        for (int i = startIndex; i != lastIndex; i += step) {
            final View child = getChildAt(i);
            if (isChildVisible(child, top, bottom, completelyVisible)) {
                visibleAnchor = child;
                break;
            }
        }

        if (visibleAnchor == null) {
            return null;
        }

        SectionData sd = getSectionData(getPosition(visibleAnchor));
        View visibleContent = null;
        for (int i = startIndex; i != lastIndex; i += step) {
            final View child = getChildAt(i);
            if (!sd.containsItem(child)) {
                break;
            }

            if (((LayoutParams) child.getLayoutParams()).isHeader()) {
                continue;
            }

            if (isChildVisible(child, top, bottom, completelyVisible)) {
                visibleContent = child;
                break;
            }
        }

        if (step < 0 && visibleContent != null) {
            return visibleContent;
        }

        final int headerIndex = Utils.findHeaderIndexFromFirstIndex(0, sd, mHelperDelegate);
        if (headerIndex == Utils.INVALID_INDEX) {
            return visibleContent;
        }

        final View header = getChildAt(headerIndex);
        return isChildVisible(header, top, bottom, completelyVisible) ? header : visibleContent;

    }

    private void fixOverscroll(int bottomLine, Recycler recycler, RecyclerView.State state) {
        if (!isOverscrolled(state)) {
            return;
        }

        // Shunt content down to the bottom of the screen.
        int delta = getHeight() - getPaddingBottom() - bottomLine;
        offsetChildrenVertical(delta);

        // Fill back towards the top.
        int topLine = fillToStart(0, recycler, state);

        if (topLine > getPaddingTop()) {
            // Not enough content to fill all the way back up so we shunt it back up.
            offsetChildrenVertical(getPaddingTop() - topLine);
        }
    }

    /**
     * Work out the borderline from the given anchor view and the intended direction to fill the
     * view hierarchy.
     *
     * @param anchorView Anchor view to determine borderline from.
     * @param direction  Direction fill will be taken towards.
     * @return Borderline.
     */
    private int getBorderLine(View anchorView, @Direction int direction) {
        int borderline;
        if (anchorView == null) {
            if (direction == DIRECTION_START) {
                borderline = getPaddingBottom();
            } else {
                borderline = getPaddingTop();
            }
        } else if (direction == DIRECTION_START) {
            borderline = getDecoratedBottom(anchorView);
        } else {
            borderline = getDecoratedTop(anchorView);
        }
        return borderline;
    }

    private int getDirectionToPosition(int targetPosition) {
        View child = getChildAt(0);
        SectionData sd = getSectionData(getPosition(child));
        final View firstVisibleView = findOneVisibleChild(0, getChildCount(), false);
        return targetPosition < getPosition(firstVisibleView) ? -1 : 1;
    }

    private float getFractionOfContentAbove(boolean ignorePosition) {
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

        SectionData sd = getSectionData(getPosition(child));
        if (sd.getSectionParams().isHeader() && sd.getSectionParams().isHeaderInline()) {
            // Header must not be stickied as it is not attached after section items.
            return fractionOffscreen;
        }

        // Run through all views in the section and add up values offscreen.
        int firstPosition = fractionOffscreen == 0 ? -1 : anchorPosition;
        SparseArray<Boolean> positionsOffscreen = new SparseArray<>();
        for (int i = 1; i < getChildCount(); i++) {
            child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!sd.containsItem(lp.getViewPosition())) {
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

            if (!lp.isHeader()) {
                if (firstPosition == -1) {
                    firstPosition = position;
                }
                positionsOffscreen.put(position, true);
            }
        }

        final LayoutHelper helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
        helper.init(sd, 0, 0, 0);
        final float result = fractionOffscreen - numBeforeAnchor - getSlm(sd, helper)
                .howManyMissingAbove(firstPosition, positionsOffscreen);
        helper.recycle();
        return result;
    }

    private float getFractionOfContentBelow(boolean ignorePosition) {
        final float parentHeight = getHeight();
        View child = getChildAt(getChildCount() - 1);

        final int anchorPosition = getPosition(child);
        int countAfter = 0;

        SectionData sd = getSectionData(getPosition(child));

        float fractionOffscreen = 0;
        int lastPosition = -1;
        SparseArray<Boolean> positionsOffscreen = new SparseArray<>();
        // Run through all views in the section and add up values offscreen.
        for (int i = 1; i <= getChildCount(); i++) {
            child = getChildAt(getChildCount() - i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!sd.containsItem(lp.getViewPosition())) {
                break;
            }

            int position = getPosition(child);
            if (!lp.isHeader() && !ignorePosition && position > anchorPosition) {
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

            if (!lp.isHeader()) {
                if (lastPosition == -1) {
                    lastPosition = position;
                }
                positionsOffscreen.put(position, true);
            }
        }

        final LayoutHelper helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
        helper.init(sd, 0, 0, 0);
        final float result = fractionOffscreen - countAfter - getSlm(sd, helper).
                howManyMissingBelow(lastPosition, positionsOffscreen);
        helper.recycle();
        return result;
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

    private SectionLayoutManager getSlm(SectionData sd, LayoutQueryHelper helper) {
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

        if (helper != null) {
            return slm.init(sd, helper);
        }
        return slm;
    }

    private boolean isChildVisible(View child, int top, int bottom, boolean completelyVisible) {
        final int childTop = getDecoratedTop(child);
        final int childBottom = getDecoratedBottom(child);
        if (top < childBottom && childTop < bottom) {
            if (completelyVisible) {
                if (top <= childTop && childBottom <= bottom) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isOverscrolled(RecyclerView.State state) {
        final int itemCount = state.getItemCount();

        if (getChildCount() == 0) {
            return false;
        }

        View lastVisibleView = findOneVisibleChild(getChildCount() - 1, -1, false);
        if (lastVisibleView == null) {
            lastVisibleView = getChildAt(getChildCount() - 1);
        }

        boolean reachedBottom = getPosition(lastVisibleView) == itemCount - 1;
        if (!reachedBottom ||
                getDecoratedBottom(lastVisibleView) >= getHeight() - getPaddingBottom()) {
            return false;
        }

        View firstVisibleView = findOneVisibleChild(0, getChildCount(), false);
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
     * @param recycler       Layout state.  @return Line to which content has been filled. If the
     *                       line is before the leading edge then the end of the data set has been
     */
    private int layoutChildren(int anchorPosition, int borderLine, Recycler recycler,
            RecyclerView.State state) {
        Log.d("layout", "start");
        final int bottom = getHeight();
        final int top = 0;

        if (mSections == null) {
            return 0;
        }

        final SectionData sd = getSectionData(anchorPosition);
        final LayoutHelper helper = LayoutHelperImpl.getLayoutHelperFromPool(mHelperDelegate);
        View header = recycler.getView(sd.firstPosition);
        sd.init(helper, header);
        recycler.cacheView(header);
        helper.init(sd, borderLine, bottom, 0);

        final SectionLayoutManager slm = getSlm(sd, helper);

        // Layout first section.
        int markerLine;
        int markerLineTop = borderLine;
        if (anchorPosition == sd.firstPosition || (sd.hasHeader && anchorPosition == sd.firstPosition + 1)) {
            markerLine = slm.beginFillToEnd(anchorPosition, sd, helper, recycler, state);
        } else {
            markerLine = slm.finishFillToEnd(anchorPosition, sd, helper, recycler, state);
            // Fill section back to start so we can fill any offset area and add any missed header.
            helper.init(sd, borderLine, top, top);
            markerLineTop = slm.finishFillToStart(anchorPosition - 1, sd, helper, recycler, state);
        }
        helper.recycle();

        // Fill any space left above.
        fillNextSectionsToStart(top, markerLineTop, recycler, state);

        // Fill any space left below.
        markerLine = fillNextSectionsToEnd(bottom, markerLine, recycler, state);

        return markerLine;
    }

    private boolean scrollToEndCompleted(int leadingEdge, RecyclerView.State state) {
        final int endPosition = getPosition(findAnchorAtEnd());
//        if (endPosition == state.getItemCount() - 1) {
//            return true;
//        }

        final SectionData sd = getSectionData(endPosition);
        final SectionLayoutManager slm = getSlm(sd, mHelperDelegate);

        // End edge is the bottom of the section content, or the bottom of the header, which
        // ever is lowest.
        final int lastIndex = getChildCount() - 1;
        int endEdge = slm.getLowestEdge(lastIndex, getDecoratedBottom(getChildAt(lastIndex)), sd,
                mHelperDelegate);
        if (sd.hasHeader) {
            final int endHeaderIndex = Utils
                    .findHeaderIndexFromLastIndex(lastIndex, sd, mHelperDelegate);
            if (endHeaderIndex != Utils.INVALID_INDEX) {
                final View endHeader = getChildAt(endHeaderIndex);
                endEdge = Math.max(endEdge, getDecoratedBottom(endHeader));
            }
        }

        return endEdge < getHeight() - getPaddingBottom();
    }

    private void showChildIndicies() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setText(String.valueOf(i));
            } else if (child instanceof LinearLayout) {
                ((TextView) ((LinearLayout) child).getChildAt(0)).setText(String.valueOf(i));
            }
        }
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
        getSlm(sd, helper).preTrimAtStartEdge(anchorIndex, sd, helper);
        helper.recycle();

        // Now trim views before the first visible item.
        for (int i = 0; i < anchorIndex; i++) {
//            for (int j = 0; j < getChildCount(); j++) {
//                Log.d("children" , "index " + j + " position " + getPosition(getChildAt(j)));
//            }
//
//            Log.d("trim", "index " + i + " position " + getPosition(getChildAt(0)));
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
    private void trimTail(@Direction int direction, Recycler state) {
        if (direction == DIRECTION_START) {
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

    @IntDef({DIRECTION_END, DIRECTION_START})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {

    }

    @IntDef({LinearSLM.ID, GridSLM.ID, SECTION_MANAGER_CUSTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SectionManager {

    }

    public static class Builder {

        final Context context;

        HashMap<String, SectionLayoutManager> slms = new HashMap<>();

        RecyclerView.Adapter adapter;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder addAdapter(RecyclerView.Adapter adapter) {
            if (!(adapter instanceof SectionAdapter)) {
                throw new SectionAdapterNotImplementedRuntimeException();
            }
            this.adapter = adapter;
            return this;
        }

        public Builder addSlm(String key, SectionLayoutManager slm) {
            slms.put(key, slm);
            return this;
        }

        public LayoutManager build() {
            return new LayoutManager(this);
        }
    }

    private static class SectionAdapterNotImplementedRuntimeException extends RuntimeException {

        SectionAdapterNotImplementedRuntimeException() {
            super("Adapter must implement SectionAdapter.");
        }
    }

    public static class LayoutParams extends RecyclerView.LayoutParams {

        public static final int HEADER_ALIGN_START = 0x01;

        public static final int HEADER_ALIGN_END = 0x02;

        public static final int HEADER_INLINE = 0x03;

        public static final int HEADER_ALIGNMENT_MASK = 0x03;

        public static final int HEADER_OVERLAY = 0x04;

        public static final int HEADER_STICKY = 0x08;

        public static final int MARGIN_AUTO = -0x01;

        private static final int NO_FIRST_POSITION = -0x01;

        private static final int DEFAULT_HEADER_MARGIN = -0x01;

        private static final int DEFAULT_HEADER_DISPLAY = 0;

        public
        @HeaderDisplayOptions
        int headerDisplay;

        public int marginEnd;

        public int marginStart;

        String sectionManager;

        int sectionManagerKind;

        public LayoutParams(int width, int height) {
            super(width, height);

            sectionManagerKind = SECTION_MANAGER_LINEAR;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.superslim_LayoutManager);
            //noinspection ResourceType
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

        /**
         * <em>This constructor will be removed in version 0.5.</em>
         * <br/><br/>
         * Use {@link #from} instead.
         */
        @Deprecated
        public LayoutParams(ViewGroup.MarginLayoutParams other) {
            super(other);
            init(other);
        }

        /**
         * <em>This constructor will be removed in version 0.5.</em>
         * <br/><br/>
         * Use {@link #from} instead as this constructor will not copy the margin params from the
         * source layout.
         */
        @Deprecated
        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
            init(other);
        }


        /**
         * Creates a new instance of {@link LayoutParams}.
         */
        public static LayoutParams from(@NonNull ViewGroup.LayoutParams other) {
            if (other instanceof ViewGroup.MarginLayoutParams) {
                return new LayoutParams((ViewGroup.MarginLayoutParams) other);
            } else {
                return new LayoutParams(other);
            }
        }

        public boolean areHeaderFlagsSet(int flags) {
            return (headerDisplay & flags) == flags;
        }

        public boolean isHeader() {
            return (headerDisplay & HEADER_INLINE) != 0;
        }

        public boolean isHeaderEndAligned() {
            return (headerDisplay & HEADER_ALIGNMENT_MASK) == HEADER_ALIGN_END;
        }

        public boolean isHeaderInline() {
            return (headerDisplay & HEADER_ALIGNMENT_MASK) == HEADER_INLINE;
        }

        public boolean isHeaderOverlay() {
            return (headerDisplay & HEADER_OVERLAY) == HEADER_OVERLAY;
        }

        public boolean isHeaderStartAligned() {
            return (headerDisplay & HEADER_ALIGNMENT_MASK) == HEADER_ALIGN_START;
        }

        public boolean isHeaderSticky() {
            return (headerDisplay & HEADER_STICKY) == HEADER_STICKY;
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

        @IntDef(flag = true, value = {
                HEADER_INLINE,
                HEADER_ALIGN_START,
                HEADER_ALIGN_END,
                HEADER_OVERLAY,
                HEADER_STICKY
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface HeaderDisplayOptions {

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

        private final LayoutManager mLayoutManager;

        LayoutHelperDelegate(LayoutManager lm) {
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
        public void detachAndScrapView(View child, Recycler recycler) {
            mLayoutManager.detachAndScrapView(child, recycler.inner);
        }

        @Override
        public void detachAndScrapViewAt(int index, Recycler recycler) {
            mLayoutManager.detachAndScrapViewAt(index, recycler.inner);
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
            return mLayoutManager.getLayoutDirection();
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
        public SectionData getSectionData(int position) {
            return mLayoutManager.getSectionData(position);
        }

        @Override
        public SectionLayoutManager getSlm(SectionData sectionData, LayoutQueryHelper helper) {
            return mLayoutManager.getSlm(sectionData, helper);
        }

        @Override
        public int getStickyEdge() {
            // Probably should never be called, but it is around due to the LayoutQueryHelper
            // interface.
            return 0;
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
        public void removeAndRecycleView(View child, Recycler recycler) {
            mLayoutManager.removeAndRecycleView(child, recycler.inner);
        }

        @Override
        public void removeAndRecycleViewAt(int index, Recycler recycler) {
            mLayoutManager.removeAndRecycleViewAt(index, recycler.inner);
        }

        @Override
        public void removeView(View child) {
            mLayoutManager.removeView(child);
        }

        @Override
        public void removeViewAt(int index) {
            mLayoutManager.removeViewAt(index);
        }
    }

    private class MissingSectionDataRuntimeException extends RuntimeException {

        public MissingSectionDataRuntimeException(int position) {
            super("Missing section data for item at position " + position + ".");
        }
    }
}
