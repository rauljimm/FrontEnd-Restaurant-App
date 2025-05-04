# 📱 FrontRestaurante - Aplicación Android para Gestión de Restaurantes

[![Kotlin](https://img.shields.io/badge/kotlin-1.8.0+-blue.svg)](https://kotlinlang.org/)
[![Android Studio](https://img.shields.io/badge/Android%20Studio-Flamingo+-green.svg)](https://developer.android.com/studio)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-orange.svg)](https://developer.android.com/about/versions/nougat)

## 📋 Descripción

FrontRestaurante es una aplicación Android desarrollada en Kotlin que sirve como interfaz de usuario para el sistema de gestión de restaurantes. Está diseñada para trabajar con el backend FastAPI, proporcionando una experiencia de usuario fluida y completa para camareros, cocineros y administradores.

## ✨ Características

- 🔐 **Autenticación segura**: Login con credenciales y manejo de tokens JWT
- 🧑‍🍳 **Vistas específicas por rol**: Interfaces personalizadas para camareros, cocineros y administradores
- 🪑 **Gestión de mesas**: Visualización y control del estado de mesas
- 📝 **Gestión de pedidos**: Creación, seguimiento y actualización de pedidos
- 🍽️ **Gestión de productos**: Visualización y administración del menú
- 🧾 **Facturación**: Generación de cuentas al finalizar el servicio de mesa
- 📅 **Reservas**: Creación y gestión de reservas de mesas
- 🌐 **Conexión en tiempo real**: Notificaciones y actualizaciones inmediatas mediante WebSockets

## 🎯 Funcionalidades por rol

### 👨‍💼 Administrador
- Vista completa de todas las mesas
- Gestión de productos y categorías
- Visualización de todos los pedidos
- Creación y gestión de usuarios
- Estadísticas y reportes

### 🧑‍🍳 Camarero
- Vista de mesas disponibles y ocupadas
- Creación y gestión de pedidos
- Recepción de notificaciones cuando los pedidos están listos
- Marcado de pedidos como entregados
- Cierre de mesas y generación de cuentas

### 👨‍🍳 Cocinero
- Vista de pedidos pendientes y en preparación
- Actualización del estado de los pedidos
- Notificación a camareros cuando los pedidos están listos

## 🖼️ Capturas de pantalla

[Aquí se incluirían capturas de pantalla de la aplicación]

## 🛠️ Tecnologías y bibliotecas

- **Kotlin**: Lenguaje de programación principal
- **Jetpack Compose**: Para construcción de interfaz de usuario moderna
- **Retrofit**: Cliente HTTP para comunicación con la API
- **OkHttp**: Cliente HTTP para las peticiones y WebSockets
- **ViewModel/LiveData**: Gestión de datos y ciclo de vida
- **Coroutines**: Programación asíncrona
- **Navigation Component**: Navegación entre pantallas
- **Dagger/Hilt**: Inyección de dependencias
- **Kotlin Serialization**: Serialización JSON
- **Material Design 3**: Componentes de UI modernos

## ⚙️ Requisitos e Instalación

### Requisitos
- Android Studio Flamingo o superior
- Android SDK nivel 24 mínimo (Android 7.0 Nougat)
- JDK 11 o superior
- Backend FastAPI en funcionamiento

### Instalación para desarrollo
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/rauljimm/frontrestaurante.git
   cd frontrestaurante
   ```

2. Abrir el proyecto en Android Studio

3. Configurar la dirección del servidor en `RetrofitClient.kt`:
   ```kotlin
   private const val BASE_URL = "http://tu_ip_servidor:8000/"
   ```

4. Compilar y ejecutar en un dispositivo o emulador

## 🔄 Flujo de trabajo principal

### Para camareros:
1. **Login**: Ingresar con credenciales de camarero
2. **Selección de mesa**: Ver mesas disponibles/ocupadas
3. **Toma de pedido**: Crear nuevo pedido para una mesa
4. **Gestión de pedido**: Visualizar y actualizar estado de pedidos
5. **Entrega**: Marcar pedidos como entregados
6. **Cierre de mesa**: Generar cuenta y liberar mesa

### Para cocineros:
1. **Login**: Ingresar con credenciales de cocinero
2. **Vista de pedidos activos**: Ver todos los pedidos pendientes
3. **Actualización de estado**: Cambiar pedidos a "en preparación" y "listos"

## 🧪 Pruebas

La aplicación incluye pruebas unitarias e instrumentadas:

- **Pruebas unitarias**: Verifican la lógica de negocio y las transformaciones de datos
- **Pruebas instrumentadas**: Comprueban la interfaz de usuario y la integración con el sistema Android

Para ejecutar las pruebas:
```bash
./gradlew test           # Pruebas unitarias
./gradlew connectedTest  # Pruebas instrumentadas
```

## 📊 Estructura del Proyecto

```
app/
├── src/
│   ├── main/
│   │   ├── java/rjm/frontrestaurante/
│   │   │   ├── api/         # Comunicación con la API del backend
│   │   │   ├── model/       # Modelos de datos
│   │   │   ├── ui/          # Interfaces de usuario
│   │   │   │   ├── login/    # Pantallas de login
│   │   │   │   ├── main/     # Actividad principal y navegación
│   │   │   │   ├── mesas/    # Gestión de mesas
│   │   │   │   ├── pedidos/  # Gestión de pedidos
│   │   │   │   ├── productos/# Gestión de productos
│   │   │   │   ├── reservas/ # Gestión de reservas
│   │   │   │   ├── categorias/# Gestión de categorías
│   │   │   │   └── detail/   # Vistas de detalles
│   │   │   ├── util/        # Utilidades y clases auxiliares
│   │   │   ├── adapter/     # Adaptadores para RecyclerView
│   │   │   └── RestauranteApp.kt # Clase de aplicación principal
│   │   ├── res/             # Recursos (layouts, strings, drawables, etc.)
│   │   └── AndroidManifest.xml
│   └── test/                # Pruebas unitarias e instrumentadas
└── build.gradle.kts        # Configuración de dependencias
```

## 🔗 Integración con backend

La aplicación se comunica con el backend FastAPI a través de:

- **REST API**: Para operaciones CRUD estándar
- **WebSockets**: Para notificaciones en tiempo real

La autenticación se realiza mediante JWT, almacenando el token de forma segura para solicitudes futuras.

## 🔮 Futuras mejoras

- Implementación de modo offline con sincronización
- Soporte para múltiples idiomas
- Integración con sistemas de pago
- Versión tablet optimizada para mesas interactivas
- Análisis de datos y estadísticas avanzadas
- Notificaciones push para eventos importantes

## 📧 Contacto

rauljimm.dev@gmail.com