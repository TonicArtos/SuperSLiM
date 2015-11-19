package com.tonicartos.superslim.adapter

/**
 * Configuration for a LinearSlm.
 *
 * TODO: Move this to the LinearSlm implementation.
 */
class LinearSectionConfig : Section.Config {
    constructor(customSlmLabel: String, headerMarginStart: Int = DEFAULT_MARGIN, headerMarginEnd: Int = DEFAULT_MARGIN, @HeaderStyle headerStyle: Int = DEFAULT_HEADER_STYLE) : super(customSlmLabel, headerMarginStart, headerMarginEnd, headerStyle)

    constructor(headerMarginStart: Int = DEFAULT_MARGIN, headerMarginEnd: Int = DEFAULT_MARGIN, @HeaderStyle headerStyle: Int = DEFAULT_HEADER_STYLE) : super(headerMarginStart, headerMarginEnd, headerStyle)

    // TODO: Change to use const LINEAR_SLM from LayoutManager
    override val slmKind: Int = 1
}

// TODO: Config builder
