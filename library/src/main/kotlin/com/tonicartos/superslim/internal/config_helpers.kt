package com.tonicartos.superslim.internal

import android.view.View
import com.tonicartos.superslim.SectionConfig

//internal class LtrConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {}

internal class RtlConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int)
            = base.layout(view, layoutWidth - right, top, layoutWidth - left, bottom, marginRight, marginTop, marginLeft, marginBottom)

    override fun getLeft(child: View): Int = layoutWidth - base.getRight(child)
    override fun getRight(child: View): Int = layoutWidth - base.getLeft(child)

    override val basePaddingLeft: Int get() = base.basePaddingRight
    override val basePaddingRight: Int get() = base.basePaddingLeft

    override fun getTransformedPaddingLeft(sectionConfig: SectionConfig) = base.getTransformedPaddingRight(sectionConfig)
    override fun getTransformedPaddingRight(sectionConfig: SectionConfig) = base.getTransformedPaddingLeft(sectionConfig)

    override fun offsetChildrenHorizontal(dx: Int) = base.offsetChildrenHorizontal(-dx)

    override fun toString(): String = "RtlConfigHelper(base = $base)"
}

internal class StackFromEndConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int)
            = base.layout(view, left, layoutLimit - bottom, right, layoutLimit - top, marginLeft, marginTop, marginRight, marginBottom)

    override fun getTop(child: View): Int = layoutLimit - base.getBottom(child)
    override fun getBottom(child: View): Int = layoutLimit - base.getTop(child)

    override val basePaddingTop: Int get() = base.basePaddingBottom
    override val basePaddingBottom: Int get() = base.basePaddingTop

    override fun getTransformedPaddingTop(sectionConfig: SectionConfig) = base.getTransformedPaddingBottom(sectionConfig)
    override fun getTransformedPaddingBottom(sectionConfig: SectionConfig) = base.getTransformedPaddingTop(sectionConfig)

    override fun offsetChildrenVertical(dy: Int) = base.offsetChildrenVertical(-dy)

    override fun toString(): String = "StackFromEndConfigHelper(base = $base)"
}

internal class ReverseLayoutConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int)
            = base.layout(view, layoutWidth - right, layoutLimit - bottom, layoutWidth - left, layoutLimit - top, marginRight, marginTop, marginLeft, marginBottom)

    override fun getLeft(child: View): Int = layoutWidth - base.getRight(child)
    override fun getTop(child: View): Int = layoutLimit - base.getBottom(child)
    override fun getRight(child: View): Int = layoutWidth - base.getLeft(child)
    override fun getBottom(child: View): Int = layoutLimit - base.getTop(child)

    override val basePaddingLeft: Int get() = base.basePaddingRight
    override val basePaddingRight: Int get() = base.basePaddingLeft
    override val basePaddingTop: Int get() = base.basePaddingBottom
    override val basePaddingBottom: Int get() = base.basePaddingTop

    override fun getTransformedPaddingLeft(sectionConfig: SectionConfig) = base.getTransformedPaddingRight(sectionConfig)
    override fun getTransformedPaddingRight(sectionConfig: SectionConfig) = base.getTransformedPaddingLeft(sectionConfig)
    override fun getTransformedPaddingTop(sectionConfig: SectionConfig) = base.getTransformedPaddingBottom(sectionConfig)
    override fun getTransformedPaddingBottom(sectionConfig: SectionConfig) = base.getTransformedPaddingTop(sectionConfig)

    override fun offsetChildrenHorizontal(dx: Int) = base.offsetChildrenHorizontal(-dx)
    override fun offsetChildrenVertical(dy: Int) = base.offsetChildrenVertical(-dy)

    override fun toString(): String = "ReverseLayoutConfigHelper(base = $base)"
}

//internal class VerticalConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {}

internal class HorizontalConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int)
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
    override fun getTransformedPaddingBottom(sectionConfig: SectionConfig) = base.getTransformedPaddingRight(sectionConfig)
    override fun getTransformedPaddingRight(sectionConfig: SectionConfig) = base.getTransformedPaddingBottom(sectionConfig)

    override fun offsetChildrenHorizontal(dx: Int) = base.offsetChildrenVertical(dx)
    override fun offsetChildrenVertical(dy: Int) = base.offsetChildrenHorizontal(dy)

    override fun toString(): String = "HorizontalConfigHelper(base = $base)"
}

