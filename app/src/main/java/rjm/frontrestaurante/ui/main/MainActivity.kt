package rjm.frontrestaurante.ui.main

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
                R.id.detalleCuentaFragment
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
            drawerToggle.syncState()
            
            setupActionBarWithNavController(navController, appBarConfiguration)
            binding.navView?.setupWithNavController(navController)
            
            // Configurar acciones adicionales del menú
            binding.navView?.let { navView ->
                // Configurar visibilidad de elementos del menú según el rol
                val menu = navView.menu
                when (userRole) {
                    "admin" -> {
                        // Administrador ve productos, categorías, cuentas, mesas y reservas
                        menu.findItem(R.id.nav_productos)?.isVisible = true
                        menu.findItem(R.id.nav_mesas)?.isVisible = true
                        menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
                        menu.findItem(R.id.nav_cuentas)?.isVisible = true
                        menu.findItem(R.id.nav_reservas)?.isVisible = true
                    }
                    "camarero" -> {
                        // Camarero ve mesas, pedidos activos, productos y reservas
                        menu.findItem(R.id.nav_productos)?.isVisible = true
                        menu.findItem(R.id.nav_mesas)?.isVisible = true
                        menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
                        menu.findItem(R.id.nav_cuentas)?.isVisible = false
                        menu.findItem(R.id.nav_reservas)?.isVisible = true
                    }
                    "cocinero" -> {
                        // Cocinero ve solo pedidos activos
                        menu.findItem(R.id.nav_productos)?.isVisible = false
                        menu.findItem(R.id.nav_mesas)?.isVisible = false
                        menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
                        menu.findItem(R.id.nav_cuentas)?.isVisible = false
                        menu.findItem(R.id.nav_reservas)?.isVisible = false
                    }
                }
                
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
        
        // Mostrar opciones adicionales para administradores
        if (::userRole.isInitialized && userRole == "admin") {
            menu?.findItem(R.id.action_add_producto)?.isVisible = true
            menu?.findItem(R.id.action_add_mesa)?.isVisible = true
            Log.d(TAG, "Mostrando opciones de administrador en el menú")
        } else {
            menu?.findItem(R.id.action_add_producto)?.isVisible = false
            menu?.findItem(R.id.action_add_mesa)?.isVisible = false
            Log.d(TAG, "Ocultando opciones de administrador en el menú. Rol actual: $userRole")
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
                navController.navigate(R.id.nuevaMesaFragment)
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
}

/**
 * Interfaz para implementar en fragmentos que necesitan manejar la acción de refresh
 */
interface Refreshable {
    fun onRefresh()
} 