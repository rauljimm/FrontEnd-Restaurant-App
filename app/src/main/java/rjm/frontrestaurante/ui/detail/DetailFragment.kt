package rjm.frontrestaurante.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentDetailBinding
import java.text.NumberFormat
import java.util.Locale

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: DetailViewModel
    private val args: DetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel con el ID del producto
        val factory = DetailViewModelFactory(args.productId)
        viewModel = ViewModelProvider(this, factory).get(DetailViewModel::class.java)
        
        // Cargar detalles del producto
        viewModel.cargarProducto()
        
        // Observar el producto
        viewModel.producto.observe(viewLifecycleOwner) { producto ->
            producto?.let {
                // Actualizar UI con los datos del producto
                binding.textViewNombre.text = it.nombre
                binding.textViewDescripcion.text = it.descripcion
                binding.textViewCategoria.text = "Categoría ${it.categoriaId}"
                
                // Formatear precio
                val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
                binding.textViewPrecio.text = formatoMoneda.format(it.precio)
                
                // Cargar imagen con Glide
                it.imagen?.let { url ->
                    Glide.with(requireContext())
                        .load(url)
                        .placeholder(R.drawable.placeholder_producto)
                        .error(R.drawable.error_producto)
                        .into(binding.imageViewProducto)
                } ?: run {
                    // Si no hay imagen, mostrar placeholder
                    binding.imageViewProducto.setImageResource(R.drawable.placeholder_producto)
                }
            }
        }
        
        // Observar mensaje de error
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 