package rjm.frontrestaurante.ui.categorias

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
import rjm.frontrestaurante.databinding.FragmentCategoriasBinding
import rjm.frontrestaurante.model.Categoria
import rjm.frontrestaurante.util.SessionManager

/**
 * Fragmento para listar y gestionar categorías
 */
class CategoriasFragment : Fragment() {

    private var _binding: FragmentCategoriasBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: CategoriasViewModel
    private lateinit var adapter: CategoriasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(CategoriasViewModel::class.java)
        
        // Configurar RecyclerView y adaptador
        setupRecyclerView()
        
        // Configurar botón para añadir nueva categoría
        binding.fabAddCategoria.setOnClickListener {
            findNavController().navigate(R.id.action_categoriasFragment_to_nuevaCategoriaFragment)
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
        
        // Mostrar u ocultar FAB según rol del usuario
        val rol = SessionManager.getUserRole()
        if (rol == "admin") {
            binding.fabAddCategoria.visibility = View.VISIBLE
        } else {
            binding.fabAddCategoria.visibility = View.GONE
        }
        
        // Cargar categorías
        viewModel.cargarCategorias()
    }
    
    private fun setupRecyclerView() {
        adapter = CategoriasAdapter(
            onClickListener = { categoria ->
                // Al hacer clic en una categoría navegar al detalle o edición
                // Implementación dependiendo de los requerimientos
            },
            onDeleteListener = { categoria ->
                // Eliminar categoría (solo admin)
                if (SessionManager.getUserRole() == "admin") {
                    viewModel.eliminarCategoria(categoria.id)
                }
            }
        )
        
        binding.recyclerViewCategorias.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CategoriasFragment.adapter
        }
        
        // Observar la lista de categorías
        viewModel.categorias.observe(viewLifecycleOwner) { categorias ->
            adapter.submitList(categorias)
            binding.textViewEmpty.visibility = if (categorias.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adaptador para la lista de categorías
 */
class CategoriasAdapter(
    private val onClickListener: (Categoria) -> Unit,
    private val onDeleteListener: (Categoria) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Categoria, CategoriasAdapter.CategoriaViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Categoria>() {
        override fun areItemsTheSame(oldItem: Categoria, newItem: Categoria) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Categoria, newItem: Categoria) = oldItem == newItem
    }
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val binding = rjm.frontrestaurante.databinding.ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoriaViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CategoriaViewHolder(
        private val binding: rjm.frontrestaurante.databinding.ItemCategoriaBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(categoria: Categoria) {
            binding.apply {
                textViewNombreCategoria.text = categoria.nombre
                textViewDescripcionCategoria.text = categoria.descripcion
                
                // Configurar clics
                root.setOnClickListener { onClickListener(categoria) }
                
                // Solo mostrar botón de eliminar para administradores
                if (SessionManager.getUserRole() == "admin") {
                    buttonDeleteCategoria.visibility = View.VISIBLE
                    buttonDeleteCategoria.setOnClickListener { onDeleteListener(categoria) }
                } else {
                    buttonDeleteCategoria.visibility = View.GONE
                }
            }
        }
    }
} 