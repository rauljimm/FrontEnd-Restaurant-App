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
import rjm.frontrestaurante.adapter.ProductosPorCategoriaAdapter
import rjm.frontrestaurante.databinding.FragmentMainBinding
import rjm.frontrestaurante.model.Producto
import rjm.frontrestaurante.util.SessionManager

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    private lateinit var productosPorCategoriaAdapter: ProductosPorCategoriaAdapter

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
        productosPorCategoriaAdapter = ProductosPorCategoriaAdapter { producto -> onProductoClick(producto) }
        binding.recyclerViewProductos.adapter = productosPorCategoriaAdapter
        
        // Cargar productos por categoría
        viewModel.cargarProductosPorCategoria()
        
        // Observar productos por categoría
        viewModel.productosPorCategoria.observe(viewLifecycleOwner) { productosPorCategoria ->
            productosPorCategoriaAdapter.actualizarProductosPorCategoria(productosPorCategoria)
            
            // Si no hay productos, mostrar mensaje
            if (productosPorCategoria.isEmpty()) {
                Toast.makeText(context, "No hay productos disponibles", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observar mensajes de error
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
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