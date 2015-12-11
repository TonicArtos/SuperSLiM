package com.tonicartos.superslim

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View

val View.rvLayoutParams: RecyclerView.LayoutParams
    get() = layoutParams as RecyclerView.LayoutParams
