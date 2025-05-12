package rjm.frontrestaurante.ui.mesas

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentDetalleMesaBinding
import rjm.frontrestaurante.model.EstadoMesa
import rjm.frontrestaurante.model.Pedido
import rjm.frontrestaurante.ui.main.Refreshable
import rjm.frontrestaurante.util.SessionManager

class DetalleMesaFragment : Fragment(), Refreshable {

    private var _binding: FragmentDetalleMesaBinding? = null
    private val binding get() = _binding!!
    
    private val args: DetalleMesaFragmentArgs by navArgs()
    private lateinit var viewModel: DetalleMesaViewModel
    private lateinit var pedidosAdapter: PedidoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleMesaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel con el ID de la mesa
        viewModel = ViewModelProvider(this).get(DetalleMesaViewModel::class.java)
        viewModel.setMesaId(args.mesaId)
        
        // Configurar RecyclerView para pedidos
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPedidos.layoutManager = layoutManager
        
        pedidosAdapter = PedidoAdapter { pedido ->
            val action = DetalleMesaFragmentDirections.actionDetalleMesaFragmentToDetallePedidoFragment(pedido.id)
            findNavController().navigate(action)
        }
        
        binding.recyclerViewPedidos.adapter = pedidosAdapter
        
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
        
        // Configurar FAB para nuevo pedido
        binding.fabNuevoPedido.setOnClickListener {
            val action = DetalleMesaFragmentDirections.actionDetalleMesaFragmentToNuevoPedidoFragment(args.mesaId)
            findNavController().navigate(action)
        }
        
        // Configurar botón de reserva
        binding.buttonReservar.setOnClickListener {
            val action = DetalleMesaFragmentDirections.actionDetalleMesaFragmentToNuevaReservaFragment(args.mesaId)
            findNavController().navigate(action)
        }
        
        // Configurar botón de cerrar mesa (solo visible para camareros y cuando la mesa está ocupada)
        binding.buttonCerrarMesa.setOnClickListener {
            mostrarDialogoConfirmacionCierreMesa()
        }
        
        // Observar datos de la mesa
        viewModel.mesa.observe(viewLifecycleOwner) { mesa ->
            binding.textViewNumeroMesa.text = "Mesa ${mesa.numero}"
            binding.textViewCapacidad.text = "Capacidad: ${mesa.capacidad} personas"
            
            // Formatear estado
            val estadoText = when(mesa.estado) {
                EstadoMesa.LIBRE -> "Libre"
                EstadoMesa.OCUPADA -> "Ocupada"
                EstadoMesa.RESERVADA -> "Reservada"
                EstadoMesa.MANTENIMIENTO -> "En mantenimiento"
            }
            binding.textViewEstado.text = "Estado: $estadoText"
            
            // Mostrar/ocultar botones según el estado
            val userRole = SessionManager.getUserRole()
            binding.fabNuevoPedido.visibility = if ((userRole == "camarero" || userRole == "admin") && mesa.estado != EstadoMesa.MANTENIMIENTO) View.VISIBLE else View.GONE
            binding.buttonReservar.visibility = if (userRole == "camarero" && mesa.estado == EstadoMesa.LIBRE) View.VISIBLE else View.GONE
            binding.buttonCerrarMesa.visibility = if (userRole == "camarero" && mesa.estado == EstadoMesa.OCUPADA) View.VISIBLE else View.GONE
            
            // Mostrar/ocultar panel de resumen de cuenta según el estado
            binding.cardViewResumenCuenta.visibility = if (mesa.estado == EstadoMesa.OCUPADA) View.VISIBLE else View.GONE
            
            // Mostrar/ocultar tarjeta de reserva si la mesa está reservada
            binding.cardViewReservaInfo.visibility = if (mesa.estado == EstadoMesa.RESERVADA) View.VISIBLE else View.GONE
        }
        
        // Observar datos de la reserva activa
        viewModel.reservaActiva.observe(viewLifecycleOwner) { reserva ->
            if (reserva != null) {
                // Mostrar la información de la reserva
                binding.textViewClienteReserva.text = "Cliente: ${reserva.clienteNombre}"
                binding.textViewHoraReserva.text = "Hora: ${reserva.hora}"
                binding.textViewPersonasReserva.text = "Personas: ${reserva.numPersonas}"
                
                // Configurar botones de confirmación
                binding.buttonClienteLlego.setOnClickListener {
                    viewModel.confirmarLlegadaCliente()
                }
                
                binding.buttonClienteNoLlego.setOnClickListener {
                    viewModel.confirmarNoLlegadaCliente()
                }
                
                binding.cardViewReservaInfo.visibility = View.VISIBLE
            } else {
                binding.cardViewReservaInfo.visibility = View.GONE
            }
        }
        
