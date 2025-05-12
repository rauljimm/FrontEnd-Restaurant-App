package rjm.frontrestaurante.ui.productos

import android.os.Bundle
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
import rjm.frontrestaurante.databinding.FragmentSeleccionarProductoBinding
import rjm.frontrestaurante.model.Producto
import rjm.frontrestaurante.ui.pedidos.DetallePedidoViewModel
import rjm.frontrestaurante.util.SessionManager

/**
 * Fragmento para seleccionar productos existentes y añadirlos al pedido
 */
class SeleccionarProductoFragment : Fragment() {

    private var _binding: FragmentSeleccionarProductoBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProductosViewModel
    private lateinit var pedidoViewModel: DetallePedidoViewModel
    private lateinit var productosAdapter: ProductosSeleccionAdapter
    
    private val args: SeleccionarProductoFragmentArgs by navArgs()
    private val TAG = "SeleccionarProductoFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeleccionarProductoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener ID del pedido
        val pedidoId = args.pedidoId
        
        // Inicializar ViewModels
        viewModel = ViewModelProvider(this).get(ProductosViewModel::class.java)
        pedidoViewModel = ViewModelProvider(this).get(DetallePedidoViewModel::class.java)
        pedidoViewModel.setPedidoId(pedidoId)
        
        // Configurar RecyclerView
        productosAdapter = ProductosSeleccionAdapter(
            onSeleccionarProducto = { producto, cantidad ->
                agregarProductoAlPedido(pedidoId, producto, cantidad)
            }
        )
        
        binding.recyclerViewProductos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productosAdapter
        }
        
        // Observar lista de productos
        viewModel.productos.observe(viewLifecycleOwner) { productos ->
            productosAdapter.submitList(productos)
            
            if (productos.isEmpty()) {
                binding.textViewNoProductos.visibility = View.VISIBLE
            } else {
                binding.textViewNoProductos.visibility = View.GONE
            }
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Cargar productos
        viewModel.cargarProductos()
        
        // Configurar botón de finalizar
        binding.buttonFinalizarSeleccion.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun agregarProductoAlPedido(pedidoId: Int, producto: Producto, cantidad: Int) {
        pedidoViewModel.agregarProductoAlPedido(
            pedidoId = pedidoId,
            productoId = producto.id,
            cantidad = cantidad
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adaptador para mostrar productos seleccionables
 */
class ProductosSeleccionAdapter(
    private val onSeleccionarProducto: (Producto, Int) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Producto, ProductosSeleccionAdapter.ProductoViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(oldItem: Producto, newItem: Producto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Producto, newItem: Producto) = oldItem == newItem
    }
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = rjm.frontrestaurante.databinding.ItemProductoSeleccionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductoViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ProductoViewHolder(
        private val binding: rjm.frontrestaurante.databinding.ItemProductoSeleccionBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(producto: Producto) {
            binding.apply {
                textViewNombreProducto.text = producto.nombre
                textViewDescripcionProducto.text = producto.descripcion
                textViewPrecioProducto.text = String.format("%.2f€", producto.precio)
                
                // Configurar botón de añadir
                var cantidad = 1
                textViewCantidad.text = cantidad.toString()
                
                buttonDecrementar.setOnClickListener {
                    if (cantidad > 1) {
                        cantidad--
                        textViewCantidad.text = cantidad.toString()
                    }
                }
                
                buttonIncrementar.setOnClickListener {
                    cantidad++
                    textViewCantidad.text = cantidad.toString()
                }
                
                buttonAgregarProducto.setOnClickListener {
                    onSeleccionarProducto(producto, cantidad)
                    Toast.makeText(root.context, "Producto añadido: ${producto.nombre} x $cantidad", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 