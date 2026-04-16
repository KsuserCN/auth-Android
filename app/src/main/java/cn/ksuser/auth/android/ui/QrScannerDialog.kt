package cn.ksuser.auth.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import cn.ksuser.auth.android.ui.theme.BrandButtonGradientEnd
import cn.ksuser.auth.android.ui.theme.BrandButtonGradientStart
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun QrScannerDialog(
    onDismiss: () -> Unit,
    onDetected: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val consumed = remember { AtomicBoolean(false) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null || consumed.get()) return@rememberLauncherForActivityResult
        detectQrFromImage(
            scanner = scanner,
            imageUri = uri,
            onDetected = { rawValue ->
                if (consumed.compareAndSet(false, true)) {
                    onDetected(rawValue)
                }
            },
            onMessage = onMessage,
            context = context,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
            runCatching { scanner.close() }
            analysisExecutor.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f)),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        if (consumed.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees,
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val rawValue = firstReadableQrValue(barcodes)
                                if (!rawValue.isNullOrBlank() && consumed.compareAndSet(false, true)) {
                                    previewView.post { onDetected(rawValue) }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }

                    runCatching {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis,
                        )
                    }

                    previewView
                },
            )

            ScannerOverlay(
                modifier = Modifier.fillMaxSize(),
                frameSize = 280.dp,
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "扫描网页二维码",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "请将二维码置于取景框内",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Text("关闭")
            }

            FloatingActionButton(
                onClick = {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = "从相册识别二维码",
                )
            }
        }
    }
}

private fun detectQrFromImage(
    scanner: BarcodeScanner,
    imageUri: Uri,
    context: android.content.Context,
    onDetected: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val inputImage = runCatching { InputImage.fromFilePath(context, imageUri) }
        .getOrElse {
            onMessage("读取图片失败")
            return
        }

    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            val rawValue = firstReadableQrValue(barcodes)
            if (!rawValue.isNullOrBlank()) {
                onDetected(rawValue)
            } else {
                onMessage("未在图片中识别到二维码")
            }
        }
        .addOnFailureListener {
            onMessage("图片识别失败")
        }
}

private fun firstReadableQrValue(barcodes: List<Barcode>): String? = barcodes
    .firstOrNull { !it.rawValue.isNullOrBlank() }
    ?.rawValue
    ?.trim()

@Composable
private fun ScannerOverlay(
    modifier: Modifier = Modifier,
    frameSize: Dp,
) {
    val transition = rememberInfiniteTransition(label = "qr-scan-line")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scan-progress",
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.34f)),
        )

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.72f)
                .aspectRatio(1f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        BrandButtonGradientStart.copy(alpha = 0.95f),
                        BrandButtonGradientEnd.copy(alpha = 0.85f),
                    ),
                ),
            ),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color.Transparent,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = (frameSize - 36.dp) * progress.value)
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    BrandButtonGradientStart.copy(alpha = 0.35f),
                                    BrandButtonGradientEnd,
                                    BrandButtonGradientStart.copy(alpha = 0.35f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = RoundedCornerShape(999.dp),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = (frameSize - 52.dp) * progress.value)
                        .height(20.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    BrandButtonGradientEnd.copy(alpha = 0f),
                                    BrandButtonGradientStart.copy(alpha = 0.14f),
                                    BrandButtonGradientEnd.copy(alpha = 0f),
                                ),
                            ),
                        ),
                )
            }
        }
    }
}
