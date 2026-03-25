package com.junbiao.photorecall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.junbiao.photorecall.ui.screens.HomeScreen
import com.junbiao.photorecall.ui.screens.RandomReviewScreen
import com.junbiao.photorecall.ui.screens.TimeTravelScreen
import com.junbiao.photorecall.ui.theme.PhotoRecallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoRecallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("random_review") {
                            RandomReviewScreen(navController = navController)
                        }
                        composable("time_travel") {
                            TimeTravelScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
