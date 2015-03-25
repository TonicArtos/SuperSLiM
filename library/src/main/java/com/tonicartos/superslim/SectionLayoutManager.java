package com.tonicartos.superslim;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import java.util.HashMap;

public abstract class SectionLayoutManager {

    private HashMap<SectionData, Bundle> mSavedConfiguration = new HashMap<>();

    /**
     * Start filling a new section towards the end. Might end out filling out the entire section.
     *
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    final public int beginFillToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final int countBeforeFill = helper.getChildCount();

        int markerLine = 0;
        View header = null;
        if (sectionData.hasHeader) {
            header = recycler.getView(sectionData.firstPosition);
            helper.measureHeader(header);
            markerLine = helper.layoutHeaderTowardsEnd(header, markerLine, state);
            helper.updateVerticalOffset(markerLine);
            anchorPosition += 1;
        }

        if (sectionData.subsections != null) {
            markerLine = onFillSubsectionsToEnd(
                    anchorPosition, sectionData, helper, recycler, state);
        } else {
            markerLine = onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
        }

        if (sectionData.hasHeader && header != null) {
            if (helper.getPosition(helper.getChildAt(helper.getChildCount() - 1)) ==
                    sectionData.lastPosition && helper.getBottom(header) > markerLine &&
                    helper.getTop(header) > 0) {
                int delta = Math.max(markerLine - helper.getBottom(header), 0 - helper.getTop(header));
                header.offsetTopAndBottom(delta);
            }

            addView(header, helper, recycler);
            markerLine = Math.max(markerLine, helper.getBottom(header));
            recycler.decacheView(sectionData.firstPosition);
        }
        return helper.translateFillResult(markerLine);
    }

    /**
     * Start filling a new section towards the start. Might end out filling out the entire section.
     *
     * @param anchorPosition Adapter position for the last content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    final public int beginFillToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final int countBeforeFill = helper.getChildCount();
        int markerLine;
        if (sectionData.subsections != null) {
            markerLine = onFillSubsectionsToStart(anchorPosition, sectionData, helper, recycler,
                    state);
        } else {
            markerLine = onFillToStart(anchorPosition, sectionData, helper, recycler, state);
        }
        if (sectionData.hasHeader) {
            View header = recycler.getView(sectionData.firstPosition);
            helper.measureHeader(header);
            final int offset = getHeaderOffset(sectionData, helper, recycler, header);
            markerLine = helper.layoutHeaderTowardsStart(header, offset, markerLine, 0, state);

            int attachIndex = helper.getChildCount() - countBeforeFill;
            addView(header, attachIndex, helper, recycler);
            recycler.decacheView(sectionData.firstPosition);
        }
        return helper.translateFillResult(markerLine);
    }

    /**
     * Finish filling a section towards the end.
     *
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    final public int finishFillToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        final int countBeforeFill = helper.getChildCount();
        int markerLine = onFillToEnd(anchorPosition, sectionData, helper, recycler, state);
        if (sectionData.hasHeader) {
            // Shuffle header to end of section (child index). This is the easiest way to ensure
            // the header is drawn after any other section content.
            final int headerIndex = countBeforeFill - 1; // Header should always be at the end.
            if (headerIndex != Utils.INVALID_INDEX) {
                final View header = helper.getChildAt(headerIndex);
                if (helper.getPosition(header) == sectionData.firstPosition) {
                    helper.detachView(header);
                    helper.attachView(header);

                    markerLine = Math.max(markerLine, helper.getBottom(header));
                }
            }
        }
        return helper.translateFillResult(markerLine);
    }

    /**
     * Finish filling a section towards the start.
     *
     * @param anchorPosition Adapter position for the last content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    final public int finishFillToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state) {
        // Handle case if we are trying to finish filling content, but all content other than the
        // header has been trimmed.
        if (sectionData.hasHeader && anchorPosition == sectionData.firstPosition - 1) {
            if (helper.getChildCount() > 1 &&
                    helper.getPosition(helper.getChildAt(1)) > sectionData.lastPosition) {
                helper.updateVerticalOffset(helper.getBottom(helper.getChildAt(0)));
                helper.detachAndScrapViewAt(0, recycler);
                return beginFillToStart(sectionData.lastPosition, sectionData, helper, recycler,
                        state);
            }
        }

        int markerLine = onFillToStart(anchorPosition, sectionData, helper, recycler, state);
        if (sectionData.hasHeader) {
            final int headerIndex = Utils.findHeaderIndexFromFirstIndex(0, sectionData, helper);
            final View header;
            if (headerIndex == Utils.INVALID_INDEX) {
                header = recycler.getView(sectionData.firstPosition);
                helper.measureHeader(header);
            } else {
                header = helper.getChildAt(headerIndex);
                helper.detachViewAt(headerIndex);
            }

            final int offset = getHeaderOffset(sectionData, helper, recycler, header);

            final int sectionBottom = getLowestEdge(
                    Utils.findLastIndexForSection(sectionData, helper), helper.getBottom(header),
                    sectionData, helper);

            markerLine = helper
                    .layoutHeaderTowardsStart(header, offset, markerLine, sectionBottom, state);

            // Make sure to attach after section content and to clean up any caching.
            final int attachIndex = Utils.findLastIndexForSection(sectionData, helper) + 1;
            if (headerIndex == Utils.INVALID_INDEX) {
                helper.addView(header, attachIndex);
            } else {
                helper.attachView(header, attachIndex);
            }
            recycler.decacheView(sectionData.firstPosition);
            sectionData.setTempHeaderIndex(attachIndex);
        }
        sectionData.recentlyFinishFilledToStart = true;
        return helper.translateFillResult(markerLine);
    }

    public RecyclerView.LayoutParams generateLayoutParams(RecyclerView.LayoutParams params) {
        return params;
    }

    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutManager.LayoutParams(c, attrs);
    }

    /**
     * Tell decorators which edges are internal and external. The default implementation assumes a
     * linear list.
     *
     * @param outRect     Rect to load with ege states.
     * @param child       Child to look at.
     * @param sectionData Section data.
     */
    public void getEdgeStates(Rect outRect, View child, SectionData sectionData,
            int layoutDirection) {
        outRect.left = ItemDecorator.EXTERNAL;
        outRect.right = ItemDecorator.EXTERNAL;
        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child.getLayoutParams();
        final int position = params.getViewPosition();
        int firstContentPosition = (sectionData.hasHeader ?
                Math.min(sectionData.firstPosition + 1, sectionData.lastPosition) :
                sectionData.firstPosition);
        outRect.top = position == firstContentPosition ?
                ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
        outRect.bottom = position == sectionData.lastPosition ?
                ItemDecorator.EXTERNAL : ItemDecorator.INTERNAL;
    }

