package com.example.animelib.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.animelib.data.entity.TokenEntity;

/**
 * DAO для работы с токенами
 */
@Dao
public interface TokenDao {
    
    @Query("SELECT * FROM tokens WHERE id = 1")
    TokenEntity getToken();
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateToken(TokenEntity token);
    
    @Query("DELETE FROM tokens WHERE id = 1")
    void deleteToken();
    
    @Query("SELECT COUNT(*) FROM tokens WHERE id = 1")
    int getTokenCount();
}
