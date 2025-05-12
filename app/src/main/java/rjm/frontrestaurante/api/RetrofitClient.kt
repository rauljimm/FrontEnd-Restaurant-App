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
import java.util.Date

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
            .addInterceptor { chain ->
                val request = chain.request()
                android.util.Log.d("OkHttp", "Request URL: ${request.url}")
                
                try {
                    val response = chain.proceed(request)
                    
                    // Log de respuesta para debugging
                    if (!response.isSuccessful) {
                        if (response.code == 500) {
                            android.util.Log.e("OkHttp", "HTTP 500 Error for URL: ${request.url}")
                            val responseBody = response.peekBody(1024 * 1024)
                            val errorContent = responseBody.string()
                            android.util.Log.e("OkHttp", "Error response body: $errorContent")

                            // Log additional details
                            android.util.Log.e("OkHttp", "Request method: ${request.method}")
                            android.util.Log.e("OkHttp", "Request headers: ${request.headers}")
                            
                            if (request.body != null) {
                                android.util.Log.e("OkHttp", "Request has body")
                            }
                        } else {
                            android.util.Log.w("OkHttp", "HTTP ${response.code} Error for URL: ${request.url}")
                        }
                    }
                    
                    response
                } catch (e: Exception) {
                    android.util.Log.e("OkHttp", "Error in request: ${e.message}", e)
                    throw e
                }
            }
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    // Custom GSON converter with Mesa adapter
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Mesa::class.java, MesaDeserializer())
            .registerTypeAdapter(rjm.frontrestaurante.model.Producto::class.java, ProductoDeserializer())
            .registerTypeAdapter(rjm.frontrestaurante.model.Pedido::class.java, PedidoDeserializer())
            .registerTypeAdapter(rjm.frontrestaurante.model.Cuenta::class.java, CuentaDeserializer())
            .registerTypeAdapter(rjm.frontrestaurante.model.Categoria::class.java, CategoriaDeserializer())
            .registerTypeAdapter(rjm.frontrestaurante.model.Reserva::class.java, ReservaDeserializer())
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
        
        android.util.Log.d("MesaDeserializer", "Raw JSON: $jsonObject")
        
        try {
            // Mandatory fields with defaults in case of errors
            val id = if (jsonObject.has("id") && !jsonObject.get("id").isJsonNull) {
                try {
                    jsonObject.get("id").asInt
                } catch (e: Exception) {
                    android.util.Log.e("MesaDeserializer", "Error parsing id: ${e.message}", e)
                    0
                }
            } else {
                0
            }
            
            val numero = if (jsonObject.has("numero") && !jsonObject.get("numero").isJsonNull) {
                try {
                    jsonObject.get("numero").asInt
                } catch (e: Exception) {
                    android.util.Log.e("MesaDeserializer", "Error parsing numero: ${e.message}", e)
                    0
                }
            } else {
                0
            }
            
            val capacidad = if (jsonObject.has("capacidad") && !jsonObject.get("capacidad").isJsonNull) {
                try {
                    jsonObject.get("capacidad").asInt
                } catch (e: Exception) {
                    android.util.Log.e("MesaDeserializer", "Error parsing capacidad: ${e.message}", e)
                    0
                }
            } else {
                0
            }
            
            // Handle the estado field by converting from string to EstadoMesa enum
            val estadoStr = if (jsonObject.has("estado") && !jsonObject.get("estado").isJsonNull) {
                jsonObject.get("estado").asString
            } else {
                "libre" // Default value
            }
            val estado = EstadoMesa.fromString(estadoStr)
            
            // Get ubicacion or default to empty string
            val ubicacion = if (jsonObject.has("ubicacion") && !jsonObject.get("ubicacion").isJsonNull) {
                try {
                    jsonObject.get("ubicacion").asString
                } catch (e: Exception) {
                    android.util.Log.e("MesaDeserializer", "Error parsing ubicacion: ${e.message}", e)
                    ""
                }
            } else {
                ""
            }
            
            return Mesa(id, numero, capacidad, estado, ubicacion)
        } catch (e: Exception) {
            android.util.Log.e("MesaDeserializer", "Error deserializing Mesa: ${e.message}", e)
            // Return a default Mesa object
            return Mesa(0, 0, 0, EstadoMesa.LIBRE, "")
        }
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
        
        // Agregar logs para depuración
        android.util.Log.d("ProductoDeserializer", "Deserializando producto: $jsonObject")
        
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
        
        val producto = rjm.frontrestaurante.model.Producto(
            id = id,
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            tipo = tipo,
            categoriaId = categoriaId,
            disponible = disponible,
            imagen = imagen
        )
        
        android.util.Log.d("ProductoDeserializer", "Producto deserializado: $producto")
        return producto
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
        
        // Log para depuración
        android.util.Log.d("PedidoDeserializer", "Deserializando pedido: $jsonObject")
        
        try {
            val id = jsonObject.get("id").asInt
            
            // Manejar mesaId que podría ser nulo
            val mesaId = if (jsonObject.has("mesa_id") && !jsonObject.get("mesa_id").isJsonNull) {
                try {
                    jsonObject.get("mesa_id").asInt
                } catch (e: Exception) {
                    android.util.Log.e("PedidoDeserializer", "Error parsing mesa_id: ${e.message}")
                    null
                }
            } else {
                null
            }
            
            // Manejar camareroId que podría causar problemas
            val camareroId = if (jsonObject.has("camarero_id") && !jsonObject.get("camarero_id").isJsonNull) {
                try {
                    jsonObject.get("camarero_id").asInt
                } catch (e: Exception) {
                    android.util.Log.e("PedidoDeserializer", "Error parsing camarero_id: ${e.message}")
                    null // Ahora devolvemos null en lugar de 0
                }
            } else {
                null // Valor por defecto es null
            }
            
            // Handle the estado field by converting from string to EstadoPedido enum
            val estadoStr = if (jsonObject.has("estado") && !jsonObject.get("estado").isJsonNull) {
                jsonObject.get("estado").asString
            } else {
                "recibido" // Default value
            }
            
            val estado = try {
                rjm.frontrestaurante.model.EstadoPedido.fromString(estadoStr)
            } catch (e: Exception) {
                android.util.Log.e("PedidoDeserializer", "Error parsing estado: ${e.message}")
                rjm.frontrestaurante.model.EstadoPedido.RECIBIDO // Default value
            }
            
            // Get observaciones or default to empty string
            val observaciones = if (jsonObject.has("observaciones") && !jsonObject.get("observaciones").isJsonNull) {
                jsonObject.get("observaciones").asString
            } else {
                ""
            }
            
            // Parse dates with better error handling
            val fechaStr = if (jsonObject.has("fecha_creacion") && !jsonObject.get("fecha_creacion").isJsonNull) {
                jsonObject.get("fecha_creacion").asString
            } else {
                // Default to current date if not found
                android.util.Log.w("PedidoDeserializer", "fecha_creacion not found, using current date")
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault())
                    .format(java.util.Date())
            }
            
            val fecha = try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault())
                    .parse(fechaStr) ?: java.util.Date()
            } catch (e: Exception) {
                android.util.Log.e("PedidoDeserializer", "Error parsing fecha: ${e.message}")
                java.util.Date() // Default to current date
            }
            
            // Get total or default to 0.0
            val total = if (jsonObject.has("total") && !jsonObject.get("total").isJsonNull) {
                try {
                    jsonObject.get("total").asDouble
                } catch (e: Exception) {
                    android.util.Log.e("PedidoDeserializer", "Error parsing total: ${e.message}")
                    0.0 // Default value
                }
            } else {
                0.0
            }
            
            // Get detalles if available
            val detalles = if (jsonObject.has("detalles") && !jsonObject.get("detalles").isJsonNull) {
                val detallesArray = jsonObject.getAsJsonArray("detalles")
                val detallesList = mutableListOf<rjm.frontrestaurante.model.DetallePedido>()
                
                for (i in 0 until detallesArray.size()) {
                    try {
                        val detalleJson = detallesArray.get(i).asJsonObject
                        
                        val detalleId = detalleJson.get("id").asInt
                        val detallePedidoId = id // El pedido_id es el id del pedido actual
                        val productoId = detalleJson.get("producto_id").asInt
                        val cantidad = detalleJson.get("cantidad").asInt
                        
                        // Estado del detalle
                        val detalleEstadoStr = if (detalleJson.has("estado") && !detalleJson.get("estado").isJsonNull) {
                            detalleJson.get("estado").asString
                        } else {
                            "pendiente" // Default value
                        }
                        
                        val detalleEstado = try {
                            rjm.frontrestaurante.model.EstadoDetallePedido.fromString(detalleEstadoStr)
                        } catch (e: Exception) {
                            android.util.Log.e("PedidoDeserializer", "Error parsing detalle estado: ${e.message}")
                            rjm.frontrestaurante.model.EstadoDetallePedido.PENDIENTE // Default value
                        }
                        
                        // Observaciones
                        val detalleObservaciones = if (detalleJson.has("observaciones") && !detalleJson.get("observaciones").isJsonNull) {
                            detalleJson.get("observaciones").asString
                        } else {
                            ""
                        }
                        
                        // Extraer información del producto si está disponible
                        var producto: rjm.frontrestaurante.model.Producto? = null
                        if (detalleJson.has("producto") && !detalleJson.get("producto").isJsonNull) {
                            val productoJson = detalleJson.getAsJsonObject("producto")
                            try {
                                producto = context.deserialize(productoJson, rjm.frontrestaurante.model.Producto::class.java)
                            } catch (e: Exception) {
                                android.util.Log.e("PedidoDeserializer", "Error deserializando producto en detalle: ${e.message}")
                            }
                        }
                        
                        val detalle = rjm.frontrestaurante.model.DetallePedido(
                            id = detalleId,
                            pedidoId = detallePedidoId,
                            productoId = productoId,
                            cantidad = cantidad,
                            observaciones = detalleObservaciones,
                            estado = detalleEstado,
                            producto = producto
                        )
                        
                        detallesList.add(detalle)
                    } catch (e: Exception) {
                        android.util.Log.e("PedidoDeserializer", "Error deserializando detalle: ${e.message}")
                        // Continuar con el siguiente detalle si hay un error
                    }
                }
                detallesList
            } else {
                emptyList()
            }
            
            val pedido = rjm.frontrestaurante.model.Pedido(
                id = id,
                mesaId = mesaId,
                camareroId = camareroId,
                estado = estado,
                observaciones = observaciones,
                fecha = fecha,
                detalles = detalles,
                total = total
            )
            
            android.util.Log.d("PedidoDeserializer", "Pedido deserializado: $pedido con ${detalles.size} detalles")
            return pedido
        } catch (e: Exception) {
            android.util.Log.e("PedidoDeserializer", "Error general deserializando pedido: ${e.message}")
            // Retornamos un pedido vacío en caso de error para evitar errores de la app
            return rjm.frontrestaurante.model.Pedido(
                id = 0,
                mesaId = null,
                camareroId = null,
                estado = rjm.frontrestaurante.model.EstadoPedido.RECIBIDO,
                observaciones = "Error parsing pedido",
                fecha = java.util.Date(),
                detalles = emptyList(),
                total = 0.0
            )
        }
    }
}

