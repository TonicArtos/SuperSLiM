package com.tonicartos.superslim;

import com.tonicartos.superslim.internal.HorizontalConfigHelper;
import com.tonicartos.superslim.internal.ReadWriteLayoutHelper;
import com.tonicartos.superslim.internal.ReverseLayoutConfigHelper;
import com.tonicartos.superslim.internal.RtlConfigHelper;
import com.tonicartos.superslim.internal.StackFromEndConfigHelper;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import android.view.View;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

public class TestConfigHelpers {

    static final int LEFT = 1;

    static final int TOP = 1 << 1;

    static final int RIGHT = 1 << 2;

    static final int BOTTOM = 1 << 3;

    static final int MARGIN_LEFT = 1 << 4;

    static final int MARGIN_TOP = 1 << 5;

    static final int MARGIN_RIGHT = 1 << 6;

    static final int MARGIN_BOTTOM = 1 << 7;

    static final int WIDTH = 1 << 8;

    static final int HEIGHT = 1 << 9;

    static final int LAYOUT_WIDTH = 1 << 10;

    static final int LAYOUT_HEIGHT = 1 << 11;

    static final int USED_WIDTH = 1 << 12;

    static final int USED_HEIGHT = 1 << 13;

    private View view;

    private mockReadWriteHelper baseHelper;

    @Before
    public void setup() {
        view = mock(View.class);
        baseHelper = new mockReadWriteHelper();
    }

    @Test
    public void testHorizontalConfig() {
        HorizontalConfigHelper helper = new HorizontalConfigHelper(baseHelper);
        helper.layout(view, LEFT, TOP, RIGHT, BOTTOM, MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM);
        assertThat(baseHelper.left, equalTo(TOP));
        assertThat(baseHelper.top, equalTo(LEFT));
        assertThat(baseHelper.right, equalTo(BOTTOM));
        assertThat(baseHelper.bottom, equalTo(RIGHT));
        assertThat(baseHelper.marginLeft, equalTo(MARGIN_LEFT));
        assertThat(baseHelper.marginTop, equalTo(MARGIN_TOP));
        assertThat(baseHelper.marginRight, equalTo(MARGIN_RIGHT));
        assertThat(baseHelper.marginBottom, equalTo(MARGIN_BOTTOM));

        assertThat(helper.getLeft(view), equalTo(TOP));
        assertThat(helper.getTop(view), equalTo(LEFT));
        assertThat(helper.getRight(view), equalTo(BOTTOM));
        assertThat(helper.getBottom(view), equalTo(RIGHT));

        assertThat(helper.getMeasuredWidth(view), equalTo(HEIGHT));
        assertThat(helper.getMeasuredHeight(view), equalTo(WIDTH));

        helper.measure(view, USED_WIDTH, USED_HEIGHT);
        assertThat(baseHelper.usedWidth, equalTo(USED_HEIGHT));
        assertThat(baseHelper.usedHeight, equalTo(USED_WIDTH));
    }

    @Test
    public void testReverseLayoutConfig() {
        mockReadWriteHelper mock = new mockReadWriteHelper();
        View view = mock(View.class);
        ReverseLayoutConfigHelper helper = new ReverseLayoutConfigHelper(mock);
        helper.layout(view, LEFT, TOP, RIGHT, BOTTOM, MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM);
        assertThat(mock.left, equalTo(LAYOUT_WIDTH - RIGHT));
        assertThat(mock.top, equalTo(LAYOUT_HEIGHT - BOTTOM));
        assertThat(mock.right, equalTo(LAYOUT_WIDTH - LEFT));
        assertThat(mock.bottom, equalTo(LAYOUT_HEIGHT - TOP));
        assertThat(mock.marginLeft, equalTo(MARGIN_RIGHT));
        assertThat(mock.marginTop, equalTo(MARGIN_TOP));
        assertThat(mock.marginRight, equalTo(MARGIN_LEFT));
        assertThat(mock.marginBottom, equalTo(MARGIN_BOTTOM));

        assertThat(helper.getLeft(view), equalTo(LAYOUT_WIDTH - RIGHT));
        assertThat(helper.getTop(view), equalTo(LAYOUT_HEIGHT - BOTTOM));
        assertThat(helper.getRight(view), equalTo(LAYOUT_WIDTH - LEFT));
        assertThat(helper.getBottom(view), equalTo(LAYOUT_HEIGHT - TOP));

        assertThat(helper.getMeasuredWidth(view), equalTo(WIDTH));
        assertThat(helper.getMeasuredHeight(view), equalTo(HEIGHT));

        helper.measure(view, USED_WIDTH, USED_HEIGHT);
        assertThat(mock.usedWidth, equalTo(USED_WIDTH));
        assertThat(mock.usedHeight, equalTo(USED_HEIGHT));
    }

