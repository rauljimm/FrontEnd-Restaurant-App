package rjm.frontrestaurante.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import rjm.frontrestaurante.R
import rjm.frontrestaurante.databinding.FragmentMesasBinding
import rjm.frontrestaurante.ui.main.Refreshable

class MesasFragment : Fragment(), Refreshable {

    private var _binding: FragmentMesasBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MesasViewModel
    private lateinit var adapter: MesasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMesasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(MesasViewModel::class.java)
        
        // Configurar RecyclerView
        adapter = MesasAdapter { mesa ->
            // Navegar al detalle de la mesa
            findNavController().navigate(
                MesasFragmentDirections.actionMesasFragmentToDetalleMesaFragment(mesa.id)
            )
        }
        
        binding.recyclerViewMesas.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewMesas.adapter = adapter
        
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.cargarMesas()
        }
        
        // Observar cambios en las mesas
        viewModel.mesas.observe(viewLifecycleOwner) { mesas ->
            adapter.submitList(mesas)
            binding.swipeRefreshLayout.isRefreshing = false
            binding.textViewEmpty.visibility = if (mesas.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Cargar mesas
        viewModel.cargarMesas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onRefresh() {
        viewModel.cargarMesas()
    }
} 