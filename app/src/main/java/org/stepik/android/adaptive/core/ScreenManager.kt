package org.stepik.android.adaptive.core

import android.content.Context

interface ScreenManager {
    fun showOnboardingScreen()
    fun startStudy()
    fun showImage(context: Context, path: String)
    fun showStatsScreen(context: Context, page: Int)
    fun showQuestionsPacksScreen(context: Context)
    fun showGamificationDescription(context: Context)

    fun showSignInScreen(context: Context)
    fun showSignUpScreen(context: Context)
}