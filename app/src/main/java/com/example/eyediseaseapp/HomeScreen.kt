package com.example.eyediseaseapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.colorResource
import androidx.navigation.compose.rememberNavController
import com.example.eyediseaseapp.ui.theme.EyeDiseaseAppTheme
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.home_bg_2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(200.dp))
            Spacer(modifier = Modifier.height(30.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CATARACT AND GLAUCOMA",
                    color = colorResource(id = R.color.darkPrimary),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontWeight = FontWeight.ExtraBold),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.scan_eye),
                contentDescription = "image 1",
                colorFilter = ColorFilter.tint(colorResource(id = R.color.darkPrimary)),
                modifier = Modifier
                    .width(80.dp)
                    .height(80.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                modifier = Modifier.padding(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.darkPrimary)
                ),
                shape = RoundedCornerShape(10.dp),
                onClick = { navController.navigate("camera") }
            ) {
                Text(
                    text = "SCAN EYE NOW",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontWeight = FontWeight.ExtraBold),
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = "OR",
                color = Color.DarkGray,
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
                onClick = { navController.navigate("imageClassification") }
            ) {
                Text(
                    text = "UPLOAD IMAGE",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontWeight = FontWeight.ExtraBold),
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                modifier = Modifier.clickable {
                    navController.navigate("learnMore")
                },
                text = "Learn More",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.ExtraBold),
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    EyeDiseaseAppTheme {
        HomeScreen(navController = rememberNavController())
    }
}