    /**
     * Find the highest displayed edge of the section. If there is no member found then return the
     * default edge instead.
     *
     * @param firstIndex  Child index to start looking from.
     * @param defaultEdge Default value.
     * @return Top (attached) edge of the section.
     */
    public int getHighestEdge(int firstIndex, int defaultEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        // Look from start to find children that are the highest.
        for (int i = firstIndex; i < helper.getChildCount(); i++) {
            View child = helper.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (!sectionData.containsItem(params.getViewPosition())) {
                break;
            }
            if (params.isHeader()) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return helper.getTop(child);
        }
        return defaultEdge;
    }

    /**
     * Find the lowest displayed edge of the section. If there is no member found then return the
     * default edge instead.
     *
     * @param lastIndex Index to start looking from. Usually the index of the last attached view in
     *                  this section.
     * @param altEdge   Default value.
     * @return Lowest (attached) edge of the section.
     */
    public int getLowestEdge(int lastIndex, int altEdge, SectionData sectionData,
            LayoutQueryHelper helper) {
        // Look from end to find children that are the lowest.
        for (int i = lastIndex; i >= 0; i--) {
            View child = helper.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (!sectionData.containsItem(params.getViewPosition())) {
                break;
            }
            if (params.isHeader()) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return Math.max(altEdge, helper.getBottom(child));
        }
        return altEdge;
    }

    public int howManyMissingAbove(int firstPosition, SparseArray<Boolean> positionsOffscreen) {
        int itemsSkipped = 0;
        int itemsFound = 0;
        for (int i = firstPosition; itemsFound < positionsOffscreen.size(); i++) {
            if (positionsOffscreen.get(i, false)) {
                itemsFound += 1;
            } else {
                itemsSkipped += 1;
            }
        }

        return itemsSkipped;
    }

