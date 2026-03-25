package com.junbiao.photorecall.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomReviewScreen(
    navController: NavController,
    viewModel: PhotoViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var photoCount by remember { mutableStateOf(10) }
    var showCountDialog by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.all { it.value }
        if (hasPermission) {
            viewModel.loadPhotos(context)
        }
    }
    
    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        hasPermission = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!hasPermission) {
            permissionLauncher.launch(permissions)
        } else {
            viewModel.loadPhotos(context)
        }
    }
    
    // Count selection dialog
    if (showCountDialog) {
        AlertDialog(
            onDismissRequest = { showCountDialog = false },
            title = { Text("选择展示张数") },
            text = {
                Column {
                    listOf(5, 10, 20, 50, 100).forEach { count ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = photoCount == count,
                                onClick = { photoCount = count }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$count 张")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCountDialog = false
                        viewModel.startReview(photoCount)
                    }
                ) {
                    Text("开始")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCountDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("随机整理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("需要相册访问权限")
                    Button(onClick = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(permissions)
                    }) {
                        Text("授权")
                    }
                }
            }
        } else if (!viewModel.isReviewing) {
            // Selection screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "相册共 ${viewModel.photos.size} 张照片",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "选择展示张数：$photoCount 张",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 20, 50, 100).forEach { count ->
                        FilterChip(
                            selected = photoCount == count,
                            onClick = { photoCount = count },
                            label = { Text("$count") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { viewModel.startReview(photoCount) },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    enabled = viewModel.photos.isNotEmpty()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始随机整理")
                }
                
                if (viewModel.photos.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在加载照片...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Review screen
            val currentPhoto = viewModel.reviewPhotos.getOrNull(viewModel.currentPhotoIndex)
            
            if (currentPhoto != null) {
                val swipeThreshold = 200f
                val isSwipingLeft = offsetX < -swipeThreshold
                val isSwipingRight = offsetX > swipeThreshold
                
                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        isSwipingLeft -> Color.Red.copy(alpha = 0.3f)
                        isSwipingRight -> Color.Green.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    label = "backgroundColor"
                )
                
                val scale by animateFloatAsState(
                    targetValue = when {
                        isSwipingLeft || isSwipingRight -> 0.95f
                        else -> 1f
                    },
                    label = "scale"
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(backgroundColor)
                ) {
                    // Progress
                    LinearProgressIndicator(
                        progress = { (viewModel.currentPhotoIndex + 1).toFloat() / viewModel.reviewPhotos.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Photo counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${viewModel.currentPhotoIndex + 1} / ${viewModel.reviewPhotos.size}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row {
                            Text(
                                text = "✓ ${viewModel.keptCount}",
                                color = Color.Green,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                            Text(
                                text = "✗ ${viewModel.deletedCount}",
                                color = Color.Red
                            )
                        }
                    }
                    
                    // Photo card with swipe
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX < -swipeThreshold -> {
                                                viewModel.deletePhoto(context)
                                            }
                                            offsetX > swipeThreshold -> {
                                                viewModel.keepPhoto()
                                            }
                                        }
                                        offsetX = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        offsetX += dragAmount
                                    }
                                )
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentPhoto.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        
                        // Swipe indicators
                        if (offsetX != 0f) {
                            val alpha = kotlin.math.min(kotlin.math.abs(offsetX) / swipeThreshold, 1f)
                            
                            if (offsetX < 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(
                                            Color.Red.copy(alpha = alpha * 0.7f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "删除",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(
                                            Color.Green.copy(alpha = alpha * 0.7f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "保留",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    
                    // Photo info
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
                            Text(
                                text = "拍摄时间：${dateFormat.format(Date(currentPhoto.dateTaken))}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            currentPhoto.location?.let { loc ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = loc,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.deletePhoto(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("删除")
                        }
                        
                        Button(
                            onClick = { viewModel.keepPhoto() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Green
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保留")
                        }
                    }
                }
            }
        }
    }
}
