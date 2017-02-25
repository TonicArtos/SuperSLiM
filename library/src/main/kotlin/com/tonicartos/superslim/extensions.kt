package com.tonicartos.superslim

import android.os.Parcel
import android.os.Parcelable

inline fun <reified T : Parcelable> createParcel(crossinline create: (Parcel) -> T?) = object : Parcelable.Creator<T> {
    override fun createFromParcel(source: Parcel) = create(source)
    override fun newArray(size: Int) = arrayOfNulls<T?>(size)
}