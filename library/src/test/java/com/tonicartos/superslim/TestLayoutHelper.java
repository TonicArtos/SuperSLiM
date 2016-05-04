package com.tonicartos.superslim;

import com.tonicartos.superslim.internal.ManagerHelper;
import com.tonicartos.superslim.internal.ReadWriteLayoutHelper;
import com.tonicartos.superslim.internal.RecyclerHelper;
import com.tonicartos.superslim.internal.RootLayoutHelper;
import com.tonicartos.superslim.internal.StateHelper;

import org.junit.Before;
import org.junit.Test;

import android.view.View;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

public class TestLayoutHelper {

    static final int HELPER_LEFT = 1;

    static final int HELPER_TOP = 1 << 1;

    static final int HELPER_RIGHT = 1 << 2;

    static final int VIEW_LEFT = 1 << 3;

    static final int VIEW_RIGHT = 1 << 4;

    static final int VIEW_TOP = 1 << 5;

    static final int VIEW_BOTTOM = 1 << 6;

    static final int VIEW_M_LEFT = 1 << 7;

    static final int VIEW_M_RIGHT = 1 << 8;

    static final int VIEW_M_TOP = 1 << 9;

    static final int VIEW_M_BOTTOM = 1 << 10;

    static final int USED_WIDTH = 1 << 11;

    static final int USED_HEIGHT = 1 << 12;

    static final int RECYCLER_WIDTH = 1 << 13;

    static final int RECYCLER_HEIGHT = 1 << 14;

    RootLayoutHelper root;

    private ReadWriteLayoutHelper baseHelper;

    private ManagerHelper managerHelper;

    private RecyclerHelper recyclerHelper;

    private StateHelper stateHelper;

    private View view;


    @Before
    public void setup() {
        baseHelper = mock(ReadWriteLayoutHelper.class);
        stub(baseHelper.getLayoutLimit()).toReturn(RECYCLER_HEIGHT);
        stub(baseHelper.getLayoutWidth()).toReturn(RECYCLER_WIDTH);

        managerHelper = mock(ManagerHelper.class);
        recyclerHelper = mock(RecyclerHelper.class);
        stateHelper = mock(StateHelper.class);
        view = mock(View.class);
        root = new RootLayoutHelper(managerHelper, baseHelper, recyclerHelper, stateHelper);
    }

    @Test
    public void testLayout() {
        LayoutHelper helper = root.acquireSubsectionHelper(HELPER_TOP, HELPER_LEFT, HELPER_RIGHT);

        helper.layout(view, VIEW_LEFT, VIEW_TOP, VIEW_RIGHT, VIEW_BOTTOM,
                VIEW_M_LEFT, VIEW_M_TOP, VIEW_M_RIGHT, VIEW_M_BOTTOM);
        // Make sure subsection helper offset was applied.
        verify(baseHelper).layout(view,
                VIEW_LEFT + HELPER_LEFT, VIEW_TOP + HELPER_TOP, VIEW_RIGHT + HELPER_LEFT, VIEW_BOTTOM + HELPER_TOP,
                VIEW_M_LEFT, VIEW_M_TOP, VIEW_M_RIGHT, VIEW_M_BOTTOM);
        root.releaseSubsectionHelper(helper);
    }

    @Test
    public void testLayoutProperties() {
        LayoutHelper helper = root.acquireSubsectionHelper(HELPER_TOP, HELPER_LEFT, HELPER_RIGHT);
        // Check width was calculated and returned correctly.
        assertThat(helper.getLayoutWidth(), equalTo(HELPER_RIGHT - HELPER_LEFT));
        assertThat(helper.getLayoutWidth(), equalTo(HELPER_RIGHT - HELPER_LEFT));
        root.releaseSubsectionHelper(helper);
    }

    @Test
    public void testLimitExtension() {
        LayoutHelper helper = root.acquireSubsectionHelper(HELPER_TOP, HELPER_LEFT, HELPER_RIGHT);

        helper.addIgnoredHeight(50);
        assertThat(helper.getLayoutLimit(), equalTo(RECYCLER_HEIGHT + 50 - HELPER_TOP));
        root.releaseSubsectionHelper(helper);
    }

    @Test
    public void testMeasure() {
        LayoutHelper helper = root.acquireSubsectionHelper(HELPER_TOP, HELPER_LEFT, HELPER_RIGHT);

        helper.measure(view, USED_WIDTH, USED_HEIGHT);
        // Check subsection helper offset is correctly applied to used space.
        verify(baseHelper).measure(view, RECYCLER_WIDTH - helper.getLayoutWidth() + USED_WIDTH,
                HELPER_TOP + USED_HEIGHT);
        root.releaseSubsectionHelper(helper);
    }

    @Test
    public void testSubsectionCaptureAndRelease() {
        LayoutHelper helper = root.acquireSubsectionHelper(HELPER_TOP, HELPER_LEFT, HELPER_RIGHT);
        root.releaseSubsectionHelper(helper);
        LayoutHelper other = root.acquireSubsectionHelper(HELPER_TOP, HELPER_LEFT, HELPER_RIGHT);
        assertThat(helper, sameInstance(other));
        LayoutHelper another = root.acquireSubsectionHelper(HELPER_TOP, HELPER_LEFT, HELPER_RIGHT);
        assertThat(helper, not(sameInstance(another)));
        root.releaseSubsectionHelper(other);
        root.releaseSubsectionHelper(another);
    }
}
