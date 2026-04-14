package cn.ksuser.auth.android.data.repository

import android.content.ContentResolver
import android.net.Uri
import cn.ksuser.auth.android.core.network.KsuserApiService
import cn.ksuser.auth.android.core.network.executeEnvelope
import cn.ksuser.auth.android.core.network.requireCode
import cn.ksuser.auth.android.data.model.UpdateProfileRequest
import cn.ksuser.auth.android.data.model.UserProfile
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileRepository(
    private val api: KsuserApiService,
    private val gson: Gson,
) {
    suspend fun getProfile(details: Boolean = true): UserProfile {
        val envelope = executeEnvelope(gson) { api.getUserInfo(if (details) "details" else "basic") }
        requireCode(envelope, 200)
        return envelope.data ?: error("用户信息为空")
    }

    suspend fun updateField(
        key: String,
        value: String,
    ): UserProfile {
        val envelope = executeEnvelope(gson) { api.updateProfile(UpdateProfileRequest(key, value)) }
        requireCode(envelope, 200)
        return envelope.data ?: error("更新后用户信息为空")
    }

    suspend fun uploadAvatar(
        contentResolver: ContentResolver,
        uri: Uri,
    ): UserProfile {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("无法读取头像文件")
        val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)
        val envelope = executeEnvelope(gson) { api.uploadAvatar(part) }
        requireCode(envelope, 200)
        return envelope.data ?: error("头像上传失败")
    }
}
