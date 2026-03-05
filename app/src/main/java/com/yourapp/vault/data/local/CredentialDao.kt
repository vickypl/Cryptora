package com.yourapp.vault.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials ORDER BY createdAt DESC")
    fun getCredentialsPaged(): PagingSource<Int, CredentialEntity>

    @Query(
        """
        SELECT * FROM credentials
        WHERE description LIKE :query
        ORDER BY createdAt DESC
        """
    )
    fun searchCredentialsPaged(query: String): PagingSource<Int, CredentialEntity>

    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getById(id: String): CredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CredentialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CredentialEntity>)

    @Query("SELECT * FROM credentials ORDER BY updatedAt DESC")
    suspend fun listAll(): List<CredentialEntity>

    @Delete
    suspend fun delete(entity: CredentialEntity)
}
