package rjm.frontrestaurante.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import rjm.frontrestaurante.R
import rjm.frontrestaurante.model.Producto
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador para mostrar productos en un RecyclerView
 */
class ProductoAdapter(private val onProductoClick: (Producto) -> Unit) : 
    ListAdapter<Producto, ProductoAdapter.ProductoViewHolder>(ProductoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = getItem(position)
        holder.bind(producto)
        holder.itemView.setOnClickListener { onProductoClick(producto) }
    }

    /**
     * ViewHolder para productos
     */
    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewProducto: ImageView = itemView.findViewById(R.id.imageViewProducto)
        private val textViewNombre: TextView = itemView.findViewById(R.id.textViewNombre)
        private val textViewDescripcion: TextView = itemView.findViewById(R.id.textViewDescripcion)
        private val textViewPrecio: TextView = itemView.findViewById(R.id.textViewPrecio)
        private val textViewCategoria: TextView = itemView.findViewById(R.id.textViewCategoria)

        fun bind(producto: Producto) {
            textViewNombre.text = producto.nombre
            textViewDescripcion.text = producto.descripcion
            
            // Formatear precio
            val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
            textViewPrecio.text = formatoMoneda.format(producto.precio)
            
            textViewCategoria.text = "Categoría ${producto.categoriaId}"
            
            // Cargar imagen con Glide
            producto.imagen?.let { url ->
                Glide.with(itemView.context)
                    .load(url)
                    .placeholder(R.drawable.placeholder_producto)
                    .error(R.drawable.error_producto)
                    .into(imageViewProducto)
            } ?: run {
                // Si no hay imagen, mostrar placeholder
                imageViewProducto.setImageResource(R.drawable.placeholder_producto)
            }
        }
    }

    /**
     * DiffUtil para optimizar actualizaciones del RecyclerView
     */
    class ProductoDiffCallback : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(oldItem: Producto, newItem: Producto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Producto, newItem: Producto): Boolean {
            return oldItem == newItem
        }
    }
} 