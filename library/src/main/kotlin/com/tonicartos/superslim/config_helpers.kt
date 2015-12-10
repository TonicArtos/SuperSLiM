package com.tonicartos.superslim

import android.view.View
import com.tonicartos.superslim.ReadWriteLayoutHelper

/**
 * Included for completeness.
 */
private class LtrConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {}

private class RtlConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int) = base.layout(view, layoutWidth - right, top, layoutWidth - left, bottom)

    override fun getLeft(view: View): Int = layoutWidth - base.getRight(view)

    override fun getRight(view: View): Int = layoutWidth - base.getLeft(view)
}

private class StackFromEndConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int) = base.layout(view, left, layoutLimit - bottom, right, layoutLimit - top)

    override fun getTop(view: View): Int = layoutLimit - base.getBottom(view)

    override fun getBottom(view: View): Int = layoutLimit - base.getTop(view)
}

private class ReverseConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int) = base.layout(view, layoutWidth - right, layoutLimit - bottom, layoutWidth - left, layoutLimit - top)

    override fun getLeft(view: View): Int = layoutWidth - base.getRight(view)

    override fun getTop(view: View): Int = layoutLimit - base.getBottom(view)

    override fun getRight(view: View): Int = layoutWidth - base.getLeft(view)

    override fun getBottom(view: View): Int = layoutLimit - base.getTop(view)
}

/**
 * Included for completeness.
 */
private class VerticalConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {}

private class HorizontalConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int) = base.layout(view, top, left, bottom, right)

    override fun getLeft(view: View): Int = base.getTop(view)

    override fun getTop(view: View): Int = base.getLeft(view)

    override fun getRight(view: View): Int = base.getBottom(view)

    override fun getBottom(view: View): Int = base.getRight(view)

    override fun measure(view: View, usedWidth: Int, usedHeight: Int) = base.measure(view, usedHeight, usedWidth)

    override fun getMeasuredWidth(view: View): Int = base.getMeasuredHeight(view)

    override fun getMeasuredHeight(view: View): Int = base.getMeasuredWidth(view)
}

