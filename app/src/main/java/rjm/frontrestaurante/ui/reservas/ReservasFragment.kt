package rjm.frontrestaurante.ui.reservas

import android.app.DatePickerDialog
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
import rjm.frontrestaurante.databinding.FragmentReservasBinding
import rjm.frontrestaurante.model.EstadoReserva
import rjm.frontrestaurante.model.Reserva
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragmento para listar y gestionar reservas
 */
class ReservasFragment : Fragment() {

    private var _binding: FragmentReservasBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ReservasViewModel
    private lateinit var adapter: ReservasAdapter
    
    private var fechaFiltro: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(ReservasViewModel::class.java)
        
        // Configurar RecyclerView y adaptador
        setupRecyclerView()
        
        // Configurar filtro de fecha
        binding.buttonFiltrarFecha.setOnClickListener {
            mostrarSelectorFecha()
        }
        
        // Configurar botón para limpiar filtro
        binding.buttonLimpiarFiltro.setOnClickListener {
            fechaFiltro = null
            binding.textViewFechaFiltro.text = "Todas las fechas"
            cargarReservas()
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        }
        
        // Cargar reservas iniciales
        cargarReservas()
    }
    
    private fun setupRecyclerView() {
        adapter = ReservasAdapter(
            onCancelarListener = { reserva ->
                viewModel.actualizarEstadoReserva(reserva.id, EstadoReserva.CANCELADA)
            },
            onConfirmarListener = { reserva ->
                viewModel.actualizarEstadoReserva(reserva.id, EstadoReserva.CONFIRMADA)
            },
            onCompletarListener = { reserva ->
                viewModel.actualizarEstadoReserva(reserva.id, EstadoReserva.COMPLETADA)
            }
        )
        
        binding.recyclerViewReservas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ReservasFragment.adapter
        }
        
        // Observar la lista de reservas
        viewModel.reservas.observe(viewLifecycleOwner) { reservas ->
            adapter.submitList(reservas)
            binding.textViewEmpty.visibility = if (reservas.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun mostrarSelectorFecha() {
        val calendar = Calendar.getInstance()
        if (fechaFiltro != null) {
            calendar.time = fechaFiltro!!
        }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, y, m, d ->
            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            calendar.set(Calendar.DAY_OF_MONTH, d)
            fechaFiltro = calendar.time
            
            // Actualizar texto del filtro
            binding.textViewFechaFiltro.text = "Fecha: ${dateFormat.format(fechaFiltro!!)}"
            
            // Cargar reservas con filtro
            cargarReservas()
        }, year, month, day).show()
    }
    
    private fun cargarReservas() {
        viewModel.cargarReservas(fechaFiltro)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adaptador para la lista de reservas
 */
class ReservasAdapter(
    private val onCancelarListener: (Reserva) -> Unit,
    private val onConfirmarListener: (Reserva) -> Unit,
    private val onCompletarListener: (Reserva) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Reserva, ReservasAdapter.ReservaViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Reserva>() {
        override fun areItemsTheSame(oldItem: Reserva, newItem: Reserva) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Reserva, newItem: Reserva) = oldItem == newItem
    }
) {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val binding = rjm.frontrestaurante.databinding.ItemReservaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReservaViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ReservaViewHolder(
        private val binding: rjm.frontrestaurante.databinding.ItemReservaBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(reserva: Reserva) {
            binding.apply {
                // Configurar los textos
                textViewNombreCliente.text = reserva.clienteNombre
                textViewTelefonoCliente.text = reserva.clienteTelefono
                textViewFechaHora.text = "${dateFormat.format(reserva.fecha)} ${reserva.hora}"
                textViewMesaPersonas.text = "Mesa ${reserva.mesaId} - ${reserva.numPersonas} personas"
                
                // Configurar estado y botones según el estado actual
                when (reserva.estado) {
                    EstadoReserva.PENDIENTE -> {
                        textViewEstado.text = "PENDIENTE"
                        textViewEstado.setTextColor(root.context.getColor(R.color.estado_pendiente))
                        buttonConfirmar.visibility = View.VISIBLE
                        buttonCancelar.visibility = View.VISIBLE
                        buttonCompletar.visibility = View.GONE
                    }
                    EstadoReserva.CONFIRMADA -> {
                        textViewEstado.text = "CONFIRMADA"
                        textViewEstado.setTextColor(root.context.getColor(R.color.estado_confirmado))
                        buttonConfirmar.visibility = View.GONE
                        buttonCancelar.visibility = View.VISIBLE
                        buttonCompletar.visibility = View.VISIBLE
                    }
                    EstadoReserva.CANCELADA -> {
                        textViewEstado.text = "CANCELADA"
                        textViewEstado.setTextColor(root.context.getColor(R.color.estado_cancelado))
                        buttonConfirmar.visibility = View.GONE
                        buttonCancelar.visibility = View.GONE
                        buttonCompletar.visibility = View.GONE
                    }
                    EstadoReserva.COMPLETADA -> {
                        textViewEstado.text = "COMPLETADA"
                        textViewEstado.setTextColor(root.context.getColor(R.color.estado_completado))
                        buttonConfirmar.visibility = View.GONE
                        buttonCancelar.visibility = View.GONE
                        buttonCompletar.visibility = View.GONE
                    }
                }
                
                // Configurar listeners de botones
                buttonCancelar.setOnClickListener { onCancelarListener(reserva) }
                buttonConfirmar.setOnClickListener { onConfirmarListener(reserva) }
                buttonCompletar.setOnClickListener { onCompletarListener(reserva) }
                
                // Mostrar observaciones si las hay
                if (reserva.observaciones.isNotEmpty()) {
                    textViewObservaciones.visibility = View.VISIBLE
                    textViewObservaciones.text = reserva.observaciones
                } else {
                    textViewObservaciones.visibility = View.GONE
                }
            }
        }
    }
} 