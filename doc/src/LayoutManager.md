# LayoutManager
[TOC]
#Concepts
## General
Change animation
: The recycler view's animation for changes in the data set. The initial conditions are set by the pre-layout pass, and the final state of each view change animation is given by the real-layout pass.

Pre-layout
: `RecyclerView.State.isPreLayout() == true`
: The role of the pre-layout pass is to set up the initial conditions of the change animation. The pre-layout pass lays-out regular on-screen views, on-screen disappearing views, and off-screen appearing views.
	
Real-layout	
: `RecyclerView.State.isPreLayout() == false`
: The second layout pass which gives the final position of views after any animations have run. The real-layout pass lays-out regular on-screen views, on-screen appearing views, and off-screen disappearing views.
	
Added views
: Added views are available for laying out in the real-layout pass.

Removed views
: Removed views are returned from the scrap list in the pre-layout pass to help with correctly laying out the initial position of views affected by the change animation, especially appearing views. Removed views should not be added to the child list, they are only a positional aid.

Disappearing view
: A view that is on-screen before being animated off-screen.
: A disappearing view is laid out on-screen in the pre-layout pass, and is laid out off-screen in the real-layout.
: In the post-layout pass the recycler view must be told the view is disappearing by calling `addDisappearingView()` on the layout manager.
: Disappearing views are a result of either; the view being moved, or views being added before it, thus pushing it off-screen.

Appearing view
: A view that is off-screen before being animated onto the screen.
: An appearing view is laid out off-screen in the pre-layout pass, and is laid out on-screen in the real-layout.
: An appearing view can be discovered by detecting views that will be removed after pre-layout, laying them out, but not accounting for the removed views in determining the visual limit of the layout pass; i.e., for a linear layout removed views count for zero height when being laid out, this is to the effect that 100 pixels of removed views results in at least 100 pixels of space being available for appearing views.

Scrap views
: Views attached to the recycler view which are marked for removal or reuse.
: When layout starts SuperSLiM removes and scraps all child views of the recycler view. When SLMs get views they may be reused from the scrap list or populated from the adapter. Scrap views are automatically removed from the scrap list when they are added back to the recycler view.
: Scrap views remaining after the regular post-layout pass are views that were on-screen in the pre-layout pass. These scrap views are now either removed and should be ignored, or are disappearing views and are to be laid out off-screen.

Item decorator
: Item decorators can inset child views and add extra content without complicating layout passes. Handling decorators correctly requires using helper methods with the child view as an argument rather than interacting with the child view directly.

Item changes
: Changes to the data set are notified to the recycler view through a listener interface. The changes are propagated through to the layout manager by calls to `onItemsAdded()`, `onItemsUpdated()`, `onItemsChanged()`, `onItemsMoved()`, and `onItemsRemoved()`. The affected position and ranges are passed in these calls. If the change is outside of the currently laid out area the calls are made before the pre-layout pass.

Scroll event
: A call to one to the scroll methods `scrollVerticallyBy()` or `scrollHorizontallyBy()`. Scroll distance is given in pixels and the result indicates overscroll.

Anchor position
: Logically, the current position that the user has scrolled to. For touch scrolling, this is the adapter position of the view that is logically and visually closest to the reference edge. Alternatively, for focus based scrolling, which is assistive or keyboard driven, this is the position of the focused view.
: The anchor position is updated after each scroll or focus event, or is directly modified by calls to *scroll to* or *smooth scroll to* a position.
: The current position is retained through configuration changes and used to re-layout the content with that position as an anchor.

Anchor position offset
: The distance from the reference edge that the view indicated by the anchor position is located. Use of this value allows for the logical recreation of the scrolled view after configuration changes.
: This offset is updated each time the anchor position is updated.

Over scroll
: When a layout pass displays less content than should have been. The visual effect is where the scrolled content is displayed with its bottom edge not aligned with the bottom padding edge or view edge, but further up the view instead.  This is caused when items from the anchor position to the last item are laid out, but there is still space below the content and there are items before the anchor position. The solution is to detect this situation after a layout pass, adjust all the laid out views down to the correct position, and then fill in the revealed space.

## SuperSLiM

Scroll layout
: Scrolling in SuperSLiM is handled by moving all content on the screen and then laying out revealed content on the leading edge as a special scroll layout pass.