/**
 * Custom deserializer for Cuenta that properly handles the date and details
 */
class CuentaDeserializer : JsonDeserializer<rjm.frontrestaurante.model.Cuenta> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): rjm.frontrestaurante.model.Cuenta {
        val jsonObject = json.asJsonObject

        val id = jsonObject.get("id").asInt
        val mesaId = if (jsonObject.has("mesa_id") && !jsonObject.get("mesa_id").isJsonNull) jsonObject.get("mesa_id").asInt else null
        val numeroMesa = jsonObject.get("numero_mesa").asInt
        val camareroId = if (jsonObject.has("camarero_id") && !jsonObject.get("camarero_id").isJsonNull) jsonObject.get("camarero_id").asInt else null
        val nombreCamarero = jsonObject.get("nombre_camarero").asString
        
        val fechaCobro = try {
            val fechaString = jsonObject.get("fecha_cobro").asString
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            format.parse(fechaString) ?: Date()
        } catch (e: Exception) {
            // En caso de error, usar la fecha actual
            Date()
        }
        
        val total = jsonObject.get("total").asDouble
        val metodoPago = if (jsonObject.has("metodo_pago") && !jsonObject.get("metodo_pago").isJsonNull) jsonObject.get("metodo_pago").asString else null
        
        // Deserializar detalles (que vendrán como un JSON string o null)
        val detalles = mutableListOf<rjm.frontrestaurante.model.DetalleCuenta>()
        if (jsonObject.has("detalles") && !jsonObject.get("detalles").isJsonNull) {
            try {
                // Verificar si detalles es una cadena o un array JSON directamente
                val detallesElement = jsonObject.get("detalles")
                
                if (detallesElement.isJsonPrimitive && detallesElement.asJsonPrimitive.isString) {
                    // Es una cadena JSON, intentamos parsearla
                    val detallesJsonString = detallesElement.asString
                    if (detallesJsonString.isNotEmpty()) {
                        try {
                            val detallesJsonArray = com.google.gson.JsonParser().parse(detallesJsonString).asJsonArray
                            
                            for (detalleElement in detallesJsonArray) {
                                parseDetalleElementToDetalleCuenta(detalleElement, detalles)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CuentaDeserializer", "Error parseando detalles como string JSON: ${e.message}")
                        }
                    }
                } else if (detallesElement.isJsonArray) {
                    // Es un array JSON directamente
                    val detallesJsonArray = detallesElement.asJsonArray
                    
                    for (detalleElement in detallesJsonArray) {
                        parseDetalleElementToDetalleCuenta(detalleElement, detalles)
                    }
                } else if (detallesElement.isJsonObject) {
                    // Es un solo objeto JSON, podría ocurrir en algunos casos
                    parseDetalleElementToDetalleCuenta(detallesElement, detalles)
                }
            } catch (e: Exception) {
                android.util.Log.e("CuentaDeserializer", "Error parseando detalles: ${e.message}", e)
                // En caso de error, continuar con una lista vacía
            }
        } else {
            // Si detalles es null o no existe, usar una lista vacía
            android.util.Log.i("CuentaDeserializer", "No hay detalles para esta cuenta o son null")
        }
        
        return rjm.frontrestaurante.model.Cuenta(
            id = id,
            mesaId = mesaId,
            numeroMesa = numeroMesa,
            camareroId = camareroId,
            nombreCamarero = nombreCamarero,
            fechaCobro = fechaCobro,
            total = total,
            metodoPago = metodoPago,
            detalles = detalles
        )
    }
    
