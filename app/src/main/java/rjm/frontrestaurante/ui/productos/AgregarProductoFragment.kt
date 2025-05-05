package rjm.frontrestaurante.ui.productos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rjm.frontrestaurante.R
import rjm.frontrestaurante.api.ApiClient
import rjm.frontrestaurante.databinding.FragmentAgregarProductoBinding
import rjm.frontrestaurante.model.Categoria
import rjm.frontrestaurante.ui.categorias.CategoriasViewModel
import rjm.frontrestaurante.util.SessionManager

/**
 * Fragment para agregar nuevos productos (solo disponible para administradores)
 */
class AgregarProductoFragment : Fragment() {

    private var _binding: FragmentAgregarProductoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProductosViewModel
    private lateinit var categoriasViewModel: CategoriasViewModel
    private val args: AgregarProductoFragmentArgs by navArgs()
    private val TAG = "AgregarProductoFragment"
    
    // Lista de categorías que se cargará desde la API
    private var categorias = listOf<Categoria>()
    // ID del pedido si se está añadiendo a un pedido, -1 si se está creando un nuevo producto
    private var pedidoId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgregarProductoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener el ID del pedido de los argumentos
        pedidoId = args.pedidoId
        
        // Comprobar si estamos creando un producto (admin) o añadiendo a un pedido
        val esCreacionProducto = pedidoId == -1
        Log.d(TAG, "Modo: ${if (esCreacionProducto) "creación de producto" else "añadir a pedido"}")
        
        // Inicializar ViewModels
        viewModel = ViewModelProvider(this).get(ProductosViewModel::class.java)
        categoriasViewModel = ViewModelProvider(this).get(CategoriasViewModel::class.java)
        
        // Cargar las categorías desde la API
        categoriasViewModel.cargarCategorias()
        
        // Observar las categorías
        categoriasViewModel.categorias.observe(viewLifecycleOwner) { listaCategorias ->
            categorias = listaCategorias
            actualizarSpinnerCategorias()
        }
        
        // Observar errores de carga de categorías
        categoriasViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, "Error al cargar categorías: $error", Toast.LENGTH_LONG).show()
            }
        }
        
        // Configurar botón para crear nueva categoría
        binding.buttonCrearCategoria.setOnClickListener {
            findNavController().navigate(R.id.action_agregarProductoFragment_to_nuevaCategoriaFragment)
        }
        
        // Configurar spinner de tipos de producto
        val tiposProducto = arrayOf(
            getString(R.string.tipo_comida),
            getString(R.string.tipo_bebida),
            getString(R.string.tipo_postre),
            getString(R.string.tipo_entrada),
            getString(R.string.tipo_complemento)
        )
        val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposProducto)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTipoProducto.adapter = adapterTipos
        
        // Configurar botón de guardar
        binding.buttonGuardar.setOnClickListener {
            guardarProducto()
        }
        
        // Observar resultado de creación de producto
        viewModel.resultado.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(context, "Producto guardado correctamente", Toast.LENGTH_SHORT).show()
                // Volver al listado de productos
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
    
    override fun onResume() {
        super.onResume()
        // Recargar categorías cuando se vuelva a este fragmento
        // (por ejemplo, después de crear una nueva categoría)
        categoriasViewModel.cargarCategorias()
    }
    
    private fun actualizarSpinnerCategorias() {
        if (categorias.isEmpty()) {
            binding.buttonGuardar.isEnabled = false
            binding.textViewNoCategories.visibility = View.VISIBLE
            binding.buttonCrearCategoria.visibility = View.VISIBLE
            return
        }
        
        binding.buttonGuardar.isEnabled = true
        binding.textViewNoCategories.visibility = View.GONE
        binding.buttonCrearCategoria.visibility = View.GONE
        
        val nombresCategorias = categorias.map { it.nombre }.toTypedArray()
        val adapterCategorias = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nombresCategorias)
        adapterCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoria.adapter = adapterCategorias
    }
    
    private fun guardarProducto() {
        val nombre = binding.editTextNombre.text.toString().trim()
        val descripcion = binding.editTextDescripcion.text.toString().trim()
        val precioText = binding.editTextPrecio.text.toString().trim()
        val tiempoPreparacionText = binding.editTextTiempoPreparacion.text.toString().trim()
        val tipo = binding.spinnerTipoProducto.selectedItem.toString()
        val categoriaIndex = binding.spinnerCategoria.selectedItemPosition
        
        // Validar campos obligatorios
        if (nombre.isEmpty() || precioText.isEmpty() || tiempoPreparacionText.isEmpty() || categorias.isEmpty()) {
            Toast.makeText(context, R.string.required_fields, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convertir precio a Double
        val precio = try {
            precioText.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "El precio debe ser un número válido", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convertir tiempo de preparación a Int
        val tiempoPreparacion = try {
            tiempoPreparacionText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "El tiempo de preparación debe ser un número entero", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Obtener ID de categoría seleccionada
        val categoriaId = categorias[categoriaIndex].id
        
        // Debug
        Log.d(TAG, "Guardando producto: $nombre, Precio: $precio, Categoría: $categoriaId, Tipo: $tipo, Tiempo: $tiempoPreparacion")
        
        // Crear producto con todos los parámetros
        viewModel.crearProducto(nombre, descripcion, precio, tipo, categoriaId, tiempoPreparacion)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 