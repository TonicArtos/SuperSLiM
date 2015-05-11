package com.tonicartos.superslim;

import com.tonicartos.superslim.util.TestAdapterBuilder;
import com.tonicartos.superslim.util.Utils;
import com.tonicartos.superslim.util.Utils.LayoutManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import static junit.framework.Assert.assertEquals;

/**
 * End-to-end tests for a Layout Manager and LinearSLM combination.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class LinearLayoutTests {

    private Activity mActivity;

    private RecyclerView mRecyclerView;

    private LayoutManagerWrapper mLayoutManager;

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mRecyclerView = new RecyclerView(mActivity);
        mLayoutManager = new LayoutManagerWrapper(mActivity);
    }

    @Test
    public void test_findFirstCompletelyVisibleItem() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        assertEquals(0, mLayoutManager.findFirstCompletelyVisibleItemPosition());
    }

    @Test
    public void test_findFirstCompletelyVisibleItemOfScrolledSections() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(1, mLayoutManager.findFirstCompletelyVisibleItemPosition());
    }

    @Test
    public void test_findFirstCompletelyVisibleItemOfScrolledSectionsWithHeaders() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        mRecyclerView.scrollBy(0, 50);
        assertEquals(0, mLayoutManager.findFirstCompletelyVisibleItemPosition());

        mRecyclerView.scrollBy(0, 50);
        assertEquals(1, mLayoutManager.findFirstCompletelyVisibleItemPosition());

        mRecyclerView.scrollBy(0, 1);
        assertEquals(2, mLayoutManager.findFirstCompletelyVisibleItemPosition());
    }

    @Test
    public void test_findFirstCompletelyVisibleItemWithHeader() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        assertEquals(0, mLayoutManager.findFirstCompletelyVisibleItemPosition());
    }

    @Test
    public void test_findFirstVisibleItem() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
    }

    @Test
    public void test_findFirstVisibleItemOfScrolledSections() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
    }

    @Test
    public void test_findFirstVisibleItemOfScrolledSectionsWithHeaders() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);
        mRecyclerView.scrollBy(0, 150);

        assertEquals(1, mLayoutManager.findFirstVisibleItemPosition());
    }

    @Test
    public void test_findFirstVisibleItemWithHeader() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
    }

    @Test
    public void test_findLastCompletelyVisibleItem() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, Utils.RV_WIDTH, 100, null)
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        assertEquals(11, mLayoutManager.findLastCompletelyVisibleItemPosition());
    }

    @Test
    public void test_findLastCompletelyVisibleItemOfScrolledSections() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(12, mLayoutManager.findLastCompletelyVisibleItemPosition());
    }

    @Test
    public void test_findLastCompletelyVisibleItemOfScrolledSectionsWithHeaders() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(12, mLayoutManager.findLastCompletelyVisibleItemPosition());
    }

    @Test
    public void test_findLastCompletelyVisibleItemWithHeader() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        assertEquals(11, mLayoutManager.findLastCompletelyVisibleItemPosition());
    }

    @Test
    public void test_findLastVisibleItem() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, Utils.RV_WIDTH, 100, null)
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        assertEquals(12, mLayoutManager.findLastVisibleItemPosition());
    }

    @Test
    public void test_findLastVisibleItemOfScrolledSections() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .addLinearSection(5, Utils.RV_WIDTH, 100, null)
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(13, mLayoutManager.findLastVisibleItemPosition());
    }

    @Test
    public void test_findLastVisibleItemOfScrolledSectionsWithHeaders() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);
        mRecyclerView.scrollBy(0, 150);

        assertEquals(14, mLayoutManager.findLastVisibleItemPosition());
    }

    @Test
    public void test_findLastVisibleItemWithHeader() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, Utils.RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        assertEquals(12, mLayoutManager.findLastVisibleItemPosition());
    }

    @Test
    public void test_layout() {
        final int padding = 20;
        final int itemHeight = 100;
        final int itemWidth = 720;
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, itemWidth, itemHeight, null)
                .build(mActivity);

        for (int i = 0; i < 16; i++) {
            Utils.setupLayoutPaddingPermutation(i, padding, mRecyclerView, adapter, mLayoutManager);
            Utils.checkSimpleLinearLayout(mRecyclerView, itemHeight);
        }
    }

    @Test
    public void test_overscrollAfterStartAfterEnd() throws Exception {
        final int padding = 20;
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;

        // Test once with padding.
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT, null)
                .build(mActivity);
        layoutPadding = new int[]{0, padding, 0, padding};
        Utils.doOverscrollTest(new int[]{0, dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);

        // Test with no padding.
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAfterStartAtEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT - dy, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAfterStartBeforeEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT - dy * 2, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAtStartAfterEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT + dy, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, 0}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAtStartAtEnd() throws Exception {
        final int numItems = 1;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, 0}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAtStartBeforeEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT - dy, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, 0}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollBeforeStartAfterEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT + dy * 2, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, -dy}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollBeforeStartAtEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT + dy, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, -dy}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollBeforeStartBeforeEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, Utils.RV_HEIGHT, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        Utils.doOverscrollTest(new int[]{0, -dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_scroll() {
        final int numItems = 10;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, Utils.RV_WIDTH, 100, null)
                .addLinearSection(numItems, Utils.RV_WIDTH, 100, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.INLINE))
                .addLinearSection(numItems, Utils.RV_WIDTH, 100, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.INLINE | TestAdapterBuilder.Header.OVERLAY))
                .addLinearSection(numItems, Utils.RV_WIDTH, 100, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.INLINE | TestAdapterBuilder.Header.NONSTICKY))
                .addLinearSection(numItems, Utils.RV_WIDTH, 100, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.MARGIN_START))
                .addLinearSection(numItems, Utils.RV_WIDTH, 100, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.MARGIN_START | TestAdapterBuilder.Header.OVERLAY))
                .addLinearSection(numItems, Utils.RV_WIDTH, 100, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.MARGIN_END))
                .addLinearSection(numItems, Utils.RV_WIDTH, 100, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.MARGIN_END | TestAdapterBuilder.Header.OVERLAY))
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};

        // Test scroll from start position;
        int stepSize = 1;
        int maxSteps = 10000;
        Utils.doScrollConsistencyTest(
                stepSize, maxSteps, layoutPadding, adapter, mLayoutManager, mRecyclerView);

        // Jump to end and test scroll consistency in opposite direction.
        mRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
        stepSize = -1;
        Utils.doScrollConsistencyTest(
                stepSize, maxSteps, layoutPadding, adapter, mLayoutManager, mRecyclerView);
    }
}
