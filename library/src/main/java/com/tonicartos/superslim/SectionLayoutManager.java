package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class SectionLayoutManager {

    private static final int MARGIN_UNSET = -1;

    protected RecyclerView.LayoutManager mLayoutManager;

    public SectionLayoutManager(LayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    /**
     * Measure and layout children. Make sure to only lay out views belonging to this mSection,
     * excepting headers, which are laid out by the wrapping layout manager.
     */
    public abstract FillResult fill(LayoutState state, SectionData sectionData);

    /**
     * Locate the view which has the earliest adapter position.
     *
     * @param sectionFirstPosition Section id.
     * @return View.
     */
    public abstract View getFirstView(int sectionFirstPosition);

    public int getHeaderEndMargin() {
        return MARGIN_UNSET;
    }

    public int getHeaderStartMargin() {
        return MARGIN_UNSET;
    }

    /**
     * Find the highest displayed edge of the section. If there is no member found then return the
     * start edge instead.
     */
    public abstract int getHighestEdge(int sectionFirstPosition, int startEdge);

    /**
     * Locate the view which has the latest adapter position.
     *
     * @param sectionFirstPosition Section id.
     * @return View.
     */
    public abstract View getLastView(int sectionFirstPosition);

    /**
     * Find the lowest displayed edge of the section. IF there is no member found then return the
     * end edge instead.
     */
    public abstract int getLowestEdge(int sectionFirstPosition, int endEdge);


}