        // Observar lista de pedidos
        viewModel.pedidos.observe(viewLifecycleOwner) { pedidos ->
            pedidosAdapter.submitList(pedidos)
            
            // Mostrar mensaje si no hay pedidos
            if (pedidos.isEmpty()) {
                binding.textViewSinPedidos.visibility = View.VISIBLE
                binding.cardViewResumenCuenta.visibility = View.GONE
            } else {
                binding.textViewSinPedidos.visibility = View.GONE
                
                // Actualizar el resumen de la cuenta si la mesa está ocupada
                val mesa = viewModel.mesa.value
                if (mesa?.estado == EstadoMesa.OCUPADA) {
                    binding.cardViewResumenCuenta.visibility = View.VISIBLE
                    actualizarResumenCuenta()
                } else {
                    binding.cardViewResumenCuenta.visibility = View.GONE
                }
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
        
        // Observar cierre de mesa exitoso
        viewModel.mesaCerradaExitosamente.observe(viewLifecycleOwner) { cerradaExitosamente ->
            if (cerradaExitosamente) {
                // Mostrar mensaje de éxito
                val mensaje = "Servicio finalizado correctamente. Los pedidos han sido completados."
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                
                // Esperar un poco antes de navegar de vuelta para que el usuario vea el Toast
                Handler(requireContext().mainLooper).postDelayed({
                    // Verificar que el fragmento sigue adherido a la actividad antes de navegar
                    if (isAdded && !isDetached() && view != null) {
                        findNavController().navigateUp()
                    }
                }, 1800) // 1.8 segundos
            }
        }
        
        // Cargar datos de la mesa
        viewModel.refreshData()
    }
    
    /**
     * Actualiza el panel de resumen de cuenta con los datos de los pedidos activos
     */
    private fun actualizarResumenCuenta() {
        val pedidos = viewModel.pedidos.value ?: emptyList()
        // Filtramos todos los pedidos que no están cancelados, no solo los activos
        val pedidosValidos = pedidos.filter { 
            it.estado != rjm.frontrestaurante.model.EstadoPedido.CANCELADO
        }
        
        // Calcular total y generar resumen de la cuenta
        var total = 0.0
        val detallesCuenta = mutableListOf<String>()
        
        // Agrupar productos por nombre para mostrar una cuenta más clara
        val productosAgrupados = mutableMapOf<String, Triple<Double, Int, String>>()
        
        pedidosValidos.forEach { pedido ->
            pedido.detalles.forEach { detalle ->
                val producto = detalle.producto
                
                // Añadir al resumen con nombre del producto si está disponible
                if (producto != null) {
                    val subtotal = detalle.cantidad * producto.precio
                    total += subtotal
                    
                    // Agrupar los productos por nombre
                    val key = producto.nombre
                    if (productosAgrupados.containsKey(key)) {
                        val (precio, cantidad, _) = productosAgrupados[key]!!
                        productosAgrupados[key] = Triple(precio, cantidad + detalle.cantidad, producto.nombre)
                    } else {
                        productosAgrupados[key] = Triple(producto.precio, detalle.cantidad, producto.nombre)
                    }
                } else {
                    android.util.Log.w("DetalleMesaFragment", "Producto nulo en detalle de pedido")
                }
            }
        }
        
        // Crear líneas de detalle agrupadas
        productosAgrupados.forEach { (key, value) ->
            val (precio, cantidad, nombre) = value
            val subtotal = precio * cantidad
            val lineaDetalle = "$nombre x $cantidad = ${String.format("%.2f€", subtotal)}"
            detallesCuenta.add(lineaDetalle)
        }
        
        // Actualizar la vista con los datos
        if (detallesCuenta.isNotEmpty()) {
            binding.textViewResumenCuentaDetalles.text = detallesCuenta.joinToString("\n")
            binding.textViewResumenTotal.text = "TOTAL: ${String.format("%.2f€", total)}"
            binding.cardViewResumenCuenta.visibility = View.VISIBLE
        } else {
            binding.cardViewResumenCuenta.visibility = View.GONE
        }
        
        // Log para depuración del total
        android.util.Log.d("DetalleMesaFragment", "Total calculado: $total €")
    }
    
    /**
     * Muestra un diálogo de confirmación para cerrar la mesa y generar la cuenta
     */
    private fun mostrarDialogoConfirmacionCierreMesa() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Finalizar Servicio de Mesa")
            .setMessage("¿Está seguro que desea finalizar el servicio de esta mesa?\n\n" +
                    "• Todos los pedidos pendientes se marcarán como ENTREGADOS\n" +
                    "• Se generará la cuenta final para el pago\n" +
                    "• La mesa quedará disponible para nuevos clientes")
            .setPositiveButton("Confirmar") { _, _ ->
                generarCuenta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * Genera la cuenta final y cierra la mesa
     */
    private fun generarCuenta() {
        val pedidos = viewModel.pedidos.value ?: emptyList()
        // Filtramos todos los pedidos que no están cancelados, no solo los activos
        val pedidosValidos = pedidos.filter { 
            it.estado != rjm.frontrestaurante.model.EstadoPedido.CANCELADO
        }
        
        // Si no hay pedidos válidos
        if (pedidosValidos.isEmpty()) {
            Toast.makeText(context, "No hay pedidos para cobrar", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Calcular total y generar resumen de la cuenta
        var total = 0.0
        val detallesCuenta = mutableListOf<String>()
        
        // Agrupar productos por nombre para mostrar una cuenta más clara
        val productosAgrupados = mutableMapOf<String, Triple<Double, Int, String>>()
        
        pedidosValidos.forEach { pedido ->
            pedido.detalles.forEach { detalle ->
                val producto = detalle.producto
                
                // Añadir al resumen con nombre del producto si está disponible
                if (producto != null) {
                    val subtotal = detalle.cantidad * producto.precio
                    total += subtotal
                    
                    // Agrupar los productos por nombre
                    val key = producto.nombre
                    if (productosAgrupados.containsKey(key)) {
                        val (precio, cantidad, _) = productosAgrupados[key]!!
                        productosAgrupados[key] = Triple(precio, cantidad + detalle.cantidad, producto.nombre)
                    } else {
                        productosAgrupados[key] = Triple(producto.precio, detalle.cantidad, producto.nombre)
                    }
                }
            }
        }
        
        // Crear líneas de detalle agrupadas
        productosAgrupados.forEach { (key, value) ->
            val (precio, cantidad, nombre) = value
            val subtotal = precio * cantidad
            val lineaDetalle = "$nombre x $cantidad = ${String.format("%.2f€", subtotal)}"
            detallesCuenta.add(lineaDetalle)
        }
        
        // Crear mensaje de cuenta
        val mensajeBuilder = StringBuilder()
        mensajeBuilder.append("CUENTA - Mesa ${viewModel.mesa.value?.numero}\n\n")
        mensajeBuilder.append("DETALLES:\n")
        detallesCuenta.forEach { detalle ->
            mensajeBuilder.append("$detalle\n")
        }
        mensajeBuilder.append("\nTOTAL: ${String.format("%.2f€", total)}")
        
        // Mostrar diálogo con la cuenta y opciones de pago
        val dialogView = layoutInflater.inflate(R.layout.dialog_cobro_cuenta, null)
        val textViewResumenCuenta = dialogView.findViewById<TextView>(R.id.textViewResumenCuenta)
        val radioGroupMetodoPago = dialogView.findViewById<RadioGroup>(R.id.radioGroupMetodoPago)
        
        textViewResumenCuenta.text = mensajeBuilder.toString()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Cobro de Cuenta")
            .setView(dialogView)
            .setPositiveButton("Cobrar") { _, _ ->
                // Obtener método de pago seleccionado
                val metodoPagoId = radioGroupMetodoPago.checkedRadioButtonId
                val metodoPago = when (metodoPagoId) {
                    R.id.radioButtonEfectivo -> "efectivo"
                    R.id.radioButtonTarjeta -> "tarjeta"
                    R.id.radioButtonMovil -> "móvil"
                    else -> "efectivo" // por defecto
                }
                
                // Al confirmar, cambiar estado de la mesa a libre
                viewModel.cerrarMesa(metodoPago)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    override fun onRefresh() {
        viewModel.refreshData()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adaptador para la lista de pedidos
 */
class PedidoAdapter(
    private val onPedidoClick: (Pedido) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Pedido, PedidoAdapter.PedidoViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Pedido>() {
        override fun areItemsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem == newItem
    }
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = rjm.frontrestaurante.databinding.ItemPedidoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PedidoViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class PedidoViewHolder(
        private val binding: rjm.frontrestaurante.databinding.ItemPedidoBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(pedido: Pedido) {
            binding.apply {
                textViewIdPedido.text = "Pedido #${pedido.id}"
                textViewEstadoPedido.text = when(pedido.estado) {
                    rjm.frontrestaurante.model.EstadoPedido.RECIBIDO -> root.context.getString(R.string.pedido_recibido)
                    rjm.frontrestaurante.model.EstadoPedido.EN_PREPARACION -> root.context.getString(R.string.pedido_en_preparacion)
                    rjm.frontrestaurante.model.EstadoPedido.LISTO -> root.context.getString(R.string.pedido_listo)
                    rjm.frontrestaurante.model.EstadoPedido.ENTREGADO -> root.context.getString(R.string.pedido_entregado)
                    rjm.frontrestaurante.model.EstadoPedido.CANCELADO -> root.context.getString(R.string.pedido_cancelado)
                }
                textViewFechaPedido.text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", pedido.fecha)
                textViewTotalPedido.text = root.context.getString(R.string.total, pedido.total)
                
                // Configurar color según estado
                val color = when(pedido.estado) {
                    rjm.frontrestaurante.model.EstadoPedido.RECIBIDO -> R.color.pedido_recibido
                    rjm.frontrestaurante.model.EstadoPedido.EN_PREPARACION -> R.color.pedido_en_preparacion
                    rjm.frontrestaurante.model.EstadoPedido.LISTO -> R.color.pedido_listo
                    rjm.frontrestaurante.model.EstadoPedido.ENTREGADO -> R.color.pedido_entregado
                    rjm.frontrestaurante.model.EstadoPedido.CANCELADO -> R.color.pedido_cancelado
                }
                cardViewPedido.setCardBackgroundColor(root.resources.getColor(color, null))
                
                // Configurar clic
                root.setOnClickListener { onPedidoClick(pedido) }
            }
        }
    }
} 