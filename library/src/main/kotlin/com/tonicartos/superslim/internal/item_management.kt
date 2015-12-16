package com.tonicartos.superslim.internal

import java.util.*

private const val HEADER = 1
private const val ADD = 1 shl 1
private const val REMOVE = 1 shl 2
private const val MOVE = 1 shl 3

private val reusedPair = Pair(0, 0)
private val reusedEvent = EventData(0, 0)

internal data class EventData(var action: Int, var section: Int) {
    companion object {
        const val HEADER = com.tonicartos.superslim.internal.HEADER
        const val ADD = com.tonicartos.superslim.internal.ADD
        const val REMOVE = com.tonicartos.superslim.internal.REMOVE
        const val MOVE = com.tonicartos.superslim.internal.MOVE

        fun stringify(opCode: Int) = when {
            opCode and ADD > 0    -> "ADD"
            opCode and REMOVE > 0 -> "REMOVE"
            opCode and MOVE > 0   -> "MOVE"
            else                  -> "Unknown"
        } + if (opCode and HEADER > 0) "|HEADER" else ""
    }

    override fun toString(): String {
        return "section: $section, cmd: ${stringify(action)}"
    }
}

/**
 * Takes ops and reorders them to match recycler views reordering. Then reconciles the ops with notifications coming in
 * from the recycler view. All this is done so the affected sections can be matched with the data change notifications
 * when they happen.
 *
 * # Wish list
 *
 * It would be really handy if layout manager information could be bundled with data change notifications, i.e.,
 * `Adapter$notifyItemRangeInserted(int positionStart, int itemCount, bundle Bundle)`. Then data changes would come into
 * the layout manager as
 * `LayoutManager$onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount, Bundle bundle)`, and we
 * could get rid of DataChangeHelper.
 */
internal class ItemChangeHelper {
    private val ops = arrayListOf<Op>()

    fun queueSectionHeaderAdded(section: Int, positionStart: Int) {
        SimpleOp.acquire(ADD or HEADER, section, positionStart, 1).insertInto(ops)
    }

    fun queueSectionHeaderRemoved(section: Int, positionStart: Int) {
        SimpleOp.acquire(REMOVE or HEADER, section, positionStart, 1).insertInto(ops)
    }

    fun queueSectionItemsAdded(section: Int, positionStart: Int, itemCount: Int) {
        SimpleOp.acquire(ADD, section, positionStart, itemCount).insertInto(ops)
    }

    fun queueSectionItemsRemoved(section: Int, positionStart: Int, itemCount: Int) {
        SimpleOp.acquire(REMOVE, section, positionStart, itemCount).insertInto(ops)
    }

    fun queueSectionItemsMoved(fromSection: Int, from: Int, toSection: Int, to: Int) {
        MoveOp.acquire(fromSection, from, toSection, to).insertInto(ops)
    }

    fun pullMoveEventData(from: Int, to: Int): Pair<Int, Int> {
        for ((i, op) in ops.withIndex()) {
            if (op is MoveOp && op.from == from && op.to == to) {
                val result = reusedPair.copy(op.fromSection, op.toSection)
                ops.removeAt(i).release()
                return result
            }
        }
        // Should have found a match.
        throw NoMatchedOpException("Could not find a matching op for cmd: MOVE, from: $from, to: $to")
    }

    fun pullAddEventData(positionStart: Int, itemCount: Int) = pullEventData(ADD, positionStart, itemCount)
    fun pullRemoveEventData(positionStart: Int, itemCount: Int) = pullEventData(REMOVE, positionStart, itemCount)

    private fun pullEventData(opCode: Int, positionStart: Int, itemCount: Int): EventData {
        for ((i, op) in ops.withIndex()) {
            if (op is SimpleOp && (op.cmd and opCode > 0) && op.positionStart == positionStart && op.itemCount >= itemCount) {
                op.positionStart += itemCount
                op.itemCount -= itemCount

                val result = reusedEvent.copy(op.cmd, op.section)
                if (op.itemCount == 0) {
                    ops.removeAt(i).release()
                }
                return result
            }
        }
        // Should have found a match
        throw NoMatchedOpException("Could not find a matching op for cmd: ${EventData.stringify(opCode)}, positionStart: $positionStart, itemCount: $itemCount")
    }
}

private class NoMatchedOpException(detailMessage: String) : RuntimeException(detailMessage)

private abstract class Op {
    abstract val cmd: Int

    abstract fun insertInto(ops: ArrayList<Op>)
    /**
     * Apply the effect of an op if it was to move ahead of this one.
     *
     * @param[op] Op to apply.
     * @param[positionInOps] Position of this op in backing list.
     * @param[ops] Backing list to modify if the current op has to change.
     */
    open fun apply(op: SimpleOp, positionInOps: Int, ops: ArrayList<Op>): Int = 0

