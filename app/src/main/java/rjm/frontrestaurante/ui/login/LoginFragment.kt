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
        
        Log.d(TAG, "Estado inicial: isLoggedIn=${SessionManager.isLoggedIn()}, secondLoginNeeded=${preferences.isSecondLoginNeeded()}")
        
        // Si ya tenemos un token válido y no necesitamos segundo login, navegamos directamente
        if (SessionManager.isLoggedIn() && !preferences.isSecondLoginNeeded()) {
            Log.d(TAG, "Ya hay sesión válida, navegando automáticamente")
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
            return
        }
        
        // Configurar botón de inicio de sesión
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }
        
        // Observar estado de login
        viewModel.loginState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LoginResult.Success -> {
                    // Navegar al fragmento principal
                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
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
            !binding.editTextPassword.text.isNullOrEmpty()) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 