    private fun parseDetalleElementToDetalleCuenta(
        detalleElement: JsonElement,
        detallesList: MutableList<rjm.frontrestaurante.model.DetalleCuenta>
    ) {
        try {
            val detalleObj = detalleElement.asJsonObject
            
            // Validar que todos los campos requeridos existen antes de crear el objeto
            if (detalleObj.has("pedido_id") && 
                detalleObj.has("producto_id") && 
                detalleObj.has("nombre_producto") && 
                detalleObj.has("cantidad") &&
                detalleObj.has("precio_unitario") &&
                detalleObj.has("subtotal")) {
                
                val detalle = rjm.frontrestaurante.model.DetalleCuenta(
                    pedidoId = detalleObj.get("pedido_id").asInt,
                    productoId = detalleObj.get("producto_id").asInt,
                    nombreProducto = detalleObj.get("nombre_producto").asString,
                    cantidad = detalleObj.get("cantidad").asInt,
                    precioUnitario = detalleObj.get("precio_unitario").asDouble,
                    subtotal = detalleObj.get("subtotal").asDouble,
                    observaciones = if (detalleObj.has("observaciones") && !detalleObj.get("observaciones").isJsonNull) 
                        detalleObj.get("observaciones").asString else null
                )
                detallesList.add(detalle)
            } else {
                android.util.Log.w("CuentaDeserializer", "Detalle de cuenta incompleto: $detalleObj")
            }
        } catch (e: Exception) {
            android.util.Log.e("CuentaDeserializer", "Error parseando elemento de detalle: ${e.message}")
        }
    }
}

