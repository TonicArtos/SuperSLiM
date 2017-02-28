package com.tonicartos.superslim


class HeaderTests {
    //TODO: mock helper
    //TODO: mock section state
    //TODO: mock layout state
}

//import android.support.v7.widget.RecyclerView
//import android.view.View
//import android.view.ViewGroup.LayoutParams
//import com.nhaarman.mockito_kotlin.*
//import com.tonicartos.superslim.internal.*
//import com.tonicartos.superslim.internal.layout.HeaderLayoutManager
//import com.tonicartos.superslim.internal.layout.LinearSectionConfig
//import org.hamcrest.MatcherAssert.assertThat
//import org.hamcrest.Matchers.equalTo
//import org.hamcrest.Matchers.sameInstance
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Matchers.anyInt
//import org.mockito.invocation.InvocationOnMock
//import java.util.*
//
///**
// *
// */
//class LayoutTests {
//    private companion object {
//        const val WIDTH = 601
//        const val HEIGHT = 1000
//        const val MEASURED_HEADER_HEIGHT = 100
//        const val MEASURED_HEADER_WIDTH = 600
//    }
//
//    private lateinit var helper: LayoutHelper
//    private lateinit var section: NoLayoutSection
//    private val manager: ManagerHelper = mock()
//    private val rwHelper: ReadWriteLayoutHelper = mock()
//    private val recyclerHelper: RecyclerHelper = mock(withSettings().verboseLogging())
//    private val stateHelper: StateHelper = mock()
//    private var testingPreLayout = false
//    private val view: Array<View> = Array(10) {
//        mock<View>().apply {
//            doReturn(RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)).whenever(this).layoutParams
//        }
//    }
//    private val viewLayoutParams: HashMap<Int, ViewLayout> = HashMap()
//    private val viewMeasurementParams: HashMap<Int, ViewMeasurement> = HashMap()
//
//    private data class ViewLayout(val left: Int, val top: Int, val right: Int, val bottom: Int)
//    private data class ViewMeasurement(val width: Int, val height: Int)
//
//    private fun <T> getForView(v: View, map: Map<Int, T>): T {
//        view.forEachIndexed { i, it -> if (it == v) return map[i] as T }
//        throw RuntimeException("Error: Unknown view to test.")
//    }
//
//    private fun <T> storeForView(v: View, map: HashMap<Int, T>, value: T) {
//        val i = view.indexOf(v)
//        if (i == -1) throw RuntimeException("Error: Unknown view to test.")
//        map[i] = value
//    }
//
//    private fun InvocationOnMock.intArg(i: Int): Int = arguments[i] as Int
//    private fun InvocationOnMock.arg(i: Int): Any = arguments[i]
//    private val InvocationOnMock.view: View
//        get() = arguments[0] as View
//
//    class NoLayoutSection(config: SectionConfig, oldState: SectionState?) : SectionState(config, oldState) {
//        var laidOut = false
//        var filledTop = false
//        var filledBottom = false
//        var trimmedTop = false
//        var trimmedBottom = false
//
//        override fun doLayout(helper: LayoutHelper) {
//            laidOut = true
//        }
//
//        override fun doFillTop(dy: Int, helper: LayoutHelper): Int {
//            filledTop = true
//            return 0
//        }
//
//        override fun doFillBottom(dy: Int, helper: LayoutHelper): Int {
//            filledBottom = true
//            return 0
//        }
//
//        override fun doTrimTop(helper: LayoutHelper) {
//            trimmedTop = true
//        }
//
//        override fun doTrimBottom(helper: LayoutHelper) {
//            trimmedBottom = true
//        }
//    }
//
//    fun initManagerHelper() {
//        whenever(manager.supportsPredictiveItemAnimations).then { true }
//    }
//
//    fun initReadWriteHelper() {
//        whenever(rwHelper.layoutWidth).then { WIDTH }
//        whenever(rwHelper.layoutLimit).then { HEIGHT }
//
//        whenever(rwHelper.basePaddingBottom).then { 0 }
//        whenever(rwHelper.basePaddingLeft).then { 0 }
//        whenever(rwHelper.basePaddingRight).then { 0 }
//        whenever(rwHelper.basePaddingTop).then { 0 }
//
//        whenever(rwHelper.getLeft(any<View>())).then { getForView(it.view, viewLayoutParams).left }
//        whenever(rwHelper.getTop(any<View>())).then { getForView(it.view, viewLayoutParams).top }
//        whenever(rwHelper.getRight(any<View>())).then { getForView(it.view, viewLayoutParams).right }
//        whenever(rwHelper.getBottom(any<View>())).then { getForView(it.view, viewLayoutParams).bottom }
//
//        // Store view layout values set by SLM.
//        whenever(rwHelper.layout(any<View>(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).then {
//            storeForView(it.view, viewLayoutParams, ViewLayout(it.intArg(1), it.intArg(2), it.intArg(3), it.intArg(4)))
//        }
//
//        whenever(rwHelper.getMeasuredHeight(any<View>())).then { MEASURED_HEADER_HEIGHT }
//        whenever(rwHelper.getMeasuredWidth(any<View>())).then { MEASURED_HEADER_WIDTH }
//    }
//
//    fun initRecyclerHelper() {
//        whenever(recyclerHelper.getView(anyInt())).then { view[it.intArg(0)] }
//    }
//
//    fun initStateHelper() {
//        whenever(stateHelper.willRunPredictiveAnimations).then { true }
//        whenever(stateHelper.isPreLayout).then { testingPreLayout }
//        whenever(stateHelper.itemCount).then { 10 }
//    }
//
//    @Before
//    fun init() {
//        initManagerHelper()
//        initReadWriteHelper()
//        initRecyclerHelper()
//        initStateHelper()
//        helper = LayoutHelper(RootLayoutHelper(manager, rwHelper, recyclerHelper, stateHelper), 0, 0, WIDTH)
//        section = NoLayoutSection(LinearSectionConfig(headerStyle = SectionConfig.HEADER_STICKY), null)
//        section.addHeader()
//        section.addItems(0, 10)
//    }
//
//    /**
//     * Test layout of header. Check header was laid out aligned to the top edge of the area, that outputs match expected
//     * values, and that call to layout content was made.
//     */
//    @Test
//    fun layout() {
////        HeaderLayoutManager.onLayout(helper, section, )
//
//        assertThat("After: Section was laid out.", section.laidOut, equalTo(true))
////        assertThat("After: Section was laid out.", section.layout.overdraw, equalTo(true))
//    }
//
//    @Test
//    fun preLayout() {
//        testingPreLayout = true
////        HeaderLayoutManager.onLayout(helper, section, )
//    }
//
//    @Test
//    fun preLayout_disappearingHeader() {
//        testingPreLayout = true
////        HeaderLayoutManager.onLayout(helper, section, )
//    }
//
//    @Test
//    fun postLayout() {
//
//    }
//
//    @Test
//    fun postLayout_appearingItems() {
//
//    }
//}
//
//class ScrollTests {
//    fun scrollToBeginning() {
//    }
//
//    fun scrollToEnd() {
//    }
//}
