package rjm.frontrestaurante.ui.pedidos

import android.os.Bundle
import android.util.Log
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
import rjm.frontrestaurante.databinding.FragmentNuevoPedidoBinding
import rjm.frontrestaurante.model.Producto

/**
 * Fragmento para crear un nuevo pedido
 */
class NuevoPedidoFragment : Fragment() {

    private var _binding: FragmentNuevoPedidoBinding? = null
    private val binding get() = _binding!!
    
    private val args: NuevoPedidoFragmentArgs by navArgs()
    private lateinit var viewModel: NuevoPedidoViewModel
    private lateinit var productosAdapter: ProductosSeleccionAdapter
    
    private val TAG = "NuevoPedidoFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevoPedidoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(NuevoPedidoViewModel::class.java)
        viewModel.setMesaId(args.mesaId)
        
        // Configurar RecyclerView para productos
        productosAdapter = ProductosSeleccionAdapter(
            onProductoClick = { producto ->
                viewModel.agregarProductoAlPedido(producto, 1)
            }
        )
        
        binding.recyclerViewProductos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productosAdapter
        }
        
        // Configurar botón de enviar pedido
        binding.buttonEnviarPedido.setOnClickListener {
            val observaciones = binding.editTextObservaciones.text.toString().trim()
            viewModel.crearPedido(observaciones)
        }
        
        // Observar productos disponibles
        viewModel.productos.observe(viewLifecycleOwner) { productos ->
            productosAdapter.submitList(productos)
            binding.textViewNoProductos.visibility = if (productos.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Observar productos seleccionados
        viewModel.productosSeleccionados.observe(viewLifecycleOwner) { productosSeleccionados ->
            // Actualizar UI para mostrar los productos seleccionados
            actualizarResumenPedido(productosSeleccionados)
            
            // Habilitar/deshabilitar botón de enviar según si hay productos seleccionados
            binding.buttonEnviarPedido.isEnabled = productosSeleccionados.isNotEmpty()
        }
        
        // Observar resultado de creación de pedido
        viewModel.pedidoCreado.observe(viewLifecycleOwner) { exito ->
            if (exito) {
                Toast.makeText(context, "Pedido creado correctamente", Toast.LENGTH_SHORT).show()
                // Volver a la pantalla anterior
                findNavController().popBackStack()
            }
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        }
        
        // Cargar productos
        viewModel.cargarProductos()
    }
    
    private fun actualizarResumenPedido(productos: Map<Producto, Int>) {
        if (productos.isEmpty()) {
            binding.cardViewResumenPedido.visibility = View.GONE
            return
        }
        
        binding.cardViewResumenPedido.visibility = View.VISIBLE
        
        // Calcular total
        var total = 0.0
        val resumen = StringBuilder()
        
        productos.forEach { (producto, cantidad) ->
            val subtotal = producto.precio * cantidad
            total += subtotal
            resumen.append("${producto.nombre} x $cantidad = $subtotal€\n")
        }
        
        binding.textViewResumenPedido.text = resumen.toString()
        binding.textViewTotalPedido.text = String.format("Total: %.2f€", total)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adaptador para la lista de productos seleccionables
 */
class ProductosSeleccionAdapter(
    private val onProductoClick: (Producto) -> Unit
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
                
                // Configurar click
                root.setOnClickListener { onProductoClick(producto) }
                buttonAgregarProducto.setOnClickListener { onProductoClick(producto) }
            }
        }
    }
} 