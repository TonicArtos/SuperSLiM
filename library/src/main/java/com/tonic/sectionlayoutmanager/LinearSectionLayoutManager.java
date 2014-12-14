package com.tonic.sectionlayoutmanager;

import android.view.View;

public class LinearSectionLayoutManager extends SectionLayoutManager {

    private void setupChild(LayoutState state, android.view.View child) {
        measureChild(state, child);
        layoutChild(state, child);
    }

    private void layoutChild(LayoutState state, View child) {
        final int height = mLayoutManager.getDecoratedMeasuredHeight(child);
        final int width = mLayoutManager.getDecoratedMeasuredWidth(child);

        int left = state.headerStartMargin;
        int right = left + width;
        int top;
        int bottom;

        if (state.isDirectionEnd()) {
            top = state.markerLine;
            bottom = state.markerLine + height;
        } else {
            top = state.markerLine - height;
            bottom = state.markerLine;
        }
        mLayoutManager.layoutDecorated(child, left, top, right, bottom);

        if (state.isDirectionEnd()) {
            mLayoutManager.addView(child);
            state.markerLine += height;
        } else {
            mLayoutManager.addView(child, 0);
            state.markerLine -= height;
        }
    }

    private void measureChild(LayoutState state, View child) {
        mLayoutManager.measureChildWithMargins(child, state.headerStartMargin + state.headerEndMargin, 0);
    }

    @Override
    public android.view.View getBottomView(int section) {
        int lookAt = mLayoutManager.getChildCount() - 1;
        while (true) {
            if (lookAt < 0) {
                return null;
            }

            android.view.View view = mLayoutManager.getChildAt(lookAt);
            if (memberOfSection(section, view)) {
                return view;
            }
            lookAt -= 1;
        }
    }

    private boolean memberOfSection(int section, android.view.View view) {
        LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) view.getLayoutParams();
        return section == lp.section && !lp.isHeader;
    }

    @Override
    public android.view.View getTopView(int section) {
        int lookAt = 0;
        int childCount = mLayoutManager.getChildCount();
        while (true) {
            if (lookAt >= childCount) {
                return null;
            }

            android.view.View view = mLayoutManager.getChildAt(lookAt);
            if (memberOfSection(section, view)) {
                return view;
            }

            lookAt += 1;
        }
    }

    @Override
    public int fill(LayoutState state, int fromPosition) {
        final int itemCount = state.recyclerState.getItemCount();
        final int height = mLayoutManager.getHeight();

        int countAdded = 0;
        int currentPosition = fromPosition;

        while (true) {
            if (currentPosition < 0 || currentPosition >= itemCount) {
                break;
            }
            if ((state.isDirectionStart() && state.markerLine <= 0) || (state.isDirectionEnd()
                    && state.markerLine >= height)) {
                break;
            }

            LayoutState.View r = state.getView(currentPosition);

            LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) r.view.getLayoutParams();
            if (lp.isHeader || lp.section != state.section) {
                break;
            }

            if (r.wasCached) {
                reattachView(state, r);
                state.decacheView(currentPosition);
            } else {
                setupChild(state, r.view);
            }

            countAdded += 1;
            if (state.isDirectionStart()) {
                currentPosition -= 1;
            } else {
                currentPosition += 1;
            }
        }

        // If filling to start, need to check header height and adjust markerline to right location.
        if (state.isDirectionStart() && state.headerOverlap > 0) {
            int position = state.sectionFirstPosition + 1;
            int headerCoverageLeft = state.headerOverlap;

            while (true) {
                // Look to see if the header overlaps with the displayed area of the section.
                LayoutState.View child;

                if (position < currentPosition) {
                    child = state.getView(position);
                    if (!child.wasCached) {
                        measureChild(state, child.view);
                    }
                } else {
                    // Run into an item that is displayed, indicating header overlap.
                    state.headerOffset = state.headerOverlap - headerCoverageLeft;
                    break;
                }

                headerCoverageLeft -= mLayoutManager.getDecoratedMeasuredHeight(child.view);
                if (headerCoverageLeft <= 0) {
                    state.headerOffset = LayoutState.NO_HEADER_OFFSET;
                    break;
                }

                position += 1;
            }
        }

        return countAdded;
    }

    private void reattachView(LayoutState state, LayoutState.View r) {
        if (state.isDirectionStart()) {
            mLayoutManager.attachView(r.view, 0);
            state.markerLine -= mLayoutManager.getDecoratedMeasuredHeight(r.view);
        } else {
            mLayoutManager.attachView(r.view);
            state.markerLine += mLayoutManager.getDecoratedMeasuredHeight(r.view);
        }
    }
}
