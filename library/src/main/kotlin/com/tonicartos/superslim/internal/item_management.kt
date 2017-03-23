package com.tonicartos.superslim.internal

import android.support.v7.widget.RecyclerView
import com.tonicartos.superslim.AdapterContract

internal interface ItemManagement {
    fun applyChanges(adapter: AdapterContract<*>, graph: GraphManager, recycler: RecyclerView.Recycler)
    fun addItems(start: Int, count: Int)
    fun removeItems(start: Int, count: Int)
    fun moveItems(from: Int, to: Int, count: Int)
}

/**
 *
 */
internal class ItemManager : ItemManagement {
    private val changes = ArrayList<Op>()

    override fun applyChanges(adapter: AdapterContract<*>, graph: GraphManager, recycler: RecyclerView.Recycler) {
        changes.forEach { it.apply(adapter, graph, recycler) }
        changes.clear()
    }

    override fun addItems(start: Int, count: Int) {
        changes.add(Add.acquire(start, count))
    }

    override fun removeItems(start: Int, count: Int) {
        changes.add(Remove.acquire(start, count))
    }

    override fun moveItems(from: Int, to: Int, count: Int) {
        changes.add(Move.acquire(from, to, count))
    }
}

private interface Op {
    fun apply(adapter: AdapterContract<*>, graph: GraphManager, recycler: RecyclerView.Recycler)
}

private data class Add(var start: Int, var count: Int) : Op {
    companion object {
        private val pool = arrayListOf<Add>()

        fun acquire(start: Int, count: Int) =
                if (pool.isEmpty()) {
                    Add(start, count)
                } else {
                    pool.removeAt(0).apply {
                        this.start = start
                        this.count = count
                    }
                }

        fun release(obj: Add) {
            pool.add(obj)
        }
    }

    override fun apply(adapter: AdapterContract<*>, graph: GraphManager, recycler: RecyclerView.Recycler) {
        var currentSection = 0
        var currentStart = 0
        var currentCount = 0
        // Handle in chunks per section.
        for (i in start until start + count) {
            val about = adapter.getData(recycler.convertPreLayoutPositionToPostLayout(i))
            when {
                about.isHeader                  -> graph.addHeader(about.section)
                about.isFooter                  -> graph.addFooter(about.section)
                about.section == currentSection -> currentCount += 1
                else                            -> {
                    graph.addItems(currentSection, currentStart, currentCount)
                    currentSection = about.section
                    currentStart = about.position
                    currentCount = 1
                }
            }

            if (i == start + count - 1) {
                graph.addItems(currentSection, currentStart, currentCount)
            }
        }
        release(this)
    }
}

private data class Remove(var start: Int, var count: Int) : Op {
    companion object {
        private val pool = arrayListOf<Remove>()

        fun acquire(start: Int, count: Int) =
                if (pool.isEmpty()) {
                    Remove(start, count)
                } else {
                    pool.removeAt(0).apply {
                        this.start = start
                        this.count = count
                    }
                }

        fun release(obj: Remove) {
            pool.add(obj)
        }
    }

    override fun apply(adapter: AdapterContract<*>, graph: GraphManager, recycler: RecyclerView.Recycler) {
        graph.root.removeItems(start, count)
        release(this)
    }
}

private data class Move(var from: Int, var to: Int, var count: Int) : Op {
    companion object {
        private val pool = arrayListOf<Move>()

        fun acquire(from: Int, to: Int, count: Int) =
                if (pool.isEmpty()) {
                    Move(from, to, count)
                } else {
                    pool.removeAt(0).apply {
                        this.from = from
                        this.to = to
                        this.count = count
                    }
                }

        fun release(obj: Move) {
            pool.add(obj)
        }
    }

    override fun apply(adapter: AdapterContract<*>, graph: GraphManager, recycler: RecyclerView.Recycler) {

    }
}