Scroll pre-trim
: Before the scroll trim is performed, a scroll pre-trim pass is made on sections intersecting the trailing edge. Any special positioning of views after the scroll event can be made here. The base `SectionLayoutManager` implementation uses this to correctly reposition stickied headers on the up edge.

Scroll trim
: After a scroll layout, any content wholly outside the trailing edge is trimmed.

Trim notification
: When a view is trimmed due to a scroll, a notification is passed down the section hierarchy so that section layout managers can update any cached data about a section.

Section coordinate space
: Each section has its own virtual space. Values in this space are transformed up the section graph to the root coordinate space used by the layout manager and the recycler view.

Config transformation
: The layout manager is configured by the locale and user set values. A config transformation is applied to coordinates coming into and out of the recycler view. In this manner, it is easy to support different layout directions without custom logic in each section layout manager implementation.

Start edge
: The '*left edge*' of the layout managed area. For a RTL locale, this is actually the right edge.

End edge
: The '*right edge*' of the layout managed area. For a RTL locale, this is actually the left edge.

Reverse layout
: Layout is typically performed from top left to bottom right, translated for locale differences. When reversed, the layout is performed from the bottom right to the top right.

Orientation
: The orientation can be horizontal or vertical.

Stack from end
: Views are laid out from the bottom to the top. The bottom edge is the reference edge for determining current position. Headers still appear at the top of the section.

Headers at end
: Headers are positioned after the section content. This makes for a reversal of the sticky logic. Instead of sticking to the logical top of the section, they will stick to the logical bottom of the section.
: Handled inside `BaseSectionLayoutManager`.

Configuration transformer
: A function that transforms one coordinate system into another; i.e., turning the LTR, TTB coordinate system used by section layout managers, into the coordinate system used by a RTL recycler view - a configuration derived from the users locale.

Semi-real coordinate
: A coordinate that has been transformed into a virtual coordinate by a configuration transformation, but is also not nested within another virtual coordinate space. In this manner the top edge of this space can be called the semi-real top, though it could actually be the left edge of a horizontally scrolling recycler view.

# Layout transformation
Section layout managers work in their own virtual coordinate space that is transformed into the window coordinates. As such, section layout managers behave as though they are always operating in a vertical scrolling, top-to-bottom, left-to-right coordinate space. The width of the section space is defined by the parent section, and may have an invisible offset as defined by the parent. 

**Writes**
```sequence
section->helper: layoutChild(view, values)
helper->helper: apply inverse subspace transformation
helper->helper parent: layoutChild(view, values)
helper parent->helper parent: apply inverse subspace transformation
Note over helper parent: recurse layoutChild up graph ancestry
helper parent->layout manager: layoutChild(view, values)
layout manager->layout manager: apply inverse configuration transformation
layout manager->recycler view: layoutChild(view, values)
```

**Reads**
```sequence
section->helper: request
helper->helper parent: request
Note over helper parent: recurse request up graph ancestry
helper parent->layout manager: request
layout manager->recycler view: request
recycler view-->layout manager: result
layout manager->layout manager: apply configuration transformation
layout manager-->helper parent: result
helper parent->helper parent: apply subspace transformation
helper parent-->helper: result
helper->helper: apply subspace transformation
helper-->section: result
```

The above diagrams give the logical effect of the behaviour of the helper. However, transformations can be combined together and be expressed 

**Writes**
```sequence
section->helper: write values
helper->helper: apply inverse subspace transformation
helper->layout manager: write values in layout managers coords
layout manager->layout manager: apply inverse configuration transformation
layout manager->recycler view: write values in recycler views coords
```

**Reads**
```sequence
section->helper: request
helper->layout manager: request
layout manager->recycler view: request
recycler view-->layout manager: result in recycler views coords
layout manager->layout manager: apply configuration transformation
layout manager-->helper: result in layout managers coords
helper->helper: apply subspace transformation
helper-->section: result in subsections coords
```

## Configuration transformations
The `LayoutHelper` has a number of `ConfigTransformation` s.
> w = width of recycler view, h = height of recycler view.
*All of the config transformations are involutory, so can be applied to transform the coordinate space in either direction.*

LTR
: A no-op transformation.
$$\text{ltr}\begin{pmatrix}l\\t\\r\\b\end{pmatrix} = \begin{pmatrix}l\\t\\r\\b\end{pmatrix}$$

RTL
: Mirrors the horizontal values.
$$\text{rtl}\begin{pmatrix}l\\t\\r\\b\end{pmatrix} = \begin{pmatrix}w-r\\t\\w-l\\b\end{pmatrix}$$

