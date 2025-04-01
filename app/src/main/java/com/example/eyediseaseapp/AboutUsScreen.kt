package com.example.eyediseaseapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun AboutUsScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp), // Adjust the height as needed
            contentAlignment = Alignment.Center // Center the text inside the Box
        ) {
            Image(
                painter = painterResource(id = R.drawable.header_bg), // Use header_bg here
                contentDescription = "Header Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth // Changed to FillWidth
            )
            Text(
                text = "About Us",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(top = 100.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 180.dp)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "Welcome to our Eye Disease Detection App! We are dedicated to providing a user-friendly and reliable tool for early detection of eye diseases such as cataracts and glaucoma.",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Normal),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "History",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "This project originated from the need to address eye disease detection challenges in underserved areas, particularly rural communities with limited access to ophthalmologists, recognizing that early detection is crucial in preventing disease progression. The project team leveraged advancements in artificial intelligence and mobile technology to bridge the gap in eye care accessibility by developing a mobile application that empowers non-medical practitioners, such as community health workers, to utilize smartphones for preliminary eye disease assessments.",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Normal),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "This was achieved by training a convolutional neural network on a large dataset of classified eye images to enable rapid identification of various eye conditions from smartphone images. The development process focused on creating a user-friendly interface for ease of use and included telemedicine features to connect users with ophthalmologists for further consultation and verification. Through rigorous testing and a commitment to accuracy and reliability, this project aims to provide a scalable and impactful solution that can significantly improve eye care outcomes and reduce the burden of preventable blindness in underserved communities.",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Normal),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Our Mission",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "To make eye disease detection accessible and timely, especially for underserved communities, by empowering non-medical practitioners with a user-friendly mobile application that utilizes AI to provide preliminary diagnoses and connect users with ophthalmologists for further verification.",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Normal),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Meet Our Team",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(20.dp))
            TeamMember(
                name = "Jhaydee F. De Peralta ",
                role = "Tarlac",
                email = "jhaydeperalta@gmail.com",
                contact = "0952 635 0012",
//                image = R.drawable.team_member_placeholder
            )
            Spacer(modifier = Modifier.height(10.dp))
            TeamMember(
                name = "Edilberto S. Guevarra ",
                role = "Tarlac",
                email = "guevarrabert@gmail.com",
                contact = "0995 469 1006",
//                image = R.drawable.team_member_placeholder
            )
            Spacer(modifier = Modifier.height(10.dp))
            TeamMember(
                name = "Ivan Carl C. Onofre",
                role = "Tarlac",
                email = "civancarl@gmail.com",
                contact = "0931 034 9746",
//                image = R.drawable.team_member_placeholder
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun TeamMember(name: String, role: String, email: String, contact: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Image(
//            painter = painterResource(id = image),
//            contentDescription = "$name Profile",
//            modifier = Modifier
//                .size(100.dp)
//                .clip(CircleShape),
//            contentScale = ContentScale.Crop
//        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            color = colorResource(id = R.color.darkPrimary),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = role,
            color = colorResource(id = R.color.darkPrimary),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = email,
            color = colorResource(id = R.color.darkPrimary),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = contact,
            color = colorResource(id = R.color.darkPrimary),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AboutUsScreenPreview() {
    EyeDiseaseAppTheme {
        AboutUsScreen(navController = rememberNavController())
    }
}
