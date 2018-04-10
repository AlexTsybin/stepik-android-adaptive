package org.stepik.android.adaptive.di.network

import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import org.stepik.android.adaptive.api.rating.RatingService
import org.stepik.android.adaptive.api.StepikService
import org.stepik.android.adaptive.api.auth.AuthInterceptor
import org.stepik.android.adaptive.configuration.Config
import org.stepik.android.adaptive.di.AppSingleton
import org.stepik.android.adaptive.util.setTimeoutsInSeconds

@Module(includes = [AuthModule::class, RatingModule::class, RemoteStorageModule::class])
abstract class NetworkModule {

    @Module
    companion object {
        @Provides
        @AppSingleton
        @JvmStatic
        internal fun provideStepikService(authInterceptor: AuthInterceptor, config: Config): StepikService {
            val okHttpBuilder = OkHttpClient.Builder()
            okHttpBuilder.addInterceptor(authInterceptor)

            okHttpBuilder.setTimeoutsInSeconds(NetworkHelper.TIMEOUT_IN_SECONDS)
            val retrofit = NetworkHelper.createRetrofit(okHttpBuilder.build(), config.host)

            return retrofit.create(StepikService::class.java)
        }
    }

}