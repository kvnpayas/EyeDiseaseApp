package com.example.eyediseaseapp.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.widget.Toast
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.List
import com.itextpdf.layout.element.ListItem
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            .setFontSize(24f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
        document.add(titleParagraph)

        // Add date to PDF
        val currentDate = Date()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormatter.format(currentDate)
        document.add(Paragraph("Date: $formattedDate"))

        // Add image to PDF
        if (imageBitmap != null) {
            val stream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageData = ImageDataFactory.create(stream.toByteArray())
            val image = Image(imageData)

            // Set fixed height and width for the image
            val fixedWidth = 200f
            val fixedHeight = 200f
            image.setWidth(fixedWidth)
            image.setHeight(fixedHeight)

            // Wrap the image in a Paragraph and set alignment to center
            val centeredImage = Paragraph().add(image).apply {
                setTextAlignment(TextAlignment.CENTER)
                setHorizontalAlignment(HorizontalAlignment.CENTER)
            }
            document.add(centeredImage)
        }

        // Add text to PDF
        document.add(Paragraph("Diagnosis: $className"))
        document.add(Paragraph("Confidence: ${String.format("%.2f", confidence * 100)}%"))

        // Add recommendations in bullet form with bold title
        val recommendationTitle = Paragraph("Recommendations:")
            .setFontSize(14f)
            .setBold()
        document.add(recommendationTitle)

        val recommendationsList = List() // Use iText's List constructor
            .setSymbolIndent(12f)
            .setListSymbol("\u2022") // Bullet symbol
        var recommendations = listOf<String>()
        if(className == "Cataract"){
            recommendations = listOf(
                "Consult an Ophthalmologist: Schedule a comprehensive eye examination with an ophthalmologist as soon as possible.",
                "Visual Acuity Test: A professional eye doctor will determine the extent of the cataract's impact on your vision.",
                "Further Examination: The doctor will perform a slit-lamp examination and other relevant tests.",
                "Treatment Options: Discuss treatment options, including cataract surgery, with your ophthalmologist.",
                "Protect your eyes: Wear sunglasses to protect your eyes from UV light."
            )
        }else if(className == "Glaucoma"){
            recommendations = listOf(
                "Urgent Ophthalmologist Consultation: Schedule an immediate appointment with an ophthalmologist.",
                "Intraocular Pressure (IOP) Measurement: The doctor will measure the pressure inside your eye.",
                "Visual Field Testing: This test will assess your peripheral vision.",
                "Optic Nerve Evaluation: The doctor will examine the optic nerve for signs of damage.",
                "Follow-up Care: Regular monitoring and treatment are crucial to prevent vision loss."
            )
        }
        recommendations.forEach { recommendation ->
            val bulletPoint = "• $recommendation"
            val paragraph = Paragraph(bulletPoint)
            document.add(paragraph)
        }
        document.add(recommendationsList)


        val additionalInfoTitle = Paragraph("Additional Information:")
            .setFontSize(14f)
            .setBold()
        document.add(additionalInfoTitle)

        val additionalInfoLists = List() // Use iText's List constructor
            .setSymbolIndent(12f)
            .setListSymbol("\u2022") // Bullet symbol
        var additionalInfo = listOf<String>()
        if(className == "Cataract"){
            additionalInfo = listOf(
                "Symptoms: Common symptoms include cloudy vision, glare, halos around lights, and difficulty seeing at night.",
                "Risk Factors: Age, diabetes, smoking, and prolonged sun exposure are risk factors.",
                "Importance of Early Detection: Early detection and treatment can help prevent significant vision loss.",
            )
        }else if(className == "Glaucoma"){
            additionalInfo = listOf(
                "Types of Glaucoma: Open-angle glaucoma and angle-closure glaucoma are the most common types.",
                "Symptoms: In early stages, glaucoma often has no symptoms. Peripheral vision loss is a common symptom in later stages.",
                "Risk Factors: Age, family history, and certain medical conditions increase the risk.",
                "Importance of Regular Screening: Early detection and treatment can prevent irreversible vision loss.",
            )
        }
        additionalInfo.forEach { info ->
            val bulletPoint = "• $info"
            val paragraph = Paragraph(bulletPoint)
            document.add(paragraph)
        }
        document.add(additionalInfoLists)

        val disclaimerTitle = Paragraph("Disclaimer:")
            .setFontSize(14f)
            .setBold()
        document.add(disclaimerTitle)

        val disclaimer = Paragraph("This scan is a screening tool and does not provide a definitive diagnosis. A comprehensive eye examination by an ophthalmologist is essential for accurate diagnosis and treatment.")
            .setFontSize(12f)
        document.add(disclaimer)

        document.close()

        // Optionally, show a toast message to indicate the PDF has been saved
        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
    }
}
