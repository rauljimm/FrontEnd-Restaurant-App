package rjm.frontrestaurante.ui.categorias

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentNuevaCategoriaBinding

/**
 * Fragmento para crear una nueva categoría de productos
 */
class NuevaCategoriaFragment : Fragment() {

    private var _binding: FragmentNuevaCategoriaBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: CategoriasViewModel
    private val TAG = "NuevaCategoriaFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevaCategoriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(CategoriasViewModel::class.java)
        
        // Configurar botón de guardar
        binding.buttonGuardar.setOnClickListener {
            guardarCategoria()
        }
        
        // Observar resultado de creación de categoría
        viewModel.operationSuccess.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(context, "Categoría guardada correctamente", Toast.LENGTH_SHORT).show()
                // Volver al listado de categorías
                requireActivity().onBackPressed()
            }
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun guardarCategoria() {
        val nombre = binding.editTextNombre.text.toString().trim()
        val descripcion = binding.editTextDescripcion.text.toString().trim()
        
        // Validar campo obligatorio
        if (nombre.isEmpty()) {
            Toast.makeText(context, R.string.required_fields, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Crear categoría
        viewModel.crearCategoria(nombre, descripcion)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 