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
- 🍽️ **Gestión de productos**: Visualización y administración del menú sin imágenes de vista previa
- 🧾 **Facturación**: Generación de cuentas al finalizar el servicio de mesa
- 📅 **Reservas**: Creación, gestión y eliminación de reservas
- 🌐 **Conexión en tiempo real**: Notificaciones y actualizaciones inmediatas
- 🍔 **Menú hamburguesa**: Navegación global desde cualquier pantalla
- 🛠️ **Creación de elementos**: Interfaces para añadir productos y mesas (admin)

## 🎯 Funcionalidades por rol

### 👨‍💼 Administrador
- Vista completa de todas las mesas
- Gestión de productos y categorías
- Creación de nuevos productos y mesas
- Visualización de todos los pedidos
- Creación y gestión de usuarios
- Estadísticas y reportes
- Gestión completa de reservas

### 🧑‍🍳 Camarero
- Vista de mesas disponibles y ocupadas
- Creación y gestión de pedidos
- Recepción de notificaciones cuando los pedidos están listos
- Marcado de pedidos como entregados
- Cierre de mesas y generación de cuentas
- Gestión de reservas, incluyendo la eliminación de reservas completadas

### 👨‍🍳 Cocinero
- Vista de pedidos pendientes y en preparación
- Actualización del estado de los pedidos
- Notificación a camareros cuando los pedidos están listos

## 🛠️ Tecnologías y bibliotecas

- **Kotlin**: Lenguaje de programación principal
- **Retrofit/OkHttp**: Cliente HTTP para comunicación con la API
- **ViewModel/LiveData**: Gestión de datos y ciclo de vida
- **Coroutines**: Programación asíncrona
- **Navigation Component**: Navegación entre pantallas
- **RecyclerView**: Visualización de listas
- **Material Design**: Componentes de UI modernos
- **GSON**: Serialización JSON
- **DrawerLayout/NavigationView**: Navegación con menú lateral

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

## 📱 Interfaz de Usuario

### Mejoras recientes:
- **Menú hamburguesa global**: Acceso a la navegación desde cualquier pantalla
- **Diseño optimizado**: Interfaz limpia sin imágenes de previsualización en productos
- **Gestión de reservas simplificada**: Botón único para eliminar reservas completadas
- **Modo admin mejorado**: Acceso rápido a creación de productos y mesas

## 🔄 Flujo de trabajo principal

### Para camareros:
1. **Login**: Ingresar con credenciales de camarero
2. **Selección de mesa**: Ver mesas disponibles/ocupadas
3. **Toma de pedido**: Crear nuevo pedido para una mesa
4. **Gestión de pedido**: Visualizar y actualizar estado de pedidos
5. **Entrega**: Marcar pedidos como entregados
6. **Cierre de mesa**: Generar cuenta y liberar mesa
7. **Gestión de reservas**: Eliminar reservas completadas o canceladas

### Para cocineros:
1. **Login**: Ingresar con credenciales de cocinero
2. **Vista de pedidos activos**: Ver todos los pedidos pendientes
3. **Actualización de estado**: Cambiar pedidos a "en preparación" y "listos"

### Para administradores:
1. **Login**: Ingresar con credenciales de administrador
2. **Acceso completo**: Navegar por todas las secciones de la aplicación
3. **Creación de recursos**: Añadir nuevos productos y mesas desde el menú superior
4. **Gestión general**: Administrar todos los aspectos del restaurante

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
- **Autenticación JWT**: Almacenando el token de forma segura para solicitudes futuras

Principales endpoints utilizados:
- `/login`: Autenticación de usuarios
- `/mesas`: Gestión de mesas y su estado
- `/pedidos`: Creación y seguimiento de pedidos
- `/productos`: Visualización y administración del menú
- `/reservas`: Gestión de reservas
- `/categorias`: Organización de productos

## 🔮 Futuras mejoras

- Implementación de modo offline con sincronización
- Soporte para múltiples idiomas
- Integración con sistemas de pago
- Versión tablet optimizada para mesas interactivas
- Notificaciones push para eventos importantes
- Mejora de la interfaz de usuario con animaciones y transiciones
- Optimización del rendimiento en dispositivos de gama baja

## 📧 Contacto

rauljimm.dev@gmail.com
