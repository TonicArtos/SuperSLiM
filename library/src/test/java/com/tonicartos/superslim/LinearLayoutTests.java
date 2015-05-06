package com.tonicartos.superslim;

import com.tonicartos.superslim.util.TestAdapterBuilder;

import static junit.framework.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;

/**
 *
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class LinearLayoutTests {

    private static final int RV_HEIGHT = 1280;

    private static final int RV_WIDTH = 720;

    private Activity mActivity;

    private RecyclerView mRecyclerView;

    private LayoutManager mLayoutManager;

    private static void adjustPosition(RecyclerView mRecyclerView, int dx, int dy) {
        if (dx != 0) {
            mRecyclerView.offsetChildrenHorizontal(dx);
        }
        if (dy != 0) {
            mRecyclerView.offsetChildrenVertical(dy);
        }
    }

    private static void checkHeaderLayout(RecyclerView mRecyclerView, int itemHeight) {
        Log.v("checkHeaderLayout", "TODO");
    }

    private static void checkSimpleLayout(RecyclerView mRecyclerView, int itemHeight) {
        final int paddingTop = mRecyclerView.getPaddingTop();
        final int rvHeight = mRecyclerView.getHeight();

        final int expectedLeft = mRecyclerView.getPaddingLeft();
        final int expectedRight = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight();

        // Child views should be laid out in a linear fashion.
        final int childCount = mRecyclerView.getChildCount();
        final int expectedNumAttached = (rvHeight - paddingTop) / itemHeight +
                ((rvHeight - paddingTop) % itemHeight > 0 ? 1 : 0);

        View first = mRecyclerView.getChildAt(0);
        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) first.getLayoutParams();
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
     * @param p              Padding values; l, t, r, b.
     * @param expected       Expected bounds; l, t, r, b.
     * @param adapter        Adapter to use.
     * @param mLayoutManager Layout manager to use.
     * @param mRecyclerView  RecyclerView to use.
     */
    private static void doOverscrollTest(int[] offset, int[] p, boolean expected,
            RecyclerView.Adapter adapter, LayoutManager mLayoutManager, RecyclerView mRecyclerView)
            throws Exception {
        setupLayoutTest(mRecyclerView, adapter, mLayoutManager, p[0], p[1], p[2], p[3]);
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

    private static void setupLayoutPaddingPermutation(int permutation, int padding,
            RecyclerView mRecyclerView, RecyclerView.Adapter adapter,
            LayoutManager mLayoutManager) {
        final int l = (permutation & 1) != 0 ? padding : 0;
        final int t = (permutation & 2) != 0 ? padding : 0;
        final int r = (permutation & 4) != 0 ? padding : 0;
        final int b = (permutation & 8) != 0 ? padding : 0;
        setupLayoutTest(mRecyclerView, adapter, mLayoutManager, l, t, r, b);
    }

    private static void setupLayoutTest(RecyclerView mRecyclerView, RecyclerView.Adapter adapter,
            LayoutManager mLayoutManager, int l, int t, int r, int b) {
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setPadding(l, t, r, b);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);
    }

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mRecyclerView = new RecyclerView(mActivity);
        mLayoutManager = new LayoutManager(mActivity);
    }

    @Test
    public void test_findFirstCompletelyVisibleItem() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100, null)
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        assertEquals(0,
                mLayoutManager.getPosition(mLayoutManager.findFirstCompletelyVisibleItem()));
    }

    @Test
    public void test_findFirstCompletelyVisibleItemOfScrolledSections() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(1,
                mLayoutManager.getPosition(mLayoutManager.findFirstCompletelyVisibleItem()));
    }

    @Test
    public void test_findFirstCompletelyVisibleItemOfScrolledSectionsWithHeaders() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        mRecyclerView.scrollBy(0, 50);

        assertEquals(1,
                mLayoutManager.getPosition(mLayoutManager.findFirstCompletelyVisibleItem()));
    }

    @Test
    public void test_findFirstCompletelyVisibleItemWithHeader() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        assertEquals(0,
                mLayoutManager.getPosition(mLayoutManager.findFirstCompletelyVisibleItem()));
    }

    @Test
    public void test_findFirstVisibleItem() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100, null)
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        assertEquals(0,
                mLayoutManager.getPosition(mLayoutManager.findFirstVisibleItem()));
    }

    @Test
    public void test_findFirstVisibleItemOfScrolledSections() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(0,
                mLayoutManager.getPosition(mLayoutManager.findFirstVisibleItem()));
    }

    @Test
    public void test_findFirstVisibleItemOfScrolledSectionsWithHeaders() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        mRecyclerView.scrollBy(0, 150);

        assertEquals(1,
                mLayoutManager.getPosition(mLayoutManager.findFirstVisibleItem()));
    }

    @Test
    public void test_findFirstVisibleItemWithHeader() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        assertEquals(0,
                mLayoutManager.getPosition(mLayoutManager.findFirstVisibleItem()));
    }

    @Test
    public void test_findLastCompletelyVisibleItem() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, RV_WIDTH, 100, null)
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        assertEquals(11,
                mLayoutManager.getPosition(mLayoutManager.findLastCompletelyVisibleItem()));
    }

    @Test
    public void test_findLastCompletelyVisibleItemOfScrolledSections() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(12,
                mLayoutManager.getPosition(mLayoutManager.findLastCompletelyVisibleItem()));
    }

    @Test
    public void test_findLastCompletelyVisibleItemOfScrolledSectionsWithHeaders() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        mRecyclerView.scrollBy(0, 50);

        assertEquals(12,
                mLayoutManager.getPosition(mLayoutManager.findLastCompletelyVisibleItem()));
    }

    @Test
    public void test_findLastCompletelyVisibleItemWithHeader() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        assertEquals(11,
                mLayoutManager.getPosition(mLayoutManager.findLastCompletelyVisibleItem()));
    }

    @Test
    public void test_findLastVisibleItem() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, RV_WIDTH, 100, null)
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        assertEquals(12,
                mLayoutManager.getPosition(mLayoutManager.findLastVisibleItem()));
    }

    @Test
    public void test_findLastVisibleItemOfScrolledSections() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .addLinearSection(5, RV_WIDTH, 100, null)
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);
        mRecyclerView.scrollBy(0, 50);

        assertEquals(13,
                mLayoutManager.getPosition(mLayoutManager.findLastVisibleItem()));
    }

    @Test
    public void test_findLastVisibleItemOfScrolledSectionsWithHeaders() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .addLinearSection(5, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        mRecyclerView.scrollBy(0, 150);

        assertEquals(14,
                mLayoutManager.getPosition(mLayoutManager.findLastVisibleItem()));
    }

    @Test
    public void test_findLastVisibleItemWithHeader() {
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, RV_WIDTH, 100,
                        TestAdapterBuilder.Header.with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.measure(0, 0);
        mRecyclerView.layout(0, 0, RV_WIDTH, RV_HEIGHT);

        assertEquals(12,
                mLayoutManager.getPosition(mLayoutManager.findLastVisibleItem()));
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
            setupLayoutPaddingPermutation(i, padding, mRecyclerView, adapter, mLayoutManager);
            checkSimpleLayout(mRecyclerView, itemHeight);
        }
    }

    @Test
    public void test_layoutWithInlineHeaders() {
        final int padding = 20;
        final int itemHeight = 100;
        final int itemWidth = 720;
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, itemWidth, itemHeight, TestAdapterBuilder.Header
                        .with(TestAdapterBuilder.Header.INLINE))
                .build(mActivity);

        for (int i = 0; i < 16; i++) {
            setupLayoutPaddingPermutation(i, padding, mRecyclerView, adapter, mLayoutManager);
            checkHeaderLayout(mRecyclerView, itemHeight);
        }
    }

    @Test
    public void test_layoutWithInlineNonStickyHeaders() {
        final int padding = 20;
        final int itemHeight = 100;
        final int itemWidth = 720;
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, itemWidth, itemHeight, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.INLINE | TestAdapterBuilder.Header.NONSTICKY))
                .build(mActivity);

        for (int i = 0; i < 16; i++) {
            setupLayoutPaddingPermutation(i, padding, mRecyclerView, adapter, mLayoutManager);
            checkHeaderLayout(mRecyclerView, itemHeight);
        }
    }

    @Test
    public void test_layoutWithInlineOverlayHeaders() {
        final int padding = 20;
        final int itemHeight = 100;
        final int itemWidth = 720;
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, itemWidth, itemHeight, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.INLINE | TestAdapterBuilder.Header.OVERLAY))
                .build(mActivity);

        for (int i = 0; i < 16; i++) {
            setupLayoutPaddingPermutation(i, padding, mRecyclerView, adapter, mLayoutManager);
            checkHeaderLayout(mRecyclerView, itemHeight);
        }
    }

    @Test
    public void test_layoutWithMarginHeaders() {
        final int padding = 20;
        final int itemHeight = 100;
        final int itemWidth = 720;
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, itemWidth, itemHeight, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.MARGIN_START))
                .build(mActivity);

        for (int i = 0; i < 16; i++) {
            setupLayoutPaddingPermutation(i, padding, mRecyclerView, adapter, mLayoutManager);
            checkHeaderLayout(mRecyclerView, itemHeight);
        }
    }

    @Test
    public void test_layoutWithMarginOverlayHeaders() {
        final int padding = 20;
        final int itemHeight = 100;
        final int itemWidth = 720;
        RecyclerView.Adapter adapter = new TestAdapterBuilder()
                .addLinearSection(20, itemWidth, itemHeight, TestAdapterBuilder.Header.with(
                        TestAdapterBuilder.Header.MARGIN_START | TestAdapterBuilder.Header.OVERLAY))
                .build(mActivity);

        for (int i = 0; i < 16; i++) {
            setupLayoutPaddingPermutation(i, padding, mRecyclerView, adapter, mLayoutManager);
            checkHeaderLayout(mRecyclerView, itemHeight);
        }
    }

    @Test
    public void test_overscrollAfterStartAfterEnd() throws Exception {
        final int padding = 20;
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;

        // Test Start< End<
        // Test once with padding.
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT, null)
                .build(mActivity);
        layoutPadding = new int[]{0, padding, 0, padding};
        doOverscrollTest(new int[]{0, dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);

        // Test with no padding.
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAfterStartAtEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        // Test Start< =End
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT - dy, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAfterStartBeforeEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        // Test Start< <End
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT - dy * 2, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAtStartAfterEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        // Test Start= End<
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT + dy, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, 0}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAtStartAtEnd() throws Exception {
        final int numItems = 1;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        // Test Start= =End
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, 0}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollAtStartBeforeEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        // Test Start= <End
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT - dy, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, 0}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollBeforeStartAfterEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        // Test <Start End<
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT + dy * 2, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, -dy}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollBeforeStartAtEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        // Test <Start =End
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT + dy, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, -dy}, layoutPadding, false, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_overscrollBeforeStartBeforeEnd() throws Exception {
        final int numItems = 1;
        final int dy = 100;
        RecyclerView.Adapter adapter;
        int[] layoutPadding;
        // Test <Start <End
        adapter = new TestAdapterBuilder()
                .addLinearSection(numItems, RV_WIDTH, RV_HEIGHT, null)
                .build(mActivity);
        layoutPadding = new int[]{0, 0, 0, 0};
        doOverscrollTest(new int[]{0, -dy}, layoutPadding, true, adapter, mLayoutManager,
                mRecyclerView);
    }

    @Test
    public void test_scroll() {
        Log.v("scroll1", "TODO");
    }

    @Test
    public void test_scrollWithInlineHeaders() {
        Log.v("scroll2", "TODO");
    }

    @Test
    public void test_scrollWithInlineNonStickyHeaders() {
        Log.v("scroll3", "TODO");
    }

    @Test
    public void test_scrollWithInlineOverlayHeaders() {
        Log.v("scroll4", "TODO");
    }

    @Test
    public void test_scrollWithMarginHeaders() {
        Log.v("scroll5", "TODO");
    }

    @Test
    public void test_scrollWithMarginOverlayHeaders() {
        Log.v("scroll6", "TODO");
    }
}
