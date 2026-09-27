// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.flowpay.app.data

import android.os.Parcel
import android.os.Parcelable

data class UPIData(
    var vpa: String,
    val payeeName: String,
    var amount: String,
    val transactionNote: String,
    val currency: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(vpa)
        parcel.writeString(payeeName)
        parcel.writeString(amount)
        parcel.writeString(transactionNote)
        parcel.writeString(currency)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UPIData> {
        override fun createFromParcel(parcel: Parcel): UPIData = UPIData(parcel)
        override fun newArray(size: Int): Array<UPIData?> = arrayOfNulls(size)
    }
}
