package rjm.frontrestaurante.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rjm.frontrestaurante.R
import rjm.frontrestaurante.api.Usuario

/**
 * Adaptador para la lista de usuarios
 */
class UsuariosAdapter(
    private val onEditClick: (Usuario) -> Unit,
    private val onDeleteClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    private var usuarios: List<Usuario> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.bind(usuario)
    }

    override fun getItemCount(): Int = usuarios.size

    /**
     * Actualiza la lista de usuarios
     */
    fun actualizarUsuarios(nuevosUsuarios: List<Usuario>) {
        this.usuarios = nuevosUsuarios
        notifyDataSetChanged()
    }

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreUsuario: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val tvRol: TextView = itemView.findViewById(R.id.tvRol)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditUsuario)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteUsuario)

        fun bind(usuario: Usuario) {
            // Completar nombre completo
            tvNombreUsuario.text = "${usuario.nombre} ${usuario.apellido}"
            tvEmail.text = usuario.email
            
            // Formatear el rol para mostrarlo con la primera letra en mayúscula
            val rolFormateado = when (usuario.rol.lowercase()) {
                "admin" -> "Administrador"
                "camarero" -> "Camarero"
                "cocinero" -> "Cocinero"
                else -> usuario.rol
            }
            tvRol.text = rolFormateado
            
            // Configurar botones
            btnEdit.setOnClickListener { onEditClick(usuario) }
            btnDelete.setOnClickListener { onDeleteClick(usuario) }
        }
    }
} 