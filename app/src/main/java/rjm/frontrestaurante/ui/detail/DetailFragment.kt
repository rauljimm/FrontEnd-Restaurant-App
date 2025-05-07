package rjm.frontrestaurante.ui.detail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentDetailBinding
import rjm.frontrestaurante.util.SessionManager
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
        
        // Mostrar/ocultar botón de eliminar según el rol del usuario
        configureDeleteButton()
        
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
            }
        }
        
        // Observar resultado de eliminación 
        viewModel.productoEliminado.observe(viewLifecycleOwner) { eliminado ->
            if (eliminado) {
                Toast.makeText(context, "Producto eliminado correctamente", Toast.LENGTH_SHORT).show()
                // Volver a la pantalla anterior
                findNavController().navigateUp()
            }
        }
        
        // Observar mensaje de error
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun configureDeleteButton() {
        // Mostrar botón de eliminar solo para administradores
        val userRole = SessionManager.getUserRole()
        if (userRole == "admin") {
            binding.buttonEliminarProducto.visibility = View.VISIBLE
            
            // Configurar acción del botón eliminar
            binding.buttonEliminarProducto.setOnClickListener {
                mostrarDialogoConfirmacion()
            }
        } else {
            binding.buttonEliminarProducto.visibility = View.GONE
        }
    }
    
    private fun mostrarDialogoConfirmacion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar producto")
            .setMessage("¿Está seguro que desea eliminar este producto? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarProducto()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 