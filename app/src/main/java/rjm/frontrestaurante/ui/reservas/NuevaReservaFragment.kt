package rjm.frontrestaurante.ui.reservas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentNuevaReservaBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragmento para crear una nueva reserva
 */
class NuevaReservaFragment : Fragment() {

    private var _binding: FragmentNuevaReservaBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ReservasViewModel
    private val args: NuevaReservaFragmentArgs by navArgs()
    private val TAG = "NuevaReservaFragment"
    
    private var fechaSeleccionada = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevaReservaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(ReservasViewModel::class.java)
        
        // Configurar selectores de fecha y hora
        setupDateTimePickers()
        
        // Configurar números de personas
        val personas = (1..12).map { it.toString() }.toTypedArray()
        val adapterPersonas = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, personas)
        adapterPersonas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPersonas.adapter = adapterPersonas
        
        // Configurar botón de guardar
        binding.buttonGuardar.setOnClickListener {
            guardarReserva()
        }
        
        // Mostrar mesa seleccionada
        binding.textViewMesa.text = getString(R.string.mesa_num, args.mesaId)
        
        // Observar resultado de creación de reserva
        viewModel.operationSuccess.observe(viewLifecycleOwner) { exitoso ->
            if (exitoso) {
                Toast.makeText(context, "Reserva guardada correctamente", Toast.LENGTH_SHORT).show()
                // Volver al listado
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
    
    private fun setupDateTimePickers() {
        // Configurar fecha actual
        updateDateDisplay()
        updateTimeDisplay()
        
        // Selector de fecha
        binding.buttonFecha.setOnClickListener {
            val year = fechaSeleccionada.get(Calendar.YEAR)
            val month = fechaSeleccionada.get(Calendar.MONTH)
            val day = fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
            
            DatePickerDialog(requireContext(), { _, y, m, d ->
                fechaSeleccionada.set(Calendar.YEAR, y)
                fechaSeleccionada.set(Calendar.MONTH, m)
                fechaSeleccionada.set(Calendar.DAY_OF_MONTH, d)
                updateDateDisplay()
            }, year, month, day).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
                show()
            }
        }
        
        // Selector de hora
        binding.buttonHora.setOnClickListener {
            val hour = fechaSeleccionada.get(Calendar.HOUR_OF_DAY)
            val minute = fechaSeleccionada.get(Calendar.MINUTE)
            
            TimePickerDialog(requireContext(), { _, h, m ->
                fechaSeleccionada.set(Calendar.HOUR_OF_DAY, h)
                fechaSeleccionada.set(Calendar.MINUTE, m)
                updateTimeDisplay()
            }, hour, minute, true).show()
        }
    }
    
    private fun updateDateDisplay() {
        binding.textViewFecha.text = dateFormat.format(fechaSeleccionada.time)
    }
    
    private fun updateTimeDisplay() {
        binding.textViewHora.text = timeFormat.format(fechaSeleccionada.time)
    }
    
    private fun guardarReserva() {
        val clienteNombre = binding.editTextNombre.text.toString().trim()
        val clienteApellido = binding.editTextApellido.text.toString().trim()
        val clienteTelefono = binding.editTextTelefono.text.toString().trim()
        val observaciones = binding.editTextObservaciones.text.toString().trim()
        val numPersonas = binding.spinnerPersonas.selectedItem.toString().toInt()
        
        // Validar campos obligatorios
        if (clienteNombre.isEmpty() || clienteApellido.isEmpty() || clienteTelefono.isEmpty()) {
            Toast.makeText(context, R.string.required_fields, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Crear reserva
        viewModel.crearReserva(
            mesaId = args.mesaId,
            clienteNombre = clienteNombre,
            clienteApellido = clienteApellido,
            clienteTelefono = clienteTelefono,
            fecha = fechaSeleccionada.time,
            hora = timeFormat.format(fechaSeleccionada.time),
            numPersonas = numPersonas,
            observaciones = observaciones
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 