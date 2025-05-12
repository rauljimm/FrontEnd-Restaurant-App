package rjm.frontrestaurante.ui.usuarios

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import rjm.frontrestaurante.R
import rjm.frontrestaurante.adapter.UsuariosAdapter
import rjm.frontrestaurante.api.Usuario
import rjm.frontrestaurante.databinding.DialogUsuarioFormBinding
import rjm.frontrestaurante.databinding.FragmentUsuariosBinding

/**
 * Fragmento para gestionar usuarios (solo para administradores)
 */
class UsuariosFragment : Fragment() {

    private var _binding: FragmentUsuariosBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: UsuariosViewModel
    private lateinit var usuariosAdapter: UsuariosAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar el ViewModel
        viewModel = ViewModelProvider(this).get(UsuariosViewModel::class.java)
        
        // Verificar si es administrador
        if (!viewModel.isAdmin()) {
            Toast.makeText(requireContext(), R.string.admin_requerido, Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }
        
        // Configurar RecyclerView
        setupRecyclerView()
        
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshUsuarios.setOnRefreshListener {
            viewModel.cargarUsuarios()
        }
        
        // Configurar FloatingActionButton para agregar usuarios
        binding.fabAgregarUsuario.setOnClickListener {
            mostrarDialogoNuevoUsuario()
        }
        
        // Observar estado de carga
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshUsuarios.isRefreshing = isLoading
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje.isNotEmpty()) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
            }
        }
        
        // Observar lista de usuarios
        viewModel.usuarios.observe(viewLifecycleOwner) { usuarios ->
            usuariosAdapter.actualizarUsuarios(usuarios)
            binding.tvNoUsuarios.visibility = if (usuarios.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Observar operaciones completadas
        viewModel.usuarioCreado.observe(viewLifecycleOwner) { created ->
            if (created) {
                Toast.makeText(requireContext(), R.string.usuario_creado, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.usuarioActualizado.observe(viewLifecycleOwner) { updated ->
            if (updated) {
                Toast.makeText(requireContext(), R.string.usuario_actualizado, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.usuarioEliminado.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                Toast.makeText(requireContext(), R.string.usuario_eliminado, Toast.LENGTH_SHORT).show()
            }
        }
        
        // Cargar usuarios
        viewModel.cargarUsuarios()
    }
    
    private fun setupRecyclerView() {
        usuariosAdapter = UsuariosAdapter(
            onEditClick = { usuario -> mostrarDialogoEditarUsuario(usuario) },
            onDeleteClick = { usuario -> confirmarEliminacionUsuario(usuario) }
        )
        
        binding.recyclerViewUsuarios.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = usuariosAdapter
        }
    }
    
    private fun mostrarDialogoNuevoUsuario() {
        val dialogBinding = DialogUsuarioFormBinding.inflate(layoutInflater)
        val vista = dialogBinding.root
        
        // Seleccionar rol de camarero por defecto
        dialogBinding.rbCamarero.isChecked = true
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.nuevo_usuario_title)
            .setView(vista)
            .create()
        
        // Configurar botones
        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnGuardar.setOnClickListener {
            // Obtener valores
            val username = dialogBinding.etUsername.text.toString().trim()
            val password = dialogBinding.etPassword.text.toString().trim()
            val email = dialogBinding.etEmail.text.toString().trim()
            val nombre = dialogBinding.etNombre.text.toString().trim()
            val apellido = dialogBinding.etApellido.text.toString().trim()
            
            // Obtener rol seleccionado
            val rol = when {
                dialogBinding.rbAdmin.isChecked -> "admin"
                dialogBinding.rbCamarero.isChecked -> "camarero"
                dialogBinding.rbCocinero.isChecked -> "cocinero"
                else -> "camarero" // Por defecto
            }
            
            // Validar campos
            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
                Toast.makeText(requireContext(), R.string.required_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Crear usuario
            viewModel.crearUsuario(username, password, email, nombre, apellido, rol)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun mostrarDialogoEditarUsuario(usuario: Usuario) {
        val dialogBinding = DialogUsuarioFormBinding.inflate(layoutInflater)
        val vista = dialogBinding.root
        
        // Rellenar los campos con los datos del usuario
        dialogBinding.etUsername.setText(usuario.username)
        dialogBinding.etUsername.isEnabled = false // No permitir cambiar el username
        dialogBinding.etEmail.setText(usuario.email)
        dialogBinding.etNombre.setText(usuario.nombre)
        dialogBinding.etApellido.setText(usuario.apellido)
        
        // La contraseña se deja vacía, solo se actualiza si se introduce algo
        dialogBinding.layoutPassword.hint = getString(R.string.password) + " (dejar vacío para no cambiar)"
        
        // Seleccionar el rol actual
        when (usuario.rol.lowercase()) {
            "admin" -> dialogBinding.rbAdmin.isChecked = true
            "camarero" -> dialogBinding.rbCamarero.isChecked = true
            "cocinero" -> dialogBinding.rbCocinero.isChecked = true
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.editar_usuario_title)
            .setView(vista)
            .create()
        
        // Configurar botones
        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnGuardar.setOnClickListener {
            // Obtener valores
            val password = dialogBinding.etPassword.text.toString().trim()
            val email = dialogBinding.etEmail.text.toString().trim()
            val nombre = dialogBinding.etNombre.text.toString().trim()
            val apellido = dialogBinding.etApellido.text.toString().trim()
            
            // Obtener rol seleccionado
            val rol = when {
                dialogBinding.rbAdmin.isChecked -> "admin"
                dialogBinding.rbCamarero.isChecked -> "camarero"
                dialogBinding.rbCocinero.isChecked -> "cocinero"
                else -> usuario.rol.lowercase() // Mantener el rol actual en minúsculas
            }
            
            // Validar campos
            if (email.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
                Toast.makeText(requireContext(), R.string.required_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Actualizar usuario (password solo se manda si no está vacío)
            viewModel.actualizarUsuario(
                userId = usuario.id,
                email = email,
                nombre = nombre,
                apellido = apellido,
                password = if (password.isEmpty()) null else password,
                rol = rol
            )
            
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun confirmarEliminacionUsuario(usuario: Usuario) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_delete_usuario)
            .setMessage(getString(R.string.confirmar_eliminar_usuario))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.eliminarUsuario(usuario.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 