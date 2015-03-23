package com.tonicartos.superslim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic decorator that can decorate items with drawables and/or padding in a flexible and
 * selectable manner. <p> To control which edges are affected, flags are used to determine whether
 * to apply the padding or drawable to a given edge. Edges can be internal or external as determined
 * by the section layout manager. Drawables take precedence over padding in being applied to an
 * edge. </p> Use the Builder.decorates* methods, or assignmentCheckers to limit the decorator to a
 * subset of the items.
 */
public class ItemDecorator extends RecyclerView.ItemDecoration {

    public static final int INTERNAL = 0x01;

    public static final int EXTERNAL = 0x02;

    public static final int ANY_EDGE = INTERNAL | EXTERNAL;

    private static final int DEFAULT_FLAGS = ANY_EDGE;

    public static final int INSET = 0x04;

    public static final int HEADER = 0x01;

    public static final int CONTENT = 0x02;

    public static final int ANY_KIND = HEADER | CONTENT;

    static final int UNSET = -1;

    private final Spacing mSpacing;

    private final Rect mEdgeState = new Rect();

    private final Edge mLeft;

    private final Edge mRight;

    private final Edge mTop;

    private final Edge mBottom;

    private final Edge mStart;

    private final Edge mEnd;

    private List<AssignmentChecker> mCheckers;

    private boolean initialised = false;

    private boolean layoutDirectionResolved = false;

    ItemDecorator(Builder b) {
        mCheckers = new ArrayList<>(b.assignments);
        mStart = new Edge(
                b.startDrawable, b.startDrawableFlags, b.startPadding, b.startPaddingFlags);
        mEnd = new Edge(
                b.endDrawable, b.endDrawableFlags, b.endPadding, b.endPaddingFlags);
        mLeft = new Edge(
                b.leftDrawable, b.leftDrawableFlags, b.leftPadding, b.leftPaddingFlags);
        mTop = new Edge(
                b.topDrawable, b.topDrawableFlags, b.topPadding, b.topPaddingFlags);
        mRight = new Edge(
                b.rightDrawable, b.rightDrawableFlags, b.rightPadding, b.rightPaddingFlags);
        mBottom = new Edge(
                b.bottomDrawable, b.bottomDrawableFlags, b.bottomPadding, b.bottomPaddingFlags);
        mSpacing = new Spacing(b);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        resolveLayoutDirection(parent);
        // Check decorator is assigned to section by sfp or slm.
        LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) view
                .getLayoutParams();
        LayoutManager lm = (LayoutManager) parent.getLayoutManager();
        if (!assignedTo(lm.getSectionData(params.getViewPosition()), params)) {
            outRect.left = 0;
            outRect.top = 0;
            outRect.right = 0;
            outRect.bottom = 0;
            return;
        }

        mSpacing.getOffsets(outRect, view, lm, state);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        resolveLayoutDirection(parent);
        LayoutManager lm = (LayoutManager) parent.getLayoutManager();

