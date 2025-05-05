package rjm.frontrestaurante.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import rjm.frontrestaurante.R
import rjm.frontrestaurante.RestauranteApp
import rjm.frontrestaurante.databinding.FragmentLoginBinding
import rjm.frontrestaurante.util.SessionManager

class LoginFragment : Fragment() {
    private val TAG = "LoginFragment"

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: LoginViewModel
    private val preferences by lazy { RestauranteApp.getInstance().preferences }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        
        // Verificar si llegamos aquí después de un logout (argumento pasado por navegación)
        val args = arguments
        val isFromLogout = args?.getBoolean("from_logout", false) ?: false
        
        Log.d(TAG, "Estado inicial: isLoggedIn=${SessionManager.isLoggedIn()}, secondLoginNeeded=${preferences.isSecondLoginNeeded()}, fromLogout=$isFromLogout")
        
        // Solo navegamos automáticamente si tenemos una sesión válida, no necesitamos segundo login,
        // y no venimos de un logout
        if (SessionManager.isLoggedIn() && !preferences.isSecondLoginNeeded() && !isFromLogout) {
            // Verificación adicional: comprobar que el token no está vacío y que tenemos un rol de usuario
            val token = SessionManager.getToken()
            val userRole = SessionManager.getUserRole()
            
            if (!token.isNullOrEmpty() && userRole.isNotEmpty()) {
                Log.d(TAG, "Ya hay sesión válida, navegando automáticamente")
                navigateBasedOnUserRole()
                return
            } else {
                Log.d(TAG, "Sesión inválida a pesar de isLoggedIn=true, limpiando sesión")
                SessionManager.logout()
                preferences.clearPreferences()
            }
        }
        
        // Si venimos de un logout, limpiar campos
        if (isFromLogout) {
            binding.editTextEmail.setText("")
            binding.editTextPassword.setText("")
            // Asegurarnos de que la sesión esté limpia
            SessionManager.logout()
            preferences.clearPreferences()
        }
        
        // Configurar botón de inicio de sesión
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }
        
        // Observar estado de login
        viewModel.loginState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LoginResult.Success -> {
                    // Navegar al fragmento correspondiente según el rol del usuario
                    navigateBasedOnUserRole()
                }
                is LoginResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
                is LoginResult.Loading -> {
                    // Mostrar indicador de carga si es necesario
                    binding.buttonLogin.isEnabled = !result.isLoading
                }
            }
        }
        
        // Comprobar si necesitamos un segundo intento automático de login
        if (preferences.isSecondLoginNeeded() && 
            !binding.editTextEmail.text.isNullOrEmpty() &&
            !binding.editTextPassword.text.isNullOrEmpty() &&
            !isFromLogout) {
            // Intentar login automáticamente
            Log.d(TAG, "Realizando segundo intento automático de login")
            performLogin()
        }
    }
    
    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            viewModel.login(email, password)
        } else {
            Toast.makeText(context, R.string.required_fields, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Navega al fragmento correspondiente según el rol del usuario
     */
    private fun navigateBasedOnUserRole() {
        val userRole = SessionManager.getUserRole()
        Log.d(TAG, "Navegando según rol de usuario: $userRole")
        
        when (userRole) {
            "admin" -> {
                // Administrador va a la pantalla de productos
                findNavController().navigate(R.id.action_global_to_mainFragment)
            }
            "cocinero" -> {
                // Cocinero va directamente a pedidos activos
                findNavController().navigate(R.id.action_global_to_pedidosActivosFragment)
            }
            else -> {
                // Camarero u otros roles van a la pantalla de mesas por defecto
                findNavController().navigate(R.id.action_global_to_mainFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 