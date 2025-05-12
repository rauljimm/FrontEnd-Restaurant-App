package rjm.frontrestaurante.ui.pedidos

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentDetallePedidoBinding
import rjm.frontrestaurante.model.DetallePedido
import rjm.frontrestaurante.model.EstadoDetallePedido
import rjm.frontrestaurante.model.EstadoPedido
import rjm.frontrestaurante.util.SessionManager
import java.text.SimpleDateFormat
import java.util.Locale

class DetallePedidoFragment : Fragment() {

    private var _binding: FragmentDetallePedidoBinding? = null
    private val binding get() = _binding!!
    
    private val args: DetallePedidoFragmentArgs by navArgs()
    private lateinit var viewModel: DetallePedidoViewModel
    private lateinit var detallesAdapter: DetallesPedidoAdapter
    private val userRole = SessionManager.getUserRole()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetallePedidoBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true) // Indicar que este fragmento tiene menú de opciones
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel con el ID del pedido
        viewModel = ViewModelProvider(this).get(DetallePedidoViewModel::class.java)
        viewModel.setPedidoId(args.pedidoId)
        
        // Configurar RecyclerView para detalles
        detallesAdapter = DetallesPedidoAdapter()
        binding.recyclerViewDetalles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = detallesAdapter
        }
        
        // Configurar botón de actualizar estado según el rol
        setupButtonByRole()
        
        // Configurar botón de eliminar pedido (solo visible para admin y camareros)
        setupDeleteButton()
        
        // Configurar botón de actualizar pedido (solo visible para admin y camareros)
        setupUpdateButton()
        
        // Observar datos del pedido
        viewModel.pedido.observe(viewLifecycleOwner) { pedido ->
            // Actualizar UI con los datos del pedido
            binding.textViewIdPedido.text = "Pedido #${pedido.id}"
            binding.textViewNumeroMesa.text = "Mesa: ${pedido.mesaId}"
            
            // Formatear estado
            val estadoText = when(pedido.estado) {
                EstadoPedido.RECIBIDO -> getString(R.string.pedido_recibido)
                EstadoPedido.EN_PREPARACION -> getString(R.string.pedido_en_preparacion)
                EstadoPedido.LISTO -> getString(R.string.pedido_listo)
                EstadoPedido.ENTREGADO -> getString(R.string.pedido_entregado)
                EstadoPedido.CANCELADO -> getString(R.string.pedido_cancelado)
            }
            binding.textViewEstadoPedido.text = "Estado: $estadoText"
            
            // Formatear fecha
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.textViewFechaPedido.text = "Fecha: ${dateFormat.format(pedido.fecha)}"
            
            // Mostrar observaciones si existen
            if (pedido.observaciones.isNotEmpty()) {
                binding.textViewObservaciones.text = "Observaciones: ${pedido.observaciones}"
                binding.textViewObservaciones.visibility = View.VISIBLE
            } else {
                binding.textViewObservaciones.visibility = View.GONE
            }
            
            // Mostrar total
            binding.textViewTotal.text = getString(R.string.total, pedido.total)
            
            // Configurar adaptador con funcionalidades de edición para admins y camareros
            detallesAdapter.setup(
                pedidoId = pedido.id.toLong(), 
                userRole = userRole,
                pedidoEstado = pedido.estado,
                onUpdate = { detalle, nuevaCantidad ->
                    viewModel.actualizarCantidadProducto(detalle.id, nuevaCantidad)
                },
                onDelete = { detalle ->
                    viewModel.eliminarProductoDePedido(detalle.id)
                }
            )
            
            // Actualizar lista de detalles
            detallesAdapter.submitList(pedido.detalles)
            
            // Mostrar mensaje si no hay productos
            if (pedido.detalles.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE 
                binding.recyclerViewDetalles.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewDetalles.visibility = View.VISIBLE
            }
            
            // Actualizar visibilidad del botón según el estado del pedido y el rol
            updateButtonVisibility(pedido.estado)
            updateDeleteButtonVisibility(pedido.estado)
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
        
        // Observar evento de pedido eliminado
        viewModel.pedidoEliminado.observe(viewLifecycleOwner) { eliminado ->
            if (eliminado) {
                Toast.makeText(context, "Pedido eliminado correctamente", Toast.LENGTH_SHORT).show()
                // Volver a la pantalla anterior
                findNavController().navigateUp()
            }
        }
        
        // Cargar datos del pedido
        viewModel.cargarPedido()
    }
    
    private fun setupButtonByRole() {
        when (userRole) {
            "cocinero" -> {
                binding.buttonActualizarEstado.text = getString(R.string.action_mark_ready)
                binding.buttonActualizarEstado.setOnClickListener {
                    viewModel.actualizarEstadoPedido(EstadoPedido.LISTO)
                }
                binding.buttonActualizarEstado.visibility = View.VISIBLE
            }
            "camarero" -> {
                binding.buttonActualizarEstado.text = getString(R.string.action_mark_delivered)
                binding.buttonActualizarEstado.setOnClickListener {
                    viewModel.actualizarEstadoPedido(EstadoPedido.ENTREGADO)
                }
                binding.buttonActualizarEstado.visibility = View.VISIBLE
            }
            else -> {
                binding.buttonActualizarEstado.visibility = View.GONE
            }
        }
    }
    
    private fun updateButtonVisibility(estadoPedido: EstadoPedido) {
        // Llevar registro del estado del pedido para debugging
        android.util.Log.d("DetallePedidoFragment", "Estado del pedido: $estadoPedido, Rol de usuario: $userRole")
        
        binding.buttonActualizarEstado.visibility = when {
            userRole == "cocinero" && (estadoPedido == EstadoPedido.RECIBIDO || estadoPedido == EstadoPedido.EN_PREPARACION) -> {
                android.util.Log.d("DetallePedidoFragment", "Mostrando botón para cocinero")
                View.VISIBLE
            }
            userRole == "camarero" && estadoPedido == EstadoPedido.LISTO -> {
                android.util.Log.d("DetallePedidoFragment", "Mostrando botón para camarero")
                View.VISIBLE
            }
            else -> {
                android.util.Log.d("DetallePedidoFragment", "Ocultando botón")
                View.GONE
            }
        }
        
        // Actualizar visibilidad del botón de actualizar pedido
        binding.buttonActualizarPedido.visibility = when {
            (userRole == "admin" || userRole == "camarero") && 
            estadoPedido != EstadoPedido.ENTREGADO && 
            estadoPedido != EstadoPedido.CANCELADO -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun setupDeleteButton() {
        if (userRole == "admin" || userRole == "camarero") {
            binding.buttonEliminarPedido.setOnClickListener {
                // Mostrar diálogo de confirmación
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar pedido")
                    .setMessage("¿Estás seguro de que deseas eliminar este pedido?")
                    .setPositiveButton("Sí") { _, _ ->
                        viewModel.eliminarPedido()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        } else {
            binding.buttonEliminarPedido.visibility = View.GONE
        }
    }
    
    private fun setupUpdateButton() {
        binding.buttonActualizarPedido.setOnClickListener {
            // Navegar a la pantalla de selección de productos para añadir al pedido
            val action = DetallePedidoFragmentDirections.actionDetallePedidoFragmentToSeleccionarProductoFragment(args.pedidoId)
            findNavController().navigate(action)
        }
    }

    private fun updateDeleteButtonVisibility(estadoPedido: EstadoPedido) {
        binding.buttonEliminarPedido.visibility = when {
            (userRole == "admin" || userRole == "camarero") && estadoPedido != EstadoPedido.ENTREGADO -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        
        // Ya no necesitamos este menú, ya que ahora tenemos un botón principal
        // para añadir productos al pedido
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val MENU_ADD_PRODUCT = 1 // ID para el ítem de menú
    }
}

/**
 * Adaptador para la lista de detalles del pedido
 */
class DetallesPedidoAdapter : androidx.recyclerview.widget.ListAdapter<DetallePedido, DetallesPedidoAdapter.DetalleViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<DetallePedido>() {
        override fun areItemsTheSame(oldItem: DetallePedido, newItem: DetallePedido) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DetallePedido, newItem: DetallePedido) = oldItem == newItem
    }
) {
    private var pedidoId: Long = 0
    private var userRole: String = ""
    private var pedidoEstado: EstadoPedido = EstadoPedido.RECIBIDO
    private var onItemUpdate: ((DetallePedido, Int) -> Unit)? = null
    private var onItemDelete: ((DetallePedido) -> Unit)? = null
    
    fun setup(pedidoId: Long, userRole: String, pedidoEstado: EstadoPedido, 
              onUpdate: (DetallePedido, Int) -> Unit, 
              onDelete: (DetallePedido) -> Unit) {
        this.pedidoId = pedidoId
        this.userRole = userRole
        this.pedidoEstado = pedidoEstado
        this.onItemUpdate = onUpdate
        this.onItemDelete = onDelete
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalleViewHolder {
        val binding = rjm.frontrestaurante.databinding.ItemDetallePedidoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DetalleViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DetalleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DetalleViewHolder(
        private val binding: rjm.frontrestaurante.databinding.ItemDetallePedidoBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(detalle: DetallePedido) {
            binding.apply {
                // Mostrar información del producto
                val producto = detalle.producto
                if (producto != null) {
                    textViewNombreProducto.text = producto.nombre
                    textViewPrecioProducto.text = "Precio: ${String.format("%.2f€", producto.precio)}"
                } else {
                    textViewNombreProducto.text = "Producto #${detalle.productoId}"
                    textViewPrecioProducto.text = ""
                }
                
                // Mostrar cantidad y subtotal
                textViewCantidadProducto.text = "Cantidad: ${detalle.cantidad}"
                val subtotal = producto?.precio?.times(detalle.cantidad) ?: 0.0
                textViewSubtotalProducto.text = "Subtotal: ${String.format("%.2f€", subtotal)}"
                
                // Mostrar estado del detalle
                val estadoDetalle = when(detalle.estado) {
                    EstadoDetallePedido.PENDIENTE -> "Pendiente"
                    EstadoDetallePedido.EN_PREPARACION -> "En preparación"
                    EstadoDetallePedido.LISTO -> "Listo"
                    EstadoDetallePedido.ENTREGADO -> "Entregado"
                    EstadoDetallePedido.CANCELADO -> "Cancelado"
                }
                textViewEstadoDetalle.text = estadoDetalle
                
                // Aplicar color según el estado
                val colorId = when(detalle.estado) {
                    EstadoDetallePedido.PENDIENTE -> R.color.pedido_recibido
                    EstadoDetallePedido.EN_PREPARACION -> R.color.pedido_en_preparacion
                    EstadoDetallePedido.LISTO -> R.color.pedido_listo
                    EstadoDetallePedido.ENTREGADO -> R.color.pedido_entregado
                    EstadoDetallePedido.CANCELADO -> R.color.pedido_cancelado
                }
                textViewEstadoDetalle.setBackgroundColor(root.context.getColor(colorId))
                
                // Mostrar observaciones si existen
                if (detalle.observaciones.isNotEmpty()) {
                    textViewObservacionesDetalle.text = "Obs: ${detalle.observaciones}"
                    textViewObservacionesDetalle.visibility = View.VISIBLE
                } else {
                    textViewObservacionesDetalle.visibility = View.GONE
                }
                
                // Configurar botones para editar y eliminar (solo para admin y camareros)
                // y si el pedido no está entregado ni cancelado
                if ((userRole == "admin" || userRole == "camarero") && 
                    pedidoEstado != EstadoPedido.ENTREGADO && 
                    pedidoEstado != EstadoPedido.CANCELADO) {
                    
                    // Hacer visibles los controles de edición
                    layoutControlesEdicion.visibility = View.VISIBLE
                    
                    // Configurar botones de incremento/decremento
                    buttonDecrementar.setOnClickListener {
                        if (detalle.cantidad > 1) {
                            onItemUpdate?.invoke(detalle, detalle.cantidad - 1)
                        } else {
                            // Si la cantidad llega a 0, preguntar si desea eliminar
                            androidx.appcompat.app.AlertDialog.Builder(root.context)
                                .setTitle("Eliminar producto")
                                .setMessage("¿Desea eliminar este producto del pedido?")
                                .setPositiveButton("Sí") { _, _ ->
                                    onItemDelete?.invoke(detalle)
                                }
                                .setNegativeButton("No", null)
                                .show()
                        }
                    }
                    
                    buttonIncrementar.setOnClickListener {
                        onItemUpdate?.invoke(detalle, detalle.cantidad + 1)
                    }
                    
                    buttonEliminar.setOnClickListener {
                        androidx.appcompat.app.AlertDialog.Builder(root.context)
                            .setTitle("Eliminar producto")
                            .setMessage("¿Desea eliminar este producto del pedido?")
                            .setPositiveButton("Sí") { _, _ ->
                                onItemDelete?.invoke(detalle)
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }
                } else {
                    // Ocultar controles de edición
                    layoutControlesEdicion.visibility = View.GONE
                }
            }
        }
    }
} 