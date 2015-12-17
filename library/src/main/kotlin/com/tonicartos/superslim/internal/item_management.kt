package com.tonicartos.superslim.internal

import java.util.*

private const val HEADER = 1
private const val ADD = 1 shl 1
private const val REMOVE = 1 shl 2
private const val MOVE = 1 shl 3

private val reusedEvent = EventSectionData(0, 0, 0, 0, 0)

internal data class EventSectionData(var action: Int, var section: Int, var position1: Int, var otherSection: Int, var position2: Int) {
    val start: Int get() = position1
    val fromSection: Int get() = section
    val from: Int get() = position1
    val toSection: Int get() = otherSection
    val to: Int get() = position2

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
        if (action and MOVE == 0) {
            return "EventData(cmd = ${stringify(action)}, section = $section, start = $start)"
        } else {
            return "EventData(cmd = ${stringify(action)}, fromSection = $fromSection, from = $from, toSection = $toSection, to = $to)"
        }
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

    fun queueSectionHeaderAdded(section: Int, start: Int, startAp: Int) {
        SimpleOp.acquire(ADD or HEADER, section, start, startAp, 1).insertInto(ops)
    }

    fun queueSectionHeaderRemoved(section: Int, start: Int, startAp: Int) {
        SimpleOp.acquire(REMOVE or HEADER, section, start, startAp, 1).insertInto(ops)
    }

    fun queueSectionItemsAdded(section: Int, start: Int, startAp: Int, itemCount: Int) {
        SimpleOp.acquire(ADD, section, start, startAp, itemCount).insertInto(ops)
    }

    fun queueSectionItemsRemoved(section: Int, start: Int, startAp: Int, itemCount: Int) {
        SimpleOp.acquire(REMOVE, section, start, startAp, itemCount).insertInto(ops)
    }

    fun queueSectionItemsMoved(fromSection: Int, from: Int, fromAp: Int, toSection: Int, to: Int, toAp: Int) {
        MoveOp.acquire(fromSection, from, fromAp, toSection, to, toAp).insertInto(ops)
    }

    fun pullMoveEventData(from: Int, to: Int): EventSectionData {
        for ((i, op) in ops.withIndex()) {
            if (op is MoveOp && op.fromAp == from && op.toAp == to) {
                val result = reusedEvent.copy(MOVE, op.fromSection, op.from, op.toSection, op.to)
                ops.removeAt(i).release()
                return result
            }
        }
        // Should have found a match.
        throw NoMatchedOpException("Could not find a matching op for cmd = MOVE, from = $from, to = $to")
    }

    fun pullAddEventData(positionStart: Int, itemCount: Int) = pullEventData(ADD, positionStart, itemCount)
    fun pullRemoveEventData(positionStart: Int, itemCount: Int) = pullEventData(REMOVE, positionStart, itemCount)

