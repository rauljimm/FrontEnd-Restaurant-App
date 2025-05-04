package rjm.frontrestaurante.ui.main

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
import rjm.frontrestaurante.adapter.ProductoAdapter
import rjm.frontrestaurante.databinding.FragmentMainBinding
import rjm.frontrestaurante.model.Producto
import rjm.frontrestaurante.util.SessionManager

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    private lateinit var productoAdapter: ProductoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        
        // Configurar RecyclerView
        binding.recyclerViewProductos.layoutManager = LinearLayoutManager(requireContext())
        
        // Inicializar adapter
        productoAdapter = ProductoAdapter { producto -> onProductoClick(producto) }
        binding.recyclerViewProductos.adapter = productoAdapter
        
        // Configurar chips de navegación
        setupNavigationChips()
        
        // Cargar productos
        viewModel.cargarProductos()
        
        // Observar lista de productos
        viewModel.productos.observe(viewLifecycleOwner) { productos ->
            productoAdapter.submitList(productos)
        }
        
        // Observar mensajes de error
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupNavigationChips() {
        // Chip para mesas
        binding.chipMesas.setOnClickListener {
            findNavController().navigate(R.id.mesasFragment)
        }
        
        // Chip para pedidos
        binding.chipPedidos.setOnClickListener {
            findNavController().navigate(R.id.pedidosActivosFragment)
        }
        
        // Chip para categorías
        binding.chipCategorias.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_categoriasFragment)
        }
        
        // Chip para reservas
        binding.chipReservas.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_reservasFragment)
        }
        
        // Mostrar/ocultar chips según el rol del usuario
        val rol = SessionManager.getUserRole()
        if (rol == "admin") {
            binding.chipCategorias.visibility = View.VISIBLE
        } else {
            binding.chipCategorias.visibility = View.GONE
        }
    }
    
    /**
     * Maneja el clic en un producto
     */
    private fun onProductoClick(producto: Producto) {
        // Navegar al detalle del producto
        val action = MainFragmentDirections.actionMainFragmentToDetailFragment(producto.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 