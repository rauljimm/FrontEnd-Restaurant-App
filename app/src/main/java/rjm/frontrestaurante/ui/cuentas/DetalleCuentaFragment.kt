package rjm.frontrestaurante.ui.cuentas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentDetalleCuentaBinding
import rjm.frontrestaurante.model.DetalleCuenta
import java.text.SimpleDateFormat
import java.util.Locale

class DetalleCuentaFragment : Fragment() {

    private var _binding: FragmentDetalleCuentaBinding? = null
    private val binding get() = _binding!!
    
    private val args: DetalleCuentaFragmentArgs by navArgs()
    private lateinit var viewModel: DetalleCuentaViewModel
    private lateinit var detallesAdapter: DetallesCuentaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel con el ID de la cuenta
        viewModel = ViewModelProvider(this).get(DetalleCuentaViewModel::class.java)
        viewModel.setCuentaId(args.cuentaId)
        
        // Configurar RecyclerView para detalles
        detallesAdapter = DetallesCuentaAdapter()
        binding.recyclerViewDetallesCuenta.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = detallesAdapter
        }
        
        // Observar datos de la cuenta
        viewModel.cuenta.observe(viewLifecycleOwner) { cuenta ->
            // Información básica
            binding.textViewIdCuenta.text = getString(R.string.cuenta_id, cuenta.id)
            binding.textViewNumeroMesa.text = getString(R.string.cuenta_mesa, cuenta.numeroMesa)
            binding.textViewCamarero.text = getString(R.string.cuenta_camarero, cuenta.nombreCamarero)
            
            // Formatear fecha
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.textViewFechaCuenta.text = getString(
                R.string.cuenta_fecha, 
                dateFormat.format(cuenta.fechaCobro)
            )
            
            // Método de pago
            val metodoPago = cuenta.metodoPago ?: getString(R.string.cuenta_sin_metodo)
            binding.textViewMetodoPago.text = metodoPago
            
            // Total
            binding.textViewTotalCuenta.text = getString(R.string.cuenta_total, cuenta.total)
            
            // Actualizar lista de detalles
            if (cuenta.detalles.isNotEmpty()) {
                detallesAdapter.submitList(cuenta.detalles)
                binding.textViewSinDetalles.visibility = View.GONE
                binding.recyclerViewDetallesCuenta.visibility = View.VISIBLE
            } else {
                binding.textViewSinDetalles.visibility = View.VISIBLE
                binding.recyclerViewDetallesCuenta.visibility = View.GONE
            }
        }
        
        // Observar mensajes de error
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DetallesCuentaAdapter : androidx.recyclerview.widget.ListAdapter<DetalleCuenta, DetallesCuentaAdapter.DetalleViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<DetalleCuenta>() {
        override fun areItemsTheSame(oldItem: DetalleCuenta, newItem: DetalleCuenta) = 
            oldItem.pedidoId == newItem.pedidoId && oldItem.productoId == newItem.productoId
        override fun areContentsTheSame(oldItem: DetalleCuenta, newItem: DetalleCuenta) = oldItem == newItem
    }
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalleViewHolder {
        val binding = rjm.frontrestaurante.databinding.ItemDetalleCuentaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DetalleViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DetalleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DetalleViewHolder(
        private val binding: rjm.frontrestaurante.databinding.ItemDetalleCuentaBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(detalle: DetalleCuenta) {
            binding.apply {
                // Nombre del producto
                textViewNombreProducto.text = detalle.nombreProducto
                
                // Cantidad
                textViewCantidad.text = "x${detalle.cantidad}"
                
                // Precio unitario
                textViewPrecioUnitario.text = "Precio unitario: ${String.format("%.2f€", detalle.precioUnitario)}"
                
                // Subtotal
                textViewSubtotal.text = "Subtotal: ${String.format("%.2f€", detalle.subtotal)}"
                
                // Observaciones (mostrar solo si hay)
                if (detalle.observaciones.isNullOrEmpty()) {
                    textViewObservaciones.visibility = View.GONE
                } else {
                    textViewObservaciones.visibility = View.VISIBLE
                    textViewObservaciones.text = "Obs: ${detalle.observaciones}"
                }
            }
        }
    }
} 