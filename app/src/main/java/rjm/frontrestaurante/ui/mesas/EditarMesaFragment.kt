package rjm.frontrestaurante.ui.mesas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentNuevaMesaBinding
import rjm.frontrestaurante.model.Mesa

/**
 * Fragmento para editar una mesa existente
 */
class EditarMesaFragment : Fragment() {

    private var _binding: FragmentNuevaMesaBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MesasViewModel
    private val args: EditarMesaFragmentArgs by navArgs()
    private var mesaId: Int = 0
    private var mesaActual: Mesa? = null
    
    private val TAG = "EditarMesaFragment"
    
    // Lista de ubicaciones disponibles
    private val ubicacionesList = listOf(
        "Terraza", "Interior", "Comedor", "Barra", "Sin ubicación"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevaMesaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(MesasViewModel::class.java)
        
        // Obtener ID de la mesa a editar
        mesaId = args.mesaId
        
        // Configurar spinner de ubicaciones
        setupUbicacionesSpinner()
        
        // Cargar datos de la mesa
        cargarDatosMesa()
        
        // Configurar botón de guardar
        binding.buttonGuardar.setText(R.string.action_update)
        binding.buttonGuardar.setOnClickListener {
            actualizarMesa()
        }
        
        // Observar resultado de actualización de mesa
        viewModel.resultado.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(context, "Mesa actualizada correctamente", Toast.LENGTH_SHORT).show()
                // Volver al listado de mesas
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
    
    private fun setupUbicacionesSpinner() {
        val adapter = ArrayAdapter(
            requireContext(), 
            android.R.layout.simple_spinner_item, 
            ubicacionesList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        binding.spinnerUbicacion.adapter = adapter
    }
    
    private fun cargarDatosMesa() {
        // Buscar la mesa en la lista actual
        val mesas = viewModel.mesas.value ?: emptyList()
        mesaActual = mesas.find { it.id == mesaId }
        
        mesaActual?.let { mesa ->
            // Cargar datos en los campos
            binding.editTextNumero.setText(mesa.numero.toString())
            binding.editTextCapacidad.setText(mesa.capacidad.toString())
            
            // Seleccionar ubicación en el spinner
            val ubicacion = mesa.ubicacion.ifEmpty { "Sin ubicación" }
            val ubicacionIndex = ubicacionesList.indexOfFirst { it.equals(ubicacion, ignoreCase = true) }
            if (ubicacionIndex >= 0) {
                binding.spinnerUbicacion.setSelection(ubicacionIndex)
            }
        } ?: run {
            // No se encontró la mesa
            Toast.makeText(context, "Error: Mesa no encontrada", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
    }
    
    private fun actualizarMesa() {
        val numeroText = binding.editTextNumero.text.toString().trim()
        val capacidadText = binding.editTextCapacidad.text.toString().trim()
        val ubicacionPosition = binding.spinnerUbicacion.selectedItemPosition
        
        // Validar campos obligatorios
        if (numeroText.isEmpty() || capacidadText.isEmpty()) {
            Toast.makeText(context, R.string.required_fields, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convertir a números
        val numero = try {
            numeroText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "El número de mesa debe ser un número entero", Toast.LENGTH_SHORT).show()
            return
        }
        
        val capacidad = try {
            capacidadText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "La capacidad debe ser un número entero", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Obtener ubicación seleccionada
        val ubicacion = if (ubicacionPosition < ubicacionesList.size - 1) {
            // Usar la ubicación seleccionada (excepto "Sin ubicación")
            ubicacionesList[ubicacionPosition]
        } else {
            // "Sin ubicación" seleccionada, usar cadena vacía
            ""
        }
        
        // Debug
        Log.d(TAG, "Actualizando mesa $mesaId: Número $numero, Capacidad: $capacidad, Ubicación: $ubicacion")
        
        // Actualizar mesa
        viewModel.actualizarMesa(
            id = mesaId,
            numero = numero,
            capacidad = capacidad,
            ubicacion = ubicacion
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 