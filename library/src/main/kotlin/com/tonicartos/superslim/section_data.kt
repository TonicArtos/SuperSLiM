package com.tonicartos.superslim

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View
import com.tonicartos.superslim.adapter.Section

/**
 * Section data
 */
abstract class SectionState(val baseConfig: Section.Config, oldState: SectionState? = null) {
    /**
     * The height of the section for this layout pass. Only valid after section is laid out, and never use outside the
     * same layout pass.
     */
    var height: Int = 0

    /**
     * Position that is the head of the displayed section content.
     */
    var headPosition: Int = 0
    /**
     * Position that is the tail of the displayed section content.
     */
    var tailPosition: Int = 0

    /**
     * Total number of children. Children does not equate to items as some subsections may be empty.
     */
    var numChildren: Int = 0
        protected set
    /**
     * Total number of items in the section, including the header and items in subsections.
     */
    private var totalItems: Int = 0
    /**
     * Map of positions in this section to subsections.
     */
    private val subsections: SparseArray<SectionState>

    /**
     * Position of this section in the adapter.
     */
    private var adapterPosition: Int = 0

    init {
        if (oldState != null) {
            height = oldState.height
            headPosition = oldState.headPosition
            tailPosition = oldState.tailPosition
            totalItems = oldState.totalItems
            numChildren = oldState.numChildren
            subsections = oldState.subsections
            adapterPosition = oldState.adapterPosition
        } else {
            subsections = SparseArray<SectionState>()
        }
    }

    internal val hasHeader: Boolean = false

    internal fun getHeader(helper: LayoutHelper): Child? =
            if (hasHeader) {
                ItemChild.wrap(helper.getView(adapterPosition), helper)
            } else {
                null
            }

    fun getChildAt(helper: LayoutHelper, position: Int): Child {
        return if (subsections[position] == null) {
            SectionChild.wrap(subsections[position], helper)
        } else {
            var priorSectionIndex = -1
            for (i in 0..subsections.size()) {
                if (subsections.keyAt(i) > position) {
                    break
                }
                priorSectionIndex = i
            }

            var itemAdapterPosition =
                    if (priorSectionIndex >= 0) {
                        val s = subsections.valueAt(priorSectionIndex)
                        val priorSectionPosition = subsections.keyAt(priorSectionIndex)
                        s.adapterPosition + s.totalItems + position - priorSectionPosition - 1
                    } else {
                        adapterPosition + position
                    }

            if (hasHeader) {
                itemAdapterPosition += 1
            }

            ItemChild.wrap(helper.getView(itemAdapterPosition), helper)
        }
    }

    final fun layout(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(left, top, right)
        HeaderLayoutManager.onLayout(subsectionHelper, this)
        subsectionHelper.release()
    }

    final internal fun layoutContent(helper: LayoutHelper, left: Int, top: Int, right: Int) {
        val subsectionHelper = helper.acquireSubsectionHelper(left, top, right)
        doLayout(subsectionHelper)
        subsectionHelper.release()
    }

    protected abstract fun doLayout(helper: LayoutHelper)

    infix operator fun contains(viewHolder: RecyclerView.ViewHolder): Boolean = viewHolder.adapterPosition >= adapterPosition && viewHolder.adapterPosition < adapterPosition + totalItems

    /*************************
     * Item management
     *************************/

    internal fun addItems(adapterPositionStart: Int, itemCount: Int) {
        for (i in 0..subsections.size()) {
            val subsection = subsections.valueAt(i)
            if (subsection.adapterPosition > adapterPositionStart) {
                subsection.adapterPosition += itemCount
            }
        }
        totalItems += itemCount
    }

    internal fun removeItems(adapterPositionStart: Int, itemCount: Int) {
        for (i in 0..subsections.size()) {
            val subsection = subsections.valueAt(i)
            if (subsection.adapterPosition > adapterPositionStart) {
                subsection.adapterPosition -= itemCount
            }
        }
        totalItems -= itemCount
    }
}

/**
 * Child abstraction hiding item and subsection differences.
 */
abstract class Child(var helper: LayoutHelper) {
    companion object {
        const val INVALID = -1
        const val ANIM_NONE = 0
        const val ANIM_APPEARING = 1
        const val ANIM_DISAPPEARING = 2
    }

    abstract fun done()

