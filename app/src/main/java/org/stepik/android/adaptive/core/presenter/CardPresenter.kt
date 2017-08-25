package org.stepik.android.adaptive.core.presenter

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.stepik.android.adaptive.api.API
import org.stepik.android.adaptive.api.SubmissionResponse
import org.stepik.android.adaptive.core.presenter.contracts.CardView
import org.stepik.android.adaptive.data.AnalyticMgr
import org.stepik.android.adaptive.data.model.Card
import org.stepik.android.adaptive.data.model.RecommendationReaction
import org.stepik.android.adaptive.data.model.Submission
import org.stepik.android.adaptive.ui.listener.AdaptiveReactionListener
import org.stepik.android.adaptive.ui.listener.AnswerListener
import org.stepik.android.adaptive.util.HtmlUtil
import retrofit2.HttpException
import java.util.concurrent.TimeUnit


class CardPresenter(val card: Card, private val listener: AdaptiveReactionListener?, private val answerListener: AnswerListener?) : PresenterBase<CardView>() {
    private var submission: Submission? = null
    private var error: Throwable? = null

    private var disposable: Disposable? = null

    var isLoading = false
        get

    override fun attachView(view: CardView) {
        super.attachView(view)
        view.setTitle(card.lesson.title)
        view.setQuestion(HtmlUtil.prepareCardHtml(card.step.block.text))
        view.setAnswerAdapter(card.adapter)

        if (isLoading) view.onSubmissionLoading()
        submission?.let { view.setSubmission(it, false) }
        error?.let { onError(it) }
    }

    fun detachView() {
        view?.let {
            super.detachView(it)
        }
    }

    override fun destroy() {
        card.recycle()
        disposable?.dispose()
    }

    fun createReaction(reaction: RecommendationReaction.Reaction) {
        val lesson = card.lessonId
        when(reaction) {
            RecommendationReaction.Reaction.NEVER_AGAIN -> {
                if (card.isCorrect) {
                    AnalyticMgr.getInstance().reactionEasyAfterCorrect(lesson)
                }
                AnalyticMgr.getInstance().reactionEasy(lesson)
            }

            RecommendationReaction.Reaction.MAYBE_LATER -> {
                if (card.isCorrect) {
                    AnalyticMgr.getInstance().reactionHardAfterCorrect(lesson)
                }
                AnalyticMgr.getInstance().reactionHard(lesson)
            }
        }
        listener?.createReaction(lesson, reaction)
    }

    fun createSubmission() {
        if (disposable == null || disposable?.isDisposed ?: true) {
            card.adapter.setEnabled(false)
            view?.onSubmissionLoading()
            isLoading = true
            error = null

            val submission = card.adapter.submission
            disposable = API.getInstance().createSubmission(submission)
                    .andThen(API.getInstance().getSubmissions(submission.attempt))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onSubmissionLoaded, this::onError)

            AnalyticMgr.getInstance().onSubmissionWasMade()
        }
    }

    fun retrySubmission() {
        submission = null
        card.adapter.setEnabled(true)
    }

    private fun onSubmissionLoaded(submissionResponse: SubmissionResponse) {
        submission = submissionResponse.firstSubmission
        submission?.let {
            if (it.status == Submission.Status.EVALUATION) {
                disposable =  API.getInstance().getSubmissions(it.attempt)
                        .delay(1, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onSubmissionLoaded, this::onError)
            } else {
                isLoading = false
                AnalyticMgr.getInstance().answerResult(card.step, it)
                if (it.status == Submission.Status.CORRECT) {
                    listener?.createReaction(card.lessonId, RecommendationReaction.Reaction.SOLVED)
                    answerListener?.onCorrectAnswer(it.id)
                    card.onCorrect()
                }
                if (it.status == Submission.Status.WRONG) {
                    answerListener?.onWrongAnswer()
                }
                view?.setSubmission(it, true)
            }
        }
    }

    private fun onError(error: Throwable) {
        isLoading = false
        this.error = error
        card.adapter.setEnabled(true)
        if (error is HttpException) {
            view?.onSubmissionRequestError()
        } else {
            view?.onSubmissionConnectivityError()
        }
    }
}