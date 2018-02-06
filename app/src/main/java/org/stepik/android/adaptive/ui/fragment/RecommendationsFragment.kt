package org.stepik.android.adaptive.ui.fragment

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import org.stepik.android.adaptive.R
import org.stepik.android.adaptive.Util
import org.stepik.android.adaptive.core.ScreenManager
import org.stepik.android.adaptive.core.presenter.BasePresenterFragment
import org.stepik.android.adaptive.core.presenter.RecommendationsPresenter
import org.stepik.android.adaptive.core.presenter.contracts.RecommendationsView
import org.stepik.android.adaptive.databinding.FragmentRecommendationsBinding
import org.stepik.android.adaptive.ui.adapter.QuizCardsAdapter
import org.stepik.android.adaptive.ui.animation.CardsFragmentAnimations
import org.stepik.android.adaptive.ui.dialog.DailyRewardDialog
import org.stepik.android.adaptive.ui.dialog.ExpLevelDialog
import org.stepik.android.adaptive.ui.dialog.RateAppDialog
import org.stepik.android.adaptive.util.InventoryUtil
import org.stepik.android.adaptive.util.PopupHelper

class RecommendationsFragment : BasePresenterFragment<RecommendationsPresenter, RecommendationsView>(), RecommendationsView {
    companion object {
        const val STREAK_RESTORE_REQUEST_CODE = 3423
        const val STREAK_RESTORE_KEY = "streak"

        private const val LEVEL_DIALOG_TAG = "level_dialog"
//        private const val STREAK_RESTORE_DIALOG_TAG = "streak_restore_dialog"
        private const val RATE_APP_DIALOG_TAG = "rate_app_dialog"
        private const val DAILY_REWARD_DIALOG_TAG = "daily_reward_dialog"

        const val INVENTORY_DIALOG_TAG = "inventory_dialog"
    }

    private val loadingPlaceholders by lazy { resources.getStringArray(R.array.recommendation_loading_placeholders) }
    private val streakRestoreViewOffsetX = Resources.getSystem().displayMetrics.widthPixels.toFloat() / 4

    private var streakRestorePopup: PopupWindow? = null

    private lateinit var binding: FragmentRecommendationsBinding

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecommendationsBinding.inflate(inflater, container, false)

        binding.tryAgain.setOnClickListener { presenter?.retry() }
        binding.courseCompletedText.movementMethod = LinkMovementMethod.getInstance()

        binding.streakSuccessContainer.nestedTextView = binding.streakSuccess
        binding.streakSuccessContainer.setGradientDrawableParams(ContextCompat.getColor(context, R.color.colorAccent), 0f)

        binding.toolbar.setOnClickListener { ScreenManager.showStatsScreen(context, 0) }

        return binding.root
    }

    override fun onAdapter(cardsAdapter: QuizCardsAdapter) =
        binding.cardsContainer.setAdapter(cardsAdapter)


    override fun onLoading() {
        binding.progress.visibility = View.VISIBLE
        binding.error.visibility = View.GONE
        binding.loadingPlaceholder.text = loadingPlaceholders[Util.getRandomNumberBetween(0, 3)]
    }

    override fun onCardLoaded() {
        binding.progress.visibility = View.GONE
        binding.cardsContainer.visibility = View.VISIBLE
    }

    private fun onError() {
        binding.cardsContainer.visibility = View.GONE
        binding.error.visibility = View.VISIBLE
        binding.progress.visibility = View.GONE
    }

    override fun onConnectivityError() {
        binding.errorMessage.setText(R.string.connectivity_error)
        onError()
    }

    override fun onRequestError() {
        binding.errorMessage.setText(R.string.request_error)
        onError()
    }

    override fun onCourseCompleted() {
        binding.cardsContainer.visibility = View.GONE
        binding.progress.visibility = View.GONE
        binding.courseCompleted.visibility = View.VISIBLE
    }

    override fun updateExp(exp: Long,
                           currentLevelExp: Long,
                           nextLevelExp: Long,

                           level: Long) {

        binding.expProgress.progress = (exp - currentLevelExp).toInt()
        binding.expProgress.max = (nextLevelExp - currentLevelExp).toInt()

        binding.expCounter.text = exp.toString()
        binding.expLevel.text = getString(R.string.exp_title, level)
        binding.expLevelNext.text = getString(R.string.exp_subtitle, nextLevelExp - exp)
    }

    override fun onStreak(streak: Long) {
        binding.expInc.text = getString(R.string.exp_inc, streak)
        binding.streakSuccess.text = resources.getQuantityString(R.plurals.streak_success, streak.toInt(), streak)

        if (streak > 1) {
            CardsFragmentAnimations.playStreakSuccessAnimationSequence(binding)
        } else {
            CardsFragmentAnimations.playStreakBubbleAnimation(binding.expInc)
        }
    }

    override fun onStreakLost() =
            CardsFragmentAnimations.playStreakFailedAnimation(binding.streakFailed, binding.expProgress)

    override fun onStreakRestored() =
            CardsFragmentAnimations.playStreakRestoreAnimation(binding.streakSuccessContainer)



    override fun showDailyRewardDialog(progress: Long) =
            DailyRewardDialog.newInstance(progress).show(childFragmentManager, DAILY_REWARD_DIALOG_TAG)

    override fun showNewLevelDialog(level: Long) =
            ExpLevelDialog.newInstance(level).show(childFragmentManager, LEVEL_DIALOG_TAG)

    override fun showRateAppDialog() =
            RateAppDialog.newInstance().show(childFragmentManager, RATE_APP_DIALOG_TAG)

    override fun showStreakRestoreDialog(streak: Long, withTooltip: Boolean) {
        binding.ticketItem.counter.text = getString(R.string.amount, InventoryUtil.getItemsCount(InventoryUtil.Item.Ticket))
        CardsFragmentAnimations
                .createShowStreakRestoreWidgetAnimation(binding.ticketsContainer, streakRestoreViewOffsetX)
                .apply {
                    if (withTooltip) {
                        withEndAction {
                            streakRestorePopup = PopupHelper.showPopupAnchoredToView(context, binding.ticketsContainer, getString(R.string.streak_restore_text))
                        }
                    }
                }
                .start()
        binding.ticketsContainer.setOnClickListener {
            if (InventoryUtil.useItem(InventoryUtil.Item.Ticket)) {
                presenter?.restoreStreak(streak)
            }
            hideStreakRestoreDialog()
        }
    }

    override fun hideStreakRestoreDialog() {
        if (streakRestorePopup?.isShowing == true) {
            streakRestorePopup?.dismiss()
        }
        CardsFragmentAnimations.playHideStreakRestoreWidgetAnimation(binding.ticketsContainer, streakRestoreViewOffsetX)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == STREAK_RESTORE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            presenter?.restoreStreak(data?.getLongExtra(STREAK_RESTORE_KEY, 0) ?: 0)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onStart() {
        super.onStart()
        presenter?.attachView(this)
    }

    override fun onStop() {
        presenter?.detachView(this)
        super.onStop()
    }

    override fun getPresenterFactory() = RecommendationsPresenter.Companion
}