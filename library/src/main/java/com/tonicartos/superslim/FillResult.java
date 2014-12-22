package com.tonicartos.superslim;

public class FillResult {

    /**
     * Adapter position of start view displayed by the section.
     */
    public int positionStart;

    /**
     * Adapter position of end view displayed by the section.
     */
    public int positionEnd;

    /**
     * Line at which the displayed section area starts.
     */
    public int markerStart;

    /**
     * Line at which the displayed section area finishes.
     */
    public int markerEnd;

    /**
     * Position of earliest child added in recycler view child list. Typically 0 or
     * getChildCount().
     */
    public int firstChildIndex;

    /**
     * Number of children added tot he recycler view in the fill operation.
     */
    public int addedChildCount;

    /**
     * If negative then the header is well before the displayed part of the section.
     */
    public int headerOffset;

    @Override
    public String toString() {
        return "FillResult\nPosition " + positionStart + ":" + positionEnd + "\nMarker "
                + markerStart + ":" + markerEnd + "\nFirst child index " + firstChildIndex
                + "\nAdded child count " + addedChildCount + "\nHeader overlap " + headerOffset;
    }
}
