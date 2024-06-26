package com.example.piandroid.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LivroDao {
    //Queries
    @Insert
    suspend fun inserirLivro(livro: Livro) //suspend -> programação assincrona

    @Update
    suspend fun atualizarLivro(livro: Livro)

    @Delete
    suspend fun deletarLivro(livro: Livro)

    @Query("SELECT * FROM livro")
    fun todosLivros(): LiveData<List<Livro>>

    @Query("SELECT * FROM livro ORDER BY favorito DESC")
    fun todosLivrosOrdPorFavoritos(): LiveData<List<Livro>>

    @Query("SELECT * FROM livro WHERE LOWER(nome) LIKE '%' || LOWER(:query) || '%'")
    fun procuraLivro(query: String?): LiveData<List<Livro>>

}