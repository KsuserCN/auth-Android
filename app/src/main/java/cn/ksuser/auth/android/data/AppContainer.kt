package cn.ksuser.auth.android.data

import android.content.Context
import androidx.credentials.CredentialManager
import cn.ksuser.auth.android.core.env.EnvironmentProvider
import cn.ksuser.auth.android.core.network.AccessTokenInterceptor
import cn.ksuser.auth.android.core.network.CookieStore
import cn.ksuser.auth.android.core.network.CsrfInterceptor
import cn.ksuser.auth.android.core.network.KsuserApiService
import cn.ksuser.auth.android.core.network.PersistentCookieJar
import cn.ksuser.auth.android.core.network.UnauthorizedRetryInterceptor
import cn.ksuser.auth.android.core.passkey.PasskeyManager
import cn.ksuser.auth.android.core.session.SecureSessionStorage
import cn.ksuser.auth.android.core.session.SessionRepository
import cn.ksuser.auth.android.data.repository.AuthRepository
import cn.ksuser.auth.android.data.repository.LogsRepository
import cn.ksuser.auth.android.data.repository.ProfileRepository
import cn.ksuser.auth.android.data.repository.SecurityRepository
import cn.ksuser.auth.android.data.repository.SessionsRepository
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val environment = EnvironmentProvider.current
    private val gson = Gson()
    private val cookieStore = CookieStore(appContext, gson)
    private val cookieJar = PersistentCookieJar(cookieStore)
    private val sessionStorage = SecureSessionStorage(appContext)

    private val baseClientBuilder = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(CsrfInterceptor(cookieStore))
        .addInterceptor(AccessTokenInterceptor(sessionStorage))
        .apply {
            if (environment.enableHttpLogging) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    },
                )
            }
        }

    private val refreshApi: KsuserApiService by lazy {
        retrofit(baseClientBuilder.build()).create(KsuserApiService::class.java)
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(
            apiBaseUrl = "${environment.apiBaseUrl}/".toHttpUrl(),
            refreshApi = refreshApi,
            sessionStorage = sessionStorage,
            cookieStore = cookieStore,
            gson = gson,
        )
    }

    private val mainApi: KsuserApiService by lazy {
        val client = baseClientBuilder
            .addInterceptor(UnauthorizedRetryInterceptor(sessionRepository))
            .build()
        retrofit(client).create(KsuserApiService::class.java)
    }

    val authRepository: AuthRepository by lazy { AuthRepository(mainApi, sessionRepository, gson) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(mainApi, gson) }
    val securityRepository: SecurityRepository by lazy { SecurityRepository(mainApi, gson) }
    val sessionsRepository: SessionsRepository by lazy { SessionsRepository(mainApi, gson) }
    val logsRepository: LogsRepository by lazy { LogsRepository(mainApi, gson) }
    val passkeyManager: PasskeyManager by lazy {
        PasskeyManager(
            credentialManager = CredentialManager.create(appContext),
            gson = gson,
            environment = environment,
        )
    }

    private fun retrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("${environment.apiBaseUrl}/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
