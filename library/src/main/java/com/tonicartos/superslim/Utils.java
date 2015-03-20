package com.tonicartos.superslim;

import android.view.View;

class Utils {

    static final int INVALID_INDEX = -1;

    static int binarySearchForLastPosition(int min, int max, SectionData sd,
            LayoutQueryHelper helper) {
        if (max < min) {
            return INVALID_INDEX;
        }

        final int count = helper.getChildCount();

        int mid = min + (max - min) / 2;

        View candidate = helper.getChildAt(mid);
        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) candidate
                .getLayoutParams();
        int candidatePosition = params.getViewPosition();
        if (candidatePosition < sd.firstPosition) {
            return binarySearchForLastPosition(mid + 1, max, sd, helper);
        }

        if (candidatePosition > sd.lastPosition || params.isHeader()) {
            return binarySearchForLastPosition(min, mid - 1, sd, helper);
        }

        if (mid == count - 1) {
            return mid;
        }

        View next = helper.getChildAt(mid + 1);
        LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) next.getLayoutParams();
        if (!sd.containsItem(lp.getViewPosition())) {
            return mid;
        }

        if (lp.isHeader()) {
            if (mid + 1 == count - 1) {
                return mid;
            }

            next = helper.getChildAt(mid + 2);
            if (sd.containsItem(next)) {
                return mid;
            }
        }

        return binarySearchForLastPosition(mid + 1, max, sd, helper);
    }

    static int findFirstVisibleIndex(int edge, int anchorIndex, SectionData sd,
            LayoutQueryHelper helper) {
        final int childCount = helper.getChildCount();
        for (int i = anchorIndex; i < childCount; i++) {
            View child = helper.getChildAt(i);
            if (!sd.containsItem(helper.getPosition(child))) {
                break;
            }

            if (helper.getBottom(child) > edge) {
                return i;
            }
        }

        return INVALID_INDEX;
    }

    static int findHeaderIndexFromFirstIndex(int fvi, SectionData sd,
            LayoutQueryHelper helper) {
        final int count = helper.getChildCount();
        int fvp = helper.getPosition(helper.getChildAt(fvi));
        // Header is always attached after other section items. So start looking from there, and
        // back towards the current fvi.
        for (int i = Math.min(sd.lastPosition - fvp + 1 + fvi, count - 1); i >= fvi; i--) {
            View check = helper.getChildAt(i);
            if (helper.getPosition(check) == sd.firstPosition) {
                return i;
            }
        }
        return INVALID_INDEX;
    }

    /**
     * The header is almost guaranteed to be at the end so just use look there.
     *
     * @param sd Section data.
     * @return Header, or null if not found.
     */
    static int findHeaderIndexFromLastIndex(int lastIndex, SectionData sd,
            LayoutQueryHelper helper) {
        for (int i = lastIndex; i >= 0; i--) {
            View child = helper.getChildAt(i);
            int position = helper.getPosition(child);
            if (!sd.containsItem(position)) {
                break;
            } else if (sd.firstPosition == position) {
                return i;
            }
        }
        return INVALID_INDEX;
    }

    static int findLastIndexForSection(SectionData sd, LayoutQueryHelper helper) {
        return binarySearchForLastPosition(0, helper.getChildCount() - 1, sd, helper);
    }

    static int findLastVisibleIndex(int edge, int anchorIndex, SectionData sd,
            LayoutQueryHelper helper) {
        for (int i = anchorIndex; i >= 0; i--) {
            View child = helper.getChildAt(i);
            if (!sd.containsItem(helper.getPosition(child))) {
                break;
            }

            if (helper.getTop(child) < edge) {
                return i;
            }
        }

        return INVALID_INDEX;
    }
}
