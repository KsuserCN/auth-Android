package cn.ksuser.auth.android.core.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import cn.ksuser.auth.android.BuildConfig
import java.security.MessageDigest

data class AppIdentity(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val signingSha256: List<String>,
)

object AppIdentityProvider {
    fun current(context: Context): AppIdentity {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
1
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners.orEmpty().map { it.toByteArray() }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures.orEmpty().map { it.toByteArray() }
        }

        return AppIdentity(
            packageName = context.packageName,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            signingSha256 = signatures.map(::sha256Fingerprint).distinct(),
        )
    }

    fun userAgent(): String {
        val release = Build.VERSION.RELEASE?.takeIf { it.isNotBlank() } ?: "unknown"
        return "KsuserAuthMobile/${BuildConfig.VERSION_NAME} (Android $release; API ${Build.VERSION.SDK_INT})"
    }

    private fun sha256Fingerprint(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(":") { "%02X".format(it) }
    }
}
