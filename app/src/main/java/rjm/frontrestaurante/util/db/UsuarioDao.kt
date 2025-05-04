package rjm.frontrestaurante.util.db

import androidx.room.*
import rjm.frontrestaurante.model.Usuario

/**
 * DAO para operaciones de base de datos relacionadas con Usuario
 */
@Dao
interface UsuarioDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: Usuario)
    
    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getUsuarioById(id: Int): Usuario?
    
    @Query("DELETE FROM usuarios")
    suspend fun deleteAllUsuarios()
} 