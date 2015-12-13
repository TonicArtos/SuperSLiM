package com.tonicartos.superslim.internal

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View

internal val View.rvLayoutParams: RecyclerView.LayoutParams
    get() = layoutParams as RecyclerView.LayoutParams