    private fun pullEventData(opCode: Int, positionStart: Int, itemCount: Int): EventSectionData {
        for ((i, op) in ops.withIndex()) {
            if (op is SimpleOp && (op.cmd and opCode > 0) && op.startAp == positionStart && op.itemCount >= itemCount) {
                op.startAp += itemCount
                op.itemCount -= itemCount

                val result = reusedEvent.copy(op.cmd, op.section, op.start)
                if (op.itemCount == 0) {
                    ops.removeAt(i).release()
                }
                return result
            }
        }
        // Should have found a match
        throw NoMatchedOpException("Could not find a matching op for cmd = ${EventSectionData.stringify(opCode)}, positionStart = $positionStart, itemCount = $itemCount")
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

private class SimpleOp(private var _cmd: Int, var section: Int, var start: Int, var startAp: Int, var itemCount: Int) : Op() {
    companion object {
        private val pool = arrayListOf<SimpleOp>()

        fun acquire(cmd: Int, section: Int, start: Int, startAp: Int, itemCount: Int) =
                if (pool.isEmpty()) {
                    SimpleOp(cmd, section, start, startAp, itemCount)
                } else {
                    pool.removeAt(0).apply {
                        this._cmd = cmd
                        this.section = section
                        this.start = start
                        this.startAp = startAp
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

private class MoveOp private constructor(var fromSection: Int, var from: Int, var fromAp: Int, var toSection: Int, var to: Int, var toAp: Int) : Op() {
    companion object {
        private val pool = arrayListOf<MoveOp>()

        fun acquire(fromSection: Int, from: Int, fromAp: Int, toSection: Int, to: Int, toAp: Int) =
                if (pool.isEmpty()) {
                    MoveOp(fromSection, from, fromAp, toSection, to, toAp)
                } else {
                    pool.removeAt(0).apply {
                        this.fromSection = fromSection
                        this.from = from
                        this.fromAp = fromAp
                        this.toSection = toSection
                        this.to = to
                        this.toAp = toAp
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
        val toOffsetForAdd = if (toAp < add.startAp) -1 else 0
        val fromOffsetForAdd = if (fromAp < add.startAp) 1 else 0

        val toOffsetForMove = if (add.startAp <= toAp) add.itemCount else 0
        val fromOffsetForMove = if (add.startAp <= fromAp) add.itemCount else 0

        toAp += toOffsetForMove
        fromAp += fromOffsetForMove
        add.startAp += toOffsetForAdd + fromOffsetForAdd

        if (add.section == toSection) {
            add.start += toOffsetForAdd
            to += toOffsetForMove
        }
        if (add.section == fromSection) {
            add.start += fromOffsetForAdd
            from += fromOffsetForMove
        }

        return 0
    }

    fun applyRemove(remove: SimpleOp, ops: ArrayList<Op>, positionInOps: Int): Int {
        val toSameSection = remove.section == toSection
        val fromSameSection = remove.section == fromSection

        val moveIsBackwards = fromAp < toAp
        val reverted = if (moveIsBackwards) {
            remove.startAp == fromAp && remove.itemCount == toAp - fromAp
        } else {
            remove.startAp == toAp + 1 && remove.itemCount == fromAp - toAp
        }

        if (toAp < remove.startAp) {
            remove.startAp -= 1 // Move op currently only moves one item at a time.
            if (toSameSection) remove.start -= 1
        } else if (toAp < remove.startAp + remove.itemCount) {
            // Move is removed, so replace move with a targeted remove action and pump it into ops.
            remove.itemCount -= 1

            ops.removeAt(positionInOps).release()
            SimpleOp.acquire(REMOVE, fromSection, from, fromAp, 1).insertInto(ops)

            // Send remove back one step of insertion as we removed this op.
            return -1
        }


        // Adjust remove op for undoing effect of move
        if (fromAp <= remove.startAp) {
            remove.startAp += 1
            if (fromSameSection) remove.start += 1
        } else if (fromAp < remove.startAp + remove.itemCount) {
            // Move is in middle of remove so it needs to be split.
            val remaining = remove.startAp + remove.itemCount - fromAp
            SimpleOp.acquire(REMOVE, remove.section, if (fromSameSection) from + 1 else from, fromAp + 1, remaining).insertInto(ops)
            remove.itemCount = fromAp - remove.startAp
        }

        if (reverted) {
            ops.removeAt(positionInOps).release()
            // Send remove back one step of insertion as we removed this op.
            return -1
        }

        // Unlike source logic in AdapterHelper, we don't need toAp handle extraRm here because it was inserted intoAp
        // ops already.
        if (moveIsBackwards) {
            if (fromAp > remove.startAp) {
                fromAp -= remove.itemCount
                if (fromSameSection) from -= remove.itemCount
            }
            if (toAp > remove.startAp) {
                toAp -= remove.itemCount
                if (toSameSection) to -= remove.itemCount
            }
        } else {
            if (fromAp >= remove.startAp) {
                fromAp -= remove.itemCount
                if (fromSameSection) from -= remove.itemCount
            }
            if (toAp >= remove.startAp) {
                toAp -= remove.itemCount
                if (toSameSection) to -= remove.itemCount
            }
        }

        if (fromAp == toAp) {
            ops.removeAt(positionInOps).release()
            return -1
        }
        return 0
    }
}