    public int howManyMissingBelow(int lastPosition, SparseArray<Boolean> positionsOffscreen) {
        int itemsSkipped = 0;
        int itemsFound = 0;
        for (int i = lastPosition;
                itemsFound < positionsOffscreen.size(); i--) {
            if (positionsOffscreen.get(i, false)) {
                itemsFound += 1;
            } else {
                itemsSkipped += 1;
            }
        }

        return itemsSkipped;
    }

    final public void preTrimAtEndEdge(int lvi, SectionData sd, LayoutTrimHelper helper) {
        onPreTrimAtEndEdge(lvi, sd, helper);

        if (sd.subsections == null) {
            return;
        }
        HashMap<SectionData, Integer> selectedSubsections =
                getSectionsIntersectingEndEdge(helper.getTrimEdge(), lvi, sd, helper);

        for (SectionData subSd : selectedSubsections.keySet()) {
            LayoutTrimHelper subsectionHelper = helper.getSubsectionLayoutTrimHelper();
            subsectionHelper.init(subSd, helper.getTrimEdge(), helper.getStickyEdge());
            helper.getSlm(subSd, subsectionHelper).
                    preTrimAtEndEdge(selectedSubsections.get(subSd), subSd, subsectionHelper);
            subsectionHelper.recycle();
        }
    }

    final public void preTrimAtStartEdge(int fvi, SectionData sd, LayoutTrimHelper helper) {
        onPreTrimAtStartEdge(fvi, sd, helper);

        int subsectionStickyEdge = updateHeaderForStartEdgeTrim(fvi, sd, helper);

        if (sd.subsections == null) {
            return;
        }
        HashMap<SectionData, Integer> selectedSubsections =
                getSectionsIntersectingStartEdge(subsectionStickyEdge, fvi, sd,
                        helper);

        for (SectionData subSd : selectedSubsections.keySet()) {
            LayoutTrimHelper subsectionHelper = helper.getSubsectionLayoutTrimHelper();
            subsectionHelper.init(subSd, helper.getTrimEdge(), subsectionStickyEdge);
            helper.getSlm(subSd, subsectionHelper).
                    preTrimAtStartEdge(selectedSubsections.get(subSd), subSd, subsectionHelper);
            subsectionHelper.recycle();
        }
    }

    protected void addView(View child, LayoutHelper helper, Recycler recycler) {
        recycler.decacheView(helper.getPosition(child));
        helper.addView(child);
    }

    protected void addView(View child, int index, LayoutHelper helper, Recycler recycler) {
        recycler.decacheView(helper.getPosition(child));
        helper.addView(child, index);
    }

    /**
     * Compute the offset for side aligned headers. If the height of the non-visible area of the
     * section is taller than the header, then the header should be offscreen, in that case return
     * any +ve number.
     *
     * @param firstVisiblePosition Position of first visible item in section.
     * @param sectionData          Section data.
     * @param helper               Layout helper.
     * @param recycler             Layout state.
     * @return -ve number giving the distance the header should be offset before the anchor view. A
     * +ve number indicates the header is offscreen.
     */
    abstract protected int onComputeHeaderOffset(int firstVisiblePosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler);