    abstract fun release()
}

private class SimpleOp(private var _cmd: Int, var section: Int, var positionStart: Int, var itemCount: Int) : Op() {
    companion object {
        private val pool = arrayListOf<SimpleOp>()

        fun acquire(cmd: Int, section: Int, positionStart: Int, itemCount: Int) =
                if (pool.isEmpty()) {
                    SimpleOp(cmd, section, positionStart, itemCount)
                } else {
                    pool.removeAt(0).apply {
                        this._cmd = cmd
                        this.section = section
                        this.positionStart = positionStart
                        this.itemCount = itemCount
                    }
                }

        fun release(obj: SimpleOp) {
            pool.add(obj)
        }
    }

    override val cmd: Int
        get() = _cmd

    override fun release() {
        release(this)
    }

    override fun insertInto(ops: ArrayList<Op>) {
        var steps = 0
        if (!ops.isEmpty()) {
            while (ops[ops.size - steps - 1].cmd == MOVE) {
                steps += 1 + ops[ops.size - steps].apply(this, ops.size - steps, ops)
                if (itemCount == 0) {
                    return
                }
            }
        }
        ops.add(ops.size - steps, this)
    }
}

private class MoveOp private constructor(var fromSection: Int, var from: Int, var toSection: Int, var to: Int) : Op() {
    companion object {
        private val pool = arrayListOf<MoveOp>()

        fun acquire(fromSection: Int, from: Int, toSection: Int, to: Int) =
                if (pool.isEmpty()) {
                    MoveOp(fromSection, from, toSection, to)
                } else {
                    pool.removeAt(0).apply {
                        this.fromSection = fromSection
                        this.from = from
                        this.toSection = toSection
                        this.to = to
                    }
                }

        fun release(obj: MoveOp) {
            pool.add(obj)
        }
    }

    override val cmd: Int
        get() = MOVE

    override fun release() {
        release(this)
    }

    override fun insertInto(ops: ArrayList<Op>) {
        ops.add(this)
    }

    override fun apply(op: SimpleOp, positionInOps: Int, ops: ArrayList<Op>): Int {
        return when (op.cmd) {
            ADD    -> applyAdd(op)
            REMOVE -> applyRemove(op, ops, positionInOps)
            else   -> 0
        }
    }

    fun applyAdd(add: SimpleOp): Int {
        var offset = 0
        if (to < add.positionStart) {
            offset -= 1
        }
        if (from < add.positionStart) {
            offset += 1
        }
        if (add.positionStart <= from) {
            from += add.itemCount
        }
        if (add.positionStart <= to) {
            to += add.itemCount
        }
        add.positionStart += offset

        return 0
    }

    fun applyRemove(remove: SimpleOp, ops: ArrayList<Op>, positionInOps: Int): Int {
        val moveIsBackwards = from < to
        val reverted = if (moveIsBackwards) {
            remove.positionStart == from && remove.itemCount == to - from
        } else {
            remove.positionStart == to + 1 && remove.itemCount == from - to
        }

        if (to < remove.positionStart) {
            remove.positionStart -= 1 // Move op currently only moves one item at a time.
        } else if (to < remove.positionStart + remove.itemCount) {
            // Move is removed, so replace move with a targeted remove action and pump it into ops.
            remove.itemCount -= 1

            ops.removeAt(positionInOps).release()
            SimpleOp.acquire(REMOVE, fromSection, from, 1).insertInto(ops)

            // Send remove back one step of insertion as we removed this op.
            return -1
        }


        // Adjust remove op for undoing effect of move
        if (from <= remove.positionStart) {
            remove.positionStart += 1
        } else if (from < remove.positionStart + remove.itemCount) {
            // Move is in middle of remove so it needs to be split.
            val remaining = remove.positionStart + remove.itemCount - from
            SimpleOp.acquire(REMOVE, remove.section, from + 1, remaining).insertInto(ops)
            remove.itemCount = from - remove.positionStart
        }

        if (reverted) {
            ops.removeAt(positionInOps).release()
            // Send remove back one step of insertion as we removed this op.
            return -1
        }

        // Unlike source logic in AdapterHelper, we don't need to handle extraRm here because it was inserted into
        // ops already.
        if (moveIsBackwards) {
            if (from > remove.positionStart) {
                from -= remove.itemCount
            }
            if (to > remove.positionStart) {
                to -= remove.itemCount
            }
        } else {
            if (from >= remove.positionStart) {
                from -= remove.itemCount
            }
            if (to >= remove.positionStart) {
                to -= remove.itemCount
            }
        }

        if (from == to) {
            ops.removeAt(positionInOps).release()
            return -1
        }
        return 0
    }
}
