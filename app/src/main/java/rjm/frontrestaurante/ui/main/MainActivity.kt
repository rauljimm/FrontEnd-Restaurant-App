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
            // Configuración basada en el rol del usuario
            val topLevelDestinations = when (userRole) {
                "admin" -> setOf(R.id.mainFragment) // Admin ve productos
                "camarero" -> setOf(R.id.mesasFragment, R.id.pedidosActivosFragment) // Camarero ve mesas y pedidos
                "cocinero" -> setOf(R.id.pedidosActivosFragment) // Cocinero ve solo pedidos
                else -> setOf(R.id.mesasFragment, R.id.pedidosActivosFragment) // Default
            }
            
            appBarConfiguration = AppBarConfiguration(
                topLevelDestinations,
                drawer
            )
            
            setupActionBarWithNavController(navController, appBarConfiguration)
            binding.navView?.setupWithNavController(navController)
            
            // Si es admin, navegar directamente a mainFragment (productos)
            if (userRole == "admin") {
                navController.navigate(R.id.mainFragment)
            }
            // Si es cocinero, navegar directamente a pedidosActivosFragment
            else if (userRole == "cocinero") {
                navController.navigate(R.id.pedidosActivosFragment)
            }
            
            // Configurar acciones adicionales del menú
            binding.navView?.let { navView ->
                // Configurar visibilidad de elementos del menú según el rol
                val menu = navView.menu
                when (userRole) {
                    "admin" -> {
                        // Administrador ve productos
                        menu.findItem(R.id.nav_productos)?.isVisible = true
                        menu.findItem(R.id.nav_mesas)?.isVisible = true // Admin también necesita ver mesas para crearlas
                        menu.findItem(R.id.nav_pedidos_activos)?.isVisible = false
                    }
                    "camarero" -> {
                        // Camarero ve mesas y pedidos activos
                        menu.findItem(R.id.nav_productos)?.isVisible = false
                        menu.findItem(R.id.nav_mesas)?.isVisible = true
                        menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
                    }
                    "cocinero" -> {
                        // Cocinero ve solo pedidos activos
                        menu.findItem(R.id.nav_productos)?.isVisible = false
                        menu.findItem(R.id.nav_mesas)?.isVisible = false
                        menu.findItem(R.id.nav_pedidos_activos)?.isVisible = true
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
                        R.id.nav_logout -> {
                            viewModel.cerrarSesion()
                            finish()
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
        } else {
            menu?.findItem(R.id.action_add_producto)?.isVisible = false
            menu?.findItem(R.id.action_add_mesa)?.isVisible = false
        }
        
        return true
    }
    
    /**
     * Manejar selección de opciones del menú
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
                navController.navigate(R.id.agregarProductoFragment)
                true
            }
            R.id.action_add_mesa -> {
                // Navegar al fragmento para crear mesa (solo admin)
                navController.navigate(R.id.nuevaMesaFragment)
                true
            }
            R.id.action_logout -> {
                viewModel.cerrarSesion()
                finish()
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