package com.tonicartos.superslim

interface ColumnsSectionConfigurationMixin {
    var numColumns: Int
    var columnWidth: Int
}

class ColumnsConfiguration : ColumnsSectionConfigurationMixin {
    override var numColumns = 0
        get() = field
        set(value) {
            field = value
            columnWidth = -1
        }

    override var columnWidth: Int = -1
}

interface ColumnsSectionStateMixin {
    var numColumns: Int

    fun resolveColumns(helper: LayoutHelper)
}

class ColumnsState(configuration: ColumnsSectionConfigurationMixin) : ColumnsSectionStateMixin {
    override var numColumns: Int = 0
    private var requestedColumnWidth = -1
    private var columnsResolved = false

    init {
        numColumns = configuration.numColumns
        requestedColumnWidth = configuration.columnWidth
    }

    override fun resolveColumns(helper: LayoutHelper) {
        if (columnsResolved) {
            return
        }

        if (requestedColumnWidth != -1) {
            numColumns = helper.layoutWidth / requestedColumnWidth
        }

        if (numColumns == 0) {
            numColumns = 1
        }

        columnsResolved = true
    }
}
