package rjm.frontrestaurante.ui.mesas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentNuevaMesaBinding

/**
 * Fragmento para crear una nueva mesa
 */
class NuevaMesaFragment : Fragment() {

    private var _binding: FragmentNuevaMesaBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MesasViewModel
    private val TAG = "NuevaMesaFragment"

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
        
        // Configurar spinner de ubicaciones (ejemplo)
        val ubicaciones = arrayOf("Terraza", "Interior", "Ventana", "Barra")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ubicaciones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUbicacion.adapter = adapter
        
        // Configurar botón de guardar
        binding.buttonGuardar.setOnClickListener {
            guardarMesa()
        }
        
        // Observar resultado de creación de mesa
        viewModel.resultado.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(context, "Mesa guardada correctamente", Toast.LENGTH_SHORT).show()
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
    
    private fun guardarMesa() {
        val numeroText = binding.editTextNumero.text.toString().trim()
        val capacidadText = binding.editTextCapacidad.text.toString().trim()
        val ubicacion = binding.spinnerUbicacion.selectedItem.toString()
        
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
        
        // Debug
        Log.d(TAG, "Guardando mesa: Número $numero, Capacidad: $capacidad, Ubicación: $ubicacion")
        
        // Crear mesa
        viewModel.crearMesa(numero, capacidad, ubicacion)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 