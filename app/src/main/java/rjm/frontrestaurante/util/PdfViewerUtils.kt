package rjm.frontrestaurante.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utilidad para abrir PDFs con aplicaciones externas
 */
object PdfViewerUtils {
    
    /**
     * Abre un PDF con una aplicación externa
     * @param context Contexto de la aplicación
     * @param pdfFile Archivo PDF a abrir
     * @return true si se pudo abrir el PDF, false en caso contrario
     */
    fun openPdfFile(context: Context, pdfFile: File): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Para Android 7.0 (API 24) y superiores, usar FileProvider
                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, pdfFile)
            } else {
                // Para versiones anteriores, usar URI directa
                Uri.fromFile(pdfFile)
            }
            
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Abrir PDF con:"))
            return true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No se encontró una aplicación para visualizar PDFs en tu dispositivo. " +
                        "El archivo se guardó en: ${pdfFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            return false
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al abrir el PDF: ${e.message}. " +
                        "El archivo se guardó en: ${pdfFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }
} 