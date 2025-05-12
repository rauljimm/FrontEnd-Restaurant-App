package rjm.frontrestaurante.ui.main

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.ActivityMainBinding
import rjm.frontrestaurante.util.SessionManager

/**
 * Actividad principal que contiene la navegación entre fragmentos
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var viewModel: MainViewModel
    private lateinit var userRole: String
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configurar Toolbar
        setSupportActionBar(binding.toolbar)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        
        // Configurar Navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Añadir listener para detectar cambios de destino de navegación
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "Navegación a: ${destination.label}")
            // Forzar actualización del menú cuando cambia el destino
            invalidateOptionsMenu()
        }
        
        // Obtener el rol del usuario
        userRole = SessionManager.getUserRole()
        Log.d(TAG, "Rol de usuario: $userRole, Token: ${SessionManager.getToken()?.take(10) ?: "null"}")
        
        // Configurar DrawerLayout y NavigationView
        binding.drawerLayout?.let { drawer ->
            // Configurar todos los fragmentos como destinos para mostrar hamburguesa en vez de flecha atrás
            val allDestinations = setOf(
                R.id.mainFragment, 
                R.id.mesasFragment, 
                R.id.pedidosActivosFragment,
                R.id.cuentasFragment,
                R.id.reservasFragment,
                R.id.categoriasFragment,
                R.id.detalleMesaFragment,
                R.id.detallePedidoFragment,
                R.id.detailFragment,
                R.id.nuevaMesaFragment,
                R.id.nuevaReservaFragment,
                R.id.nuevaCategoriaFragment,
                R.id.nuevoPedidoFragment,
                R.id.agregarProductoFragment,
                R.id.detalleCuentaFragment,
                R.id.usuariosFragment
            )
            
            appBarConfiguration = AppBarConfiguration(
                allDestinations,
                drawer
            )
            
            // Configurar ActionBarDrawerToggle para el menú de hamburguesa
            drawerToggle = ActionBarDrawerToggle(
                this, drawer, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )
            drawer.addDrawerListener(drawerToggle)
            
            // Añadir listener para actualizar el menú siempre que se abra el drawer
            drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
                
                override fun onDrawerOpened(drawerView: View) {
                    // Actualizar el menú cada vez que se abre el drawer
                    binding.navView?.let { navView ->
                        configureNavigationMenu(navView)
                    }
                    Log.d(TAG, "Drawer abierto: Menú actualizado con rol actual")
                }
                
                override fun onDrawerClosed(drawerView: View) {}
                
                override fun onDrawerStateChanged(newState: Int) {}
            })
            
            drawerToggle.syncState()
            
            setupActionBarWithNavController(navController, appBarConfiguration)
            binding.navView?.setupWithNavController(navController)
            
            // Configurar acciones adicionales del menú
            binding.navView?.let { navView ->
                // Configurar visibilidad de elementos del menú según el rol actual
                configureNavigationMenu(navView)
                
                // Listener para elementos del menú
                navView.setNavigationItemSelectedListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.nav_productos -> {
                            navController.navigate(R.id.mainFragment)
                            drawer.closeDrawer(GravityCompat.START)
                            true
                        }
                        R.id.nav_mesas -> {
                            navController.navigate(R.id.mesasFragment)
                            drawer.closeDrawer(GravityCompat.START)
                            true
                        }
                        R.id.nav_pedidos_activos -> {
                            navController.navigate(R.id.pedidosActivosFragment)
                            drawer.closeDrawer(GravityCompat.START)
                            true
                        }
                        R.id.nav_cuentas -> {
                            navController.navigate(R.id.cuentasFragment)
                            drawer.closeDrawer(GravityCompat.START)
                            true
                        }
                        R.id.nav_reservas -> {
                            navController.navigate(R.id.reservasFragment)
                            drawer.closeDrawer(GravityCompat.START)
                            true
                        }
                        R.id.nav_usuarios -> {
                            navController.navigate(R.id.usuariosFragment)
                            drawer.closeDrawer(GravityCompat.START)
                            true
                        }
                        R.id.nav_logout -> {
                            // Cierra sesión y navega al login usando la acción global
                            viewModel.cerrarSesion()
                            
                            // Crear bundle con argumento from_logout
                            val bundle = Bundle()
                            bundle.putBoolean("from_logout", true)
                            navController.navigate(R.id.action_global_to_loginFragment, bundle)
                            drawer.closeDrawer(GravityCompat.START)
                            true
                        }
                        else -> {
                            drawer.closeDrawer(GravityCompat.START)
                            false
                        }
                    }
                }
            }
        }
        
        // Observar mensajes de error
        viewModel.error.observe(this) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    override fun onBackPressed() {
        // Cerrar el drawer si está abierto, de lo contrario comportamiento normal
        if (binding.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    
    /**
     * Crear menú de opciones
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        // Obtener el rol de usuario actualizado directamente desde SessionManager
        val currentRole = SessionManager.getUserRole()
        Log.d(TAG, "onCreateOptionsMenu - Rol de usuario actual: $currentRole")
        
        // Configurar visibilidad de las opciones según el rol
        when (currentRole) {
            "admin" -> {
                // Admin ve todas las opciones
                menu?.findItem(R.id.action_refresh)?.isVisible = true
                menu?.findItem(R.id.action_add_producto)?.isVisible = true
                menu?.findItem(R.id.action_add_mesa)?.isVisible = true
                menu?.findItem(R.id.action_historial_cuentas)?.isVisible = true
                menu?.findItem(R.id.action_logout)?.isVisible = true
                Log.d(TAG, "Mostrando opciones de administrador en el menú")
            }
            "camarero" -> {
                // Camarero solo ve cerrar sesión y refrescar
                menu?.findItem(R.id.action_refresh)?.isVisible = true
                menu?.findItem(R.id.action_add_producto)?.isVisible = false
                menu?.findItem(R.id.action_add_mesa)?.isVisible = false
                menu?.findItem(R.id.action_historial_cuentas)?.isVisible = false
                menu?.findItem(R.id.action_logout)?.isVisible = true
                Log.d(TAG, "Mostrando opciones limitadas para camarero")
            }
            else -> {
                // Otros roles (como cocinero) solo ven cerrar sesión y refrescar
                menu?.findItem(R.id.action_refresh)?.isVisible = true
                menu?.findItem(R.id.action_add_producto)?.isVisible = false
                menu?.findItem(R.id.action_add_mesa)?.isVisible = false
                menu?.findItem(R.id.action_historial_cuentas)?.isVisible = false
                menu?.findItem(R.id.action_logout)?.isVisible = true
                Log.d(TAG, "Ocultando opciones administrativas. Rol actual: $currentRole")
            }
        }
        
        return true
    }
    
    /**
     * Manejar selección de opciones del menú
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Gestionar botón hamburguesa
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        
        return when (item.itemId) {
            R.id.action_refresh -> {
                // Notificar al fragmento actual que debe actualizar sus datos
                supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.get(0)?.let {
                    if (it is Refreshable) {
                        it.onRefresh()
                    }
                }
                true
            }
            R.id.action_add_producto -> {
                // Navegar al fragmento para crear producto (solo admin)
                try {
                    navController.navigate(R.id.agregarProductoFragment)
                    Log.d(TAG, "Navegando al fragmento para crear producto")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al navegar a agregarProductoFragment: ${e.message}")
                    Toast.makeText(this, "Error al abrir la pantalla de creación de producto", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_add_mesa -> {
                // Navegar al fragmento para crear mesa (solo admin)
                try {
                    navController.navigate(R.id.nuevaMesaFragment)
                    Log.d(TAG, "Navegando al fragmento para crear mesa")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al navegar a nuevaMesaFragment: ${e.message}")
                    Toast.makeText(this, "Error al abrir la pantalla de creación de mesa", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_historial_cuentas -> {
                // Navegar al fragmento de historial de cuentas (solo admin)
                navController.navigate(R.id.cuentasFragment)
                Log.d(TAG, "Navegando al fragmento de historial de cuentas")
                true
            }
            R.id.action_logout -> {
                // Cierra sesión y navega al login usando la acción global
                viewModel.cerrarSesion()
                
                // Crear bundle con argumento from_logout
                val bundle = Bundle()
                bundle.putBoolean("from_logout", true)
                navController.navigate(R.id.action_global_to_loginFragment, bundle)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Configura el menú de navegación según el rol actual del usuario
     */
    private fun configureNavigationMenu(navView: NavigationView) {
        val currentRole = SessionManager.getUserRole()
        Log.d(TAG, "Configurando menú de navegación para rol: $currentRole")
        
        val menu = navView.menu
        when (currentRole.uppercase()) {
            "ADMIN" -> {
                // Administrador ve productos, categorías, cuentas, mesas, reservas y usuarios
                menu.findItem(R.id.nav_productos)?.isVisible = true
                menu.findItem(R.id.nav_mesas)?.isVisible = true
                menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
                menu.findItem(R.id.nav_cuentas)?.isVisible = true
                menu.findItem(R.id.nav_reservas)?.isVisible = true
                menu.findItem(R.id.nav_usuarios)?.isVisible = true
            }
            "CAMARERO" -> {
                // Camarero ve mesas, pedidos activos, productos y reservas
                menu.findItem(R.id.nav_productos)?.isVisible = true
                menu.findItem(R.id.nav_mesas)?.isVisible = true
                menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
                menu.findItem(R.id.nav_cuentas)?.isVisible = false
                menu.findItem(R.id.nav_reservas)?.isVisible = true
                menu.findItem(R.id.nav_usuarios)?.isVisible = false
            }
            "COCINERO" -> {
                // Cocinero ve solo pedidos activos
                menu.findItem(R.id.nav_productos)?.isVisible = false
                menu.findItem(R.id.nav_mesas)?.isVisible = false
                menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
                menu.findItem(R.id.nav_cuentas)?.isVisible = false
                menu.findItem(R.id.nav_reservas)?.isVisible = false
                menu.findItem(R.id.nav_usuarios)?.isVisible = false
            }
            else -> {
                // Rol desconocido, mostrar opciones mínimas
                menu.findItem(R.id.nav_productos)?.isVisible = false
                menu.findItem(R.id.nav_mesas)?.isVisible = false
                menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
                menu.findItem(R.id.nav_cuentas)?.isVisible = false
                menu.findItem(R.id.nav_reservas)?.isVisible = false
                menu.findItem(R.id.nav_usuarios)?.isVisible = false
            }
        }
    }
}

/**
 * Interfaz para implementar en fragmentos que necesitan manejar la acción de refresh
 */
interface Refreshable {
    fun onRefresh()
} 