    /**
     * True if the child is being removed in this layout.
     */
    abstract val isRemoved: Boolean

    abstract val measuredWidth: Int
    abstract val measuredHeight: Int
    abstract fun measure(usedWidth: Int = 0, usedHeight: Int = 0)

    abstract val left: Int
    abstract val top: Int
    abstract val right: Int
    abstract val bottom: Int
    fun layout(left: Int = 0, top: Int = 0) = layout(left, top, 0, 0)
    abstract fun layout(left: Int, top: Int, right: Int, bottom: Int)

    abstract val width: Int
    abstract val height: Int

    /**
     * The animation state for the child in this layout pass. An appearing child is one that will start offscreen and animate
     * onscreen. A disappearing child is the opposite. A normal child does neither. Valid values are per the
     * [AnimationState] annotation.
     */
    @AnimationState var animationState: Int = ANIM_NONE

    /**
     * Adds child to the recycler view. Handles disappearing or appearing state per value set in [animationState].
     */
    fun addToRecyclerView() = addToRecyclerView(-1)

    /**
     * Adds child to the recycler view.
     */
    abstract fun addToRecyclerView(i: Int)
}

private class SectionChild(var section: SectionState, helper: LayoutHelper) : Child(helper) {

    companion object {
        val pool = arrayListOf<SectionChild>()

        fun wrap(section: SectionState, helper: LayoutHelper): SectionChild {
            return if (pool.isEmpty()) {
                SectionChild(section, helper)
            } else {
                pool[0].reInit(section, helper)
            }
        }
    }

    private fun reInit(section: SectionState, helper: LayoutHelper): SectionChild {
        this.section = section
        this.helper = helper
        return this
    }

    override fun done() {
        pool.add(this)
    }

    override val isRemoved: Boolean
        get() = false

    private var _measuredWidth: Int = 0
    override val measuredWidth: Int
        get() = _measuredWidth

    override val measuredHeight: Int
        get() = INVALID

    override fun measure(usedWidth: Int, usedHeight: Int) {
        _measuredWidth = helper.layoutWidth - usedWidth
    }

    private var _left = 0
    override val left: Int
        get() = _left

    private var _top = 0
    override val top: Int
        get() = _top

    private var _right = 0
    override val right: Int
        get() = _right

    override val bottom: Int
        get() = INVALID


    override fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        _left = left
        _top = top
        _right = right
        section.layout(helper, left, top, right)
    }

    override val width: Int
        get() = _right - _left
    override val height: Int
        get() = section.height

    override fun addToRecyclerView(i: Int) {
    }
}

private class ItemChild(var view: View, helper: LayoutHelper) : Child(helper) {
    companion object {
        val pool = arrayListOf<ItemChild>()

        fun wrap(view: View, helper: LayoutHelper): ItemChild {
            return if (pool.isEmpty()) {
                ItemChild(view, helper)
            } else {
                pool[0].reInit(view, helper)
            }
        }
    }

    private fun reInit(view: View, helper: LayoutHelper): ItemChild {
        this.view = view
        this.helper = helper
        return this
    }

    override fun done() {
        pool.add(this)
    }

    override val isRemoved: Boolean
        get() = view.rvLayoutParams.isItemRemoved

    override val measuredWidth: Int
        get() = helper.getMeasuredWidth(view)
    override val measuredHeight: Int
        get() = helper.getMeasuredHeight(view)

    override fun measure(usedWidth: Int, usedHeight: Int) {
        helper.measure(view, usedWidth, usedHeight)
    }

    override val left: Int
        get() = helper.getLeft(view)
    override val top: Int
        get() = helper.getTop(view)
    override val right: Int
        get() = helper.getRight(view)
    override val bottom: Int
        get() = helper.getBottom(view)

    override fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        val helper = this.helper
        helper.layout(view, left, top, right, bottom)
    }

    override val width: Int
        get() = helper.getMeasuredWidth(view)
    override val height: Int
        get() = helper.getMeasuredHeight(view)

    override fun addToRecyclerView(i: Int) {
        val helper = this.helper
        val view = this.view
        when (animationState) {
            ANIM_APPEARING, ANIM_NONE -> helper.addView(i, view)
            ANIM_DISAPPEARING -> helper.addDisappearingView(i, view)
        }
    }
}