/**
 * Custom deserializer for Categoria that properly handles the fields
 */
class CategoriaDeserializer : JsonDeserializer<rjm.frontrestaurante.model.Categoria> {
    override fun deserialize(
        json: JsonElement, 
        typeOfT: Type, 
        context: JsonDeserializationContext
    ): rjm.frontrestaurante.model.Categoria {
        val jsonObject = json.asJsonObject
        
        // Agregar logs para depuración
        android.util.Log.d("CategoriaDeserializer", "Deserializando categoría: $jsonObject")
        
        val id = jsonObject.get("id").asInt
        val nombre = jsonObject.get("nombre").asString
        val descripcion = if (jsonObject.has("descripcion") && !jsonObject.get("descripcion").isJsonNull) {
            jsonObject.get("descripcion").asString
        } else {
            ""
        }
        
        val categoria = rjm.frontrestaurante.model.Categoria(
            id = id,
            nombre = nombre,
            descripcion = descripcion
        )
        
        android.util.Log.d("CategoriaDeserializer", "Categoría deserializada: $categoria")
        return categoria
    }
}

/**
 * Custom deserializer for Reserva that properly handles the date fields
 */
class ReservaDeserializer : JsonDeserializer<rjm.frontrestaurante.model.Reserva> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): rjm.frontrestaurante.model.Reserva {
        val jsonObject = json.asJsonObject

