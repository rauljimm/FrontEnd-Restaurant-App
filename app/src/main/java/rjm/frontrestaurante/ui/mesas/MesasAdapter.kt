package rjm.frontrestaurante.ui.mesas

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.ItemMesaBinding
import rjm.frontrestaurante.model.EstadoMesa
import rjm.frontrestaurante.model.Mesa

// Interface para manejar operaciones de menú contextual
interface MesaMenuCallback {
    fun onEditarMesa(mesa: Mesa)
    fun onEliminarMesa(mesa: Mesa)
}

class MesasAdapter(
    private val onMesaClick: (Mesa) -> Unit,
    private val menuCallback: MesaMenuCallback? = null
) : ListAdapter<Mesa, MesasAdapter.MesaViewHolder>(MesaDiffCallback()) {

    // Posición del item seleccionado en el menú contextual
    private var selectedPosition = RecyclerView.NO_POSITION
    
    // Obtener la mesa en la posición seleccionada
    fun getSelectedMesa(): Mesa? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            getItem(selectedPosition)
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MesaViewHolder(private val binding: ItemMesaBinding) : 
        RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener {
        
        init {
            // Solo activar menú contextual si hay un callback definido
            if (menuCallback != null) {
                itemView.setOnLongClickListener {
                    selectedPosition = adapterPosition
                    false
                }
                itemView.setOnCreateContextMenuListener(this)
            }
        }
        
        override fun onCreateContextMenu(
            menu: ContextMenu?, 
            v: View?, 
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu?.apply {
                add(0, MENU_ITEM_EDITAR, 0, "Editar mesa")
                add(0, MENU_ITEM_ELIMINAR, 1, "Eliminar mesa")
            }
        }
        
        fun bind(mesa: Mesa) {
            val context = binding.root.context
            
            // Configurar texto de mesa
            binding.textViewNumeroMesa.text = context.getString(R.string.numero_mesa, mesa.numero)
            binding.textViewCapacidad.text = context.getString(R.string.capacidad, mesa.capacidad)
            
            // Configurar ubicación
            val ubicacionStr = when {
                mesa.ubicacion.isEmpty() -> context.getString(R.string.sin_ubicacion)
                else -> mesa.ubicacion
            }
            binding.textViewUbicacion.text = context.getString(R.string.ubicacion, ubicacionStr)
            
            // Configurar icono según ubicación
            val iconoUbicacion = when (mesa.ubicacion.lowercase()) {
                "terraza" -> R.drawable.ic_location_outdoor
                "interior" -> R.drawable.ic_location_indoor
                "comedor" -> R.drawable.ic_location_dining
                "barra" -> R.drawable.ic_location_bar
                else -> R.drawable.ic_table
            }
            binding.iconUbicacion.setImageResource(iconoUbicacion)
            
            // Configurar color y texto según el estado
            val (colorRes, estadoText) = when (mesa.estado) {
                EstadoMesa.LIBRE -> Pair(R.color.mesa_libre, R.string.mesa_libre)
                EstadoMesa.OCUPADA -> Pair(R.color.mesa_ocupada, R.string.mesa_ocupada)
                EstadoMesa.RESERVADA -> Pair(R.color.mesa_reservada, R.string.mesa_reservada)
                EstadoMesa.MANTENIMIENTO -> Pair(R.color.mesa_mantenimiento, R.string.mesa_mantenimiento)
            }
            
            binding.textViewEstado.text = context.getString(R.string.estado, 
                context.getString(estadoText))
            binding.cardViewMesa.setCardBackgroundColor(
                ContextCompat.getColor(context, colorRes)
            )
            
            // Configurar clic
            binding.root.setOnClickListener {
                onMesaClick(mesa)
            }
        }
    }
    
    companion object {
        const val MENU_ITEM_EDITAR = 1
        const val MENU_ITEM_ELIMINAR = 2
    }
}

class MesaDiffCallback : DiffUtil.ItemCallback<Mesa>() {
    override fun areItemsTheSame(oldItem: Mesa, newItem: Mesa): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Mesa, newItem: Mesa): Boolean {
        return oldItem == newItem
    }
} 