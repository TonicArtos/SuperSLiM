package com.tonicartos.superslim.internal

import android.view.View
import com.tonicartos.superslim.ReadWriteLayoutHelper

//internal class LtrConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {}

internal class RtlConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int)
            = base.layout(view, layoutWidth - right, top, layoutWidth - left, bottom, marginRight, marginTop, marginLeft, marginBottom)

    override fun getLeft(child: View): Int = layoutWidth - base.getRight(child)
    override fun getRight(child: View): Int = layoutWidth - base.getLeft(child)
}

internal class StackFromEndConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int)
            = base.layout(view, left, layoutLimit - bottom, right, layoutLimit - top, marginLeft, marginTop, marginRight, marginBottom)

    override fun getTop(child: View): Int = layoutLimit - base.getBottom(child)
    override fun getBottom(child: View): Int = layoutLimit - base.getTop(child)
}

internal class ReverseLayoutConfigHelper(val base: ReadWriteLayoutHelper) : ReadWriteLayoutHelper by base {
    override fun layout(view: View, left: Int, top: Int, right: Int, bottom: Int, marginLeft: Int, marginTop: Int, marginRight: Int, marginBottom: Int)
            = base.layout(view, layoutWidth - right, layoutLimit - bottom, layoutWidth - left, layoutLimit - top, marginRight, marginTop, marginLeft, marginBottom)

    override fun getLeft(child: View): Int = layoutWidth - base.getRight(child)
    override fun getTop(child: View): Int = layoutLimit - base.getBottom(child)
    override fun getRight(child: View): Int = layoutWidth - base.getLeft(child)
    override fun getBottom(child: View): Int = layoutLimit - base.getTop(child)
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
}