        val id = jsonObject.get("id").asInt
        val mesaId = if (jsonObject.has("mesa_id") && !jsonObject.get("mesa_id").isJsonNull) 
            jsonObject.get("mesa_id").asInt else 0
            
        val clienteNombre = jsonObject.get("cliente_nombre").asString
        val clienteTelefono = jsonObject.get("cliente_telefono").asString
        
        // Extraer la fecha y hora
        val fechaStr = jsonObject.get("fecha").asString
        // Intentar parsear la fecha con diferentes formatos posibles
        val fecha = try {
            val isoDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            isoDateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            isoDateFormat.parse(fechaStr) ?: Date()
        } catch (e1: Exception) {
            try {
                val altDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault())
                altDateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                altDateFormat.parse(fechaStr) ?: Date()
            } catch (e2: Exception) {
                try {
                    val simpleFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    simpleFormat.parse(fechaStr) ?: Date()
                } catch (e3: Exception) {
                    Date() // Último recurso: usar la fecha actual
                }
            }
        }
        
        // Extraer la hora del string de fecha para mostrarla por separado
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val hora = timeFormat.format(fecha)
        
        val numPersonas = jsonObject.get("num_personas").asInt
        val observaciones = if (jsonObject.has("observaciones") && !jsonObject.get("observaciones").isJsonNull) 
            jsonObject.get("observaciones").asString else ""
            
        // Manejar estado
        val estado = if (jsonObject.has("estado") && !jsonObject.get("estado").isJsonNull) {
            val estadoStr = jsonObject.get("estado").asString
            rjm.frontrestaurante.model.EstadoReserva.fromString(estadoStr)
        } else {
            rjm.frontrestaurante.model.EstadoReserva.PENDIENTE
        }

        return rjm.frontrestaurante.model.Reserva(
            id = id,
            mesaId = mesaId,
            clienteNombre = clienteNombre,
            clienteTelefono = clienteTelefono,
            fecha = fecha,
            hora = hora,
            numPersonas = numPersonas,
            observaciones = observaciones,
            estado = estado
        )
    }
} 