package com.example.eyediseaseapp.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.widget.Toast
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.io.image.ImageDataFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun generatePdf(context: Context, imageBitmap: Bitmap?, className: String, confidence: Float) {
    val fileName = "diagnosis_report_${System.currentTimeMillis()}.pdf"
    val filePath = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        fileName
    )

    try {
        val writer = PdfWriter(FileOutputStream(filePath))
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        val titleParagraph = Paragraph("Eye Disease Detection Report")
            .setFontSize(24f) // Example font size
            .setBold()
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
        document.add(titleParagraph)
        // Add image to PDF
        if (imageBitmap != null) {
            val stream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageData = ImageDataFactory.create(stream.toByteArray())
            val image = Image(imageData) // Correct Image creation

            // Set fixed height and width for the image
            val fixedWidth = 200f // Example width in PDF units (points)
            val fixedHeight = 200f // Example height in PDF units (points)
            image.setWidth(fixedWidth)
            image.setHeight(fixedHeight)

            document.add(image) // Correct usage
        }

        // Add text to PDF
        document.add(Paragraph("Diagnosis: $className"))
        document.add(Paragraph("Confidence: ${String.format("%.2f", confidence * 100)}%"))

        document.close()

        // Optionally, show a toast message to indicate the PDF has been saved
        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
    }
}