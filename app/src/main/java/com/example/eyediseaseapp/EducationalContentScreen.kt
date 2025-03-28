package com.example.eyediseaseapp

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eyediseaseapp.ui.theme.EyeDiseaseAppTheme

@Composable
fun EducationalContentScreen(navController: NavController) {


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
                text = "Learn About Eye Diseases",
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
                .padding(top = 220.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                Card(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "Cataracts",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Cataracts are a clouding of the eye's lens that can cause blurry vision. They are a common condition that affects millions of people around the world. Although cataracts usually develop slowly, they can eventually interfere with your vision.",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)

                    ) {
                        Text(
                            text = "Glaucoma",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Glaucoma is a group of eye conditions that damage the optic nerve, which carries information from your eye to your brain. The most common type of glaucoma is called open-angle glaucoma, which develops slowly over time. If left untreated, glaucoma can lead to permanent vision loss.",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.darkPrimary)
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Symptoms of Cataracts",
                            color = colorResource(id = R.color.white),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "- Cloudy or blurry vision",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Difficulty seeing at night",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Sensitivity to light",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Halos around lights",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Fading or yellowing of colors",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Double vision in a single eye",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.darkPrimary)
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Symptoms of Glaucoma",
                            color = colorResource(id = R.color.white),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "- Difficulty adjusting to low light",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Eye pain",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Headache",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Halos around lights",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Nausea and vomiting",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Risk Factors for Cataracts",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "- Age",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Smoking",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Prolonged exposure to UV light",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Diabetes",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Steroid uses",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Family history of cataracts",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Previous eye surgery",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Risk Factors for Glaucoma",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "- Age",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Family history of glaucoma",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- High eye pressure",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Thinning of the cornea",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Diabetes",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Steroid use",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Eye injury",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 12.sp,
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.height(15.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.darkPrimary)
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Prevention Tips for Cataracts",
                            color = colorResource(id = R.color.white),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "- Wear sunglasses that block UV light",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Don't smoke",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Eat a healthy diet",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Maintain a healthy weight",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Control diabetes",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- See your eye doctor regularly",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.darkPrimary)
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Prevention Tips for Glaucoma",
                            color = colorResource(id = R.color.white),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "- Get regular eye exams",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Manage your blood pressure and diabetes",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Don't smoke",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "- Wear eye protection when playing sports or using power tools",
                            color = colorResource(id = R.color.white),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 50.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)

                ) {
                    Text(
                        text = "Conclusion",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Cataracts and glaucoma are both serious eye conditions that can lead to vision loss. However, there are steps you can take to reduce your risk of developing these conditions. If you have any concerns about your eye health, be sure to see your eye doctor.",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 12.sp,
                    )
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