        for (int i = 0; i < lm.getChildCount(); i++) {
            View child = lm.getChildAt(i);
            LayoutManager.LayoutParams params =
                    (LayoutManager.LayoutParams) child.getLayoutParams();
            if (!assignedTo(lm.getSectionData(params.getViewPosition()), params)) {
                continue;
            }

            lm.getEdgeStates(mEdgeState, child, state);

            final int decorLeft = lm.getDecoratedLeft(child);
            final int decorTop = lm.getDecoratedTop(child);
            final int decorRight = lm.getDecoratedRight(child);
            final int decorBottom = lm.getDecoratedBottom(child);

            final int childLeft = child.getLeft();
            final int childTop = child.getTop();
            final int childRight = child.getRight();
            final int childBottom = child.getBottom();

            final int offsetLeft = childLeft - decorLeft - params.leftMargin;
            final int offsetTop = childTop - decorTop - params.topMargin;
            final int offsetRight = decorRight - childRight - params.rightMargin;
            final int offsetBottom = decorBottom - childBottom - params.bottomMargin;

            if (mLeft.hasValidDrawableFor(mEdgeState.left)) {
                final int right = decorLeft + offsetLeft;
                final int left = right - mLeft.drawable.getIntrinsicWidth();

                boolean excludeOffsets =
                        (mLeft.drawableFlags & INSET) == INSET;
                final int top = excludeOffsets ? decorTop + offsetTop : decorTop;
                final int bottom = excludeOffsets ? decorBottom - offsetBottom
                        : decorBottom;

                mLeft.drawable.setBounds(left, top, right, bottom);
                mLeft.drawable.draw(c);
            }

            if (mTop.hasValidDrawableFor(mEdgeState.top)) {
                final int bottom = decorTop + offsetTop;
                final int top = bottom - mTop.drawable.getIntrinsicHeight();

                boolean excludeOffsets =
                        (mTop.drawableFlags & INSET) == INSET;
                int left = excludeOffsets ? decorLeft + offsetLeft : decorLeft;
                int right = excludeOffsets ? decorRight - offsetRight : decorRight;

                mTop.drawable.setBounds(left, top, right, bottom);
                mTop.drawable.draw(c);
            }

            if (mRight.hasValidDrawableFor(mEdgeState.right)) {
                final int left = decorRight - offsetRight;
                final int right = left + mRight.drawable.getIntrinsicWidth();

                boolean excludeOffsets =
                        (mRight.drawableFlags & INSET) == INSET;
                final int top = excludeOffsets ? decorTop + offsetTop : decorTop;
                final int bottom = excludeOffsets ? decorBottom - offsetBottom : decorBottom;

                mRight.drawable.setBounds(left, top, right, bottom);
                mRight.drawable.draw(c);
            }

            if (mBottom.hasValidDrawableFor(mEdgeState.bottom)) {
                final int top = decorBottom - offsetBottom;
                final int bottom = top + mBottom.drawable.getIntrinsicHeight();

                boolean excludeOffsets =
                        (mBottom.drawableFlags & INSET) == INSET;
                int left = excludeOffsets ? decorLeft + offsetLeft : decorLeft;
                int right = excludeOffsets ? decorRight - offsetRight : decorRight;

                mBottom.drawable.setBounds(left, top, right, bottom);
                mBottom.drawable.draw(c);
            }
        }

