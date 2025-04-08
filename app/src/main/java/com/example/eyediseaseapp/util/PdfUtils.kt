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

fun generatePdf(
    context: Context,
    imageBitmap: Bitmap?,
    className: String,
    confidence: Float,
) {
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
        document.add(Paragraph("Detected Eye Disease: $className").setBold())
        // Add date to PDF
        val currentDate = Date()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormatter.format(currentDate)
        document.add(Paragraph("Date: $formattedDate").setBold())
        document.add(
            Paragraph(
                "Detection Confidence: ${
                    String.format(
                        "%.2f",
                        confidence * 100
                    )
                }%"
            ).setBold()
        )


        // Severity
//        var severityPercent = String()
//        if (severity == "Mild") {
//            severityPercent = "60%-80% (Mild Cases)"
//        } else if (severity == "Moderate") {
//            severityPercent = "80%-90% (Moderate Signs Detected- Immediate Attention Required)"
//        } else {
//            severityPercent = "90-98% (Severe cases- Urgent Checkup Needed)"
//        }
//        document.add(Paragraph(severityPercent).setBold().setMarginTop(30f))

        // Eye Detection intro
        var eyeDetectIntro = "• The system has detected indications of $className"
//        if (severity == "Mild") {
//            eyeDetectIntro = "• The system has detected mild indications of $className\""
//        } else if (severity == "Moderate") {
//            eyeDetectIntro =
//                "• A moderate level of $className is detected. This may indicate an increased risk of progression."
//        } else {
//            eyeDetectIntro =
//                "• A significant presence of $className is detected. Immediate medical attention is strongly recommended."
//        }
        val paragraphEyeDetectIntro = Paragraph(eyeDetectIntro)
        paragraphEyeDetectIntro.setMarginLeft(16f)
        paragraphEyeDetectIntro.setMarginTop(8f)
        paragraphEyeDetectIntro.setBold()
        document.add(paragraphEyeDetectIntro)

        // Recommendation
        var recommendation = "• Recommendations:"
//        if (severity == "Mild") {
//            recommendation = "• Recommendations:"
//        } else if (severity == "Moderate") {
//            recommendation = "• Must-Do Actions:"
//        } else {
//            recommendation = "• Action Plan:"
//        }
        val paragraphRecommendation = Paragraph(recommendation)
        paragraphRecommendation.setMarginLeft(16f)
        paragraphRecommendation.setBold()
        document.add(paragraphRecommendation)

        val recommendationInfoLists = List() // Use iText's List constructor
            .setSymbolIndent(12f)
            .setListSymbol("\u2022") // Bullet symbol
        var recommendationInfo = listOf<String>()

        if (className == "Cataract") {
            recommendationInfo = listOf(
                "Schedule regular comprehensive eye exams for early detection and monitoring.",
                "Protect your eyes from prolonged UV exposure by wearing sunglasses and wide-brimmed hats outdoors.",
                "Avoid smoking, as it significantly increases the risk of developing cataracts.",
                "Manage underlying health conditions like diabetes effectively, as they can contribute to cataract formation.",
                "Maintain a healthy diet rich in antioxidants (e.g., leafy greens, fruits, nuts) which may support lens health.",
                "Discuss vision changes with your eye care professional; updating eyeglass prescriptions can help manage early symptoms.",
                "If cataracts interfere with daily activities (reading, driving), consult an ophthalmologist to discuss surgical options."
            )
        }else{
            recommendationInfo = listOf(
                "Ensure you understand the specific type and stage of glaucoma diagnosed by discussing it thoroughly with your ophthalmologist.",
                "Start any prescribed treatment (usually eye drops) immediately and learn the correct technique for administration. Consistency is critical.",
                "Schedule and commit to all follow-up appointments; regular monitoring of eye pressure and optic nerve health is essential.",
                "Understand that glaucoma management is typically lifelong. Adherence to treatment is key to preserving your vision.",
                "Inform your primary care physician and any other specialists about your new glaucoma diagnosis and the medications you've been prescribed.",
                "Learn about glaucoma and ask your ophthalmologist questions regarding potential side effects, lifestyle adjustments, and prognosis.",
                "Continue to protect your eyes from significant trauma or injury."
            )
        }
