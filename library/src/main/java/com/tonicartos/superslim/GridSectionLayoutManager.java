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

    public void setColumnMinimumWidth(int minimumWidth) {
        mMinimumWidth = minimumWidth;
        mColumnsSpecified = false;
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
        mMinimumWidth = 0;
        mColumnsSpecified = true;
    }

    private AddData fillRow(LayoutState state, SectionData section, LayoutState.View startChild,
            LayoutManager.Direction direction, int startPosition, int markerLine) {
        final int itemCount = state.recyclerState.getItemCount();

        AddData addData = new AddData();
        addData.earliestIndexAddedTo = -1;

        LayoutManager.LayoutParams params = startChild.getLayoutParams();
        int sectionFirst = params.sectionFirstPosition;
        LayoutManager.LayoutParams firstParams = state.getView(sectionFirst).getLayoutParams();
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
            if (nextParams.section == section.getSection()) {
                measureChild(section, next);
                // Detect already attached children and adjust effective markerline from it.
                if (!rowAlreadyPositioned && next.wasCached) {
                    rowAlreadyPositioned = true;
                    rowPriorPosition = mLayoutManager.getDecoratedTop(next.view);
                }
                int height = mLayoutManager.getDecoratedMeasuredHeight(next.view);
                rowHeight = height > rowHeight ? height : rowHeight;
            } else {
                next = null;
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
            layoutChild(child, section, col, top);
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
            addData.markerLine = rowAlreadyPositioned ? rowPriorPosition: markerLine - rowHeight;
        }

        return addData;
    }

    private void layoutChild(LayoutState.View child, SectionData section, int col, int top) {
        if (child.wasCached) {
            return;
        }

        int height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
        int width = mLayoutManager.getDecoratedMeasuredWidth(child.view);
        int bottom = top + height;
        int left = section.getContentStartMargin() + col * mColumnWidth;
        int right = left + width;

        mLayoutManager.layoutDecorated(child.view, left, top, right, bottom);
    }

    private void measureChild(SectionData section, LayoutState.View child) {
        if (child.wasCached) {
            return;
        }

        int widthOtherColumns = (mNumColumns - 1) * mColumnWidth;
        mLayoutManager.measureChildWithMargins(child.view,
                section.getHeaderStartMargin() + section.getHeaderEndMargin() + widthOtherColumns,
                0);
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

            View view = mLayoutManager.getChildAt(lookAt);
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
    public View getLastView(int section) {
        int lookAt = mLayoutManager.getChildCount() - 1;
        View candidate = null;
        while (true) {
            if (lookAt < 0) {
                return candidate;
            }

            View view = mLayoutManager.getChildAt(lookAt);
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
        int bottomMostEdge = 0;
        int leftPosition = mLayoutManager.getWidth();
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

            if (child.getLeft() >= leftPosition) {
                // Last one in row already checked.
                return bottomMostEdge;
            } else {
                leftPosition = child.getLeft();
            }
            int bottomEdge = mLayoutManager.getDecoratedBottom(child);
            if (bottomMostEdge < bottomEdge) {
                bottomMostEdge = bottomEdge;
            }
        }
        return bottomMostEdge;
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

    private void calculateColumnWidthValues(SectionData section) {
        int availableWidth = mLayoutManager.getWidth()
                - section.getContentStartMargin() - section.getContentEndMargin();
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
            Log.e("GridSectionLayoutManager",
                    "Too many columns (" + mNumColumns + ") for available width" + availableWidth
                            + ".");
        }
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
                if (height > rowHeight) {
                    rowHeight = height;
                }
            }
            viewSpan += rowHeight;
        }
        return viewSpan;
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

    class AddData {

        int numChildrenAdded;

        int earliestIndexAddedTo;

        int markerLine;
    }
}
