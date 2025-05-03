package com.example.eyediseaseapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eyediseaseapp.ui.theme.EyeDiseaseAppTheme
import kotlinx.coroutines.CoroutineScope


@Composable
fun EducationalContentScreen(
    navController: NavController,
    modifier: Modifier = Modifier, // Modifier from Scaffold padding if used
    drawerState: DrawerState? = null, // Accept nullable if sidebar is optional
    scope: CoroutineScope? = null //
) {
    var showCataractsDialog by remember { mutableStateOf(false) }
    var showGlaucomaDialog by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.header_bg),
                contentDescription = "Header Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )
            Text(
                text = "Understanding Cataracts and Glaucoma",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(top = 100.dp)
            )

        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 220.dp, start = 16.dp, end = 16.dp, bottom = 50.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Cataracts and glaucoma are two of the most common eye conditions that can significantly impact vision, particularly as we age. While both can lead to vision loss, they affect different parts of the eye and have distinct causes, symptoms, and treatments. This educational resource aims to provide a comprehensive overview of these conditions, empowering individuals with knowledge about their eye health.",
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Text(
                    text = "Click on the buttons below to learn more about these diseases",
                    color = colorResource(id = R.color.darkPrimary),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontWeight = FontWeight.ExtraBold),
                )

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    modifier = Modifier.padding(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.darkPrimary)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {
                        showCataractsDialog = true // Show the Glaucoma dialog
                    }
                ) {
                    Text(
                        text = "Cataracts",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))
                Button(
                    modifier = Modifier.padding(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.darkPrimary)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {
                        showGlaucomaDialog = true // Show the Glaucoma dialog
                    }
                ) {
                    Text(
                        text = "Glaucoma",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Conclusion",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                style = TextStyle(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Text(
                    text = "Cataracts and glaucoma are serious eye conditions that require professional medical attention. Understanding the differences between them, their causes, symptoms, and treatments is crucial for maintaining good eye health. Regular comprehensive eye examinations are the best way to detect these conditions early and protect your vision. If you experience any changes in your vision, it is important to consult with an eye care professional promptly.",
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "References:",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                style = TextStyle(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                val context = LocalContext.current
                val annotatedText = buildAnnotatedString {
                    append("1. American Academy of Ophthalmology. (n.d.). Cataracts. Retrieved from ")
                    pushStringAnnotation(tag = "URL", annotation = "https://www.aao.org/eye-health/diseases/what-are-cataracts")
                    withStyle(
                        style = SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append("https://www.aao.org/eye-health/diseases/what-are-cataracts")
                    }
                }

                ClickableText(
                    text = annotatedText,
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        )
                            .firstOrNull()?.let { annotation ->
                                // Handle the click (e.g., open a URL)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            }
                    }
                )

                val annotatedText2 = buildAnnotatedString {
                    append("2. American Academy of Ophthalmology. (n.d.). Glaucoma. Retrieved from ")
                    pushStringAnnotation(tag = "URL", annotation = "https://www.aao.org/eye-health/diseases/what-is-glaucoma")
                    withStyle(
                        style = SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append("https://www.aao.org/eye-health/diseases/what-is-glaucoma")
                    }
                }

                ClickableText(
                    text = annotatedText2,
                    onClick = { offset ->
                        annotatedText2.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        )
                            .firstOrNull()?.let { annotation ->
                                // Handle the click (e.g., open a URL)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            }
                    }
                )
                val annotatedText3 = buildAnnotatedString {
                    append("3. National Eye Institute. (2023, September 28). Cataracts. Retrieved from ")
                    pushStringAnnotation(tag = "URL", annotation = "https://www.nei.nih.gov/learn-about-eye-health/eye-conditions-and-diseases/cataracts")
                    withStyle(
                        style = SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append("https://www.nei.nih.gov/learn-about-eye-health/eye-conditions-and-diseases/cataracts")
                    }
                }

                ClickableText(
                    text = annotatedText3,
                    onClick = { offset ->
                        annotatedText3.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        )
                            .firstOrNull()?.let { annotation ->
                                // Handle the click (e.g., open a URL)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            }
                    }
                )

                val annotatedText4 = buildAnnotatedString {
                    append("4. National Eye Institute. (2024, March 25). Glaucoma. Retrieved from ")
                    pushStringAnnotation(tag = "URL", annotation = "https://www.nei.nih.gov/learn-about-eye-health/eye-conditions-and-diseases/glaucoma")
                    withStyle(
                        style = SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append("https://www.nei.nih.gov/learn-about-eye-health/eye-conditions-and-diseases/glaucoma")
                    }
                }

                ClickableText(
                    text = annotatedText4,
                    onClick = { offset ->
                        annotatedText4.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        )
                            .firstOrNull()?.let { annotation ->
                                // Handle the click (e.g., open a URL)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            }
                    }
                )

                val annotatedText5 = buildAnnotatedString {
                    append("5. World Health Organization. (2021, February 8). Blindness and vision impairment. Retrieved from ")
                    pushStringAnnotation(tag = "URL", annotation = "https://www.who.int/news-room/fact-sheets/detail/blindness-and-visual-impairment")
                    withStyle(
                        style = SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append("https://www.who.int/news-room/fact-sheets/detail/blindness-and-visual-impairment")
                    }
                }
                ClickableText(
                    text = annotatedText5,
                    onClick = { offset ->
                        annotatedText5.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation -> val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)); context.startActivity(intent) }
                    }
                )
            }
        }
    }

    if (showCataractsDialog) {
        Dialog(onDismissRequest = { showCataractsDialog = false }) { // Dismiss on outside click
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()), // Make dialog content scrollable
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Cataracts",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Cataracts are a clouding of the eye's lens that can cause blurry vision. They are a common condition that affects millions of people around the world. Although cataracts usually develop slowly, they can eventually interfere with your vision.",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "What Causes Cataracts?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "The most common cause of cataracts is the natural aging process. As we get older, the proteins in the lens can break down and clump together, leading to clouding. Other factors that can contribute to cataract formation include:",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )
                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Age: This is the primary risk factor.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Ultraviolet (UV) radiation: Prolonged exposure to sunlight without eye protection can increase the risk. ",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Diabetes: People with diabetes are at a higher risk of developing cataracts, often at a younger age.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Smoking: Smoking has been shown to increase the risk of cataracts.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Certain medications: Long-term use of corticosteroids, for example, can contribute to cataract development.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Eye injury or inflammation: Trauma or inflammation in the eye can lead to cataracts.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Family history: Having a family history of cataracts may increase your risk.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Other eye conditions: Certain eye diseases can be associated with cataract formation.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "What are the Symptoms of Cataracts?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Cataracts typically develop gradually, and symptoms may not be noticeable in the early stages. As the cataract progresses, common symptoms include:",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )
                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Blurred, cloudy, or hazy vision",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Difficulty seeing in low light conditions or at night",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Increased sensitivity to glare from lights, particularly headlights at night",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Seeing halos around lights",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Faded or yellowing of colors",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Frequent changes in eyeglass or contact lens prescriptions",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Double vision in one eye (this may resolve as the cataract progresses)",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "It's important to note that cataracts usually do not cause pain or redness in the eye.",
                        color = colorResource(id = R.color.normalPrimary),
                        fontSize = 14.sp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "What are the Symptoms of Cataracts?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "An eye care professional can diagnose cataracts through a comprehensive eye examination. This typically includes",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Visual acuity test: To measure the sharpness of your vision at various distances.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Dilated eye exam: Eye drops are used to widen the pupil, allowing the doctor to examine the lens and other parts of the eye.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Slit-lamp examination: A microscope is used to get a magnified view of the front of the eye, including the lens.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "How are Cataracts Treated?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "In the early stages, when symptoms are mild, a change in eyeglass prescription may help improve vision. However, surgery is the only effective treatment for cataracts. Cataract surgery is a very common and generally safe procedure. It involves removing the cloudy natural lens and replacing it with a clear artificial lens called an intraocular lens (IOL).",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Cataract surgery is typically recommended when the cataract is significantly affecting a person's ability to perform daily activities such as driving, reading, or watching television. The decision to have surgery is made on an individual basis in consultation with an eye care professional.",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "How are Cataracts Treated?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "While age-related cataracts cannot be entirely prevented, certain measures may help slow their progression or reduce the risk:",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Wearing sunglasses that block UV rays.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Not smoking.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Managing diabetes and other health conditions.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Eating a healthy diet rich in fruits and vegetables.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Having regular eye exams to detect cataracts early.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    // --- Close Button for the Dialog ---
                    Button(onClick = { showCataractsDialog = false }) { // Hide the dialog
                        Text("Close")
                    }
                }
            }
        }
    }

    if (showGlaucomaDialog) {
        Dialog(onDismissRequest = { showGlaucomaDialog = false }) { // Dismiss on outside click
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()), // Make dialog content scrollable
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Glaucoma",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Glaucoma is a group of eye diseases that damage the optic nerve, which connects the eye to the brain. This damage is often caused by abnormally high pressure inside the eye (intraocular pressure or IOP). Glaucoma is often called the \"silent thief of sight\" because it typically progresses slowly and without noticeable symptoms in its early stages. By the time symptoms appear, irreversible vision loss may have already occurred.",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "What Causes Glaucoma?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Glaucoma is usually associated with increased intraocular pressure, but it can also occur with normal eye pressure. The pressure in the eye is regulated by a balance between the production and drainage of a clear fluid called aqueous humor. In most cases of glaucoma, there is a problem with the drainage system of the eye, leading to a buildup of fluid and increased pressure.",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "There are several types of glaucoma, with the two main types being:",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )
                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Primary Open-Angle Glaucoma (POAG): This is the most common type. In POAG, the drainage angle of the eye is open, but the drainage channels are somehow blocked, causing pressure to build up gradually.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Angle-Closure Glaucoma: This type is less common and can occur suddenly. It happens when the iris is too close to the drainage angle, blocking it completely or partially. This can lead to a rapid increase in eye pressure.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Other types of glaucoma include congenital glaucoma (present at birth), secondary glaucoma (caused by another medical condition or medication), and normal-tension glaucoma (optic nerve damage occurs despite normal eye pressure).",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "What are the Risk Factors for Glaucoma?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Anyone can develop glaucoma, but some people are at a higher risk, including:",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )
                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Age: The risk increases with age, especially for individuals over 60. ",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Race: People of African descent are at a higher risk of developing POAG and experiencing more severe vision loss. Individuals of Asian descent have a higher risk of angle-closure glaucoma.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Family history: Having a family history of glaucoma significantly increases your risk.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• High intraocular pressure: This is a major risk factor, although not everyone with high IOP will develop glaucoma.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Thin corneas: The thickness of the cornea can affect IOP readings and may be a risk factor.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Medical conditions: Diabetes, high blood pressure, heart disease, and sickle cell anemia can increase the risk.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Certain medications: The prolonged use of corticosteroids can increase IOP.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Eye injury: Severe eye injuries can lead to traumatic glaucoma.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "What are the Symptoms of Glaucoma?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "As mentioned, POAG often has no symptoms in the early stages. As the disease progresses and damages the optic nerve, blind spots may develop in the peripheral (side) vision. These blind spots can enlarge over time, leading to tunnel vision. Central vision is usually affected in the later stages.",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Symptoms of acute angle-closure glaucoma can appear suddenly and may include:",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Severe eye pain",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Blurred vision",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Seeing halos or rainbows around lights",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Nausea and vomiting",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Redness of the eye",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "This is a medical emergency, and immediate medical attention is required to prevent severe vision loss.",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "How is Glaucoma Diagnosed?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Regular comprehensive eye exams are crucial for detecting glaucoma in its early stages. Several tests are used to diagnose glaucoma:",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Tonometry: Measures the pressure inside the eye. ",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Optical Coherence Tomography (OCT): Provides detailed images of the optic nerve and retina to assess for damage.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• MVisual field test (Perimetry): Measures your peripheral vision to detect any blind spots.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Pachymetry: Measures the thickness of the cornea.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Gonioscopy: Examines the drainage angle of the eye.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Dilated eye exam: Allows the doctor to visualize the optic nerve for signs of damage. ",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "How is Glaucoma Treated?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Damage caused by glaucoma is irreversible, but treatment can help manage the condition and prevent further vision loss. The primary goal of glaucoma treatment is to lower intraocular pressure. Treatment options include: ",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Column(
                        Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Eye drops: These are the most common treatment and help lower eye pressure by either decreasing the production of aqueous humor or increasing its drainage.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Oral medications: In some cases, oral medications may be prescribed to lower eye pressure.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Surgery: Various surgical procedures can be performed to create new drainage channels or implant devices to help lower eye pressure.",
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "The type of treatment recommended depends on the type and severity of glaucoma, as well as the individual patient's overall health.",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Can Glaucoma be Prevented?",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "There is currently no known way to prevent glaucoma. However, early detection and treatment are key to preventing significant vision loss. Regular comprehensive eye exams, especially for individuals at higher risk, are essential for catching glaucoma in its early stages when it is most treatable. Maintaining a healthy lifestyle and managing underlying health conditions like diabetes and high blood pressure may also play a role in eye health.",
                        color = Color.Black,
                        fontSize = 14.sp,
                    )

                    // --- Close Button for the Dialog ---
                    Button(onClick = { showGlaucomaDialog = false }) { // Hide the dialog
                        Text("Close")
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EducationalContentScreenPreview() {
    EyeDiseaseAppTheme {
        EducationalContentScreen(navController = rememberNavController())
    }
}
