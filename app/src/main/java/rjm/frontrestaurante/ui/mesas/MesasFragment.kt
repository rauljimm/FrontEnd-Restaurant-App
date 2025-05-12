package rjm.frontrestaurante.ui.mesas

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentMesasBinding
import rjm.frontrestaurante.model.Mesa
import rjm.frontrestaurante.ui.main.Refreshable
import rjm.frontrestaurante.util.SessionManager

class MesasFragment : Fragment(), Refreshable, MesaMenuCallback {

    private var _binding: FragmentMesasBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MesasViewModel
    private lateinit var mesasAdapter: MesasAdapter
    
    // Todas las ubicaciones posibles
    private val todasUbicaciones = listOf(
        "Todas", "Terraza", "Interior", "Comedor", "Barra", "Sin ubicación"
    )
    
    // Ubicación seleccionada actualmente
    private var ubicacionActual = "Todas"
    
    // Ubicaciones disponibles (con al menos una mesa)
    private val ubicacionesDisponibles = mutableListOf<String>()
    
    // Flag para determinar si el usuario es administrador
    private var esAdmin = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMesasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Verificar si el usuario es administrador
        esAdmin = SessionManager.getUserRole().equals("admin", ignoreCase = true)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(MesasViewModel::class.java)
        
        // Configurar RecyclerView
        setupRecyclerView()
        
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.cargarMesas()
        }
        
        // Mostrar el FAB para crear nuevas mesas solo para administradores
        binding.fabNuevaMesa.visibility = if (esAdmin) View.VISIBLE else View.GONE
        
        // Si es administrador, configurar acción del FAB
        if (esAdmin) {
            binding.fabNuevaMesa.setOnClickListener {
                findNavController().navigate(R.id.action_mesasFragment_to_nuevaMesaFragment)
            }
        }
        
        // Registrar para menú contextual
        registerForContextMenu(binding.recyclerViewMesas)
        
        // Observar cambios en el ViewModel
        observeViewModel()
        
        // Cargar datos
        viewModel.cargarMesas()
    }

    private fun setupRecyclerView() {
        mesasAdapter = MesasAdapter(
            onMesaClick = { mesa ->
                // Navegar al detalle de la mesa
                val action = MesasFragmentDirections.actionMesasFragmentToDetalleMesaFragment(mesa.id)
                findNavController().navigate(action)
            },
            // Solo pasar el callback de menú si el usuario es administrador
            menuCallback = if (esAdmin) this else null
        )
        
        binding.recyclerViewMesas.apply {
            adapter = mesasAdapter
            layoutManager = GridLayoutManager(requireContext(), 2) // Mostrar en grid de 2 columnas
        }
    }
    
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val mesa = mesasAdapter.getSelectedMesa() ?: return false
        
        return when (item.itemId) {
            MesasAdapter.MENU_ITEM_EDITAR -> {
                onEditarMesa(mesa)
                true
            }
            MesasAdapter.MENU_ITEM_ELIMINAR -> {
                onEliminarMesa(mesa)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }
    
    override fun onEditarMesa(mesa: Mesa) {
        // Navegar al fragmento de edición con el ID de la mesa
        val action = MesasFragmentDirections.actionMesasFragmentToEditarMesaFragment(mesa.id)
        findNavController().navigate(action)
    }
    
    override fun onEliminarMesa(mesa: Mesa) {
        // Mostrar diálogo de confirmación
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar mesa")
            .setMessage("¿Estás seguro de que deseas eliminar la mesa ${mesa.numero}?")
            .setPositiveButton("Eliminar") { _, _ ->
                // Mostrar diálogo de progreso
                val progressDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Eliminando mesa")
                    .setMessage("Procesando...")
                    .setCancelable(false)
                    .create()
                progressDialog.show()
                
                // Eliminar mesa
                viewModel.eliminarMesa(mesa.id)
                
                // Observar resultado una vez
                viewModel.resultado.observeForever(object : androidx.lifecycle.Observer<Boolean> {
                    override fun onChanged(exitoso: Boolean) {
                        progressDialog.dismiss()
                        if (exitoso) {
                            Toast.makeText(context, "Mesa ${mesa.numero} eliminada correctamente", Toast.LENGTH_SHORT).show()
                            // Recargar datos
                            viewModel.cargarMesas()
                        } else {
                            // Mostrar error más descriptivo
                            val errorMsg = viewModel.error.value ?: "Error al eliminar la mesa"
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                        // Dejar de observar para evitar múltiples notificaciones
                        viewModel.resultado.removeObserver(this)
                    }
                })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun setupTabLayout() {
        // Limpiar tabs existentes
        binding.tabLayout.removeAllTabs()
        
        // Siempre agregar "Todas" como primera opción
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Todas"))
        
        // Agregar tab solo para ubicaciones con mesas
        for (ubicacion in ubicacionesDisponibles) {
            if (ubicacion != "Todas") {  // "Todas" ya fue agregada
                binding.tabLayout.addTab(binding.tabLayout.newTab().setText(ubicacion))
            }
        }
        
        // Si no hay tabs adicionales, ocultar el TabLayout
        binding.tabLayout.visibility = if (ubicacionesDisponibles.size <= 1) View.GONE else View.VISIBLE
        
        // Listener para cambio de tab
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val index = tab.position
                ubicacionActual = if (index == 0) {
                    "Todas"
                } else if (index < ubicacionesDisponibles.size) {
                    ubicacionesDisponibles[index]
                } else {
                    "Todas"
                }
                Log.d("MesasFragment", "Tab seleccionada: $ubicacionActual")
                filtrarMesasPorUbicacion()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun actualizarUbicacionesDisponibles() {
        // Obtener todas las mesas
        val todasLasMesas = viewModel.mesas.value ?: emptyList()
        
        // Resetear la lista de ubicaciones disponibles
        ubicacionesDisponibles.clear()
        ubicacionesDisponibles.add("Todas")  // Siempre mostrar "Todas"
        
        // Recopilar ubicaciones únicas presentes en las mesas
        val ubicacionesUnicas = todasLasMesas
            .map { it.ubicacion }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        
        // Añadir a la lista de disponibles
        ubicacionesDisponibles.addAll(ubicacionesUnicas)
        
        // Añadir "Sin ubicación" si hay mesas sin ubicación
        if (todasLasMesas.any { it.ubicacion.isEmpty() }) {
            ubicacionesDisponibles.add("Sin ubicación")
        }
        
        // Actualizar las tabs con las ubicaciones disponibles
        setupTabLayout()
    }
    
    private fun observeViewModel() {
        // Observar lista de mesas
        viewModel.mesas.observe(viewLifecycleOwner) { mesas ->
            // Actualizar ubicaciones disponibles
            actualizarUbicacionesDisponibles()
            
            // Actualizar adaptador con mesas filtradas
            filtrarMesasPorUbicacion()
            
            // Mostrar mensaje si no hay mesas
            binding.textViewEmpty.visibility = if (mesas.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
        
        // Observar resultado de operaciones
        viewModel.resultado.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                // Recargar la lista de mesas después de una operación exitosa
                viewModel.cargarMesas()
            }
        }
    }
    
    private fun filtrarMesasPorUbicacion() {
        val todasLasMesas = viewModel.mesas.value ?: emptyList()
        
        val mesasFiltradas = when (ubicacionActual) {
            "Todas" -> todasLasMesas
            "Sin ubicación" -> todasLasMesas.filter { it.ubicacion.isEmpty() }
            else -> todasLasMesas.filter { it.ubicacion.equals(ubicacionActual, ignoreCase = true) }
        }
        
        mesasAdapter.submitList(mesasFiltradas)
        
        // Mostrar mensaje si no hay mesas en esta ubicación
        binding.textViewEmpty.visibility = if (mesasFiltradas.isEmpty() && todasLasMesas.isNotEmpty()) 
            View.VISIBLE else View.GONE
        
        if (mesasFiltradas.isEmpty() && todasLasMesas.isNotEmpty()) {
            binding.textViewEmpty.text = "No hay mesas en ${ubicacionActual.lowercase()}"
        } else {
            binding.textViewEmpty.text = getString(R.string.sin_mesas)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onRefresh() {
        viewModel.cargarMesas()
    }
} 