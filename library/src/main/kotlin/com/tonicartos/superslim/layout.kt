package com.tonicartos.superslim

import android.support.v7.widget.RecyclerView
import android.view.View

interface ReadLayoutHelper {
    fun getLeft(child: View): Int
    fun getTop(child: View): Int
    fun getRight(child: View): Int
    fun getBottom(child: View): Int
    fun getMeasuredWidth(child: View): Int
    fun getMeasuredHeight(child: View): Int

    /**
     * Width of the layout area.
     */
    val layoutWidth: Int

    /**
     * Y limit that constrains the layout. This is used to know when to stop laying out items, and is nominally the
     * maximum height of the visible layout area.
     *
     * **Warning**: This value can change, and as such, should not be stored.
     */
    val layoutLimit: Int
}

interface WriteLayoutHelper {
    fun measure(view: View, usedWidth: Int = 0, usedHeight: Int = 0)
    fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int = 0, marginTop: Int = 0, marginRight: Int = 0, marginBottom: Int = 0)
}

interface ReadWriteLayoutHelper : ReadLayoutHelper, WriteLayoutHelper

interface RecyclerHelper {
    fun getView(position: Int): View
    val scrap: List<RecyclerView.ViewHolder>
}

interface StateHelper {
    val isPreLayout: Boolean
    val willRunPredictiveAnimations: Boolean
    val itemCount: Int
    val hasTargetScrollPosition: Boolean
    val targetScrollPosition: Int
}

interface ManagerHelper {
    fun addView(child: View)
    fun addView(child: View, index: Int)
    fun addDisappearingView(child: View)
    fun addDisappearingView(child: View, index: Int)

    val supportsPredictiveItemAnimations: Boolean
}

/**
 * Interface for querying and modifying the assigned section layout.
 */
interface LayoutHelper : ManagerHelper, StateHelper, RecyclerHelper, ReadWriteLayoutHelper {
    /**
     * Create a new layout helper for a subsection of this helper's section.
     */
    fun acquireSubsectionHelper(left: Int, top: Int, right: Int): LayoutHelper

    fun release()
    fun addIgnoredHeight(ignoredHeight: Int)
}
