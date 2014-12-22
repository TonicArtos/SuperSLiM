package com.tonicartos.superslim;

import android.view.View;

public class LinearSectionLayoutManager extends SectionLayoutManager {

    public LinearSectionLayoutManager(LayoutManager layoutManager) {
        super(layoutManager);
    }

    private AddData layoutChild(LayoutState state, SectionData section, LayoutState.View child,
            LayoutManager.Direction direction, int currentPosition, int markerLine) {
        AddData addData = new AddData();
        if (!child.wasCached) {
            final int height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
            final int width = mLayoutManager.getDecoratedMeasuredWidth(child.view);

            int left = section.getContentStartMargin();
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

        addData.indexAddedTo = addView(state, child, currentPosition, direction);

        return addData;
    }

    private void measureChild(SectionData section, LayoutState.View child) {
        if (child.wasCached) {
            return;
        }

        mLayoutManager.measureChildWithMargins(child.view,
                section.getHeaderStartMargin() + section.getHeaderEndMargin(), 0);
    }

    @Override
    public View getFirstView(int section) {
        int lookAt = 0;
        int childCount = mLayoutManager.getChildCount();
        View candidate = null;
        while (true) {
            if (lookAt >= childCount) {
                return candidate;
            }

            android.view.View view = mLayoutManager.getChildAt(lookAt);
            LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) view.getLayoutParams();
            if (section == lp.section && !lp.isHeader) {
                return view;
            } else if (section == lp.section && lp.isHeader) {
                candidate = view;
            }

            lookAt += 1;
        }
    }

    @Override
    public android.view.View getLastView(int section) {
        int lookAt = mLayoutManager.getChildCount() - 1;
        View candidate = null;
        while (true) {
            if (lookAt < 0) {
                return candidate;
            }

            android.view.View view = mLayoutManager.getChildAt(lookAt);
            LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) view.getLayoutParams();
            if (section == lp.section && !lp.isHeader) {
                return view;
            } else if (section == lp.section && lp.isHeader) {
                candidate = view;
            }
            lookAt -= 1;
        }
    }

    @Override
    public int getHighestEdge(int section, int startEdge) {
        // Look from start to find children that are the highest.
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View child = mLayoutManager.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (params.section != section) {
                break;
            }
            if (params.isHeader) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return mLayoutManager.getDecoratedTop(child);
        }
        return startEdge;
    }

    @Override
    public int getLowestEdge(int section, int endEdge) {
        // Look from end to find children that are the lowest.
        for (int i = mLayoutManager.getChildCount() - 1; i >= 0; i--) {
            View child = mLayoutManager.getChildAt(i);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) child
                    .getLayoutParams();
            if (params.section != section) {
                break;
            }
            if (params.isHeader) {
                continue;
            }
            // A more interesting layout would have to do something more here.
            return mLayoutManager.getDecoratedBottom(child);
        }
        return endEdge;
    }

    @Override
    public FillResult fill(LayoutState state, SectionData section) {
        final int itemCount = state.recyclerState.getItemCount();
        final int height = mLayoutManager.getHeight();

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

    private FillResult fillToStart(LayoutState state, SectionData section) {
        final int itemCount = state.recyclerState.getItemCount();
        final int endEdge = mLayoutManager.getHeight();
        final int startEdge = 0;

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
                }
            }

            // Perform offset if needed.
            if (viewSpan < minimumHeight) {
                fillResult.markerEnd += section.getMinimumHeight() - viewSpan;
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
            if (params.isHeader || params.section != section.getSection()) {
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

            position += 1;
        }

        return headerOffset;
    }

    private int addView(LayoutState state, LayoutState.View child,
            int position, LayoutManager.Direction direction) {
        int addIndex;
        if (direction == LayoutManager.Direction.START) {
            addIndex = 0;
        } else {
            addIndex = mLayoutManager.getChildCount();
        }

        if (child.wasCached) {
            mLayoutManager.attachView(child.view, addIndex);
            state.decacheView(position);
        } else {
            mLayoutManager.addView(child.view, addIndex);
        }

        return addIndex;
    }

    class AddData {

        int indexAddedTo;

        int markerLine;
    }
}
