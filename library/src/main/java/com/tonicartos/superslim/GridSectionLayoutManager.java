package com.tonicartos.superslim;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

/**
 * Lays out views in a grid. The number of columns can be set directly, or a minimum size can be
 * requested. If you request a 100dip minimum column size and there is 330dip available, the layout
 * with calculate there to be 3 columns each 130dip across.
 */
public class GridSectionLayoutManager extends SectionLayoutManager {

    private final Context mContext;

    private int mMinimumWidth = 0;

    private int mNumColumns = 0;

    private int mColumnWidth;

    private boolean mColumnsSpecified;

    public GridSectionLayoutManager(LayoutManager layoutManager, Context context) {
        super(layoutManager);
        mContext = context;
    }

    @Override
    public int computeHeaderOffset(View anchor, SectionData2 sd, LayoutState state) {
        /*
         * Work from an assumed overlap and add heights from the start until the overlap is zero or
         * less, or the current position (or max items) is reached.
         */
        View firstVisibleView = getFirstVisibleView(sd.firstPosition, true);
        if (firstVisibleView == null) {
            return 0;
        }

        int firstVisiblePosition = mLayoutManager.getPosition(firstVisibleView);

        int areaAbove = 0;
        for (int position = sd.firstPosition + 1;
                areaAbove < sd.headerHeight && position < firstVisiblePosition;
                position += mNumColumns) {
            // Look to see if the header overlaps with the displayed area of the mSection.
            int rowHeight = 0;
            for (int col = 0; col < mNumColumns; col++) {
                LayoutState.View child = state.getView(position + col);
                measureChild(child, sd);
                rowHeight =
                        Math.max(rowHeight, mLayoutManager.getDecoratedMeasuredHeight(child.view));
                state.recycleView(child);
            }
            areaAbove += rowHeight;
        }

        if (areaAbove == sd.headerHeight) {
            return 0;
        } else if (areaAbove > sd.headerHeight) {
            return 1;
        } else {
            return -areaAbove;
        }
    }

    @Override
    public FillResult fill(LayoutState state, SectionData section) {
        final int itemCount = state.recyclerState.getItemCount();
        final int height = mLayoutManager.getHeight();

        calculateColumnWidthValues(section);

        FillResult fillResult;
        if (section.isFillDirectionStart()) {
            fillResult = fillToStart(state, section);
        } else if (section.isFillDirectionEnd()) {
            fillResult = fillToEnd(state, section);
        } else {
            fillResult = fillSection(state, section);
        }

        fillResult.headerOffset = calculateHeaderOffset(state, section, itemCount,
                fillResult.positionStart);

        return fillResult;
    }

    @Override
    public int fillToEnd(int leadingEdge, int markerLine, int anchorPosition, SectionData2 sd,
            LayoutState state) {
        final int itemCount = state.recyclerState.getItemCount();
        if (anchorPosition >= itemCount) {
            return markerLine;
        }

        LayoutState.View anchor = state.getView(anchorPosition);
        if (anchor.getLayoutParams().getTestedFirstPosition() != sd.firstPosition) {
            state.cacheView(anchorPosition, anchor.view);
            return markerLine;
        }

        final int firstContentPosition = sd.hasHeader ? sd.firstPosition + 1 : sd.firstPosition;

        // Ensure the anchor is the first item in the row.
        final int col = (anchorPosition - firstContentPosition) % mNumColumns;
        for (int i = 1; i <= col; i++) {
            // Detach and scrap attached items in this row, so we can re-lay them again. The last
            // child view in the index can be the header so we just skip past it if it last.
            for (int j = 1; j <= mLayoutManager.getChildCount(); j++) {
                View child = mLayoutManager.getChildAt(mLayoutManager.getChildCount() - j);
                if (mLayoutManager.getPosition(child) == anchorPosition - i) {
                    markerLine = mLayoutManager.getDecoratedTop(child);
                    mLayoutManager.detachAndScrapViewAt(j, state.recycler);
                    break;
                }

                LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                        .getLayoutParams();
                if (params.getTestedFirstPosition() != sd.firstPosition) {
                    break;
                }
            }
        }
        anchorPosition = anchorPosition - col;

        // Lay out rows to end.
        for (int i = anchorPosition; i < itemCount; i += mNumColumns) {
            if (markerLine >= leadingEdge) {
                break;
            }

            LayoutState.View view = state.getView(i);
            if (view.getLayoutParams().getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(i, view.view);
                break;
            }

            int rowHeight = fillRow(markerLine, i, LayoutManager.Direction.END, true, sd, state);
            markerLine += rowHeight;
        }

        return markerLine;
    }

