
package com.example.receiptanalyzer

import androidx.room.*

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM Receipt")
    suspend fun getAll(): List<Receipt>
    @Insert
    suspend fun insert(receipt: Receipt)
    @Query("DELETE FROM Receipt")
    suspend fun clearAll()
}
