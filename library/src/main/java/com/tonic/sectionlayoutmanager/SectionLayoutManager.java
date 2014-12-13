package com.tonic.sectionlayoutmanager;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

public abstract class SectionLayoutManager {

    private static final int MARGIN_UNSET = -1;

    protected RecyclerView.LayoutManager mLayoutManager;

    void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    public abstract View getBottomView(int section);

    public abstract View getTopView(int section);

    public int getHeaderStartMargin() {
        return MARGIN_UNSET;
    }

    public int getHeaderEndMargin() {
        return MARGIN_UNSET;
    }

    public enum Direction {
        START,
        END
    }

    /**
     * Measure and layout children from given markerLine to the start of the section. Make sure to
     * only lay out views belonging to this section, excepting headers, which are laid out by the
     * wrapping layout manager.
     * @param state        State for coordinating the overall layout.
     * @param section      The current section being laid out.
     */
    public abstract int fill(LayoutManager.LayoutState state, int section);
}
