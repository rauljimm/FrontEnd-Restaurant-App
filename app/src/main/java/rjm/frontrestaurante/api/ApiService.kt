package rjm.frontrestaurante.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import rjm.frontrestaurante.model.LoginRequest
import rjm.frontrestaurante.model.LoginResponse
import rjm.frontrestaurante.model.Producto
import rjm.frontrestaurante.model.Usuario
import rjm.frontrestaurante.model.Categoria

/**
 * Interfaz que define los endpoints de la API REST
 */
interface ApiService {
    
    /**
     * Autenticación de usuario
     */
    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    /**
     * Obtener datos del usuario actual
     */
    @GET("usuarios/me")
    suspend fun getUsuarioActual(@Header("Authorization") token: String): Response<Usuario>
    
    /**
     * Obtener lista de productos
     */
    @GET("productos")
    suspend fun getProductos(@Header("Authorization") token: String): Response<List<Producto>>
    
    /**
     * Obtener producto por ID
     */
    @GET("productos/{id}")
    suspend fun getProductoById(
        @Header("Authorization") token: String,
        @Path("id") productoId: Int
    ): Response<Producto>
    
    /**
     * Crear nuevo producto
     */
    @POST("productos")
    suspend fun crearProducto(
        @Header("Authorization") token: String,
        @Body producto: RequestBody
    ): Response<Producto>
    
    /**
     * Obtener lista de categorías
     */
    @GET("categorias")
    suspend fun getCategorias(
        @Header("Authorization") token: String
    ): Response<List<Categoria>>
    
    // Aquí puedes añadir más endpoints según necesites
} 