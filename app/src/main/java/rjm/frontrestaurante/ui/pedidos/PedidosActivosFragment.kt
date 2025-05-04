package rjm.frontrestaurante.ui.pedidos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentPedidosActivosBinding
import rjm.frontrestaurante.model.EstadoPedido
import rjm.frontrestaurante.model.Pedido
import rjm.frontrestaurante.ui.main.Refreshable
import rjm.frontrestaurante.util.SessionManager

class PedidosActivosFragment : Fragment(), Refreshable {

    private var _binding: FragmentPedidosActivosBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PedidosActivosViewModel
    private lateinit var pedidosAdapter: PedidosActivosAdapter
    private val userRole = SessionManager.getUserRole()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPedidosActivosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(PedidosActivosViewModel::class.java)
        
        // Establecer título específico para cocineros
        if (userRole == "cocinero") {
            binding.textViewTitle.text = "Pedidos pendientes de preparación"
            binding.textViewEmpty.text = "No hay pedidos pendientes de preparación"
        } else {
            binding.textViewTitle.text = "Pedidos activos"
            binding.textViewEmpty.text = getString(R.string.sin_pedidos)
        }
        
        // Configurar RecyclerView
        pedidosAdapter = PedidosActivosAdapter(
            onPedidoClick = { pedido ->
                // Navegar al detalle del pedido
                findNavController().navigate(
                    PedidosActivosFragmentDirections.actionPedidosActivosFragmentToDetallePedidoFragment(pedido.id)
                )
            },
            onMarcarListoClick = { pedido ->
                // Acciones según el rol del usuario y el estado del pedido
                when {
                    userRole == "cocinero" && pedido.estado == EstadoPedido.RECIBIDO -> {
                        // Cocinero inicia la preparación de un pedido recibido
                        viewModel.actualizarEstadoPedido(pedido.id, EstadoPedido.EN_PREPARACION)
                    }
                    userRole == "cocinero" && pedido.estado == EstadoPedido.EN_PREPARACION -> {
                        // Cocinero marca como listo un pedido en preparación
                        viewModel.actualizarEstadoPedido(pedido.id, EstadoPedido.LISTO)
                    }
                    pedido.estado == EstadoPedido.LISTO -> {
                        // Camarero marca como entregado un pedido listo
                        viewModel.actualizarEstadoPedido(pedido.id, EstadoPedido.ENTREGADO)
                    }
                }
            }
        )
        
        binding.recyclerViewPedidos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pedidosAdapter
        }
        
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.cargarPedidosActivos()
        }
        
        // Observar pedidos activos
        viewModel.pedidos.observe(viewLifecycleOwner) { pedidos ->
            pedidosAdapter.submitList(pedidos)
            binding.swipeRefreshLayout.isRefreshing = false
            binding.textViewEmpty.visibility = if (pedidos.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Cargar pedidos
        viewModel.cargarPedidosActivos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onRefresh() {
        viewModel.cargarPedidosActivos()
    }
}
