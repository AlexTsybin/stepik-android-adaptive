package org.stepik.android.adaptive.di.network


import dagger.Binds
import dagger.Module
import dagger.Provides
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.stepik.android.adaptive.api.auth.*
import org.stepik.android.adaptive.configuration.Config
import org.stepik.android.adaptive.data.preference.SharedPreferenceHelper
import org.stepik.android.adaptive.data.preference.AuthPreferences
import org.stepik.android.adaptive.di.AppSingleton
import org.stepik.android.adaptive.di.qualifiers.AuthLock
import org.stepik.android.adaptive.di.qualifiers.AuthService
import org.stepik.android.adaptive.di.qualifiers.CookieAuthService
import org.stepik.android.adaptive.di.qualifiers.SocialAuthService
import org.stepik.android.adaptive.util.AppConstants
import org.stepik.android.adaptive.util.addUserAgent
import org.stepik.android.adaptive.util.setTimeoutsInSeconds
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Named

@Module
abstract class AuthModule {

    @Binds
    @AppSingleton
    abstract fun provideAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Module
    companion object {
        @Provides
        @AppSingleton
        @JvmStatic
        @AuthLock
        internal fun provideAuthLock(): ReentrantLock = ReentrantLock()

        @Provides
        @AppSingleton
        @JvmStatic
        @SocialAuthService
        internal fun provideSocialAuthService(
                @Named(AppConstants.userAgentName)
                userAgent: String,
                config: Config
        ): OAuthService = createAuthService(Credentials.basic(config.oAuthClientIdSocial, config.oAuthClientSecretSocial), userAgent, config.host)

        @Provides
        @AppSingleton
        @JvmStatic
        @AuthService
        internal fun provideAuthService(
                @Named(AppConstants.userAgentName)
                userAgent: String,
                config: Config
        ): OAuthService = createAuthService(Credentials.basic(config.oAuthClientId, config.oAuthClientSecret), userAgent, config.host)

        @Provides
        @AppSingleton
        @JvmStatic
        internal fun provideEmptyAuthService(config: Config): EmptyAuthService {
            val okHttpBuilder = OkHttpClient.Builder()
            okHttpBuilder.setTimeoutsInSeconds(NetworkHelper.TIMEOUT_IN_SECONDS)
            val retrofit = NetworkHelper.createRetrofit(okHttpBuilder.build(), config.host)
            return retrofit.create(EmptyAuthService::class.java)
        }

        @Provides
        @AppSingleton
        @JvmStatic
        @CookieAuthService
        internal fun provideCookieAuthService(
                @Named(AppConstants.userAgentName)
                userAgent: String,
                cookieHelper: CookieHelper,
                config: Config
        ): OAuthService {
            val okHttpBuilder = OkHttpClient.Builder()
            okHttpBuilder.addNetworkInterceptor { chain ->
                cookieHelper.removeCookiesCompat()
                cookieHelper.updateCookieForBaseUrl()
                chain.proceed(cookieHelper.addCsrfTokenToRequest(chain.addUserAgent(userAgent)))
            }
            okHttpBuilder.setTimeoutsInSeconds(NetworkHelper.TIMEOUT_IN_SECONDS)

            val retrofit = NetworkHelper.createRetrofit(okHttpBuilder.build(), config.host)
            return retrofit.create(OAuthService::class.java)
        }

        private fun createAuthService(credentials: String, userAgent: String, host: String): OAuthService {
            val okHttpBuilder = OkHttpClient.Builder()

            okHttpBuilder.addInterceptor { chain ->
                chain.proceed(chain.addUserAgent(userAgent).newBuilder().header(AppConstants.authorizationHeaderName, credentials).build())
            }
            okHttpBuilder.protocols(listOf(Protocol.HTTP_1_1))
            okHttpBuilder.setTimeoutsInSeconds(NetworkHelper.TIMEOUT_IN_SECONDS)

            val retrofit = NetworkHelper.createRetrofit(okHttpBuilder.build(), host)
            return retrofit.create(OAuthService::class.java)
        }
    }

}