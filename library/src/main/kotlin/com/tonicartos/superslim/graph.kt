package com.tonicartos.superslim

import com.tonicartos.superslim.adapter.FooterStyle
import com.tonicartos.superslim.adapter.HeaderStyle
import com.tonicartos.superslim.internal.SectionState

inline fun <T : Child, R> T.use(block: (T) -> R): R {
    var done = false
    try {
        return block(this)
    } catch (e: Exception) {
        done = true
        this.done()
        throw e
    } finally {
        if (!done) {
            this.done()
        }
    }
}

// TODO: Make TrimChild version which wraps a section or attached view.
interface Child {
    companion object {
        const val INVALID = 0
        const val ANIM_NONE = 0
        const val ANIM_APPEARING = 1
        const val ANIM_DISAPPEARING = 2
    }

    fun done()

    val numViews get() = 1

    /**
     * True if the child is being removed in this layout.
     */
    val isRemoved: Boolean

    val measuredWidth: Int
    val measuredHeight: Int
    /**
     * Measure child. This should be done after [addToRecyclerView] as some views like ViewPager expect to be attached
     * before measurement.
     */
    fun measure(usedWidth: Int = 0, usedHeight: Int = 0)

    val left: Int
    val top: Int
    val right: Int
    val bottom: Int
    /**
     * Layout the child. A backing view will have the dimensions specified. A subsection will have bounds defined by
     * left, top, and right, however will ignore bottom and may fill any remaining space to the bottom of the viewable
     * area.
     */
    fun layout(left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int = 0)

    /**
     * Fill distance dy at top of the child. The child will attempt to extend into this space; only if it is a section.
     *
     * @param dy Distance to fill. Value will be -ve.
     * @param left Left edge of area to fill.
     * @param top Top edge of area to fill.
     * @param right Right edge of area to fill.
     * @param bottom Bottom edge of area to fill.
     *
     * @return How much of dy filled.
     */
    fun fillTop(dy: Int, left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int = 0): Int

    fun trimTop(scrolled: Int, top: Int, helper: LayoutHelper, numViewsBefore: Int = 0): Int

    /**
     * Fill distance dy at bottom of the child. The child will attempt to extend into this space; only if it is a section.
     *
     * @param dy Distance to fill. Value will be +ve.
     * @param left Left edge of area to fill.
     * @param top Top edge of area to fill.
     * @param right Right edge of area to fill.
     * @param bottom Bottom edge of area to fill.
     *
     * @return How much of dy filled.
     */
    fun fillBottom(dy: Int, left: Int, top: Int, right: Int, bottom: Int, numViewsBefore: Int = 0): Int

    fun trimBottom(scrolled: Int, top: Int, helper: LayoutHelper, numViewsBefore: Int = 0): Int


    val width: Int
    val height: Int
    val disappearedHeight: Int

    /**
     * Adds child to the recycler view.
     */
    fun addToRecyclerView() = addToRecyclerView(-1)

    /**
     * Adds child to the recycler view.
     */
    fun addToRecyclerView(i: Int)
}

/**
 * Configuration of a section.
 */
abstract class SectionConfig(gutterStart: Int = SectionConfig.DEFAULT_GUTTER,
                             gutterEnd: Int = SectionConfig.DEFAULT_GUTTER,
                             @HeaderStyle @JvmField var headerStyle: Int = SectionConfig.DEFAULT_HEADER_STYLE,
                             @FooterStyle @JvmField var footerStyle: Int = SectionConfig.DEFAULT_FOOTER_STYLE,
                             paddingStart: Int = 0, paddingTop: Int = 0, paddingEnd: Int = 0, paddingBottom: Int = 0) {
    var gutterStart = 0
        get() = field
        set(value) {
            field = if (value < 0) GUTTER_AUTO else value
        }
    var gutterEnd = 0
        get() = field
        set(value) {
            field = if (value < 0) GUTTER_AUTO else value
        }

    var paddingLeft = 0
    var paddingTop = 0
    var paddingRight = 0
    var paddingBottom = 0

    init {
        this.gutterStart = gutterStart
        this.gutterEnd = gutterEnd
        this.paddingLeft = paddingStart
        this.paddingTop = paddingTop
        this.paddingRight = paddingEnd
        this.paddingBottom = paddingBottom
    }

    // Remap names since internally left and right are used since section coordinates are LTR, TTB. The start and
    // end intention will be applied correctly (from left and right) through the config transformations.
    internal var gutterLeft: Int
        get() = gutterStart
        set(value) {
            gutterStart = value
        }
    internal var gutterRight: Int
        get() = gutterEnd
        set(value) {
            gutterEnd = value
        }

    internal fun makeSection(oldState: SectionState? = null) = onMakeSection(oldState)
    abstract protected fun onMakeSection(oldState: SectionState?): SectionState

    /**
     * Copy the configuration. Section configs are always copied when they are passed to the layout manager.
     */
    fun copy(): SectionConfig {
        return onCopy()
    }

    abstract protected fun onCopy(): SectionConfig

    companion object {
        /**
         * Header is positioned at the head of the section content. Content starts below the header. Sticky headers
         * stick to the top of the layout area until the entire area has scrolled off the screen. Use HEADER_INLINE for
         * a header style which is otherwise the same without the sticky property.
         */
        const val HEADER_STICKY = 1

        /**
         * Header is positioned at the head of the section content. Content starts below the header, but the header
         * never becomes sticky. Linear headers can not float and ignores that flag if set.
         */
        const val HEADER_INLINE = 1 shl 1

        /**
         * Header is placed inside the gutter at the start edge of the section. This is the left for LTR locales.
         * Gutter headers are always sticky.
         */
        const val HEADER_START = 1 shl 2

        /**
         * Header is placed inside the gutter at the end edge of the section. This is the right for LTR locales.
         * Gutter headers are always sticky.
         */
        const val HEADER_END = 1 shl 3

        /**
         * Footer is positioned at the head of the section content. Content starts below the footer. Sticky footers
         * stick to the top of the layout area until the entire area has scrolled off the screen. Use FOOTER_INLINE for
         * a footer style which is otherwise the same without the sticky property.
         */
        const val FOOTER_STICKY = 1

        /**
         * Footer is positioned at the head of the section content. Content starts below the footer, but the footer
         * never becomes sticky. Linear footers can not float and ignores that flag if set.
         */
        const val FOOTER_INLINE = 1 shl 1

        /**
         * Footer is placed inside the gutter at the start edge of the section. This is the left for LTR locales.
         * Gutter footers are always sticky.
         */
        const val FOOTER_START = 1 shl 2

        /**
         * Footer is placed inside the gutter at the end edge of the section. This is the right for LTR locales.
         * Gutter footers are always sticky.
         */
        const val FOOTER_END = 1 shl 3

        const val GUTTER_AUTO = -1

        internal const val DEFAULT_GUTTER = GUTTER_AUTO
        internal const val DEFAULT_HEADER_STYLE = HEADER_STICKY
        internal const val DEFAULT_FOOTER_STYLE = FOOTER_STICKY
    }
}