        super.onDrawOver(c, parent, state);
    }

    private boolean assignedTo(SectionData sectionData, LayoutManager.LayoutParams params) {
        if (mCheckers.size() == 0) {
            return true;
        }

        for (AssignmentChecker check : mCheckers) {
            if (check.isAssigned(sectionData, params)) {
                return true;
            }
        }
        return false;
    }

    /**
     * We can finally handle layout direction.
     *
     * @param view View providing layout direction.
     */
    private void resolveLayoutDirection(View view) {
        if (layoutDirectionResolved) {
            return;
        }
        layoutDirectionResolved = true;
        int layoutDirection = ViewCompat.getLayoutDirection(view);
        mSpacing.resolveLayoutDirection(layoutDirection);

        switch (layoutDirection) {
            case ViewCompat.LAYOUT_DIRECTION_RTL:
                if (mStart.padding != UNSET) {
                    mRight.padding = mStart.padding;
                    mRight.paddingFlags = mStart.paddingFlags;
                }
                if (mStart.drawable != null) {
                    mRight.drawable = mStart.drawable;
                    mRight.drawableFlags = mStart.drawableFlags;
                }
                if (mEnd.padding != UNSET) {
                    mLeft.padding = mEnd.padding;
                    mLeft.paddingFlags = mEnd.paddingFlags;
                }
                if (mEnd.drawable != null) {
                    mLeft.drawable = mEnd.drawable;
                    mLeft.drawableFlags = mEnd.drawableFlags;
                }
                break;
            case ViewCompat.LAYOUT_DIRECTION_LTR:
            default:
                if (mStart.padding != UNSET) {
                    mLeft.padding = mStart.padding;
                    mLeft.paddingFlags = mStart.paddingFlags;
                }
                if (mStart.drawable != null) {
                    mLeft.drawable = mStart.drawable;
                    mLeft.drawableFlags = mStart.drawableFlags;
                }
                if (mEnd.padding != UNSET) {
                    mRight.padding = mEnd.padding;
                    mRight.paddingFlags = mEnd.paddingFlags;
                }
                if (mEnd.drawable != null) {
                    mRight.drawable = mEnd.drawable;
                    mRight.drawableFlags = mEnd.drawableFlags;
                }
        }
    }

    /**
     * Checks if the decorator is assigned to an item.
     */
    public static interface AssignmentChecker {

        /**
         * Tests and item to see if it should be decorated.
         *
         * @param params Item's layout params.
         * @return True if the decorator should decorate this item.
         */
        boolean isAssigned(SectionData sectionData, LayoutManager.LayoutParams params);
    }

    static class Edge {

        Drawable drawable;

        int drawableFlags;

        int padding;

        int paddingFlags;

        Edge(Drawable drawable, int drawableFlags, int padding, int paddingFlags) {
            this.drawable = drawable;
            this.drawableFlags = drawableFlags;
            this.padding = padding;
            this.paddingFlags = paddingFlags;
        }

        boolean hasValidDrawableFor(int positionMask) {
            return drawable != null && (drawableFlags & positionMask) == positionMask;
        }
    }

    /**
     * Builder for decorator.
     */
    public static class Builder {

        public Drawable startDrawable;

        public int startDrawableFlags;

        public int startPaddingFlags;

        public Drawable endDrawable;

        public int endDrawableFlags;

        public int endPaddingFlags;

        ArrayList<AssignmentChecker> assignments = new ArrayList<>();

        int leftPadding;

        int leftPaddingFlags;

        Drawable leftDrawable;

        int leftDrawableFlags;

        int topPadding;

        int topPaddingFlags;

        Drawable topDrawable;

        int topDrawableFlags;

        int rightPadding;

        int rightPaddingFlags;

        Drawable rightDrawable;

        int rightDrawableFlags;

        int bottomPadding;

        int bottomPaddingFlags;

        Drawable bottomDrawable;

        int bottomDrawableFlags;

        private Context mContext;

        public Builder(Context context) {
            mContext = context;
        }

        /**
         * Add an assignment checker for this decorator.
         *
         * @param checker Checker to test whether items should be decorated.
         * @return Builder.
         */
        public Builder addAssignmentChecker(AssignmentChecker checker) {
            assignments.add(checker);
            return this;
        }        public int startPadding = UNSET;

        public ItemDecorator build() {
            return new ItemDecorator(this);
        }

        /**
         * Decorate items in a section.
         *
         * @param sectionFirstPosition First position in section to decorate items from.
         * @return Builder.
         */
        public Builder decorateSection(int sectionFirstPosition, int targetKind) {
            assignments.add(new SectionChecker(sectionFirstPosition, targetKind));
            return this;
        }

        /**
         * Decorate items in a section.
         *
         * @param sectionFirstPosition First position in section to decorate items from.
         * @return Builder.
         */
        public Builder decorateSection(int sectionFirstPosition) {
            return decorateSection(sectionFirstPosition, CONTENT);
        }

        /**
         * Decorate items in a section managed by one of the built-in SLMs.
         *
         * @param id SLM id.
         * @return Builder.
         */
        public Builder decorateSlm(int id, int targetKind) {
            assignments.add(new InternalSlmChecker(id, targetKind));
            return this;
        }

        /**
         * Decorate items in a section managed by a custom SLM.
         *
         * @param key SLM key..
         * @return Builder.
         */
        public Builder decorateSlm(String key, int targetKind) {
            assignments.add(new CustomSlmChecker(key, targetKind));
            return this;
        }

        /**
         * Decorate items in a section managed by one of the built-in SLMs.
         *
         * @param id SLM id.
         * @return Builder.
         */
        public Builder decorateSlm(int id) {
            return decorateSlm(id, CONTENT);
        }

        /**
         * Decorate items in a section managed by a custom SLM.
         *
         * @param key SLM key..
         * @return Builder.
         */
        public Builder decorateSlm(String key) {
            return decorateSlm(key, CONTENT);
        }

        /**
         * Decorates a specific item.
         */
        public Builder decoratesPosition(int position) {
            assignments.add(new PositionChecker(position));
            return this;
        }

        public Builder setDrawableAbove(int resId) {
            return setDrawableAbove(resId, DEFAULT_FLAGS);
        }

        public Builder setDrawableAbove(int resId, int flags) {
            return setDrawableAbove(mContext.getResources().getDrawable(resId), flags);
        }        public int endPadding = UNSET;

        public Builder setDrawableAbove(Drawable drawable) {
            return setDrawableAbove(drawable, DEFAULT_FLAGS);
        }

        public Builder setDrawableAbove(Drawable drawable, int flags) {
            topDrawable = drawable;
            topDrawableFlags = flags;
            return this;
        }

        public Builder setDrawableBelow(int resId) {
            return setDrawableBelow(resId, DEFAULT_FLAGS);
        }

        public Builder setDrawableBelow(int resId, int flags) {
            return setDrawableBelow(mContext.getResources().getDrawable(resId), flags);
        }

        public Builder setDrawableBelow(Drawable drawable) {
            return setDrawableBelow(drawable, DEFAULT_FLAGS);
        }

        public Builder setDrawableBelow(Drawable drawable, int flags) {
            bottomDrawable = drawable;
            bottomDrawableFlags = flags;
            return this;
        }

        public Builder setDrawableEnd(int resId) {
            return setDrawableEnd(resId, DEFAULT_FLAGS);
        }

        public Builder setDrawableEnd(int resId, int flags) {
            return setDrawableEnd(mContext.getResources().getDrawable(resId), flags);
        }

        public Builder setDrawableEnd(Drawable drawable) {
            return setDrawableEnd(drawable, DEFAULT_FLAGS);
        }

        public Builder setDrawableEnd(Drawable drawable, int flags) {
            endDrawable = drawable;
            endDrawableFlags = flags;
            return this;
        }

        public Builder setDrawableLeft(int resId) {
            return setDrawableLeft(resId, DEFAULT_FLAGS);
        }

        public Builder setDrawableLeft(int resId, int flags) {
            return setDrawableLeft(mContext.getResources().getDrawable(resId), flags);
        }

        public Builder setDrawableLeft(Drawable drawable) {
            return setDrawableLeft(drawable, DEFAULT_FLAGS);
        }

        public Builder setDrawableLeft(Drawable drawable, int flags) {
            leftDrawable = drawable;
            leftDrawableFlags = flags;
            return this;
        }

        public Builder setDrawableRight(int resId) {
            return setDrawableRight(resId, DEFAULT_FLAGS);
        }

        public Builder setDrawableRight(int resId, int flags) {
            return setDrawableRight(mContext.getResources().getDrawable(resId), flags);
        }

        public Builder setDrawableRight(Drawable drawable) {
            return setDrawableRight(drawable, DEFAULT_FLAGS);
        }

        public Builder setDrawableRight(Drawable drawable, int flags) {
            rightDrawable = drawable;
            rightDrawableFlags = flags;
            return this;
        }

        public Builder setDrawableStart(int resId) {
            return setDrawableStart(resId, DEFAULT_FLAGS);
        }

        public Builder setDrawableStart(int resId, int flags) {
            return setDrawableStart(mContext.getResources().getDrawable(resId), flags);
        }

        public Builder setDrawableStart(Drawable drawable) {
            return setDrawableStart(drawable, DEFAULT_FLAGS);
        }

        public Builder setDrawableStart(Drawable drawable, int flags) {
            startDrawable = drawable;
            startDrawableFlags = flags;
            return this;
        }

        public Builder setPaddingAbove(int padding) {
            return setPaddingAbove(padding, DEFAULT_FLAGS);
        }

        public Builder setPaddingAbove(int padding, int flags) {
            topPadding = padding;
            topPaddingFlags = flags;
            return this;
        }

        public Builder setPaddingBelow(int padding) {
            return setPaddingBelow(padding, DEFAULT_FLAGS);
        }

        public Builder setPaddingBelow(int padding, int flags) {
            bottomPadding = padding;
            bottomPaddingFlags = flags;
            return this;
        }

        public Builder setPaddingDimensionAbove(int resId) {
            return setPaddingDimensionAbove(resId, DEFAULT_FLAGS);
        }

        public Builder setPaddingDimensionAbove(int resId, int flags) {
            return setPaddingAbove(mContext.getResources().getDimensionPixelSize(resId), flags);
        }

        public Builder setPaddingDimensionBelow(int resId) {
            return setPaddingDimensionBelow(resId, DEFAULT_FLAGS);
        }

        public Builder setPaddingDimensionBelow(int resId, int flags) {
            return setPaddingBelow(mContext.getResources().getDimensionPixelSize(resId), flags);
        }

        public Builder setPaddingDimensionEnd(int resId) {
            return setPaddingDimensionEnd(resId, DEFAULT_FLAGS);
        }

        public Builder setPaddingDimensionEnd(int resId, int flags) {
            return setPaddingEnd(mContext.getResources().getDimensionPixelSize(resId), flags);
        }

        public Builder setPaddingDimensionLeft(int resId) {
            return setPaddingDimensionLeft(resId, DEFAULT_FLAGS);
        }

        public Builder setPaddingDimensionLeft(int resId, int flags) {
            return setPaddingLeft(mContext.getResources().getDimensionPixelSize(resId), flags);
        }

        public Builder setPaddingDimensionRight(int resId) {
            return setPaddingDimensionRight(resId, DEFAULT_FLAGS);
        }

        public Builder setPaddingDimensionRight(int resId, int flags) {
            return setPaddingRight(mContext.getResources().getDimensionPixelSize(resId), flags);
        }

        public Builder setPaddingDimensionStart(int resId) {
            return setPaddingDimensionStart(resId, DEFAULT_FLAGS);
        }

        public Builder setPaddingDimensionStart(int resId, int flags) {
            return setPaddingStart(mContext.getResources().getDimensionPixelSize(resId), flags);
        }

        public Builder setPaddingEnd(int padding) {
            return setPaddingEnd(padding, DEFAULT_FLAGS);
        }

        public Builder setPaddingEnd(int padding, int flags) {
            endPadding = padding;
            endPaddingFlags = flags;
            return this;
        }

        public Builder setPaddingLeft(int padding) {
            return setPaddingLeft(padding, DEFAULT_FLAGS);
        }

        public Builder setPaddingLeft(int padding, int flags) {
            leftPadding = padding;
            leftPaddingFlags = flags;
            return this;
        }

        public Builder setPaddingRight(int padding) {
            return setPaddingRight(padding, DEFAULT_FLAGS);
        }

        public Builder setPaddingRight(int padding, int flags) {
            rightPadding = padding;
            rightPaddingFlags = flags;
            return this;
        }

        public Builder setPaddingStart(int padding) {
            return setPaddingStart(padding, DEFAULT_FLAGS);
        }

        public Builder setPaddingStart(int padding, int flags) {
            startPadding = padding;
            startPaddingFlags = flags;
            return this;
        }






    }

    static class Spacing {


        final int internalTop;

        final int internalBottom;

        final int externalTop;

        final int externalBottom;

        int internalLeft;

        int internalRight;

        int externalLeft;

        int externalRight;

        int internalStart;

        int internalEnd;

        int externalStart;

        int externalEnd;

        public Spacing(Builder b) {
            int mask = INTERNAL;
            internalStart = calculateOffset(b.startDrawable, b.startDrawableFlags,
                    b.startPadding, b.startPaddingFlags, mask, false);

            internalEnd = calculateOffset(b.endDrawable, b.endDrawableFlags,
                    b.endPadding, b.endPaddingFlags, mask, false);

            internalLeft = calculateOffset(b.leftDrawable, b.leftDrawableFlags,
                    b.leftPadding, b.leftPaddingFlags, mask, false);

            internalTop = calculateOffset(b.topDrawable, b.topDrawableFlags,
                    b.topPadding, b.topPaddingFlags, mask, true);

            internalRight = calculateOffset(b.rightDrawable, b.rightDrawableFlags,
                    b.rightPadding, b.rightPaddingFlags, mask, false);

            internalBottom = calculateOffset(b.bottomDrawable, b.bottomDrawableFlags,
                    b.bottomPadding, b.bottomPaddingFlags, mask, true);

            mask = EXTERNAL;
            externalStart = calculateOffset(b.startDrawable, b.startDrawableFlags,
                    b.leftPadding, b.leftPaddingFlags, mask, false);

            externalEnd = calculateOffset(b.endDrawable, b.endDrawableFlags,
                    b.endPadding, b.endPaddingFlags, mask, false);

            externalLeft = calculateOffset(b.leftDrawable, b.leftDrawableFlags,
                    b.leftPadding, b.leftPaddingFlags, mask, false);

            externalTop = calculateOffset(b.topDrawable, b.topDrawableFlags,
                    b.topPadding, b.topPaddingFlags, mask, true);

            externalRight = calculateOffset(b.rightDrawable, b.rightDrawableFlags,
                    b.rightPadding, b.rightPaddingFlags, mask, false);

            externalBottom = calculateOffset(b.bottomDrawable, b.bottomDrawableFlags,
                    b.bottomPadding, b.bottomPaddingFlags, mask, true);
        }

        public void getOffsets(Rect outRect, View view, LayoutManager lm,
                RecyclerView.State state) {
            // Reuse the rect to get the edge states, either internal or external.
            lm.getEdgeStates(outRect, view, state);
            outRect.left = outRect.left == EXTERNAL ? externalLeft : internalLeft;
            outRect.right = outRect.right == EXTERNAL ? externalRight : internalRight;
            outRect.top = outRect.top == EXTERNAL ? externalTop : internalTop;
            outRect.bottom = outRect.bottom == EXTERNAL ? externalBottom : internalBottom;
        }

        public void resolveLayoutDirection(int resolvedLayoutDirection) {
            switch (resolvedLayoutDirection) {
                case ViewCompat.LAYOUT_DIRECTION_RTL:
                    if (internalStart != UNSET) {
                        internalRight = internalStart;
                    }

                    if (externalStart != UNSET) {
                        externalRight = externalStart;
                    }

                    if (internalEnd != UNSET) {
                        internalLeft = internalEnd;
                    }

                    if (externalEnd != UNSET) {
                        externalLeft = externalEnd;
                    }
                    break;
                case ViewCompat.LAYOUT_DIRECTION_LTR:
                default:
                    if (internalStart != UNSET) {
                        internalLeft = internalStart;
                    }

                    if (externalStart != UNSET) {
                        externalLeft = externalStart;
                    }

                    if (internalEnd != UNSET) {
                        internalRight = internalEnd;
                    }

                    if (externalEnd != UNSET) {
                        externalRight = externalEnd;
                    }
            }
        }

        private int calculateOffset(Drawable drawable, int drawableFlags, int padding,
                int paddingFlags, int positionMask, boolean isVerticalOffset) {
            final boolean drawableSelected = drawable != null &&
                    (drawableFlags & positionMask) == positionMask;
            final boolean paddingSelected = padding > 0 &&
                    (paddingFlags & positionMask) == positionMask;
            if (drawableSelected) {
                return isVerticalOffset ?
                        drawable.getIntrinsicHeight() : drawable.getIntrinsicWidth();
            } else if (paddingSelected) {
                return padding;
            }
            return 0;
        }
    }

    static class PositionChecker implements AssignmentChecker {

        private int mPosition;

        PositionChecker(int position) {
            mPosition = position;
        }

        @Override
        public boolean isAssigned(SectionData sd, LayoutManager.LayoutParams params) {
            return params.getViewPosition() == mPosition;
        }
    }

    static class InternalSlmChecker implements AssignmentChecker {

        final private int mSlmId;

        final private int mTargetKind;

        InternalSlmChecker(int slmId, int targetKind) {
            mSlmId = slmId;
            mTargetKind = targetKind;
        }

        @Override
        public boolean isAssigned(SectionData sd, LayoutManager.LayoutParams params) {
            int kind = params.isHeader() ? HEADER : CONTENT;
            return sd.sectionManagerKind == mSlmId && (kind & mTargetKind) != 0;
        }
    }

    static class CustomSlmChecker implements AssignmentChecker {

        final private String mSlmKey;

        final private int mTargetKind;

        CustomSlmChecker(String slmKey, int targetKind) {
            mSlmKey = slmKey;
            mTargetKind = targetKind;
        }

        @Override
        public boolean isAssigned(SectionData sd, LayoutManager.LayoutParams params) {
            int kind = params.isHeader() ? HEADER : CONTENT;
            return sd.sectionManagerKind == LayoutManager.SECTION_MANAGER_CUSTOM &&
                    (kind & mTargetKind) != 0 &&
                    TextUtils.equals(sd.sectionManager, mSlmKey);
        }
    }

    static class SectionChecker implements AssignmentChecker {

        final private int mSfp;

        final private int mTargetKind;

        SectionChecker(int sfp, int targetKind) {
            // TODO: A better way of targeting sections.
            mSfp = sfp;
            mTargetKind = targetKind;
        }

        @Override
        public boolean isAssigned(SectionData sd, LayoutManager.LayoutParams params) {
            int kind = params.isHeader() ? HEADER : CONTENT;
            return sd.firstPosition == mSfp && (kind & mTargetKind) != 0;
        }
    }
}