    @Override
    public int fillToStart(int leadingEdge, int markerLine, int anchorPosition, SectionData2 sd,
            LayoutState state) {
        final int firstContentPosition = sd.hasHeader ? sd.firstPosition + 1 : sd.firstPosition;

        // Check to see if we have to adjust for minimum section height. We don't if there is an
        // attached non-header view in this section.
        boolean applyMinHeight = false;
        for (int i = 0; i < state.recyclerState.getItemCount(); i++) {
            View check = mLayoutManager.getChildAt(0);
            LayoutManager.LayoutParams checkParams =
                    (LayoutManager.LayoutParams) check.getLayoutParams();
            if (checkParams.getTestedFirstPosition() != sd.firstPosition) {
                applyMinHeight = true;
                break;
            }

            if (!checkParams.isHeader) {
                applyMinHeight = false;
                break;
            }
        }

        // _ _ ^ a b
        final int col = (anchorPosition - firstContentPosition) % mNumColumns;
        for (int i = 1; i < mNumColumns - col; i++) {
            // Detach and scrap attached items in this row, so we can re-lay them again. The last
            // child view in the index can be the header so we just skip past it if it last.
            for (int j = 0; j <= mLayoutManager.getChildCount(); j++) {
                View child = mLayoutManager.getChildAt(j);
                LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                        .getLayoutParams();
                if (params.getTestedFirstPosition() != sd.firstPosition) {
                    break;
                }

                if (mLayoutManager.getPosition(child) == anchorPosition + i) {
                    mLayoutManager.detachAndScrapViewAt(j, state.recycler);
                    break;
                }
            }
        }
        // Ensure the anchor is the first item in the row.
        anchorPosition = anchorPosition - col;

        // Work out offset to marker line by measuring rows from the end. If section height is less
        // than min height, then adjust marker line and then lay out items.
        int measuredPositionsMarker = -1;
        int sectionHeight = 0;
        int minHeightOffset = 0;
        if (applyMinHeight) {
            for (int i = anchorPosition; i >= 0; i -= mNumColumns) {
                LayoutState.View check = state.getView(i);
                state.cacheView(i, check.view);
                LayoutManager.LayoutParams checkParams = check.getLayoutParams();
                if (checkParams.getTestedFirstPosition() != sd.firstPosition) {
                    break;
                }

                int rowHeight = 0;
                for (int j = 0; j < mNumColumns; j++) {
                    LayoutState.View measure = state.getView(i + j);
                    state.cacheView(i + j, measure.view);
                    LayoutManager.LayoutParams measureParams = measure.getLayoutParams();
                    if (measureParams.getTestedFirstPosition() != sd.firstPosition) {
                        break;
                    }

                    if (measureParams.isHeader) {
                        continue;
                    }

                    measureChild(measure, sd);
                    rowHeight = Math.max(rowHeight,
                            mLayoutManager.getDecoratedMeasuredHeight(measure.view));
                }

                sectionHeight += rowHeight;
                measuredPositionsMarker = i;
                if (sectionHeight >= sd.minimumHeight) {
                    break;
                }
            }

            if (sectionHeight < sd.minimumHeight) {
                minHeightOffset = sectionHeight - sd.minimumHeight;
                markerLine += minHeightOffset;
            }
        }

        // Lay out rows to end.
        for (int i = anchorPosition; i >= 0; i -= mNumColumns) {
            if (markerLine - minHeightOffset < leadingEdge) {
                break;
            }

            LayoutState.View rowAnchor = state.getView(i);
            LayoutManager.LayoutParams params = rowAnchor.getLayoutParams();
            if (params.isHeader || params.getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(i, rowAnchor.view);
                break;
            }

            boolean measureRowItems = !applyMinHeight || i < measuredPositionsMarker;
            int rowHeight = fillRow(markerLine, i, LayoutManager.Direction.START, measureRowItems,
                    sd, state);
            markerLine -= rowHeight;
        }

        return markerLine;
    }

