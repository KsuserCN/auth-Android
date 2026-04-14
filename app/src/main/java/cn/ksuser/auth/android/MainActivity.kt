package cn.ksuser.auth.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.ksuser.auth.android.ui.KsuserAuthApp
import cn.ksuser.auth.android.ui.theme.KsuserAuthAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KsuserAuthAndroidTheme {
                KsuserAuthApp()
            }
        }
    }
}