//        recommendationInfo = listOf(
//            "Maintain a healthy lifestyle and proper eye hygiene.",
//            "Consult an optometrist as soon as possible for a more detailed evaluation.",
//            "Avoid prolonged exposure to bright lights or screens.",
//            "Adjust your screen time and ensure proper eye rest.",
//            "Implement a diet rich in Vitamin A (e.g., carrots, leafy greens, and fish).",
//            "Follow-up: A professional consultation is advised within 1-3 months."
//        )
//        if (severity == "Mild") {
//            recommendationInfo = listOf(
//                "Maintain a healthy lifestyle and proper eye hygiene.",
//                "Regular eye exercises and protective eyewear may help slow progression.",
//                "Monitor symptoms for any noticeable changes, such as blurry vision or discomfort.",
//                "Follow-up: Consider scheduling an eye checkup within 6-12 months for further evaluation."
//            )
//        } else if (severity == "Moderate") {
//            recommendationInfo = listOf(
//                "Consult an optometrist as soon as possible for a more detailed evaluation.",
//                "Adjust your screen time and ensure proper eye rest.",
//                "Avoid prolonged exposure to bright lights or screens.",
//                "Implement a diet rich in Vitamin A (e.g., carrots, leafy greens, and fish).",
//                "Follow-up: A professional consultation is advised within 1-3 months."
//            )
//        } else {
//            recommendationInfo = listOf(
//                "Schedule an urgent appointment with an ophthalmologist for a comprehensive eye exam.",
//                "Possible medical or surgical intervention may be required, depending on severity.",
//                "Avoid self-medication and over-the-counter eye drops without professional advice.",
//                "Follow-up: Seek medical attention within 1-2 weeks to prevent further vision impairment."
//            )
//        }

        recommendationInfo.forEach { info ->
            val bulletPoint = "o $info"
            val paragraph = Paragraph(bulletPoint).setMarginLeft(32f)
            document.add(paragraph)
        }
        document.add(recommendationInfoLists)

        // General eye tips
        val generalEyeTips = "• General Eye Care Tips"
        document.add(Paragraph(generalEyeTips).setBold().setMarginLeft(16f))

        val eyeTipLists = List() // Use iText's List constructor
            .setSymbolIndent(12f)
            .setListSymbol("\u2022") // Bullet symbol
        var eyeTips = listOf<String>()
        eyeTips = listOf(
            "Stay hydrated and maintain a balanced diet.",
            "Avoid excessive screen time; use blue-light filters if needed.",
            "Wear UV-protection sunglasses when outdoors.",
            "Ensure proper lighting while reading or using digital devices.",
            "Get regular eye exams even if no symptoms are present."
        )
        eyeTips.forEach { info ->
            val bulletPoint = "✔ $info"
            val paragraph = Paragraph(bulletPoint).setMarginLeft(32f)
            document.add(paragraph)
        }
        document.add(eyeTipLists)


        val disclaimer =
            Paragraph("Note: This report is an initial assessment based on Application analysis. A professional consultation is necessary for an accurate diagnosis and treatment plan.")
        document.add(disclaimer)

        val clinicTitle =
            Paragraph("Nearest Eye Clinic Recommendation:").setBold().setMarginTop(15f)
        document.add(clinicTitle)

        var firstClinic = listOf<String>()
        firstClinic = listOf(
            "Gueco Optical",
            "Rizal St, Poblacion, Gerona, 2302 Tarlac",
            "(045) 925 0303",
        )
        firstClinic.forEachIndexed { index, info ->
            val paragraph = Paragraph(info)
            if (index == 0) {
                paragraph.setMarginTop(15f)
            }
            document.add(paragraph)
        }

        var secondClinic = listOf<String>()
        secondClinic = listOf(
            "Chu Eye Center, ENT & Optical Clinic",
            "M.H. Del Pilar St, Paniqui, 2307 Tarlac",
            "(045) 470 8260",
        )
        secondClinic.forEachIndexed { index, info ->
            val paragraph = Paragraph(info)
            if (index == 0) {
                paragraph.setMarginTop(15f)
            }
            document.add(paragraph)
        }

        var thirdClinic = listOf<String>()
        thirdClinic = listOf(
            "Uycoco Optical Clinic",
            "127 Burgos St, Paniqui, 2307 Tarlac",
            "0933-856-1668",
        )
        thirdClinic.forEachIndexed { index, info ->
            val paragraph = Paragraph(info)
            if (index == 0) {
                paragraph.setMarginTop(15f)
            }
            document.add(paragraph)
        }

        document.close()

        // Optionally, show a toast message to indicate the PDF has been saved
        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
    }
}