    abstract protected int onFillSubsectionsToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state);

    abstract protected int onFillSubsectionsToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state);

    /**
     * Fill section content towards the end.
     *
     * @param anchorPosition Adapter position for the first content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    abstract protected int onFillToEnd(int anchorPosition, SectionData sectionData,
            LayoutHelper helper,
            Recycler recycler, RecyclerView.State state);

    /**
     * Fill section content towards the start.
     *
     * @param anchorPosition Adapter position for the last content item in the section.
     * @param sectionData    Section data.
     * @param helper         Layout helper.
     * @param recycler       Recycler.
     * @return Line to which content has been filled.
     */
    abstract protected int onFillToStart(int anchorPosition, SectionData sectionData,
            LayoutHelper helper, Recycler recycler, RecyclerView.State state);

    protected void onInit(Bundle savedConfiguration, SectionData sectionData,
            LayoutQueryHelper helper) {
    }

    /**
     * Called before items are trimmed for any section that intersects the end edge. This is the
     * opportunity to update views before they might otherwise be trimmed for being beyond the
     * edge.
     *
     * @param lastVisibleIndex Index of last item in this section that is visible.
     * @param sectionData      Section data.
     * @param helper           Layout query helper.
     */
    protected void onPreTrimAtEndEdge(final int lastVisibleIndex, final SectionData sectionData,
            final LayoutTrimHelper helper) {
    }

    /**
     * Called before items are trimmed for any section that intersects the start edge. This is the
     * opportunity to update views before they might otherwise be trimmed for being beyond the
     * edge.
     *
     * @param firstVisibleIndex Index of first item in this section that is visible.
     * @param sectionData       Section data.
     * @param helper            Layout query helper.
     */
    protected void onPreTrimAtStartEdge(final int firstVisibleIndex, final SectionData sectionData,
            final LayoutTrimHelper helper) {
    }

    protected void onReset() {
    }

    void clearConfigurationForSection(SectionData sectionData) {
        mSavedConfiguration.remove(sectionData);
    }

    protected void saveConfiguration(SectionData sectionData, Bundle configuration) {
        mSavedConfiguration.put(sectionData, configuration);
    }

    SectionLayoutManager init(SectionData sectionData, LayoutQueryHelper helper) {
        onInit(mSavedConfiguration.get(sectionData), sectionData, helper);
        return this;
    }

    /**
     * There exists a problem in that when filling towards the start edge, that headers can only be
     * added after the section content has been placed. However, a subsection's sticky header's
     * position is dependent on the supersection's sticky header position and height. So that means
     * we have to make a second pass to make sure all the sticky headers are properly positioned.
     *
     * In other words, this method exists only to fix nested sticky headers and is implemented
     * solely by the SlmWrapper, but needs to be called by the LayoutManager, which is why it is
     * package private.
     */
    void postFinishFillToStart(SectionData sd, LayoutTrimHelper helper) {
        final int headerIndex = sd.getTempHeaderIndex();
        sd.clearTempHeaderIndex();
        final int subsectionStickyEdge = updateHeader(headerIndex, sd, helper);

        if (sd.subsections == null) {
            return;
        }

        for (SectionData subSd : sd.subsections) {
            if (sd.recentlyFinishFilledToStart) {
                sd.recentlyFinishFilledToStart = false;
                LayoutTrimHelper subsectionHelper = helper.getSubsectionLayoutTrimHelper();
                subsectionHelper.init(subSd, helper.getTrimEdge(), subsectionStickyEdge);
                helper.getSlm(subSd, subsectionHelper).postFinishFillToStart(sd, subsectionHelper);
                subsectionHelper.recycle();
            }
        }
    }

    void reset() {
        mSavedConfiguration.clear();
        onReset();
    }

    private int getHeaderOffset(SectionData sd, LayoutHelper helper, Recycler recycler,
            View header) {
        final int offset;
        final LayoutManager.LayoutParams layoutParams =
                (LayoutManager.LayoutParams) header.getLayoutParams();
        if (!layoutParams.isHeaderSticky() || !layoutParams.isHeaderInline()) {
            offset = onComputeHeaderOffset(
                    helper.getPosition(helper.getChildAt(0)), sd, helper, recycler);
        } else {
            offset = 0;
        }
        return offset;
    }

    private HashMap<SectionData, Integer> getSectionsIntersectingEndEdge(int endEdge,
            int lastVisibleIndex, SectionData sd, LayoutQueryHelper helper) {
        // Work out max number of items we have to check to find sections which intersect start
        // edge. Also, cap to  number of items after fvi.
        int range = Math.min(helper.getPosition(helper.getChildAt(lastVisibleIndex)) -
                sd.firstPosition + 1, lastVisibleIndex + 1);

        // Select subsections which have items overlapping or before the edge.
        HashMap<SectionData, Integer> selectedSubsections = new HashMap<>();
        for (int i = 0; i < range; i++) {
            int childIndex = lastVisibleIndex - i;
            View child = helper.getChildAt(childIndex);
            if (endEdge < helper.getBottom(child)) {
                int childPosition = helper.getPosition(child);
                for (SectionData subSd : sd.subsections) {
                    if (selectedSubsections.get(subSd) == null && subSd
                            .containsItem(childPosition)) {
                        int subsectionLvi = Utils
                                .findLastVisibleIndex(endEdge, childIndex, subSd,
                                        helper);
                        if (subsectionLvi != Utils.INVALID_INDEX) {
                            selectedSubsections.put(subSd, subsectionLvi);
                        }
                        break;
                    }
                }
            }

            if (selectedSubsections.size() == sd.subsections.size()) {
                // Already added every section.
                break;
            }
        }
        return selectedSubsections;
    }

    /**
     * Get a map of sections and their first visible positions that intersect the start edge.
     *
     * <p>The basic implementation looks through all attached child views for this section. You
     * should consider an implementation that constrains the search to a minimal range.</p>
     *
     * @param startEdge         Edge line. Generally 0.
     * @param firstVisibleIndex First visible index for this section.
     * @param sd                Section data.
     * @param helper            Layout query helper.
     * @return Map of subsection data to subsection first visible edges.
     */
    private HashMap<SectionData, Integer> getSectionsIntersectingStartEdge(int startEdge,
            int firstVisibleIndex, SectionData sd, LayoutQueryHelper helper) {
        // Work out max number of items we have to check to find sections which intersect start
        // edge. Also, cap to  number of items after fvi.
        int range = Math.min(sd.lastPosition
                        - helper.getPosition(helper.getChildAt(firstVisibleIndex)) + 1,
                helper.getChildCount() - firstVisibleIndex);

        // Select subsections which have items overlapping or before the start edge.
        HashMap<SectionData, Integer> selectedSubsections = new HashMap<>();
        for (int i = 0; i < range; i++) {
            int childIndex = i + firstVisibleIndex;
            View child = helper.getChildAt(childIndex);
            if (helper.getTop(child) < startEdge) {
                int childPosition = helper.getPosition(child);
                for (SectionData subSd : sd.subsections) {
                    if (selectedSubsections.get(subSd) == null && subSd
                            .containsItem(childPosition)) {
                        int subsectionFvi = Utils
                                .findFirstVisibleIndex(startEdge, childIndex, subSd,
                                        helper);
                        if (subsectionFvi != Utils.INVALID_INDEX) {
                            selectedSubsections.put(subSd, subsectionFvi);
                        }
                        break;
                    }
                }
            }

            if (selectedSubsections.size() == sd.subsections.size()) {
                // Already added every section.
                break;
            }
        }
        return selectedSubsections;
    }

    private int updateHeader(int headerIndex, SectionData sd, LayoutQueryHelper helper) {
        final int stickyEdge = helper.getStickyEdge();
        if (headerIndex == Utils.INVALID_INDEX) {
            // No header found to update. It must not need to be updated.
            return stickyEdge;
        }

        final View header = helper.getChildAt(headerIndex);
        final LayoutManager.LayoutParams headerParams =
                (LayoutManager.LayoutParams) header.getLayoutParams();
        if (!headerParams.isHeaderSticky()) {
            // Only need to update stickied headers.
            return stickyEdge;
        }

        final int headerTop = helper.getTop(header);
        if (headerTop >= stickyEdge) {
            // Only need to update sticky headers if they are above the sticky edge.
            return stickyEdge;
        }

        SectionLayoutManager slm = helper.getSlm(sd, helper);
        final int sectionBottom = slm.
                getLowestEdge(headerIndex, helper.getBottom(header), sd, helper);

        final int headerHeight = helper.getMeasuredHeight(header);
        int top = headerHeight + stickyEdge > sectionBottom ?
                sectionBottom - headerHeight : stickyEdge;

        int delta = headerTop - top;
        header.offsetTopAndBottom(-delta);

        if (headerParams.isHeaderInline()) {
            return stickyEdge - delta + headerHeight;
        }
        return stickyEdge;
    }

    private int updateHeaderForStartEdgeTrim(int fvi, SectionData sd, LayoutQueryHelper helper) {
        if (!sd.hasHeader) {
            return helper.getStickyEdge();
        }

        int headerIndex = Utils.findHeaderIndexFromFirstIndex(fvi, sd, helper);
        return updateHeader(headerIndex, sd, helper);
    }

    public class SlmConfig {

        int marginStart;

        int marginEnd;

        @LayoutManager.SectionManager
        int sectionManagerKind;

        String sectionManager;

        public SlmConfig(int marginStart, int marginEnd, String sectionManager) {
            this.marginStart = marginStart;
            this.marginEnd = marginEnd;
            this.sectionManagerKind = LayoutManager.SECTION_MANAGER_CUSTOM;
            this.sectionManager = sectionManager;
        }

        public SlmConfig(int marginStart, int marginEnd,
                @LayoutManager.SectionManager int sectionManager) {
            this.marginStart = marginStart;
            this.marginEnd = marginEnd;
            this.sectionManagerKind = sectionManager;
        }
    }

}
