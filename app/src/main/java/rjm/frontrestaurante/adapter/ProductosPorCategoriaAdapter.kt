package rjm.frontrestaurante.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import rjm.frontrestaurante.R
import rjm.frontrestaurante.model.Categoria
import rjm.frontrestaurante.model.Producto
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador para mostrar productos agrupados por categorías
 */
class ProductosPorCategoriaAdapter(
    private val onProductoClick: (Producto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_CATEGORIA = 0
    private val VIEW_TYPE_PRODUCTO = 1
    
    // Lista que contiene todos los elementos a mostrar (categorías y productos)
    private val items = mutableListOf<Any>()
    
    // Mapa para controlar qué categorías están expandidas
    private val expandedCategories = mutableMapOf<Int, Boolean>()
    
    /**
     * Actualiza la lista de productos por categoría
     */
    fun actualizarProductosPorCategoria(productosPorCategoria: Map<Categoria, List<Producto>>) {
        items.clear()
        
        // Depuración
        android.util.Log.d("ProductosAdapter", "Actualizando adaptador con ${productosPorCategoria.size} categorías")
        
        // Inicialmente todas las categorías están expandidas
        productosPorCategoria.keys.forEach { categoria ->
            expandedCategories[categoria.id] = true
            android.util.Log.d("ProductosAdapter", "Categoría ${categoria.nombre} con ${productosPorCategoria[categoria]?.size ?: 0} productos")
        }
        
        // Crear lista plana con categorías y productos
        productosPorCategoria.forEach { (categoria, productos) ->
            items.add(categoria)
            
            // Solo añadir productos si la categoría está expandida
            if (expandedCategories[categoria.id] == true) {
                items.addAll(productos)
                android.util.Log.d("ProductosAdapter", "Añadidos ${productos.size} productos de categoría ${categoria.nombre}")
            }
        }
        
        android.util.Log.d("ProductosAdapter", "Total de items en el adaptador: ${items.size}")
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Categoria -> VIEW_TYPE_CATEGORIA
            is Producto -> VIEW_TYPE_PRODUCTO
            else -> throw IllegalArgumentException("Tipo de vista no soportado")
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORIA -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_categoria_header, parent, false)
                CategoriaViewHolder(view, this::toggleCategoria)
            }
            VIEW_TYPE_PRODUCTO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_producto, parent, false)
                ProductoViewHolder(view)
            }
            else -> throw IllegalArgumentException("Tipo de vista no soportado")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoriaViewHolder -> {
                val categoria = items[position] as Categoria
                holder.bind(categoria, expandedCategories[categoria.id] ?: true)
            }
            is ProductoViewHolder -> {
                holder.bind(items[position] as Producto)
            }
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    /**
     * Expande o colapsa una categoría
     */
    private fun toggleCategoria(categoria: Categoria) {
        // Buscar la posición de la categoría
        val categoriaPosition = items.indexOfFirst { it is Categoria && it.id == categoria.id }
        if (categoriaPosition == -1) return
        
        // Cambiar estado de expansión
        val isExpanded = !(expandedCategories[categoria.id] ?: true)
        expandedCategories[categoria.id] = isExpanded
        
        // Obtener los productos de esta categoría
        val productos = items.filterIsInstance<Producto>().filter { it.categoriaId == categoria.id }
        
        if (isExpanded) {
            // Añadir productos después de la categoría
            items.addAll(categoriaPosition + 1, productos)
            notifyItemRangeInserted(categoriaPosition + 1, productos.size)
        } else {
            // Eliminar productos después de la categoría
            var removeCount = 0
            var i = categoriaPosition + 1
            while (i < items.size && items[i] is Producto && (items[i] as Producto).categoriaId == categoria.id) {
                items.removeAt(i)
                removeCount++
            }
            notifyItemRangeRemoved(categoriaPosition + 1, removeCount)
        }
        
        // Actualizar el icono de expansión
        notifyItemChanged(categoriaPosition)
    }
    
    /**
     * ViewHolder para categorías
     */
    class CategoriaViewHolder(
        itemView: View,
        private val onCategoriaClick: (Categoria) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textViewCategoriaHeader: TextView = itemView.findViewById(R.id.textViewCategoriaHeader)
        private val imageViewExpand: ImageView = itemView.findViewById(R.id.imageViewExpand)
        
        fun bind(categoria: Categoria, isExpanded: Boolean) {
            textViewCategoriaHeader.text = categoria.nombre
            
            // Actualizar icono según estado de expansión
            imageViewExpand.setImageResource(
                if (isExpanded) android.R.drawable.arrow_up_float
                else android.R.drawable.arrow_down_float
            )
            
            // Configurar clic para expandir/colapsar
            itemView.setOnClickListener { onCategoriaClick(categoria) }
        }
    }
    
    /**
     * ViewHolder para productos
     */
    inner class ProductoViewHolder(itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
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
            
            // Ocultar categoría, ya que se muestra en el header
            textViewCategoria.visibility = View.GONE
            
            // Configurar clic
            itemView.setOnClickListener { onProductoClick(producto) }
        }
    }
} 