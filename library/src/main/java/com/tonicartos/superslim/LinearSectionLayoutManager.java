package com.tonicartos.superslim;

import android.view.View;

public class LinearSectionLayoutManager extends SectionLayoutManager {

    public LinearSectionLayoutManager(LayoutManager layoutManager) {
        super(layoutManager);
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
                position++) {
            // Look to see if the header overlaps with the displayed area of the mSection.
            LayoutState.View child = state.getView(position);
            measureChild(child, sd);

            areaAbove += mLayoutManager.getDecoratedMeasuredHeight(child.view);
            state.recycleView(child);
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

        for (int i = anchorPosition; i < itemCount; i++) {
            if (markerLine >= leadingEdge) {
                break;
            }

            LayoutState.View next = state.getView(i);
            LayoutManager.LayoutParams params = next.getLayoutParams();
            if (params.getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(i, next.view);
                break;
            }

            measureChild(next, sd);
            markerLine = layoutChild(next, markerLine, LayoutManager.Direction.END, sd, state);
            addView(next, i, LayoutManager.Direction.END, state);
        }

        return markerLine;
    }

    @Override
    public int fillToStart(int leadingEdge, int markerLine, int anchorPosition, SectionData2 sd,
            LayoutState state) {
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

        // Work out offset to marker line by measuring items from the end. If section height is less
        // than min height, then adjust marker line and then lay out items.
        int measuredPositionsMarker = -1;
        int sectionHeight = 0;
        int minHeightOffset = 0;
        if (applyMinHeight) {
            for (int i = anchorPosition; i >= 0; i--) {
                LayoutState.View measure = state.getView(i);
                state.cacheView(i, measure.view);
                LayoutManager.LayoutParams params = measure.getLayoutParams();
                if (params.getTestedFirstPosition() != sd.firstPosition) {
                    break;
                }

                if (params.isHeader) {
                    continue;
                }

                measureChild(measure, sd);
                sectionHeight += mLayoutManager.getDecoratedMeasuredHeight(measure.view);
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

        for (int i = anchorPosition; i >= 0; i--) {
            if (markerLine - minHeightOffset < leadingEdge) {
                break;
            }

            LayoutState.View next = state.getView(i);
            LayoutManager.LayoutParams params = next.getLayoutParams();
            if (params.isHeader) {
                state.cacheView(i, next.view);
                break;
            }
            if (params.getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(i, next.view);
                break;
            }

            if (!applyMinHeight || i < measuredPositionsMarker) {
                measureChild(next, sd);
            } else {
                state.decacheView(i);
            }
            markerLine = layoutChild(next, markerLine, LayoutManager.Direction.START, sd, state);
            addView(next, i, LayoutManager.Direction.START, state);
        }

        return markerLine;
    }

    @Override
    public int finishFillToEnd(int leadingEdge, View anchor, SectionData2 sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        final int markerLine = mLayoutManager.getDecoratedBottom(anchor);

        return fillToEnd(leadingEdge, markerLine, anchorPosition + 1, sd, state);
    }

    @Override
    public int finishFillToStart(int leadingEdge, View anchor, SectionData2 sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        final int markerLine = mLayoutManager.getDecoratedTop(anchor);

        return fillToStart(leadingEdge, markerLine, anchorPosition - 1, sd, state);
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
            // Look to see if the header overlaps with the displayed area of the mSection.
            LayoutState.View child;

            if (position < displayedPosition) {
                // Make sure to measure current position if fill direction is to the start.
                child = state.getView(position);
                measureChild(section, child);
            } else {
                // Run into an item that is displayed, indicating header overlap.
                break;
            }

            headerOffset -= mLayoutManager.getDecoratedMeasuredHeight(child.view);
            state.recycleView(child);

            position += 1;
        }

        return headerOffset;
    }

    private FillResult fillSection(LayoutState state, SectionData section) {
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

        int minimumHeight = section.getMinimumHeight();
        // Push section children and start marker up if section is shorter than header.
        if (minimumHeight > 0) {
            int viewSpan = fillResult.markerEnd - fillResult.markerStart;
            if (section.getFirstPosition() != fillResult.positionStart + 1) {
                // Haven't checked over entire area to see if the section is indeed smaller than the
                // header. The assumption is that there is a header because we have a minimum
                // height.
                int rangeToCheck = fillResult.positionStart - (section.getFirstPosition() + 1);
                for (int i = 1; i <= rangeToCheck && viewSpan < minimumHeight; i++) {
                    LayoutState.View child = state.getView(fillResult.positionStart - i);
                    measureChild(section, child);
                    viewSpan += mLayoutManager.getDecoratedMeasuredHeight(child.view);
                    state.recycleView(child);
                }
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
        int viewSpan = fillResult.markerEnd - fillResult.markerStart;
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

        int minimumHeight = section.getMinimumHeight();
        // Push section children and start marker up if section is shorter than header.
        if (minimumHeight > 0) {
            int viewSpan = fillResult.markerEnd - fillResult.markerStart;
            if (section.getFirstPosition() != fillResult.positionStart + 1) {
                // Haven't checked over entire area to see if the section is indeed smaller than the
                // header. The assumption is that there is a header because we have a minimum
                // height.
                int rangeToCheck = fillResult.positionStart - (section.getFirstPosition() + 1);
                for (int i = 1; i <= rangeToCheck && viewSpan < minimumHeight; i++) {
                    LayoutState.View child = state.getView(fillResult.positionStart - i);
                    measureChild(section, child);
                    viewSpan += mLayoutManager.getDecoratedMeasuredHeight(child.view);
                    state.recycleView(child);
                }
            }

            // Perform offset if needed.
            if (viewSpan < minimumHeight) {
                int offset = viewSpan - minimumHeight;
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
            measureChild(section, child);
            AddData r = layoutChild(state, section, child, direction, currentPosition, markerLine);
            currentPosition += direction == LayoutManager.Direction.START ? -1 : 1;
            markerLine = r.markerLine;
            fillResult.addedChildCount += 1;
            if (fillResult.firstChildIndex == -1) {
                fillResult.firstChildIndex = r.indexAddedTo;
            } else {
                fillResult.firstChildIndex = r.indexAddedTo < fillResult.firstChildIndex
                        ? r.indexAddedTo : fillResult.firstChildIndex;
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

    private int layoutChild(LayoutState.View child, int markerLine,
            LayoutManager.Direction direction, SectionData2 sd, LayoutState state) {
        final int height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
        final int width = mLayoutManager.getDecoratedMeasuredWidth(child.view);

        int left = state.isLTR ? sd.contentStart : sd.contentEnd;
        int right = left + width;
        int top;
        int bottom;

        if (direction == LayoutManager.Direction.END) {
            top = markerLine;
            bottom = top + height;
        } else {
            bottom = markerLine;
            top = bottom - height;
        }
        mLayoutManager.layoutDecorated(child.view, left, top, right, bottom);

        if (direction == LayoutManager.Direction.END) {
            markerLine = mLayoutManager.getDecoratedBottom(child.view);
        } else {
            markerLine = mLayoutManager.getDecoratedTop(child.view);
        }

        return markerLine;
    }

    private AddData layoutChild(LayoutState state, SectionData section, LayoutState.View child,
            LayoutManager.Direction direction, int currentPosition, int markerLine) {
        AddData addData = new AddData();
        if (!child.wasCached) {
            final int height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
            final int width = mLayoutManager.getDecoratedMeasuredWidth(child.view);

            int left = state.isLTR ? section.getContentMarginStart()
                    : section.getContentMarginEnd();
            int right = left + width;
            int top;
            int bottom;

            if (direction == LayoutManager.Direction.END) {
                top = markerLine;
                bottom = top + height;
            } else {
                bottom = markerLine;
                top = bottom - height;
            }
            mLayoutManager.layoutDecorated(child.view, left, top, right, bottom);
        }
        if (direction == LayoutManager.Direction.END) {
            addData.markerLine = mLayoutManager.getDecoratedBottom(child.view);
        } else {
            addData.markerLine = mLayoutManager.getDecoratedTop(child.view);
        }

        addData.indexAddedTo = addView(child, currentPosition, direction, state);

        return addData;
    }

    private void measureChild(LayoutState.View child, SectionData2 sd) {
        if (child.wasCached) {
            return;
        }

        mLayoutManager.measureChildWithMargins(child.view, sd.getTotalMarginWidth(), 0);
    }

    private void measureChild(SectionData section, LayoutState.View child) {
        if (child.wasCached) {
            return;
        }

        mLayoutManager.measureChildWithMargins(child.view,
                section.getHeaderMarginStart() + section.getHeaderMarginEnd(), 0);
    }

    class AddData {

        int indexAddedTo;

        int markerLine;
    }
}
