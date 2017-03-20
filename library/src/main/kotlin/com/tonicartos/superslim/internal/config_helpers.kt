package com.tonicartos.superslim.internal

import android.support.v7.widget.RecyclerView
import android.view.View
import com.tonicartos.superslim.SectionConfig

internal interface ConfigHelper : ReadWriteLayoutHelper {
    fun scrollBy(d: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int
}

//internal class LtrConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {}

internal class RtlConfigHelper(val base: ConfigHelper) : ConfigHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int,
                        marginRight: Int, marginBottom: Int)
            = base.layout(view, layoutWidth - right, top, layoutWidth - left, bottom, marginRight, marginTop,
                          marginLeft, marginBottom)

    override fun getLeft(child: View): Int = layoutWidth - base.getRight(child)
    override fun getRight(child: View): Int = layoutWidth - base.getLeft(child)

    override val basePaddingLeft: Int get() = base.basePaddingRight
    override val basePaddingRight: Int get() = base.basePaddingLeft

    override fun getTransformedPaddingLeft(sectionConfig: SectionConfig) = base.getTransformedPaddingRight(
            sectionConfig)

    override fun getTransformedPaddingRight(sectionConfig: SectionConfig) = base.getTransformedPaddingLeft(
            sectionConfig)

    override fun offsetChildrenHorizontal(dx: Int) = base.offsetChildrenHorizontal(-dx)

    override fun scrollBy(d: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State)
            = -base.scrollBy(-d, recycler, state)

    override fun toString(): String = "RtlConfigHelper(base = $base)"
}

internal class StackFromEndConfigHelper(val base: ConfigHelper) : ConfigHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int,
                        marginRight: Int, marginBottom: Int)
            = base.layout(view, left, layoutLimit - bottom, right, layoutLimit - top, marginLeft, marginTop,
                          marginRight, marginBottom)

    override fun getTop(child: View): Int = layoutLimit - base.getBottom(child)
    override fun getBottom(child: View): Int = layoutLimit - base.getTop(child)

    override val basePaddingTop: Int get() = base.basePaddingBottom
    override val basePaddingBottom: Int get() = base.basePaddingTop

    override fun getTransformedPaddingTop(sectionConfig: SectionConfig) = base.getTransformedPaddingBottom(
            sectionConfig)

    override fun getTransformedPaddingBottom(sectionConfig: SectionConfig) = base.getTransformedPaddingTop(
            sectionConfig)

    override fun offsetChildrenVertical(dy: Int) = base.offsetChildrenVertical(-dy)
    override fun offsetVertical(view: View, dy: Int) = base.offsetVertical(view, -dy)

    override fun scrollBy(d: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State)
            = -base.scrollBy(-d, recycler, state)

    override fun toString(): String = "StackFromEndConfigHelper(base = $base)"
}

internal class ReverseLayoutConfigHelper(val base: ConfigHelper) : ConfigHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int,
                        marginRight: Int, marginBottom: Int)
            = base.layout(view, layoutWidth - right, layoutLimit - bottom, layoutWidth - left, layoutLimit - top,
                          marginRight, marginTop, marginLeft, marginBottom)

    override fun getLeft(child: View): Int = layoutWidth - base.getRight(child)
    override fun getTop(child: View): Int = layoutLimit - base.getBottom(child)
    override fun getRight(child: View): Int = layoutWidth - base.getLeft(child)
    override fun getBottom(child: View): Int = layoutLimit - base.getTop(child)

    override val basePaddingLeft: Int get() = base.basePaddingRight
    override val basePaddingRight: Int get() = base.basePaddingLeft
    override val basePaddingTop: Int get() = base.basePaddingBottom
    override val basePaddingBottom: Int get() = base.basePaddingTop

    override fun getTransformedPaddingLeft(sectionConfig: SectionConfig) = base.getTransformedPaddingRight(
            sectionConfig)

    override fun getTransformedPaddingRight(sectionConfig: SectionConfig) = base.getTransformedPaddingLeft(
            sectionConfig)

    override fun getTransformedPaddingTop(sectionConfig: SectionConfig) = base.getTransformedPaddingBottom(
            sectionConfig)

    override fun getTransformedPaddingBottom(sectionConfig: SectionConfig) = base.getTransformedPaddingTop(
            sectionConfig)

    override fun offsetChildrenHorizontal(dx: Int) = base.offsetChildrenHorizontal(-dx)
    override fun offsetChildrenVertical(dy: Int) = base.offsetChildrenVertical(-dy)
    override fun offsetHorizontal(view: View, dx: Int) = base.offsetHorizontal(view, -dx)
    override fun offsetVertical(view: View, dy: Int) = base.offsetVertical(view, -dy)

    override fun scrollBy(d: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State)
            = -base.scrollBy(-d, recycler, state)

    override fun toString(): String = "ReverseLayoutConfigHelper(base = $base)"
}

//internal class VerticalConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {}

internal class HorizontalConfigHelper(val base: ConfigHelper) : ConfigHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int,
                        marginRight: Int, marginBottom: Int)
            = base.layout(view, top, left, bottom, right, marginLeft, marginTop, marginRight, marginBottom)

    override fun getLeft(child: View): Int = base.getTop(child)
    override fun getTop(child: View): Int = base.getLeft(child)
    override fun getRight(child: View): Int = base.getBottom(child)
    override fun getBottom(child: View): Int = base.getRight(child)
    override fun measure(view: View, usedWidth: Int, usedHeight: Int) = base.measure(view, usedHeight, usedWidth)
    override fun getMeasuredWidth(child: View): Int = base.getMeasuredHeight(child)
    override fun getMeasuredHeight(child: View): Int = base.getMeasuredWidth(child)

    override val basePaddingTop: Int get() = base.basePaddingLeft
    override val basePaddingLeft: Int get() = base.basePaddingTop
    override val basePaddingBottom: Int get() = base.basePaddingRight
    override val basePaddingRight: Int get() = base.basePaddingBottom

    override fun getTransformedPaddingTop(sectionConfig: SectionConfig) = base.getTransformedPaddingLeft(sectionConfig)
    override fun getTransformedPaddingLeft(sectionConfig: SectionConfig) = base.getTransformedPaddingTop(sectionConfig)
    override fun getTransformedPaddingBottom(sectionConfig: SectionConfig) = base.getTransformedPaddingRight(
            sectionConfig)

    override fun getTransformedPaddingRight(sectionConfig: SectionConfig) = base.getTransformedPaddingBottom(
            sectionConfig)

    override fun offsetChildrenHorizontal(dx: Int) = base.offsetChildrenVertical(dx)
    override fun offsetChildrenVertical(dy: Int) = base.offsetChildrenHorizontal(dy)
    override fun offsetHorizontal(view: View, dx: Int) = base.offsetVertical(view, dx)
    override fun offsetVertical(view: View, dy: Int) = base.offsetHorizontal(view, dy)

    override val layoutLimit get() = base.layoutWidth
    override val layoutWidth get() = base.layoutLimit

    override fun toString(): String = "HorizontalConfigHelper(base = $base)"
}

