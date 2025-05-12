package rjm.frontrestaurante.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import rjm.frontrestaurante.R
import rjm.frontrestaurante.ui.main.MainActivity

/**
 * Actividad para la pantalla de login
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        // Cambiar el tema de splash al tema normal
        setTheme(R.style.Theme_FrontRestaurante)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        // Inicializar vistas
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)

        // Configurar botón de inicio de sesión
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Observar estado de login
        viewModel.loginState.observe(this) { result ->
            when (result) {
                is LoginResult.Success -> {
                    // Navegar a la actividad principal
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is LoginResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
                is LoginResult.Loading -> {
                    // Mostrar indicador de carga si es necesario
                    buttonLogin.isEnabled = !result.isLoading
                }
            }
        }
    }
} 