package com.junbiao.photorecall.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val dateTaken: Long,
    val location: String? = null,
    val fileName: String
)

class PhotoViewModel : ViewModel() {
    var photos by mutableStateOf<List<PhotoItem>>(emptyList())
        private set
    
    var keptCount by mutableStateOf(0)
        private set
    
    var deletedCount by mutableStateOf(0)
        private set
    
    var currentPhotoIndex by mutableStateOf(0)
        private set
    
    var reviewCount by mutableStateOf(10)
    
    var isReviewing by mutableStateOf(false)
        private set
    
    var reviewPhotos by mutableStateOf<List<PhotoItem>>(emptyList())
        private set
    
    var deletedUris by mutableStateOf<MutableSet<Uri>>(mutableSetOf())
        private set
    
    fun loadPhotos(context: Context) {
        val photosList = mutableListOf<PhotoItem>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val latColumn = cursor.getColumnIndex(MediaStore.Images.Media.LATITUDE)
            val lonColumn = cursor.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateTaken = cursor.getLong(dateColumn)
                val fileName = cursor.getString(nameColumn)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                
                val latitude = if (latColumn >= 0) cursor.getDouble(latColumn) else 0.0
                val longitude = if (lonColumn >= 0) cursor.getDouble(lonColumn) else 0.0
                val location = if (latitude != 0.0 && longitude != 0.0) {
                    "$latitude, $longitude"
                } else null
                
                photosList.add(PhotoItem(id, uri, dateTaken, location, fileName))
            }
        }
        
        photos = photosList
    }
    
    fun startReview(count: Int) {
        reviewCount = count
        reviewPhotos = photos.shuffled().take(count)
        currentPhotoIndex = 0
        isReviewing = true
        keptCount = 0
        deletedCount = 0
        deletedUris = mutableSetOf()
    }
    
    fun keepPhoto() {
        keptCount++
        nextPhoto()
    }
    
    fun deletePhoto(context: Context) {
        val currentPhoto = reviewPhotos.getOrNull(currentPhotoIndex) ?: return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11+, we need to use MediaStore API
                context.contentResolver.delete(currentPhoto.uri, null, null)
            } else {
                @Suppress("DEPRECATION")
                context.contentResolver.delete(currentPhoto.uri, null, null)
            }
            deletedUris.add(currentPhoto.uri)
            deletedCount++
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        nextPhoto()
    }
    
    private fun nextPhoto() {
        currentPhotoIndex++
        if (currentPhotoIndex >= reviewPhotos.size) {
            isReviewing = false
        }
    }
    
    fun getTimeTravelPhotos(yearsAgo: Int): List<PhotoItem> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -yearsAgo)
        val startTime = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        
        val endTime = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        
        return photos.filter { it.dateTaken in startTime..endTime }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: PhotoViewModel = viewModel()
) {
    val context = LocalContext.current
    var photoCount by remember { mutableStateOf(10) }
    
    LaunchedEffect(Unit) {
        viewModel.loadPhotos(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("相册回忆录") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "相册概览",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "共 ${viewModel.photos.size} 张照片",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "今日整理：保留 ${viewModel.keptCount} 张，删除 ${viewModel.deletedCount} 张",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Random Review Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate("random_review") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "随机回忆整理",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "随机展示照片，左滑删除右滑保留",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Time Travel Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate("time_travel") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "时光穿越",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "查看X年前的今天",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Quick actions
            OutlinedButton(
                onClick = { viewModel.loadPhotos(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("刷新相册")
            }
        }
    }
}