Stack from end
: Mirrors the vertical values.
$$\text{sfe}\begin{pmatrix}l\\t\\r\\b\end{pmatrix} = \begin{pmatrix}l\\h-b\\r\\h-t\end{pmatrix}$$

Reverse
: Mirrors both the horizontal and vertical values.
$$\text{rev}\begin{pmatrix}l\\t\\r\\b\end{pmatrix} = \begin{pmatrix}w - r\\h-b\\w-l\\h-t\end{pmatrix}$$

Vertical orientation
: A no-op transformation.
$$\text{ver}\begin{pmatrix}l\\t\\r\\b\end{pmatrix} = \begin{pmatrix}l\\t\\r\\b\end{pmatrix}$$

Horizontal orientation
: Swaps the top and left values, and the bottom and right values. Effectively mirroring along a line drawn from the NW corner to the SE corner.
$$\text{hor}\begin{pmatrix}l\\t\\r\\b\end{pmatrix} = \begin{pmatrix}t\\l\\b\\r\end{pmatrix}$$

: It is important to note that for horizontal scrolling configurations, width and height measurement calls are transposed.

## Nesting
Subsections have their virtual space inset inside the parent section. When creating the subsection layout the parent SLM gives the available width, and the point of the top left corner. The derivative values for the subsections total x and y offsets within the root coordinate system are calculated and calls from the subsection helper can be made directly to the root interface.

# Tracking graph changes
~~Recycler view maintains its own state for tracking what items are available for layout or not. When items changes are notified, the layout manager gets informed through the data change callbacks - *onItems\*()*. These events happen at a time determined by the recycler view, either before layout or after pre-layout. Superslim layout manager also receives events for changes to the section graph direct from the adapter. The events from the adapter are stored pending the events from the recycler view. Once matching events, one from each source, are received, they are reconciled and the appropriate change is made to the internal section graph.~~

To keep the internal section graph consistent with the recycler view model, the layout manager has to reconcile recycler views reordered item change notifications with changes that happened before that to the adapter section graph. To do this, the layout manager receives item change callbacks from the recycler view, and updates an internal section graph accordingly. When needed, the layout manager can refer to the section graph in the adapter to fetch additional data about the event; e.g., is the item being added, in the callback, a member of a new or already existing section?

To track sections, an internal id is used separate from the user given section id.

## Section graph events
Section level events need additional section data passed to the layout manager. This needs to be paired with the appropriate callback from recycler view before updating the internal graph.

Add section
: Add section -> add items to item manager -> notify items added

Remove section
: Remove section -> remove items from item manager -> notify items removed

Move section
: Move section -> move items in item manager -> notify items moved one by one

Update section
: Change section configuration -> update items in item manager -> notify items updated

Item level events are simple and don't involve extra data.

Add item or header
: Add item or header -> add item to item manager -> notify item added

Remove item or header
: Remove item or header -> remove item in item manager -> notify item removed

Move item
: Move item -> move item in item manager -> notify item moved

Update item
: Update item -> update item in item manager -> notify item updated


# Layout manager code paths

onLayoutChild
scrollViewBy

# Adapter
sectionLookup: map of T to section
internalLookup: map of integer to section
root: section

#Section graph
##Section
userId: T
hasHeader: boolean
totalChildren: integer
: constraint: totalChildren >= subsections.size()

subsections: list of sections

# Internal section graph
## Section
adapterPosition: integer
: Position of section in adapter. If the section is not empty, this is also the adapter position of the first item in the section.

hasHeader: boolean
totalChildren: integer
: **constraint:** totalChildren >= subsections.size() + 1 if hasHeader is true

subsections: list of sections

# Section layout manager
## Laying out children
The difference between items and subsections is hidden from the SLM. Children can be measured as per the usual layout manager pattern, however subsections always fill the available width and have an undefined height, items behave as normal. To layout and add the child, the SLM defines the bounds and position of the child and the abstraction handles the rest. Laying out a subsection cascades into a layout call for that section and SLM with the given layout area.

## Pre-layout child handling
A child object abstracts the difference between subsections and items. The layout helper 

```
child = getChild(i)
measure(child)
layoutAndAdd(child, left, top, right, bottom)
child.done()
```

## Child abstraction
```Kotlin
interface Child {
	val measuredWidth
	val measuredHeight
	val left
	val top
	val right
	val bottom
}
```
