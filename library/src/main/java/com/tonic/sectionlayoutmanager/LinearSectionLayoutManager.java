package com.tonic.sectionlayoutmanager;

import android.view.View;

public class LinearSectionLayoutManager extends SectionLayoutManager {

    private void setupChild(LayoutManager.LayoutState markerLine, View child, Direction direction) {
        measureChild(child);
        layoutChild(child, markerLine, direction);
    }

    private void layoutChild(View child, LayoutManager.LayoutState markerLine,
            Direction direction) {
        final LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) child.getLayoutParams();
        final int height = mLayoutManager.getDecoratedMeasuredHeight(child);
        final int width = mLayoutManager.getDecoratedMeasuredWidth(child);

        int left = lp.headerStartMargin;
        int right = left + width;
        int top;
        int bottom;

        if (direction == Direction.END) {
            top = markerLine.markerLine;
            bottom = markerLine.markerLine + height;
        } else {
            top = markerLine.markerLine - height;
            bottom = markerLine.markerLine;
        }
        mLayoutManager.layoutDecorated(child, left, top, right, bottom);

        if (direction == Direction.END) {
            mLayoutManager.addView(child);
            markerLine.markerLine += height;
        } else {
            mLayoutManager.addView(child, 0);
            markerLine.markerLine -= height;
        }
    }

    private void measureChild(View child) {
        final LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) child.getLayoutParams();
        mLayoutManager.measureChildWithMargins(child, lp.headerStartMargin + lp.headerEndMargin, 0);
    }

    @Override
    public View getBottomView(int section) {
        int lookAt = mLayoutManager.getChildCount() - 1;
        while (true) {
            if (lookAt < 0) {
                return null;
            }

            View view = mLayoutManager.getChildAt(lookAt);
            if (memberOfSection(section, view)) {
                return view;
            }
            lookAt -= 1;
        }
    }

    private boolean memberOfSection(int section, View view) {
        LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) view.getLayoutParams();
        return section == lp.section && !lp.isHeader;
    }

    @Override
    public View getTopView(int section) {
        int lookAt = 0;
        int childCount = mLayoutManager.getChildCount();
        while (true) {
            if (lookAt >= childCount) {
                return null;
            }

            View view = mLayoutManager.getChildAt(lookAt);
            if (memberOfSection(section, view)) {
                return view;
            }

            lookAt += 1;
        }
    }

    @Override
    public int fill(LayoutManager.LayoutState state, int fromPosition) {
        final int itemCount = state.recyclerState.getItemCount();
        final int height = mLayoutManager.getHeight();

        int countAdded = 0;
        int currentPosition = fromPosition;

        while (true) {
            if (currentPosition < 0 || currentPosition >= itemCount) {
                break;
            }
            if (((direction == Direction.START) && (state.markerLine <= 0)) || (
                    direction == Direction.END && (state.markerLine
                            >= height))) {
                break;
            }

            LayoutManager.CachedView r = state.getView(currentPosition);

            LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) r.view.getLayoutParams();
            if (lp.isHeader || lp.section != state.section) {
                break;
            }

            if (r.wasCached) {
                reattachView(state, direction, r);
                state.decacheView(currentPosition);
            } else {
                setupChild(state, r.view, direction);
            }

            countAdded += 1;
            if (direction == Direction.START) {
                currentPosition -= 1;
            } else {
                currentPosition += 1;
            }
        }

        // If filling to start, need to check header height and adjust markerline to right location.
        if (direction == Direction.START && state.headerOverlap > 0) {
            int position = state.sectionFirstPosition + 1;
            int headerCoverageLeft = state.headerOverlap;

            while (true) {
                // Look to see if the header overlaps with the displayed area of the section.
                LayoutManager.CachedView view;

                if (position < currentPosition) {
                    view = state.getView(position);
                    if (!view.wasCached) {
                        measureChild(view.view);
                    }
                } else {
                    // Run into an item that is displayed, indicating header overlap.
                    state.headerOffset = state.headerOverlap - headerCoverageLeft;
                    break;
                }

                headerCoverageLeft -= mLayoutManager.getDecoratedMeasuredHeight(view.view);
                if (headerCoverageLeft <= 0) {
                    state.headerOffset = LayoutManager.LayoutState.NO_HEADER_OFFSET;
                    break;
                }

                position += 1;
            }
        }

        return countAdded;
    }

    private void reattachView(LayoutManager.LayoutState state,
            Direction direction, LayoutManager.CachedView r) {
        if (direction == Direction.START) {
            mLayoutManager.attachView(r.view, 0);
            state.markerLine -= mLayoutManager.getDecoratedMeasuredHeight(r.view);
        } else {
            mLayoutManager.attachView(r.view);
            state.markerLine += mLayoutManager.getDecoratedMeasuredHeight(r.view);
        }
    }
}
