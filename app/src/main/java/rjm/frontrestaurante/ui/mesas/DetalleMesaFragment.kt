package rjm.frontrestaurante.ui.mesas

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            viewModel.cargarDatosMesa()
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
            binding.fabNuevoPedido.visibility = if (userRole == "camarero" && mesa.estado != EstadoMesa.MANTENIMIENTO) View.VISIBLE else View.GONE
            binding.buttonReservar.visibility = if (userRole == "camarero" && mesa.estado == EstadoMesa.LIBRE) View.VISIBLE else View.GONE
            binding.buttonCerrarMesa.visibility = if (userRole == "camarero" && mesa.estado == EstadoMesa.OCUPADA) View.VISIBLE else View.GONE
        }
        
        // Observar lista de pedidos
        viewModel.pedidos.observe(viewLifecycleOwner) { pedidos ->
            pedidosAdapter.submitList(pedidos)
            
            // Mostrar mensaje si no hay pedidos
            if (pedidos.isEmpty()) {
                binding.textViewSinPedidos.visibility = View.VISIBLE
            } else {
                binding.textViewSinPedidos.visibility = View.GONE
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
                    findNavController().navigateUp()
                }, 1800) // 1.8 segundos
            }
        }
        
        // Cargar datos de la mesa
        viewModel.cargarDatosMesa()
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
        // Calcular el total de todos los pedidos
        val pedidos = viewModel.pedidos.value ?: emptyList()
        val total = pedidos.sumOf { it.total }
        
        // Preparar detalle de pedidos para mostrar en la cuenta
        val pedidosDetalle = pedidos.joinToString("\n") { 
            "• Pedido #${it.id}: ${String.format("%.2f€", it.total)}" 
        }
        
        // Mostrar el resumen de la cuenta
        val formattedTotal = String.format("%.2f€", total)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cuenta Final")
            .setMessage("Resumen de pedidos:\n$pedidosDetalle\n\nTotal a pagar: $formattedTotal\n\n¿Confirma el pago?")
            .setPositiveButton("Confirmar Pago") { _, _ ->
                // Cerrar la mesa (cambiar estado a LIBRE)
                viewModel.cerrarMesa()
                Toast.makeText(context, "Cuenta cobrada: $formattedTotal. Servicio finalizado.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    override fun onRefresh() {
        viewModel.cargarDatosMesa()
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