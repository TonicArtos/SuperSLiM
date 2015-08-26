package com.tonicartos.superslim;

import android.view.View;

public class LinearSLM extends SectionLayoutManager {

    public static int ID = LayoutManager.SECTION_MANAGER_LINEAR;

    public LinearSLM(LayoutManager layoutManager) {
        super(layoutManager);
    }

    @Override
    public int computeHeaderOffset(int firstVisiblePosition, SectionData sd, LayoutState state) {
        /*
         * Work from an assumed overlap and add heights from the start until the overlap is zero or
         * less, or the current position (or max items) is reached.
         */

        int areaAbove = 0;
        for (int position = sd.firstPosition + 1;
                areaAbove < sd.headerHeight && position < firstVisiblePosition;
                position++) {
            // Look to see if the header overlaps with the displayed area of the mSection.
            LayoutState.View child = state.getView(position);
            measureChild(child, sd);

            areaAbove += mLayoutManager.getDecoratedMeasuredHeight(child.view);
            state.cacheView(position, child.view);
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
    public int fillToEnd(int leadingEdge, int markerLine, int anchorPosition, SectionData sd,
            LayoutState state) {
        final int itemCount = state.getRecyclerState().getItemCount();

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
    public int fillToStart(int leadingEdge, int markerLine, int anchorPosition, SectionData sd,
            LayoutState state) {
        // Check to see if we have to adjust for minimum section height. We don't if there is an
        // attached non-header view in this section.
        boolean applyMinHeight = false;
        for (int i = 0; i < state.getRecyclerState().getItemCount(); i++) {
            View check = mLayoutManager.getChildAt(0);
            if (check == null) {
                applyMinHeight = false;
                break;
            }

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
            if (markerLine - minHeightOffset <= leadingEdge) {
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
    public int finishFillToEnd(int leadingEdge, View anchor, SectionData sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        final int markerLine = mLayoutManager.getDecoratedBottom(anchor);

        return fillToEnd(leadingEdge, markerLine, anchorPosition + 1, sd, state);
    }

    @Override
    public int finishFillToStart(int leadingEdge, View anchor, SectionData sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        final int markerLine = mLayoutManager.getDecoratedTop(anchor);

        return fillToStart(leadingEdge, markerLine, anchorPosition - 1, sd, state);
    }

    private int layoutChild(LayoutState.View child, int markerLine,
            LayoutManager.Direction direction, SectionData sd, LayoutState state) {
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

    private void measureChild(LayoutState.View child, SectionData sd) {
        mLayoutManager.measureChildWithMargins(child.view, sd.getTotalMarginWidth(), 0);
    }
}
