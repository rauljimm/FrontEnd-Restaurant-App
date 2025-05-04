package rjm.frontrestaurante.model

/**
 * Modelo de datos para representar un producto en el restaurante
 */
data class Producto(
    val id: Int,
    val nombre: String,
    val descripcion: String = "",
    val precio: Double,
    val tipo: TipoProducto,
    val categoriaId: Int,
    val disponible: Boolean = true,
    val imagen: String = ""
)

/**
 * Modelo de datos para representar una categoría de productos
 */
data class Categoria(
    val id: Int,
    val nombre: String,
    val descripcion: String = ""
)

/**
 * Enumeración para representar los tipos de productos
 */
enum class TipoProducto {
    COMIDA, BEBIDA, POSTRE, ENTRADA, COMPLEMENTO;
    
    companion object {
        fun fromString(tipo: String): TipoProducto {
            return when (tipo.lowercase()) {
                "comida" -> COMIDA
                "bebida" -> BEBIDA
                "postre" -> POSTRE
                "entrada" -> ENTRADA
                "complemento" -> COMPLEMENTO
                else -> COMIDA
            }
        }
    }
} 