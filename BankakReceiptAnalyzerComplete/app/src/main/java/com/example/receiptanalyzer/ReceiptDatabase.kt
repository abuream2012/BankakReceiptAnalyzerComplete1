
package com.example.receiptanalyzer

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Receipt::class], version = 1)
abstract class ReceiptDatabase: RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
}