    @Test
    public void testRtlConfig() {
        RtlConfigHelper helper = new RtlConfigHelper(baseHelper);
        helper.layout(view, LEFT, TOP, RIGHT, BOTTOM, MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM);
        assertThat(baseHelper.left, equalTo(LAYOUT_WIDTH - RIGHT));
        assertThat(baseHelper.top, equalTo(TOP));
        assertThat(baseHelper.right, equalTo(LAYOUT_WIDTH - LEFT));
        assertThat(baseHelper.bottom, equalTo(BOTTOM));
        assertThat(baseHelper.marginLeft, equalTo(MARGIN_RIGHT));
        assertThat(baseHelper.marginTop, equalTo(MARGIN_TOP));
        assertThat(baseHelper.marginRight, equalTo(MARGIN_LEFT));
        assertThat(baseHelper.marginBottom, equalTo(MARGIN_BOTTOM));

        assertThat(helper.getLeft(view), equalTo(LAYOUT_WIDTH - RIGHT));
        assertThat(helper.getTop(view), equalTo(TOP));
        assertThat(helper.getRight(view), equalTo(LAYOUT_WIDTH - LEFT));
        assertThat(helper.getBottom(view), equalTo(BOTTOM));

        assertThat(helper.getMeasuredWidth(view), equalTo(WIDTH));
        assertThat(helper.getMeasuredHeight(view), equalTo(HEIGHT));

        helper.measure(view, USED_WIDTH, USED_HEIGHT);
        assertThat(baseHelper.usedWidth, equalTo(USED_WIDTH));
        assertThat(baseHelper.usedHeight, equalTo(USED_HEIGHT));
    }

    @Test
    public void testStackFromEndConfig() {
        StackFromEndConfigHelper helper = new StackFromEndConfigHelper(baseHelper);
        helper.layout(view, LEFT, TOP, RIGHT, BOTTOM, MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM);
        assertThat(baseHelper.left, equalTo(LEFT));
        assertThat(baseHelper.top, equalTo(LAYOUT_HEIGHT - BOTTOM));
        assertThat(baseHelper.right, equalTo(RIGHT));
        assertThat(baseHelper.bottom, equalTo(LAYOUT_HEIGHT - TOP));
        assertThat(baseHelper.marginLeft, equalTo(MARGIN_LEFT));
        assertThat(baseHelper.marginTop, equalTo(MARGIN_TOP));
        assertThat(baseHelper.marginRight, equalTo(MARGIN_RIGHT));
        assertThat(baseHelper.marginBottom, equalTo(MARGIN_BOTTOM));

        assertThat(helper.getLeft(view), equalTo(LEFT));
        assertThat(helper.getTop(view), equalTo(LAYOUT_HEIGHT - BOTTOM));
        assertThat(helper.getRight(view), equalTo(RIGHT));
        assertThat(helper.getBottom(view), equalTo(LAYOUT_HEIGHT - TOP));

        assertThat(helper.getMeasuredWidth(view), equalTo(WIDTH));
        assertThat(helper.getMeasuredHeight(view), equalTo(HEIGHT));

        helper.measure(view, USED_WIDTH, USED_HEIGHT);
        assertThat(baseHelper.usedWidth, equalTo(USED_WIDTH));
        assertThat(baseHelper.usedHeight, equalTo(USED_HEIGHT));
    }

    class mockReadWriteHelper implements ReadWriteLayoutHelper {

        public int left;

        public int top;

        public int right;

        public int bottom;

        public int marginLeft;

        public int marginTop;

        public int marginRight;

        public int marginBottom;

        public int usedWidth;

        public int usedHeight;

        @Override
        public int getLeft(@NotNull View child) {
            return LEFT;
        }

        @Override
        public int getTop(@NotNull View child) {
            return TOP;
        }

        @Override
        public int getRight(@NotNull View child) {
            return RIGHT;
        }

        @Override
        public int getBottom(@NotNull View child) {
            return BOTTOM;
        }

        @Override
        public int getMeasuredWidth(@NotNull View child) {
            return WIDTH;
        }

        @Override
        public int getMeasuredHeight(@NotNull View child) {
            return HEIGHT;
        }

        @Override
        public int getLayoutWidth() {
            return LAYOUT_WIDTH;
        }

        @Override
        public int getLayoutLimit() {
            return LAYOUT_HEIGHT;
        }

        @Override
        public void measure(@NotNull View view, int usedWidth, int usedHeight) {
            this.usedWidth = usedWidth;
            this.usedHeight = usedHeight;
        }

        @Override
        public void layout(@NotNull View view, int left, int top, int right, int bottom, int marginLeft, int marginTop,
                int marginRight, int marginBottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;

            this.marginLeft = marginLeft;
            this.marginTop = marginTop;
            this.marginRight = marginRight;
            this.marginBottom = marginBottom;
        }
    }
}
