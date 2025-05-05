package rjm.frontrestaurante.ui.cuentas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import rjm.frontrestaurante.databinding.FragmentCuentasBinding
import rjm.frontrestaurante.model.Cuenta
import rjm.frontrestaurante.ui.main.Refreshable
import java.text.SimpleDateFormat
import java.util.Locale

class CuentasFragment : Fragment(), Refreshable {

    private var _binding: FragmentCuentasBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: CuentasViewModel
    private lateinit var cuentasAdapter: CuentasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCuentasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(CuentasViewModel::class.java)
        
        // Configurar RecyclerView
        cuentasAdapter = CuentasAdapter { cuenta ->
            // Navegar al detalle de la cuenta
            val action = CuentasFragmentDirections.actionCuentasFragmentToDetalleCuentaFragment(cuenta.id)
            findNavController().navigate(action)
        }
        
        binding.recyclerViewCuentas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cuentasAdapter
        }
        
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.cargarCuentas()
        }
        
        // Configurar FAB para filtros
        binding.fabFiltroCuentas.setOnClickListener {
            mostrarDialogoFiltros()
        }
        
        // Observar lista de cuentas
        viewModel.cuentas.observe(viewLifecycleOwner) { cuentas ->
            cuentasAdapter.submitList(cuentas)
            
            // Mostrar mensaje si no hay cuentas
            if (cuentas.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewCuentas.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewCuentas.visibility = View.VISIBLE
            }
            
            // Ocultar indicador de refresco
            binding.swipeRefreshLayout.isRefreshing = false
        }
        
        // Observar mensajes de error
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    private fun mostrarDialogoFiltros() {
        // Implementar un diálogo para filtrar cuentas por fecha y mesa
        // Por ahora, solo limpiamos los filtros como ejemplo
        viewModel.limpiarFiltros()
        Toast.makeText(context, "Filtros reiniciados", Toast.LENGTH_SHORT).show()
    }
    
    override fun onRefresh() {
        viewModel.cargarCuentas()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CuentasAdapter(
    private val onCuentaClick: (Cuenta) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Cuenta, CuentasAdapter.CuentaViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Cuenta>() {
        override fun areItemsTheSame(oldItem: Cuenta, newItem: Cuenta) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Cuenta, newItem: Cuenta) = oldItem == newItem
    }
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuentaViewHolder {
        val binding = rjm.frontrestaurante.databinding.ItemCuentaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CuentaViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CuentaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CuentaViewHolder(
        private val binding: rjm.frontrestaurante.databinding.ItemCuentaBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(cuenta: Cuenta) {
            binding.apply {
                // Información básica
                textViewIdCuenta.text = "Cuenta #${cuenta.id}"
                textViewNumeroMesa.text = "Mesa: ${cuenta.numeroMesa}"
                textViewCamarero.text = "Camarero: ${cuenta.nombreCamarero}"
                
                // Formatear fecha
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                textViewFechaCuenta.text = dateFormat.format(cuenta.fechaCobro)
                
                // Total
                textViewTotalCuenta.text = "Total: ${String.format("%.2f€", cuenta.total)}"
                
                // Método de pago
                textViewMetodoPago.text = cuenta.metodoPago ?: "Sin especificar"
                
                // Configurar clic
                cardViewCuenta.setOnClickListener { onCuentaClick(cuenta) }
            }
        }
    }
} 