    @Override
    public int finishFillToEnd(int leadingEdge, View anchor, SectionData2 sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        final int markerLine = getLowestEdge(sd.firstPosition, mLayoutManager.getChildCount() - 1,
                mLayoutManager.getDecoratedBottom(anchor));

        return fillToEnd(leadingEdge, markerLine, anchorPosition + 1, sd, state);
    }

    @Override
    public int finishFillToStart(int leadingEdge, View anchor, SectionData2 sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        final int markerLine = mLayoutManager.getDecoratedTop(anchor);

        if (anchorPosition - 1 == sd.firstPosition && sd.hasHeader) {
            // Already at first content position, so no more to do.
            return markerLine;
        }

        return fillToStart(leadingEdge, markerLine, anchorPosition - 1, sd, state);
    }

    @Override
    public int getAnchorPosition(LayoutState state, SectionData section, int position) {
        calculateColumnWidthValues(section);

        int firstPosition = section.getFirstPosition();
        LayoutState.View firstView = state.getView(firstPosition);
        if (firstView.getLayoutParams().isHeader) {
            firstPosition += 1;
        }
        state.recycleView(firstView);
        return position - ((position - firstPosition) % mNumColumns);
    }

    @Override
    public int getLowestEdge(int sectionFirstPosition, int lastIndex, int endEdge) {
        int bottomMostEdge = 0;
        int leftPosition = mLayoutManager.getWidth();
        boolean foundItems = false;
        // Look from end to find children that are the lowest.
        for (int i = lastIndex; i >= 0; i--) {
            View look = mLayoutManager.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) look.getLayoutParams();
            if (params.getTestedFirstPosition() != sectionFirstPosition) {
                break;
            }

            if (params.isHeader) {
                continue;
            }

            if (look.getLeft() < leftPosition) {
                leftPosition = look.getLeft();
            } else {
                break;
            }

            foundItems = true;
            bottomMostEdge = Math.max(bottomMostEdge, mLayoutManager.getDecoratedBottom(look));
        }

