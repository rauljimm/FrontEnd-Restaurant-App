package rjm.frontrestaurante.ui.pedidos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.ItemPedidoActivoBinding
import rjm.frontrestaurante.model.EstadoPedido
import rjm.frontrestaurante.model.Pedido
import rjm.frontrestaurante.util.SessionManager

/**
 * Adaptador para la lista de pedidos activos
 */
class PedidosActivosAdapter(
    private val onPedidoClick: (Pedido) -> Unit,
    private val onMarcarListoClick: (Pedido) -> Unit
) : ListAdapter<Pedido, PedidosActivosAdapter.PedidoViewHolder>(
    object : DiffUtil.ItemCallback<Pedido>() {
        override fun areItemsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem == newItem
    }
) {
    
    private val isCocinero = SessionManager.getUserRole() == "cocinero"
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoActivoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PedidoViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class PedidoViewHolder(
        private val binding: ItemPedidoActivoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(pedido: Pedido) {
            binding.apply {
                textViewIdPedido.text = "Pedido #${pedido.id}"
                textViewNumeroMesa.text = "Mesa ${pedido.mesaId}"
                textViewEstadoPedido.text = when(pedido.estado) {
                    EstadoPedido.RECIBIDO -> root.context.getString(R.string.pedido_recibido)
                    EstadoPedido.EN_PREPARACION -> root.context.getString(R.string.pedido_en_preparacion)
                    EstadoPedido.LISTO -> root.context.getString(R.string.pedido_listo)
                    EstadoPedido.ENTREGADO -> root.context.getString(R.string.pedido_entregado)
                    EstadoPedido.CANCELADO -> root.context.getString(R.string.pedido_cancelado)
                }
                textViewFechaPedido.text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", pedido.fecha)
                
                // Configurar visibilidad del botón según rol y estado
                if (isCocinero) {
                    // Cocineros: Botón para iniciar preparación si está RECIBIDO
                    // o botón para marcar como listo si está EN_PREPARACION
                    when(pedido.estado) {
                        EstadoPedido.RECIBIDO -> {
                            buttonMarcarListo.visibility = View.VISIBLE
                            buttonMarcarListo.text = "Iniciar preparación"
                            buttonMarcarListo.setOnClickListener {
                                onMarcarListoClick(pedido)
                            }
                        }
                        EstadoPedido.EN_PREPARACION -> {
                            buttonMarcarListo.visibility = View.VISIBLE
                            buttonMarcarListo.text = root.context.getString(R.string.action_mark_ready)
                            buttonMarcarListo.setOnClickListener {
                                onMarcarListoClick(pedido)
                            }
                        }
                        else -> {
                            buttonMarcarListo.visibility = View.GONE
                        }
                    }
                } else {
                    // Camareros: Solo ven botón para pedidos LISTOS
                    buttonMarcarListo.visibility = if (pedido.estado == EstadoPedido.LISTO) View.VISIBLE else View.GONE
                    buttonMarcarListo.text = root.context.getString(R.string.action_mark_delivered)
                }
                
                // Configurar colores según estado
                val color = when(pedido.estado) {
                    EstadoPedido.RECIBIDO -> R.color.pedido_recibido
                    EstadoPedido.EN_PREPARACION -> R.color.pedido_en_preparacion
                    EstadoPedido.LISTO -> R.color.pedido_listo
                    EstadoPedido.ENTREGADO -> R.color.pedido_entregado
                    EstadoPedido.CANCELADO -> R.color.pedido_cancelado
                }
                cardViewPedido.setCardBackgroundColor(root.resources.getColor(color, null))
                
                // Configurar clic en el pedido completo
                root.setOnClickListener { onPedidoClick(pedido) }
            }
        }
    }
} 