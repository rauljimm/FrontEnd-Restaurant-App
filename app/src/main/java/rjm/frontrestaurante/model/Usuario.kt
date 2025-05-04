package rjm.frontrestaurante.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para Usuario
 */
@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("nombre")
    val nombre: String,
    
    @SerializedName("apellido")
    val apellido: String,
    
    @SerializedName("rol")
    val rol: String,
    
    @SerializedName("activo")
    val activo: Boolean = true,
    
    @SerializedName("fecha_creacion")
    val fechaCreacion: String
) {
    /**
     * Obtiene el nombre completo del usuario
     */
    fun nombreCompleto(): String = "$nombre $apellido"
} 