package org.stepik.android.adaptive.gamification

import org.stepik.android.adaptive.api.API
import org.stepik.android.adaptive.data.Analytics
import org.stepik.android.adaptive.data.SharedPreferenceMgr
import org.stepik.android.adaptive.data.db.DataBaseMgr

import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.internal.functions.Functions
import org.stepik.android.adaptive.di.AppSingleton
import org.stepik.android.adaptive.di.qualifiers.BackgroundScheduler
import org.stepik.android.adaptive.gamification.achievements.AchievementEventPoster
import org.stepik.android.adaptive.gamification.achievements.AchievementManager
import retrofit2.HttpException
import javax.inject.Inject

@AppSingleton
class ExpManager
@Inject
constructor(
        private val api: API,
        @BackgroundScheduler
        private val backgroundScheduler: Scheduler,
        private val achievementEventPoster: AchievementEventPoster,
        private val sharedPreferenceMgr: SharedPreferenceMgr,
        private val dataBaseMgr: DataBaseMgr,
        private val analytics: Analytics
) {
    companion object {
        private const val EXP_KEY = "exp_key"
        private const val STREAK_KEY = "streak_key"

        fun syncRating(dataBaseMgr: DataBaseMgr, api: API): Completable = dataBaseMgr.getExp().flatMapCompletable { e -> api.putRating(e) }
    }

    private val compositeDisposable = CompositeDisposable()

    val exp: Long
        get() = sharedPreferenceMgr.getLong(EXP_KEY)

    val streak: Long
        get() = sharedPreferenceMgr.getLong(STREAK_KEY)

    fun changeExp(delta: Long, submissionId: Long): Long {
        val exp = sharedPreferenceMgr.changeLong(EXP_KEY, delta)
        analytics.onExpReached(exp - delta, delta)

        achievementEventPoster.onEvent(AchievementManager.Event.EXP, exp, true)

        compositeDisposable.add(
                dataBaseMgr.onExpGained(delta, submissionId)
                        .andThen(syncRating(dataBaseMgr, api))
                        .subscribeOn(backgroundScheduler)
                        .subscribe(Functions.EMPTY_ACTION, Consumer { e ->
                            if (e is HttpException) {
                                analytics.onRatingError()
                            }
                        })
        )

        return exp
    }

    fun incStreak() = changeStreak(1)

    fun changeStreak(delta: Long): Long {
        val streak = sharedPreferenceMgr.changeLong(STREAK_KEY, delta)
        analytics.onStreak(streak)
        achievementEventPoster.onEvent(AchievementManager.Event.STREAK, streak, true)
        return streak
    }

    fun getCurrentLevel(exp: Long): Long {
        if (exp < 5) return 1

        val level = 2 + (Math.log((exp / 5).toDouble()) / Math.log(2.0)).toLong()

        achievementEventPoster.onEvent(AchievementManager.Event.LEVEL, level, true)

        return level
    }

    fun getNextLevelExp(currentLevel: Long) =
            if (currentLevel == 1L) 5 else 5 * Math.pow(2.0, (currentLevel - 1).toDouble()).toLong()

    fun resetStreak() {
        analytics.onStreakLost(streak)
        sharedPreferenceMgr.saveLong(STREAK_KEY, 0)
    }

    fun reset() {
        sharedPreferenceMgr.remove(EXP_KEY)
        sharedPreferenceMgr.remove(STREAK_KEY)
    }
}