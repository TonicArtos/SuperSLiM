package com.tonicartos.superslim;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class LayoutHelper implements LayoutQueryHelper, LayoutModifierHelper {

    public abstract int getLeadingEdge();

    public abstract LayoutHelper getSubsectionLayoutHelper();

    /**
     * Init layout helper.
     *
     * @param sd          Sublayout's section data.
     * @param markerLine  Line at which the sublayout will begin.
     * @param leadingEdge Line up to which the sublayout can fill.
     * @param stickyEdge  Line at which sticky headers will be aligned.
     */
    public abstract void init(SectionData sd, int markerLine, int leadingEdge, int stickyEdge);

    /**
     * Init layout helper.
     *
     * @param sd               Sublayout's section data.
     * @param horizontalOffset Left position of the sublayout relative to super layout.
     * @param unavailableWidth Width of super layout unavailable to the sublayout.
     * @param markerLine       Line at which the sublayout will begin.
     * @param leadingEdge      Line up to which the sublayout can fill.
     * @param stickyEdge       Line at which sticky headers will be aligned.
     */
    public abstract void init(SectionData sd, int horizontalOffset, int unavailableWidth,
            int markerLine, int leadingEdge, int stickyEdge);

    public abstract void recycle();

    abstract int layoutHeaderTowardsEnd(View header, int markerLine, RecyclerView.State state);

    abstract int layoutHeaderTowardsStart(View header, int offset, int sectionTop,
            int sectionBottom, RecyclerView.State state);

    abstract void measureHeader(View header);

    abstract int translateFillResult(int markerLine);

    abstract void updateVerticalOffset(int markerLine);

    static interface Parent extends LayoutQueryHelper, LayoutModifierHelper {

        void measureHeader(View header, int widthUsed, int heightUsed);
    }
}
