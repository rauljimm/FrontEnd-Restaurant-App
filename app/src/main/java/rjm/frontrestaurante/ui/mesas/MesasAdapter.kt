package rjm.frontrestaurante.ui.mesas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.ItemMesaBinding
import rjm.frontrestaurante.model.EstadoMesa
import rjm.frontrestaurante.model.Mesa

class MesasAdapter(private val onMesaClick: (Mesa) -> Unit) : 
    ListAdapter<Mesa, MesasAdapter.MesaViewHolder>(MesaDiffCallback()) {

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
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(mesa: Mesa) {
            binding.textViewNumeroMesa.text = binding.root.context.getString(R.string.numero_mesa, mesa.numero)
            binding.textViewCapacidad.text = binding.root.context.getString(R.string.capacidad, mesa.capacidad)
            
            // Configurar color y texto según el estado
            val (colorRes, estadoText) = when (mesa.estado) {
                EstadoMesa.LIBRE -> Pair(R.color.mesa_libre, R.string.mesa_libre)
                EstadoMesa.OCUPADA -> Pair(R.color.mesa_ocupada, R.string.mesa_ocupada)
                EstadoMesa.RESERVADA -> Pair(R.color.mesa_reservada, R.string.mesa_reservada)
                EstadoMesa.MANTENIMIENTO -> Pair(R.color.mesa_mantenimiento, R.string.mesa_mantenimiento)
            }
            
            binding.textViewEstado.text = binding.root.context.getString(R.string.estado, 
                binding.root.context.getString(estadoText))
            binding.cardViewMesa.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )
            
            // Configurar clic
            binding.root.setOnClickListener {
                onMesaClick(mesa)
            }
        }
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