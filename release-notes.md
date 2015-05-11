## early_release_4
*v0.4.13*

Added tests and fixes for layout, scrolling, and find methods. Deprecated set methods on GridSLM, these will be removed in 0.5.

- Fixed find first completely visible methods to actually find something (#88).
- Fixed numerous minor layout bugs that resulted in views being attached just outside the viewable area.
- Fixed a few layout bugs that resulted in some views overlapping by one pixel or being off by one pixel.

Due to pixel fitting on calculated columns, some grid columns and cells can be a slightly different width from others. In previous versions they were all the same, but could end out overlapping.

*v0.4.12*

Two small fixes in this update. The grid slm should now honour the RTL layout direction. Also, when the recycler view is rotated or recreated, it should now restore to the correct adapter position.

- Fix RTL layout in grid SLM (#16).
- Fix position restoration (#85).

*v0.4.11*
- Fix display error on scrolling over an empty section with a header (#83).
- Work around for items without layout params (#74).
- Update to support libs v22.1.1 (#76).

*v0.4.10.1*
- Remove unneeded library drawables (default launcher icons) (#80) - @kingargyle.

*v0.4.10*
- Add static factory method to preserve margin layout parameters (#73) - @adi1133.
- Fix overscroll after notifyDataSetChanged() is called (#70).

*v0.4.9.1*
- Change minSdk to 9 (#72).
- Add read me disclaimer for support only from API 15+.

*v0.4.9*
- Fix jumpy layout when scrolling with header as last item (#67).

*v0.4.8*
- Fix RCE on change to smaller data set (#65).

*v0.4.7*
- Fix NPE when computing scroll indicator (#62).

*v0.4.6*
- Fix scroll to start over section with one item (#60).

*v0.4.5*
- Fix scroll at content end with grid (#55). Thank you @Eifelschaf for your help.
- Lots of minor fixes.

*v0.4.4*
- Fix regression spamming onCreateViewHolder (#50).

*v0.4.3*
- Fix layout requested position (orientation change) when content doesn't fill the recycler view. [#49](../issues/49).
- Fix scroll jumping if content doesn't fill the recycler view. [#47](../issues/47).

*v0.4.1*
- Fix smooth scrolling to sticky header.

*v0.4.0*
- Simpler initialisation.
- Configuration of SLMs through layout params and xml.
- Support for layout params and xml attrs for custom SLMs.
- Support margins.
- New layout implementation.
- New scroll implementation.

## early_release_3
*0.3.0*
- Fix layout corruption on fling. [#35](../issues/35)
- Add scroll indicator. [#14](../issues/14)
- Change layoutId to sectionManager. [#28](../issures/28)

## early_release_2
*0.2.3*
- Fix jump to inline header on scroll. [#31](../issues/31).

*0.2.2*
- Fix NPE for sections without headers. [#27](../issues/27).

*0.2.1*
- Add methods required for infinite scrolling. [#22](../issues/22).
- Fix extraneous creation of view holders by grid section layout manager. [#15](../issues/15).

*0.2.0*
- Clearer naming to better convey meaning of attributes and layout parameters and their values. [Updated documentation](../wiki/Content-View-Attributes-and-Parameters).
- Change from using a factory to instantiate SectionLayoutManagers to registering SLMs to ids.
- Support for RTL layout direction.

## Initial Release
**0.1.0**
*10-02-2015*
- Removal of scroll indicator support. Replacement will be made after changes to section layout managers.
- Consolidation of header display configuration into slm_headerDisplay attribute using flags.

*08-01-2015*
- Support back to API 14 in library and example.

*22-12-2014*
- Fixed package name to com.tonicartos.superslim
- Grid section layout.
- Scroll indicator support.
- Smooth scroll to position.
- Scroll to position.
- Example app enhancements for the above features.

*18-12-2014*
- Basic example app.
- Linear section layout.
- Margin headers.
- Overlay headers.
- Sticky headers.
- A Layout manager with per section headers and layouts.

