package com.tonicartos.superslim.util;

import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LayoutState;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class Utils {

    public static final int RV_HEIGHT = 1280;

    public static final int RV_WIDTH = 720;

    public static void checkSimpleGridLayout(RecyclerView recyclerView, int itemHeight,
            int numColumns) {
        final int width = recyclerView.getWidth();
        final int gridWidth = width - recyclerView.getPaddingLeft() - recyclerView
                .getPaddingRight();
        final int columnWidth = gridWidth / numColumns;

        for (int i = 0; i < 20; i += numColumns) {
            final int top = i / numColumns * itemHeight;
            for (int j = 0; j < numColumns && i + j < 20; j++) {
                View child = recyclerView.getChildAt(i + j);

                assertThat(child).hasHeight(itemHeight);
                assertThat(child).hasLeft(j * columnWidth + recyclerView.getPaddingLeft());
                if (j == numColumns - 1) {
                    assertThat(child).hasRight(width - recyclerView.getPaddingRight());
                } else {
                    assertThat(child).hasRight(
                            j * columnWidth + columnWidth + recyclerView.getPaddingLeft());
                }
                assertThat(child).hasTop(top + recyclerView.getPaddingTop());
                assertThat(child).hasBottom(top + itemHeight + recyclerView.getPaddingTop());
            }
        }
    }

    public static void checkSimpleLinearLayout(RecyclerView mRecyclerView, int itemHeight) {
        final int paddingTop = mRecyclerView.getPaddingTop();
        final int rvHeight = mRecyclerView.getHeight();

        final int expectedLeft = mRecyclerView.getPaddingLeft();
        final int expectedRight = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight();

        // Child views should be laid out in a linear fashion.
        final int childCount = mRecyclerView.getChildCount();
        final int expectedNumAttached = (rvHeight - paddingTop) / itemHeight +
                ((rvHeight - paddingTop) % itemHeight > 0 ? 1 : 0);

        View first = mRecyclerView.getChildAt(0);
        LayoutManagerWrapper.LayoutParams params = (LayoutManagerWrapper.LayoutParams) first
                .getLayoutParams();
        final int viewLayoutPosition = params.getViewLayoutPosition();
        assertEquals(0, viewLayoutPosition);
        assertEquals(expectedNumAttached, childCount);

        for (int i = 0; i < childCount; i++) {
            View child = mRecyclerView.getChildAt(i);
            assertThat(child).hasHeight(itemHeight);
            assertThat(child).hasLeft(expectedLeft);
            assertThat(child).hasRight(expectedRight);
            assertThat(child).hasTop(itemHeight * i + paddingTop);
            assertThat(child).hasBottom(itemHeight * (i + 1) + paddingTop);
        }
    }

    /**
     * Perform an overscroll test.
     *
     * @param offset         Offset to apply to simulate overscroll; dx, dy.
     * @param padding        Padding values; l, t, r, b.
     * @param expected       Expected bounds; l, t, r, b.
     * @param adapter        Adapter to use.
     * @param mLayoutManager Layout manager to use.
     * @param mRecyclerView  RecyclerView to use.
     */
    public static void doOverscrollTest(int[] offset, int[] padding, boolean expected,
            RecyclerView.Adapter adapter, LayoutManagerWrapper mLayoutManager,
            RecyclerView mRecyclerView)
            throws Exception {
        setupLayoutTest(padding, adapter, mLayoutManager, mRecyclerView);
        // Shift so that bounds are after start and end.
        adjustPosition(mRecyclerView, offset[0], offset[1]);

        Method isOverscrolled =
                LayoutManager.class.getDeclaredMethod("isOverscrolled", LayoutState.class);
        isOverscrolled.setAccessible(true);

        LayoutState state = mock(LayoutState.class);
        RecyclerView.State mRecyclerViewState = mock(RecyclerView.State.class);
        when(state.getRecyclerState()).thenReturn(mRecyclerViewState);
        when(mRecyclerViewState.getItemCount()).thenReturn(adapter.getItemCount());

        boolean result = (Boolean) isOverscrolled.invoke(mLayoutManager, state);
        assertEquals(expected, result);
    }

    /**
     * Scroll all the way to the end in 1 pixel increments. Then scroll back checking recorded
     * history that layout for each repeated state is the same.
     *
     * @param stepSize      Scroll increment, signed.
     * @param maxSteps      Constraint on number of steps to take. Use to ensure test ends, but
     *                      value should be enough to cover the entire scroll distance.
     * @param padding       RecyclerView padding.
     * @param adapter       Adapter.
     * @param layoutManager LayoutManager.
     * @param recyclerView  RecyclerView.
     */
    public static void doScrollConsistencyTest(int stepSize, int maxSteps, int[] padding,
            RecyclerView.Adapter adapter, LayoutManagerWrapper layoutManager,
            RecyclerView recyclerView) {
        setupLayoutTest(padding, adapter, layoutManager, recyclerView);

        List<FrameState> frames = new ArrayList<>();

        // Scroll to end and record frames.
        for (int i = 0; i < maxSteps; i++) {
            frames.add(FrameState.from(recyclerView, layoutManager));

            recyclerView.scrollBy(0, stepSize);
            if (layoutManager.delta == 0) {
                // Hit end.
                break;
            }
        }
        assertTrue(frames.size() > 0);
        // Scroll back while checking frames match.
        for (int i = 0; i < maxSteps; i++) {
            assertTrue(frames.size() > 0);
            FrameState frame = frames.remove(frames.size() - 1);
            assertTrue(frame.matches(recyclerView, layoutManager));

            recyclerView.scrollBy(0, -stepSize);
            if (layoutManager.delta == 0) {
                // Hit end.
                break;
            }
        }
        assertTrue(frames.size() == 0);
    }

    public static void setupLayoutPaddingPermutation(int permutation, int padding,
            RecyclerView mRecyclerView, RecyclerView.Adapter adapter,
            LayoutManagerWrapper layoutManager) {
        final int l = (permutation & 1) != 0 ? padding : 0;
        final int t = (permutation & 2) != 0 ? padding : 0;
        final int r = (permutation & 4) != 0 ? padding : 0;
        final int b = (permutation & 8) != 0 ? padding : 0;
        setupLayoutTest(new int[]{l, t, r, b}, adapter, layoutManager, mRecyclerView);
    }

    public static void setupLayoutTest(int[] p, RecyclerView.Adapter adapter,
            LayoutManagerWrapper mLayoutManager, RecyclerView mRecyclerView) {
        if (p != null) {
            mRecyclerView.setPadding(p[0], p[1], p[2], p[3]);
        }
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);
    }

    private static void adjustPosition(RecyclerView mRecyclerView, int dx, int dy) {
        if (dx != 0) {
            mRecyclerView.offsetChildrenHorizontal(dx);
        }
        if (dy != 0) {
            mRecyclerView.offsetChildrenVertical(dy);
        }
    }

    public static class FrameState {

        private Record[] mRecords;

        private FrameState(Record[] records) {
            mRecords = records;
        }

        public static FrameState from(RecyclerView rv, LayoutManagerWrapper lm) {
            final int childCount = rv.getChildCount();
            final Record[] records = new Record[childCount];
            for (int i = 0; i < childCount; i++) {
                records[i] = Record.from(rv.getChildAt(i), lm);
            }

            return new FrameState(records);
        }

        public boolean matches(RecyclerView rv, LayoutManagerWrapper lm) {
            final int childCount = rv.getChildCount();
            if (childCount != mRecords.length) {
                Log.v("Check", "Mismatched view count. Have "
                        + childCount + ", expected " + mRecords.length + ".");
                logViewsAndRecords(childCount, lm, rv);
                return false;
            }

            for (int i = 0; i < childCount; i++) {
                if (!mRecords[i].matches(rv.getChildAt(i), lm)) {
                    Log.v("Check", "Mismatched view in index " + i + ".");
                    logViewsAndRecords(childCount, lm, rv);
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Record r : mRecords) {
                sb.append(r.toString()).append("\n");
            }
            return sb.toString();
        }

        private void logViewsAndRecords(int childCount, LayoutManagerWrapper lm, RecyclerView rv) {
            for (Record record : mRecords) {
                Log.v("Frame", record.toString());
            }
            for (int i = 0; i < childCount; i++) {
                Log.v("View", Record.from(rv.getChildAt(i), lm).toString());
            }
        }

        public static class Record {

            public int left, top, right, bottom;

            public int position;

            private Record() {
            }

            public static Record from(View v, LayoutManagerWrapper lm) {
                Record r = new Record();
                r.left = lm.getDecoratedLeft(v);
                r.top = lm.getDecoratedTop(v);
                r.right = lm.getDecoratedRight(v);
                r.bottom = lm.getDecoratedBottom(v);

                r.position = lm.getPosition(v);

                return r;
            }

            public boolean matches(View v, LayoutManagerWrapper lm) {
                return left == lm.getDecoratedLeft(v) &&
                        top == lm.getDecoratedTop(v) &&
                        right == lm.getDecoratedRight(v) &&
                        bottom == lm.getDecoratedBottom(v) &&
                        position == lm.getPosition(v);

            }

            @Override
            public String toString() {
                return "Position: " + position +
                        " Dimensions: " + left + ", " + top + ", " + right + ", " + bottom;
            }
        }
    }

    public static class LayoutManagerWrapper extends com.tonicartos.superslim.LayoutManager {

        public int delta;

        public LayoutManagerWrapper(Context context) {
            super(context);
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            delta = super.scrollVerticallyBy(dy, recycler, state);
            return delta;
        }
    }
}
