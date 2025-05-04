package rjm.frontrestaurante.util.db

import androidx.room.Database
import androidx.room.RoomDatabase
import rjm.frontrestaurante.model.Usuario

/**
 * Base de datos local de la aplicación utilizando Room
 */
@Database(
    entities = [Usuario::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun usuarioDao(): UsuarioDao
} 