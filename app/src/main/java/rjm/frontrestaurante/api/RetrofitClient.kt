package rjm.frontrestaurante.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rjm.frontrestaurante.model.EstadoMesa
import rjm.frontrestaurante.model.Mesa
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit para la comunicación con la API
 */
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"
    
    private val okHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Custom GSON converter with Mesa adapter
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Mesa::class.java, MesaDeserializer())
            .registerTypeAdapter(rjm.frontrestaurante.model.Producto::class.java, ProductoDeserializer())
            .registerTypeAdapter(rjm.frontrestaurante.model.Pedido::class.java, PedidoDeserializer())
            .create()
    }
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }
    
    /**
     * Obtiene una instancia del cliente Retrofit
     */
    fun getClient(): Retrofit = retrofit
}

/**
 * Custom deserializer for Mesa that properly handles the estado field
 */
class MesaDeserializer : JsonDeserializer<Mesa> {
    override fun deserialize(
        json: JsonElement, 
        typeOfT: Type, 
        context: JsonDeserializationContext
    ): Mesa {
        val jsonObject = json.asJsonObject
        
        val id = jsonObject.get("id").asInt
        val numero = jsonObject.get("numero").asInt
        val capacidad = jsonObject.get("capacidad").asInt
        
        // Handle the estado field by converting from string to EstadoMesa enum
        val estadoStr = jsonObject.get("estado").asString
        val estado = EstadoMesa.fromString(estadoStr)
        
        // Get ubicacion or default to empty string
        val ubicacion = if (jsonObject.has("ubicacion") && !jsonObject.get("ubicacion").isJsonNull) {
            jsonObject.get("ubicacion").asString
        } else {
            ""
        }
        
        return Mesa(id, numero, capacidad, estado, ubicacion)
    }
}

/**
 * Custom deserializer for Producto that properly handles the tipo field
 */
class ProductoDeserializer : JsonDeserializer<rjm.frontrestaurante.model.Producto> {
    override fun deserialize(
        json: JsonElement, 
        typeOfT: Type, 
        context: JsonDeserializationContext
    ): rjm.frontrestaurante.model.Producto {
        val jsonObject = json.asJsonObject
        
        val id = jsonObject.get("id").asInt
        val nombre = jsonObject.get("nombre").asString
        val descripcion = if (jsonObject.has("descripcion") && !jsonObject.get("descripcion").isJsonNull) 
            jsonObject.get("descripcion").asString else ""
        val precio = jsonObject.get("precio").asDouble
        val categoriaId = jsonObject.get("categoria_id").asInt
        val disponible = if (jsonObject.has("disponible")) jsonObject.get("disponible").asBoolean else true
        val imagen = if (jsonObject.has("imagen_url") && !jsonObject.get("imagen_url").isJsonNull) 
            jsonObject.get("imagen_url").asString else ""
        
        // Handle the tipo field by converting from string to TipoProducto enum
        val tipo = if (jsonObject.has("tipo") && !jsonObject.get("tipo").isJsonNull) {
            val tipoStr = jsonObject.get("tipo").asString
            rjm.frontrestaurante.model.TipoProducto.fromString(tipoStr)
        } else {
            rjm.frontrestaurante.model.TipoProducto.COMIDA
        }
        
        return rjm.frontrestaurante.model.Producto(
            id = id,
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            tipo = tipo,
            categoriaId = categoriaId,
            disponible = disponible,
            imagen = imagen
        )
    }
}

/**
 * Custom deserializer for Pedido that properly handles the estado field
 */
class PedidoDeserializer : JsonDeserializer<rjm.frontrestaurante.model.Pedido> {
    override fun deserialize(
        json: JsonElement, 
        typeOfT: Type, 
        context: JsonDeserializationContext
    ): rjm.frontrestaurante.model.Pedido {
        val jsonObject = json.asJsonObject
        
        val id = jsonObject.get("id").asInt
        val mesaId = jsonObject.get("mesa_id").asInt
        val camareroId = jsonObject.get("camarero_id").asInt
        
        // Handle the estado field by converting from string to EstadoPedido enum
        val estadoStr = jsonObject.get("estado").asString
        val estado = rjm.frontrestaurante.model.EstadoPedido.fromString(estadoStr)
        
        // Get observaciones or default to empty string
        val observaciones = if (jsonObject.has("observaciones") && !jsonObject.get("observaciones").isJsonNull) {
            jsonObject.get("observaciones").asString
        } else {
            ""
        }
        
        // Parse dates
        val fechaStr = jsonObject.get("fecha_creacion").asString
        val fecha = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault())
            .parse(fechaStr) ?: java.util.Date()
        
        // Get total or default to 0.0
        val total = if (jsonObject.has("total") && !jsonObject.get("total").isJsonNull) {
            jsonObject.get("total").asDouble
        } else {
            0.0
        }
        
        // Get detalles if available
        val detalles = if (jsonObject.has("detalles") && !jsonObject.get("detalles").isJsonNull) {
            val detallesArray = jsonObject.getAsJsonArray("detalles")
            val detallesList = mutableListOf<rjm.frontrestaurante.model.DetallePedido>()
            
            for (i in 0 until detallesArray.size()) {
                val detalleJson = detallesArray.get(i).asJsonObject
                
                // Handle the fields and conversion similar to Pedido
                // This can be expanded as needed
                
                // For now, just skip deserializing detalles to keep it simple
            }
            detallesList
        } else {
            emptyList()
        }
        
        return rjm.frontrestaurante.model.Pedido(
            id = id,
            mesaId = mesaId,
            camareroId = camareroId,
            estado = estado,
            observaciones = observaciones,
            fecha = fecha,
            detalles = detalles,
            total = total
        )
    }
} 