        return foundItems ? bottomMostEdge : endEdge;
    }

    public GridSectionLayoutManager init(SectionData2 sd) {
        super.init(sd);

        calculateColumnWidthValues(sd);

        return this;
    }

    /**
     * Fill a row.
     *
     * @param markerLine      Line indicating the top edge of the row.
     * @param anchorPosition  Position of the first view in the row.
     * @param measureRowItems Measure the row items.
     * @param sd              Section data.
     * @param state           Layout state.   @return The height of the new row.
     */
    public int fillRow(int markerLine, int anchorPosition, LayoutManager.Direction direction,
            boolean measureRowItems, SectionData2 sd, LayoutState state) {
        int rowHeight = 0;
        LayoutState.View[] views = new LayoutState.View[mNumColumns];
        for (int i = 0; i < mNumColumns; i++) {
            final int position = anchorPosition + i;
            if (position >= state.recyclerState.getItemCount()) {
                break;
            }

            LayoutState.View view = state.getView(position);
            if (view.getLayoutParams().getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(position, view.view);
                break;
            }

            if (measureRowItems) {
                measureChild(view, sd);
            } else {
                state.decacheView(i);
            }
            rowHeight = Math.max(rowHeight, mLayoutManager.getDecoratedMeasuredHeight(view.view));
            views[i] = view;
        }

        boolean directionIsStart = direction == LayoutManager.Direction.START;
        if (directionIsStart) {
            markerLine -= rowHeight;
        }

        for (int i = 0; i < mNumColumns; i++) {
            int col = directionIsStart ? mNumColumns - i - 1 : i;
            if (views[col] == null) {
                continue;
            }
            layoutChild(views[col], markerLine, col, rowHeight, sd, state);
            addView(views[col], col + anchorPosition, direction, state);
        }

        return rowHeight;
    }

    public void setColumnMinimumWidth(int minimumWidth) {
        mMinimumWidth = minimumWidth;
        mColumnsSpecified = false;
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
        mMinimumWidth = 0;
        mColumnsSpecified = true;
    }

    private int addView(LayoutState state, LayoutState.View child, int position,
            LayoutManager.Direction direction) {
        final int addIndex = direction == LayoutManager.Direction.START ? 0
                : mLayoutManager.getChildCount();

        if (child.wasCached) {
            mLayoutManager.attachView(child.view, addIndex);
            state.decacheView(position);
        } else {
            mLayoutManager.addView(child.view, addIndex);
        }

        return addIndex;
    }

    private void calculateColumnWidthValues(SectionData2 section) {
        int availableWidth = mLayoutManager.getWidth() - section.contentStart - section.contentEnd;
        if (!mColumnsSpecified) {
            if (mMinimumWidth <= 0) {
                mMinimumWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                        mContext.getResources().getDisplayMetrics());
            }
            mNumColumns = availableWidth / Math.abs(mMinimumWidth);
        }
        if (mNumColumns < 1) {
            mNumColumns = 1;
        }
        mColumnWidth = availableWidth / mNumColumns;
        if (mColumnWidth == 0) {
            Log.e("GridSection",
                    "Too many columns (" + mNumColumns + ") for available width" + availableWidth
                            + ".");
        }
    }

    private void calculateColumnWidthValues(SectionData section) {
        int availableWidth = mLayoutManager.getWidth()
                - section.getContentMarginStart() - section.getContentMarginEnd();
        if (!mColumnsSpecified) {
            if (mMinimumWidth <= 0) {
                mMinimumWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                        mContext.getResources().getDisplayMetrics());
            }
            mNumColumns = availableWidth / Math.abs(mMinimumWidth);
        }
        if (mNumColumns < 1) {
            mNumColumns = 1;
        }
        mColumnWidth = availableWidth / mNumColumns;
        if (mColumnWidth == 0) {
            Log.e("GridSection",
                    "Too many columns (" + mNumColumns + ") for available width" + availableWidth
                            + ".");
        }
    }

    /**
     * Work out by how much the header overlaps with the displayed content.
     *
     * @param state             Current layout state.
     * @param section           Section data
     * @param itemCount         Total number of items.
     * @param displayedPosition Closest position to start being displayed.
     * @return Header overlap.
     */
    private int calculateHeaderOffset(LayoutState state, SectionData section, int itemCount,
            int displayedPosition) {
        /*
         * Work from an assumed overlap and add heights from the start until the overlap is zero or
         * less, or the current position (or max items) is reached.
         */
        int headerOffset = 0;

        int position = section.getFirstPosition() + 1;
        while (headerOffset > -section.getHeaderHeight() && position < itemCount) {
            int rowHeight = 0;
            // Look to see if the header overlaps with the first item in the row. If not
            // get the largest height from the row and subtract.
            if (position < displayedPosition) {
                //Run through row and get largest height.
                for (int col = 0; col < mNumColumns; col++) {
                    // Make sure to measure current position if fill direction is to the start.
                    LayoutState.View child = state.getView(position + col);
                    measureChild(section, child);
                    int height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
                    if (height > rowHeight) {
                        rowHeight = height;
                    }

                    state.recycleView(child);
                }
            } else {
                // Run into an item that is displayed, indicating header overlap.
                break;
            }

            headerOffset -= rowHeight;
            position += mNumColumns; // Skip past row.
        }

        return headerOffset;
    }

    private AddData fillRow(LayoutState state, SectionData section, LayoutState.View startChild,
            LayoutManager.Direction direction, int startPosition, int markerLine) {
        final int itemCount = state.recyclerState.getItemCount();

        AddData addData = new AddData();
        addData.earliestIndexAddedTo = -1;

        LayoutManager.LayoutParams params = startChild.getLayoutParams();
        state.recycleView(startChild);
        int sectionFirst = params.getTestedFirstPosition();
        LayoutState.View sectionFirstView = state.getView(sectionFirst);
        LayoutManager.LayoutParams firstParams = sectionFirstView.getLayoutParams();
        state.recycleView(sectionFirstView);
        int sectionStart = firstParams.isHeader ? sectionFirst + 1 : sectionFirst;
        int startColumn = (startPosition - sectionStart) % mNumColumns;

        // Fill out a row at a time. That way we can position them all correctly.

        boolean rowAlreadyPositioned = false;
        int rowPriorPosition = 0;

        // Measure all children in the row.
        int rowStart = startColumn != 0 ? startPosition - startColumn : startPosition;
        int rowHeight = 0;
        LayoutState.View[] rowViews = new LayoutState.View[mNumColumns];
        for (int i = 0; i < mNumColumns; i++) {
            int nextPosition = rowStart + i;
            if (nextPosition < 0 || itemCount <= nextPosition) {
                // Keep going because we might have something in range.
                rowViews[i] = null;
                continue;
            }

            LayoutState.View next = state.getView(nextPosition);
            LayoutManager.LayoutParams nextParams = next.getLayoutParams();
            // Only measure and keep children belonging to the section.
            if (nextParams.getTestedFirstPosition() == section.getFirstPosition()) {
                measureChild(section, next);
                // Detect already attached children and adjust effective markerline from it.
                if (!rowAlreadyPositioned && next.wasCached) {
                    rowAlreadyPositioned = true;
                    rowPriorPosition = mLayoutManager.getDecoratedTop(next.view);
                }
                int height = mLayoutManager.getDecoratedMeasuredHeight(next.view);
                rowHeight = height > rowHeight ? height : rowHeight;
            } else {
                state.recycleView(next);
                for (; i < mNumColumns; i++) {
                    rowViews[i] = null;
                }
                break;
            }
            rowViews[i] = next;
        }

        final int top;
        if (rowAlreadyPositioned) {
            top = rowPriorPosition;
        } else {
            top = direction == LayoutManager.Direction.END ? markerLine : markerLine - rowHeight;
        }

        // Layout children in row starting from start child (startColumn).
        for (int col = startColumn; 0 <= col && col < mNumColumns;
                col += direction == LayoutManager.Direction.END ? 1 : -1) {
            LayoutState.View child = rowViews[col];
            if (child == null) {
                continue;
            }
            layoutChild(child, section, state, state.isLTR ? col : mNumColumns - 1 - col, top);
            int attachIndex = addView(state, child, mLayoutManager.getPosition(child.view),
                    direction);
            addData.numChildrenAdded += 1;
            if (addData.earliestIndexAddedTo == -1 || addData.earliestIndexAddedTo > attachIndex) {
                addData.earliestIndexAddedTo = attachIndex;
            }
        }

        if (direction == LayoutManager.Direction.END) {
            addData.markerLine = markerLine + rowHeight;
        } else {
            addData.markerLine = rowAlreadyPositioned ? rowPriorPosition : markerLine - rowHeight;
        }

        return addData;
    }

    private FillResult fillSection(LayoutState state, SectionData section) {
        final int itemCount = state.recyclerState.getItemCount();
        final int endEdge = mLayoutManager.getHeight();
        final int startEdge = 0;

        /*
         * First fill section to end from anchor position. Then fill to start from position above
         * anchor position. Then check minimum height requirement is met, if not offset the section
         * bottom marker by required amount.
         */
        FillResult fillResult = new FillResult();
        fillResult.firstChildIndex = -1;

        fillResult = fillViews(state, section, fillResult, section.getAnchorPosition(),
                section.getMarkerLine(), LayoutManager.Direction.END);
        fillResult = fillViews(state, section, fillResult, section.getAnchorPosition() - 1,
                section.getMarkerLine(), LayoutManager.Direction.START);

        final int minimumHeight = section.getMinimumHeight();
        // Push section children and start marker up if section is shorter than header.
        if (minimumHeight > 0) {
            int viewSpan = fillResult.markerEnd - fillResult.markerStart;
            if (section.getFirstPosition() != fillResult.positionStart + 1) {
                viewSpan = getViewSpan(state, section, fillResult, minimumHeight, viewSpan);
            }

            // Perform offset if needed.
            if (viewSpan < minimumHeight) {
                fillResult.markerEnd += section.getMinimumHeight() - viewSpan;
            }
        }

        return fillResult;
    }

    private FillResult fillToEnd(LayoutState state, SectionData section) {
        /*
         * First fill section to end from anchor position. Then check minimum height requirement
         * is met, if not offset the section bottom marker by required amount.
         */
        FillResult fillResult = new FillResult();
        fillResult.firstChildIndex = -1;
        fillResult.markerStart = section.getMarkerLine();
        fillResult.positionStart = section.getAnchorPosition();

        fillResult = fillViews(state, section, fillResult, section.getAnchorPosition(),
                section.getMarkerLine(), LayoutManager.Direction.END);

        // Push end marker down if section is shorter than the header.
        final int viewSpan = fillResult.markerEnd - fillResult.markerStart;
        if (viewSpan < section.getMinimumHeight()) {
            fillResult.markerEnd += section.getMinimumHeight() - viewSpan;
        }

        return fillResult;
    }

    private FillResult fillToStart(LayoutState state, SectionData section) {
        /*
         * First fill section to start from anchor position. Then check minimum height requirement
         * is met, if not offset all children added by the required margin.
         */
        FillResult fillResult = new FillResult();
        fillResult.firstChildIndex = -1;
        fillResult.markerEnd = section.getMarkerLine();
        fillResult.positionEnd = section.getAnchorPosition();

        fillResult = fillViews(state, section, fillResult, section.getAnchorPosition(),
                section.getMarkerLine(), LayoutManager.Direction.START);

        final int minimumHeight = section.getMinimumHeight();
        // Push section children and start marker up if section is shorter than header.
        if (minimumHeight > 0) {
            int viewSpan = fillResult.markerEnd - fillResult.markerStart;
            if (section.getFirstPosition() != fillResult.positionStart + 1) {
                viewSpan = getViewSpan(state, section, fillResult, minimumHeight, viewSpan);
            }

            // Perform offset if needed.
            if (viewSpan < minimumHeight) {
                final int offset = viewSpan - minimumHeight;
                for (int i = 0; i < fillResult.addedChildCount; i++) {
                    View child = mLayoutManager.getChildAt(fillResult.firstChildIndex + i);
                    child.offsetTopAndBottom(offset);
                }
                fillResult.markerStart += offset;
            }
        }

        return fillResult;
    }

    private FillResult fillViews(LayoutState state, SectionData section, FillResult fillResult,
            int anchorPosition, final int anchorLine, LayoutManager.Direction direction) {
        final int itemCount = state.recyclerState.getItemCount();
        final int parentHeight = mLayoutManager.getHeight();

        int markerLine = anchorLine;
        int currentPosition = anchorPosition;

        while ((direction == LayoutManager.Direction.START && currentPosition >= 0
                && markerLine >= 0) || (direction == LayoutManager.Direction.END
                && currentPosition < itemCount && markerLine < parentHeight)) {
            LayoutState.View child = state.getView(currentPosition);

            LayoutManager.LayoutParams params = child.getLayoutParams();
            if (params.isHeader || params.getTestedFirstPosition() != section.getFirstPosition()) {
                state.recycleView(child);
                break;
            }
            AddData r = fillRow(state, section, child, direction, currentPosition, markerLine);
            currentPosition += r.numChildrenAdded
                    * (direction == LayoutManager.Direction.START ? -1 : 1);
            markerLine = r.markerLine;
            fillResult.addedChildCount += r.numChildrenAdded;
            if (fillResult.firstChildIndex == -1) {
                fillResult.firstChildIndex = r.earliestIndexAddedTo;
            } else {
                fillResult.firstChildIndex = r.earliestIndexAddedTo < fillResult.firstChildIndex
                        ? r.earliestIndexAddedTo : fillResult.firstChildIndex;
            }
        }

        if (direction == LayoutManager.Direction.START) {
            fillResult.markerStart = markerLine;
            fillResult.positionStart = currentPosition + 1;
        } else {
            fillResult.markerEnd = markerLine;
            fillResult.positionEnd = currentPosition - 1;
        }

        return fillResult;
    }

    private int getViewSpan(LayoutState state, SectionData section, FillResult fillResult,
            int minimumHeight, int viewSpan) {
        // Haven't checked over entire area to see if the section is indeed smaller than the
        // header. The assumption is that there is a header because we have a minimum
        // height.
        final int rangeToCheck = fillResult.positionStart - (section.getFirstPosition() + 1);
        for (int i = mNumColumns; i <= rangeToCheck && viewSpan < minimumHeight; i += mNumColumns) {
            int rowHeight = 0;
            for (int col = 0; col < mNumColumns; col++) {
                final LayoutState.View child = state.getView(fillResult.positionStart - i + col);
                measureChild(section, child);
                final int height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
                state.recycleView(child);
                if (height > rowHeight) {
                    rowHeight = height;
                }
            }
            viewSpan += rowHeight;
        }
        return viewSpan;
    }

    /**
     * Layout out a view for the given column in a row. Views that have a height param of
     * MATCH_PARENT are fixed to the height of the row.
     *
     * @param child     View to lay out.
     * @param top       Line indicating the top edge of the row.
     * @param col       Column view is being placed into.
     * @param rowHeight Height of the row.
     * @param sd        Section data.
     * @param state     Layout state.
     */
    private void layoutChild(LayoutState.View child, int top, int col, int rowHeight,
            SectionData2 sd, LayoutState state) {
        final int height;
        if (child.getLayoutParams().height == LayoutManager.LayoutParams.MATCH_PARENT) {
            height = rowHeight;
        } else {
            height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
        }
        final int width = mLayoutManager.getDecoratedMeasuredWidth(child.view);

        final int bottom = top + height;
        final int left = (state.isLTR ? sd.contentStart : sd.contentEnd) + col * mColumnWidth;
        final int right = left + width;

        mLayoutManager.layoutDecorated(child.view, left, top, right, bottom);
    }

    private void layoutChild(LayoutState.View child, SectionData section, LayoutState state,
            int col, int top) {
        if (child.wasCached) {
            return;
        }

        int height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
        int width = mLayoutManager.getDecoratedMeasuredWidth(child.view);
        int bottom = top + height;
        int left = (state.isLTR ? section.getContentMarginStart() : section.getContentMarginEnd())
                + col * mColumnWidth;
        int right = left + width;

        mLayoutManager.layoutDecorated(child.view, left, top, right, bottom);
    }

    /**
     * Measure view. A view is given an area as wide as a single column with an undefined height.
     *
     * @param child View to measure.
     * @param sd    Section data.
     */
    private void measureChild(LayoutState.View child, SectionData2 sd) {
        int widthOtherColumns = (mNumColumns - 1) * mColumnWidth;
        mLayoutManager.measureChildWithMargins(child.view,
                sd.marginStart + sd.marginEnd + widthOtherColumns,
                0);
    }

    private void measureChild(SectionData section, LayoutState.View child) {
        if (child.wasCached) {
            return;
        }

        int widthOtherColumns = (mNumColumns - 1) * mColumnWidth;
        mLayoutManager.measureChildWithMargins(child.view,
                section.getHeaderMarginStart() + section.getHeaderMarginEnd() + widthOtherColumns,
                0);
    }

    class AddData {

        int numChildrenAdded;

        int earliestIndexAddedTo;

        int markerLine;
    }
}
