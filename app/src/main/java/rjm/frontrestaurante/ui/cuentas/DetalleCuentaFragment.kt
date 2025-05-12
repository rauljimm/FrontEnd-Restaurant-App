package rjm.frontrestaurante.ui.cuentas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentDetalleCuentaBinding
import rjm.frontrestaurante.model.DetalleCuenta
import rjm.frontrestaurante.util.PdfViewerUtils
import rjm.frontrestaurante.util.SessionManager
import java.text.SimpleDateFormat
import java.util.Locale

class DetalleCuentaFragment : Fragment() {

    private var _binding: FragmentDetalleCuentaBinding? = null
    private val binding get() = _binding!!
    
    private val args: DetalleCuentaFragmentArgs by navArgs()
    private lateinit var viewModel: DetalleCuentaViewModel
    private lateinit var detallesAdapter: DetallesCuentaAdapter
    
    // Rol del usuario actual
    private val userRole = SessionManager.getUserRole()
    
    // Launcher para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, generar PDF
            generarPdf()
        } else {
            // Permiso denegado
            Toast.makeText(
                requireContext(),
                "Se necesita permiso de almacenamiento para generar el PDF",
                Toast.LENGTH_LONG
            ).show()
        }
    }

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
        
        // Configurar visibilidad del botón de imprimir según el rol
        configureImprimirButton()
        
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
        
        // Observar cuando el PDF se ha generado correctamente
        viewModel.pdfGenerado.observe(viewLifecycleOwner) { pdfFile ->
            if (pdfFile != null) {
                // Abrir el PDF con el visualizador interno
                PdfViewerUtils.openPdfFile(requireContext(), pdfFile)
            }
        }
        
        // Observar cuando la cuenta ha sido eliminada
        viewModel.cuentaEliminada.observe(viewLifecycleOwner) { eliminada ->
            if (eliminada) {
                Toast.makeText(requireContext(), "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                // Navegar hacia atrás
                findNavController().navigateUp()
            }
        }
    }
    
    /**
     * Configura el botón de imprimir según el rol del usuario
     */
    private fun configureImprimirButton() {
        // Solo mostrar el botón a administradores
        if (userRole == "admin") {
            binding.buttonImprimirCuenta.visibility = View.VISIBLE
            binding.buttonImprimirCuenta.setOnClickListener {
                checkPermissionAndGeneratePdf()
            }
            
            // Configurar botón de eliminar cuenta (solo para admin)
            binding.buttonEliminarCuenta.visibility = View.VISIBLE
            binding.buttonEliminarCuenta.setOnClickListener {
                mostrarDialogoConfirmacionEliminar()
            }
        } else {
            binding.buttonImprimirCuenta.visibility = View.GONE
            binding.buttonEliminarCuenta.visibility = View.GONE
        }
    }
    
    /**
     * Verifica permisos antes de generar el PDF
     */
    private fun checkPermissionAndGeneratePdf() {
        // En Android 10+ (API 29+) no se necesita pedir permiso para escribir en el directorio privado de la app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            generarPdf()
            return
        }
        
        // Para versiones anteriores a Android 10, verificar permiso de almacenamiento
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Ya tenemos permiso
                generarPdf()
            }
            
            shouldShowRequestPermissionRationale(permission) -> {
                // Explicar al usuario por qué necesitamos el permiso
                Toast.makeText(
                    requireContext(),
                    "Se necesita acceso al almacenamiento para guardar el PDF",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(permission)
            }
            
            else -> {
                // Primera vez o "No volver a preguntar" no marcado
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    
    /**
     * Genera el PDF de la cuenta
     */
    private fun generarPdf() {
        viewModel.generarPdf(requireContext())
    }
    
    /**
     * Muestra un diálogo de confirmación para eliminar la cuenta
     */
    private fun mostrarDialogoConfirmacionEliminar() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Cuenta")
            .setMessage("¿Estás seguro de que deseas eliminar esta cuenta? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarCuenta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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