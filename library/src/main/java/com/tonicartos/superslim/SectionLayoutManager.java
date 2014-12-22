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
     * Locate the view which has the earliest adapter position.
     * @param section Section id.
     * @return View.
     */
    public abstract View getFirstView(int section);

    /**
     * Locate the view which has the latest adapter position.
     *
     * @param section Section id.
     * @return View.
     */
    public abstract View getLastView(int section);

    /**
     * Find the highest displayed edge of the section. If there is no member found then return the
     * start edge instead.
     */
    public abstract int getHighestEdge(int section, int startEdge);

    /**
     * Find the lowest displayed edge of the section. IF there is no member found then return the
     * end edge instead.
     */
    public abstract int getLowestEdge(int section, int endEdge);

    /**
     * Measure and layout children. Make sure to only lay out views belonging to this mSection,
     * excepting headers, which are laid out by the wrapping layout manager.
     */
    public abstract FillResult fill(LayoutState state, SectionData sectionData);

    public int getHeaderStartMargin() {
        return MARGIN_UNSET;
    }

    public int getHeaderEndMargin() {
        return MARGIN_UNSET;
    }


}
