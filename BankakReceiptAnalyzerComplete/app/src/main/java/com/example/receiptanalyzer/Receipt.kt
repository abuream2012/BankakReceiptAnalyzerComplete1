
package com.example.receiptanalyzer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id:Int,
    val transactionId:String,
    val name:String,
    val amount:String,
    val note:String
)
