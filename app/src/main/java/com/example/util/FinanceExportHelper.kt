package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.Client
import com.example.data.Operation
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinanceExportHelper {

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        return format.format(amount)
    }

    /**
     * Generates a beautiful PDF statement for a specific client and their operations.
     */
    fun generateClientStatementPdf(
        context: Context,
        client: Client,
        operations: List<Operation>,
        balance: Double,
        totalGiven: Double,
        totalReceived: Double,
        dateRangeStr: String? = null
    ): File {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        
        // standard A4 limits: 595 width, 842 height
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1
        
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Custom margin setup
        val margin = 50f
        var yPos = 60f

        // Helper function to draw text safely
        fun drawText(text: String, x: Float, y: Float, size: Float, isBold: Boolean = false, color: Int = Color.BLACK) {
            paint.reset()
            paint.color = color
            paint.textSize = size
            paint.isAntiAlias = true
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(text, x, y, paint)
        }

        // Header Background Accents
        paint.color = Color.parseColor("#121212") // Dark slate background brand accent
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 130f, paint)

        // Draw App Title & Metadata
        drawText("CLIENT FINANCIAL STATEMENT", margin, 50f, 18f, isBold = true, color = Color.WHITE)
        drawText("Client Ledger App | Statement generated on ${SimpleFormatDate(System.currentTimeMillis())}", margin, 75f, 9f, isBold = false, color = Color.LTGRAY)
        if (dateRangeStr != null) {
            drawText("Period: $dateRangeStr", margin, 95f, 10f, isBold = true, color = Color.parseColor("#FFD700"))
        }

        yPos = 160f
        // Client Info block
        drawText("CLIENT DETAILS", margin, yPos, 12f, isBold = true, color = Color.parseColor("#4A4A4A"))
        yPos += 20f
        drawText("Name: ${client.name}", margin, yPos, 14f, isBold = true)
        yPos += 18f
        drawText("Phone: ${if (client.phone.isNotEmpty()) client.phone else "N/A"}", margin, yPos, 11f)
        yPos += 16f
        drawText("Email: ${if (client.email.isNotEmpty()) client.email else "N/A"}", margin, yPos, 11f)
        yPos += 16f
        if (client.notes.isNotEmpty()) {
            drawText("Client notes: ${client.notes}", margin, yPos, 11f)
            yPos += 16f
        }

        // Draw Divider
        yPos += 10f
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, paint)

        // Financial Metrics Panel (Given, Received, Current Balance)
        yPos += 25f
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(margin, yPos, pageWidth - margin, yPos + 70f, paint)

        // Labels
        val boxWidth = (pageWidth - 2 * margin) / 3f
        
        drawText("Total Given (I Gave)", margin + 15f, yPos + 25f, 9f, color = Color.GRAY)
        drawText(formatCurrency(totalGiven), margin + 15f, yPos + 50f, 14f, isBold = true, color = Color.parseColor("#2E7D32")) // Green

        drawText("Total Received (Client Gave)", margin + boxWidth + 15f, yPos + 25f, 9f, color = Color.GRAY)
        drawText(formatCurrency(totalReceived), margin + boxWidth + 15f, yPos + 50f, 14f, isBold = true, color = Color.parseColor("#C62828")) // Red

        drawText("Net Balance", margin + 2 * boxWidth + 15f, yPos + 25f, 10f, isBold = true, color = Color.DKGRAY)
        val balanceColor = when {
            balance > 0 -> Color.parseColor("#2E7D32") // Green
            balance < 0 -> Color.parseColor("#C62828") // Red
            else -> Color.BLACK
        }
        drawText(formatCurrency(balance), margin + 2 * boxWidth + 15f, yPos + 50f, 14f, isBold = true, color = balanceColor)
        
        yPos += 85f
        // Current balance status explanation
        val explanation = when {
            balance > 0 -> "Status: You owe the client money (Positive balance)"
            balance < 0 -> "Status: Client owes you money (Negative balance)"
            else -> "Status: Settled (Zero balance)"
        }
        drawText(explanation, margin, yPos, 11f, isBold = true, color = balanceColor)
        
        // Operations Table Header
        yPos += 35f
        drawText("OPERATION LOGS", margin, yPos, 13f, isBold = true, color = Color.parseColor("#4A4A4A"))
        
        yPos += 15f
        paint.color = Color.parseColor("#333333")
        canvas.drawRect(margin, yPos, pageWidth - margin, yPos + 25f, paint)

        drawText("Date", margin + 10f, yPos + 17f, 10f, isBold = true, color = Color.WHITE)
        drawText("Type", margin + 150f, yPos + 17f, 10f, isBold = true, color = Color.WHITE)
        drawText("Amount", margin + 260f, yPos + 17f, 10f, isBold = true, color = Color.WHITE)
        drawText("Notes / Attachment", margin + 370f, yPos + 17f, 10f, isBold = true, color = Color.WHITE)

        yPos += 25f

        // Loop and draw transaction items
        var rowCount = 0
        for (op in operations) {
            // Check if page height remains within bounds, if not add a new page
            if (yPos > pageHeight - 60f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
                
                // Redraw table headers on new page
                paint.color = Color.parseColor("#333333")
                canvas.drawRect(margin, yPos, pageWidth - margin, yPos + 25f, paint)
                
                drawText("Date", margin + 10f, yPos + 17f, 10f, isBold = true, color = Color.WHITE)
                drawText("Type", margin + 150f, yPos + 17f, 10f, isBold = true, color = Color.WHITE)
                drawText("Amount", margin + 260f, yPos + 17f, 10f, isBold = true, color = Color.WHITE)
                drawText("Notes / Attachment", margin + 370f, yPos + 17f, 10f, isBold = true, color = Color.WHITE)
                yPos += 25f
            }

            // Alternating backgrounds
            paint.color = if (rowCount % 2 == 0) Color.WHITE else Color.parseColor("#FAFAFA")
            canvas.drawRect(margin, yPos, pageWidth - margin, yPos + 25f, paint)

            drawText(formatTimestamp(op.date), margin + 10f, yPos + 17f, 9f)

            val typeText = if (op.type == "GIVEN") "Given (I paid)" else "Received (Got)"
            val typeColor = if (op.type == "GIVEN") Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            drawText(typeText, margin + 150f, yPos + 17f, 9f, isBold = true, color = typeColor)

            drawText(formatCurrency(op.amount), margin + 260f, yPos + 17f, 9f, isBold = true)

            val notesTxt = op.notes.take(24) + (if (op.notes.length > 24) "..." else "") + 
                if (op.attachmentPath != null) " [Attachment]" else ""
            drawText(notesTxt, margin + 370f, yPos + 17f, 9f, color = Color.DKGRAY)

            // Bottom row border
            paint.color = Color.parseColor("#E0E0E0")
            paint.strokeWidth = 0.5f
            canvas.drawLine(margin, yPos + 25f, pageWidth - margin, yPos + 25f, paint)

            yPos += 25f
            rowCount++
        }

        // Draw page footer on the final page
        drawText("Page $pageNum", pageWidth - margin - 40f, pageHeight - 35f, 9f, color = Color.GRAY)
        pdfDocument.finishPage(page)

        // Save to file
        val outputDir = context.cacheDir
        val tempFile = File.createTempFile("statement_${client.id}_", ".pdf", outputDir)
        val fos = FileOutputStream(tempFile)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()

        return tempFile
    }

    private fun SimpleFormatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Generates an Excel-compatible CSV string from client financial operation logs.
     */
    fun exportOperationsToCsv(operations: List<Operation>, clientsMap: Map<Int, Client>): String {
        val csv = StringBuilder()
        // CSV headers
        csv.append("Client,Date,Type,Amount,Notes,Attachment\n")
        
        for (op in operations) {
            val clientName = clientsMap[op.clientId]?.name ?: "Unknown Client (${op.clientId})"
            val dateStr = formatTimestamp(op.date)
            val typeStr = if (op.type == "GIVEN") "Given" else "Received"
            val notesEscaped = op.notes.replace("\"", "\"\"")
            val attachEscaped = (op.attachmentPath ?: "None").replace("\"", "\"\"")

            csv.append("\"${clientName}\",")
            csv.append("\"${dateStr}\",")
            csv.append("\"${typeStr}\",")
            csv.append("${op.amount},")
            csv.append("\"${notesEscaped}\",")
            csv.append("\"${attachEscaped}\"\n")
        }
        return csv.toString()
    }

    /**
     * Exports the entire application database into a JSON backup file.
     */
    fun exportBackupJson(clients: List<Client>, operations: List<Operation>): String {
        val rootJson = JSONObject()
        rootJson.put("backupVersion", 1)
        rootJson.put("timestamp", System.currentTimeMillis())

        val clientsArray = JSONArray()
        for (client in clients) {
            val cj = JSONObject()
            cj.put("id", client.id)
            cj.put("name", client.name)
            cj.put("phone", client.phone)
            cj.put("email", client.email)
            cj.put("notes", client.notes)
            cj.put("createdAt", client.createdAt)
            clientsArray.put(cj)
        }
        rootJson.put("clients", clientsArray)

        val opsArray = JSONArray()
        for (op in operations) {
            val oj = JSONObject()
            oj.put("id", op.id)
            oj.put("clientId", op.clientId)
            oj.put("date", op.date)
            oj.put("amount", op.amount)
            oj.put("type", op.type)
            oj.put("notes", op.notes)
            oj.put("attachmentPath", op.attachmentPath ?: JSONObject.NULL)
            opsArray.put(oj)
        }
        rootJson.put("operations", opsArray)

        return rootJson.toString(2)
    }

    /**
     * Parses a JSON backup string and returns a pair of deserialized clients and operations list.
     */
    fun importBackupJson(jsonString: String): Pair<List<Client>, List<Operation>> {
        val root = JSONObject(jsonString)
        val clientsList = mutableListOf<Client>()
        val operationsList = mutableListOf<Operation>()

        val clientsArray = root.getJSONArray("clients")
        for (i in 0 until clientsArray.length()) {
            val c = clientsArray.getJSONObject(i)
            clientsList.add(
                Client(
                    id = c.optInt("id", 0),
                    name = c.getString("name"),
                    phone = c.optString("phone", ""),
                    email = c.optString("email", ""),
                    notes = c.optString("notes", ""),
                    createdAt = c.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val opsArray = root.getJSONArray("operations")
        for (i in 0 until opsArray.length()) {
            val o = opsArray.getJSONObject(i)
            operationsList.add(
                Operation(
                    id = o.optInt("id", 0),
                    clientId = o.getInt("clientId"),
                    date = o.getLong("date"),
                    amount = o.getDouble("amount"),
                    type = o.getString("type"),
                    notes = o.optString("notes", ""),
                    attachmentPath = if (o.isNull("attachmentPath")) null else o.getString("attachmentPath")
                )
            )
        }

        return Pair(clientsList, operationsList)
    }
}
