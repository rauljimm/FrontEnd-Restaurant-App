package rjm.frontrestaurante.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import rjm.frontrestaurante.R
import rjm.frontrestaurante.model.Cuenta
import rjm.frontrestaurante.model.DetalleCuenta
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidad para generar PDFs de tickets de restaurante
 */
object PdfGenerator {
    private const val TAG = "PdfGenerator"
    
    // Colores para el PDF
    private val COLOR_ACCENT = BaseColor(33, 150, 243) // Color primario de la app
    private val COLOR_LIGHT_GRAY = BaseColor(230, 230, 230)
    private val COLOR_DARK_GRAY = BaseColor(100, 100, 100)
    
    /**
     * Genera un PDF para una cuenta en formato ticket pequeño y devuelve el archivo generado
     */
    fun generatePdf(context: Context, cuenta: Cuenta): File? {
        try {
            // Crear el directorio para los PDFs si no existe
            val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "tickets")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            
            // Crear archivo con nombre único basado en ID de cuenta y timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(pdfDir, "ticket_${cuenta.id}_$timestamp.pdf")
            
            // Crear documento PDF con tamaño de ticket
            // Ancho aproximado de un ticket de 80mm (8cm) convertido a puntos
            val ticketWidth = 226f // 8cm ≈ 226 puntos
            // Alto suficiente para contenido, se ajustará automáticamente
            val ticketHeight = 1000f
            val pageSize = Rectangle(ticketWidth, ticketHeight)
            val document = Document(pageSize, 10f, 10f, 10f, 10f) // Márgenes reducidos
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()
            
            // Agregar contenido al ticket
            addTicketHeader(document, context)
            addTicketInfo(document, cuenta)
            addTicketItems(document, cuenta.detalles)
            addTicketTotals(document, cuenta)
            addTicketFooter(document)
            
            document.close()
            
            Log.d(TAG, "Ticket PDF generado correctamente: ${pdfFile.absolutePath}")
            return pdfFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar PDF de ticket", e)
            return null
        }
    }
    
    /**
     * Agrega el encabezado del ticket
     */
    private fun addTicketHeader(document: Document, context: Context) {
        try {
            // Título del restaurante centrado
            val nameFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, COLOR_ACCENT)
            val normalFont = Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL)
            
            // Encabezado
            val headerParagraph = Paragraph()
            headerParagraph.alignment = Element.ALIGN_CENTER
            
            // Intentar cargar el logo desde recursos (pequeño)
            val logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
            if (logoDrawable != null) {
                val bmp = (logoDrawable as BitmapDrawable).bitmap
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val image = Image.getInstance(stream.toByteArray())
                image.scaleToFit(50f, 50f)
                image.alignment = Element.ALIGN_CENTER
                headerParagraph.add(image)
            }
            
            // Nombre del restaurante y detalles
            val title = Paragraph("RESTAURANTE GOURMET", nameFont)
            title.alignment = Element.ALIGN_CENTER
            headerParagraph.add(title)
            
            headerParagraph.add(Paragraph("Calle Ejemplo, 123 - Madrid", normalFont))
            headerParagraph.add(Paragraph("Tel: 91 123 45 67", normalFont))
            headerParagraph.add(Paragraph("CIF: B12345678", normalFont))
            
            // Línea separadora
            headerParagraph.add(Paragraph("--------------------------------", normalFont))
            
            document.add(headerParagraph)
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar encabezado de ticket", e)
        }
    }
    
    /**
     * Agrega la información del ticket
     */
    private fun addTicketInfo(document: Document, cuenta: Cuenta) {
        try {
            val normalFont = Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL)
            val boldFont = Font(Font.FontFamily.HELVETICA, 8f, Font.BOLD)
            
            val infoParagraph = Paragraph()
            infoParagraph.alignment = Element.ALIGN_CENTER
            
            // Título de Ticket
            val title = Paragraph("TICKET DE VENTA", boldFont)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 5f
            infoParagraph.add(title)
            
            // Información básica del ticket
            val infoTable = PdfPTable(2)
            infoTable.widthPercentage = 100f
            infoTable.setWidths(floatArrayOf(1f, 1f))
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            
            // Número de ticket y fecha
            addTicketInfoRow(infoTable, "Nº Ticket:", "TK-${cuenta.id}", boldFont, normalFont)
            addTicketInfoRow(infoTable, "Fecha:", dateFormat.format(cuenta.fechaCobro), boldFont, normalFont)
            addTicketInfoRow(infoTable, "Mesa:", "${cuenta.numeroMesa}", boldFont, normalFont)
            addTicketInfoRow(infoTable, "Usuario:", cuenta.nombreCamarero, boldFont, normalFont)
            
            infoParagraph.add(infoTable)
            
            // Línea separadora
            infoParagraph.add(Paragraph("--------------------------------", normalFont))
            
            document.add(infoParagraph)
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar info de ticket", e)
        }
    }
    
    /**
     * Agrega una fila a la tabla de información del ticket
     */
    private fun addTicketInfoRow(
        table: PdfPTable, 
        label: String, 
        value: String, 
        labelFont: Font, 
        valueFont: Font
    ) {
        val labelCell = PdfPCell(Paragraph(label, labelFont))
        labelCell.border = Rectangle.NO_BORDER
        labelCell.horizontalAlignment = Element.ALIGN_LEFT
        
        val valueCell = PdfPCell(Paragraph(value, valueFont))
        valueCell.border = Rectangle.NO_BORDER
        valueCell.horizontalAlignment = Element.ALIGN_RIGHT
        
        table.addCell(labelCell)
        table.addCell(valueCell)
    }
    
    /**
     * Agrega los productos al ticket
     */
    private fun addTicketItems(document: Document, detalles: List<DetalleCuenta>) {
        try {
            val normalFont = Font(Font.FontFamily.HELVETICA, 7f, Font.NORMAL)
            val boldFont = Font(Font.FontFamily.HELVETICA, 7f, Font.BOLD)
            
            // Tabla compacta para los items
            val itemsTable = PdfPTable(4)
            itemsTable.widthPercentage = 100f
            itemsTable.setWidths(floatArrayOf(0.5f, 2f, 0.5f, 1f))
            
            // Encabezados
            addTicketTableHeader(itemsTable, "CANT", boldFont)
            addTicketTableHeader(itemsTable, "PRODUCTO", boldFont)
            addTicketTableHeader(itemsTable, "PRECIO", boldFont)
            addTicketTableHeader(itemsTable, "IMPORTE", boldFont)
            
            // Productos
            for (detalle in detalles) {
                addTicketTableCell(itemsTable, detalle.cantidad.toString(), normalFont)
                addTicketTableCell(itemsTable, detalle.nombreProducto, normalFont)
                addTicketTableCell(itemsTable, String.format("%.2f€", detalle.precioUnitario), normalFont)
                addTicketTableCell(itemsTable, String.format("%.2f€", detalle.subtotal), normalFont)
            }
            
            document.add(itemsTable)
            
            // Línea separadora
            val separator = Paragraph("--------------------------------", normalFont)
            separator.alignment = Element.ALIGN_CENTER
            document.add(separator)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar items al ticket", e)
        }
    }
    
    /**
     * Agrega un encabezado a la tabla de productos del ticket
     */
    private fun addTicketTableHeader(table: PdfPTable, text: String, font: Font) {
        val cell = PdfPCell(Paragraph(text, font))
        cell.border = Rectangle.BOTTOM
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.paddingTop = 2f
        cell.paddingBottom = 2f
        table.addCell(cell)
    }
    
    /**
     * Agrega una celda a la tabla de productos del ticket
     */
    private fun addTicketTableCell(table: PdfPTable, text: String, font: Font) {
        val cell = PdfPCell(Paragraph(text, font))
        cell.border = Rectangle.NO_BORDER
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.paddingTop = 2f
        cell.paddingBottom = 2f
        table.addCell(cell)
    }
    
    /**
     * Agrega los totales al ticket
     */
    private fun addTicketTotals(document: Document, cuenta: Cuenta) {
        try {
            val boldFont = Font(Font.FontFamily.HELVETICA, 8f, Font.BOLD)
            val normalFont = Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL)
            
            val totalsTable = PdfPTable(2)
            totalsTable.widthPercentage = 100f
            
            // Base imponible e IVA (21% para España)
            val baseImponible = cuenta.total / 1.21
            val iva = cuenta.total - baseImponible
            
            addTicketTotalRow(totalsTable, "Base Imponible:", String.format("%.2f€", baseImponible), normalFont)
            addTicketTotalRow(totalsTable, "IVA (21%):", String.format("%.2f€", iva), normalFont)
            
            // Línea antes del total
            val line = PdfPCell(Paragraph(""))
            line.colspan = 2
            line.border = Rectangle.TOP
            line.fixedHeight = 1f
            totalsTable.addCell(line)
            
            // Total
            addTicketTotalRow(totalsTable, "TOTAL:", String.format("%.2f€", cuenta.total), boldFont)
            
            // Método de pago
            val metodoPago = cuenta.metodoPago ?: "Pendiente de pago"
            addTicketTotalRow(totalsTable, "Forma de pago:", metodoPago, normalFont)
            
            document.add(totalsTable)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar totales al ticket", e)
        }
    }
    
    /**
     * Agrega una fila a la tabla de totales del ticket
     */
    private fun addTicketTotalRow(table: PdfPTable, label: String, value: String, font: Font) {
        val labelCell = PdfPCell(Paragraph(label, font))
        labelCell.border = Rectangle.NO_BORDER
        labelCell.horizontalAlignment = Element.ALIGN_LEFT
        
        val valueCell = PdfPCell(Paragraph(value, font))
        valueCell.border = Rectangle.NO_BORDER
        valueCell.horizontalAlignment = Element.ALIGN_RIGHT
        
        table.addCell(labelCell)
        table.addCell(valueCell)
    }
    
    /**
     * Agrega el pie de página al ticket
     */
    private fun addTicketFooter(document: Document) {
        try {
            val normalFont = Font(Font.FontFamily.HELVETICA, 6f, Font.NORMAL, COLOR_DARK_GRAY)
            val boldFont = Font(Font.FontFamily.HELVETICA, 8f, Font.BOLD, COLOR_ACCENT)
            
            // Línea separadora
            val separator = Paragraph("--------------------------------", normalFont)
            separator.alignment = Element.ALIGN_CENTER
            document.add(separator)
            
            val footerParagraph = Paragraph()
            footerParagraph.alignment = Element.ALIGN_CENTER
            
            footerParagraph.add(Paragraph("GRACIAS POR SU VISITA", boldFont))
            footerParagraph.add(Paragraph("Conserve este ticket para cualquier reclamación", normalFont))
            
            document.add(footerParagraph)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar pie de página", e)
        }
    }
} 