package org.stepik.android.adaptive.core.presenter

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.solovyev.android.checkout.*
import org.stepik.android.adaptive.api.Api
import org.stepik.android.adaptive.api.storage.RemoteStorageRepository
import org.stepik.android.adaptive.content.questions.QuestionsPacksManager
import org.stepik.android.adaptive.content.questions.QuestionsPacksResolver
import org.stepik.android.adaptive.core.presenter.contracts.QuestionsPacksView
import org.stepik.android.adaptive.data.Analytics
import org.stepik.android.adaptive.content.questions.QuestionsPack
import org.stepik.android.adaptive.data.preference.SharedPreferenceHelper
import org.stepik.android.adaptive.di.qualifiers.BackgroundScheduler
import org.stepik.android.adaptive.di.qualifiers.MainScheduler
import org.stepik.android.adaptive.ui.adapter.QuestionsPacksAdapter
import org.stepik.android.adaptive.util.*
import javax.inject.Inject

class QuestionsPacksPresenter
@Inject
constructor(
        private val api: Api,
        private val analytics: Analytics,
        @BackgroundScheduler
        private val backgroundScheduler: Scheduler,
        @MainScheduler
        private val mainScheduler: Scheduler,

        private val questionsPacksManager: QuestionsPacksManager,
        private val questionsPacksResolver: QuestionsPacksResolver,

        private val remoteStorageRepository: RemoteStorageRepository,
        private val sharedPreferenceHelper: SharedPreferenceHelper,
        billing: Billing
): PaidContentPresenterBase<QuestionsPacksView>(billing) {
    private val adapter = QuestionsPacksAdapter(this::onPackPressed, questionsPacksResolver)
    private val skus = questionsPacksManager.ids
    private var isPacksLoaded = false

    private val compositeDisposable = CompositeDisposable()

    init {
        adapter.selection = questionsPacksManager.currentPackIndex
    }

    fun loadContent() {
        view?.showContentProgress()
        compositeDisposable addDisposable getInventoryRx(ProductTypes.IN_APP, skus).subscribeOn(mainScheduler).map {
            it.map {
                sku -> sku to questionsPacksManager.getPackById(sku.id.code)!!
            }
        }.flatMap { packs ->
            val ids = packs.map { it.second.courseId }.toLongArray()
            packs.forEach {
                questionsPacksManager.onQuestionsPackViewed(it.second)
            }

            api.getCourses(ids).map { it.courses }.map { courses ->
                courses.mapNotNull { course ->
                    val pack = packs.find { it.second.courseId == course.id }
                    pack?.second?.size = course.totalUnits
                    pack
                }
            }.subscribeOn(backgroundScheduler)
        }.observeOn(mainScheduler).subscribe({
            adapter.items = it
            view?.hideContentProgress()
            isPacksLoaded = true
            restorePurchases()
        }, {
            if (it is PurchasesNotSupportedException) {
                view?.onPurchasesNotSupported()
            } else {
                view?.onContentError()
            }
        })
    }

    fun restorePurchases() {
        view?.showProgress()
        compositeDisposable addDisposable consume(getAllPurchases())
    }

    private fun consume(observable: Observable<Purchase>) = consumeIfNotFake(observable.subscribeOn(mainScheduler).filter {
            skus.contains(it.sku)
        }).observeOn(mainScheduler).subscribe({
            adapter.addOwnedContent(it)
            view?.hideProgress()
        }, {
            if (it !is BillingException || it.response != ResponseCodes.USER_CANCELED && it.response != ResponseCodes.BILLING_UNAVAILABLE) {
                view?.onPurchaseError()
            }
            view?.hideProgress()
        })

    private fun consumeIfNotFake(observable: Observable<Purchase>) = sharedPreferenceHelper.isFakeUser().flatMap { isFake ->
        if (isFake) {
            observable.map { it.sku }.toList()
        } else {
            observable.flatMapCompletable { saveToRemoteStorageAndThenConsume(it) } then
                    remoteStorageRepository.getQuestionsPacks().subscribeOn(backgroundScheduler)
        }
    }

    private fun saveToRemoteStorageAndThenConsume(purchase: Purchase): Completable =
            remoteStorageRepository
                    .storeQuestionsPack(purchase.sku)
                    .subscribeOn(backgroundScheduler) then
                checkout?.onReady()
                        ?.flatMapCompletable { it.consumeRx(purchase.token) }
                        ?.subscribeOn(mainScheduler)


    private fun onPackPressed(sku: Sku, pack: QuestionsPack, isOwned: Boolean) {
        if (isOwned || questionsPacksResolver.isAvailableForFree(pack)) {
            changeCourse(pack)
        } else {
            purchase(sku)
        }
    }

    private fun purchase(sku: Sku) {
        analytics.logEvent(Analytics.EVENT_ON_QUESTIONS_PACK_PURCHASE_BUTTON_CLICKED)
        val purchaseObservable = checkout?.startPurchaseFlowRx(sku) ?: Observable.empty<Purchase>()
        compositeDisposable addDisposable consume(purchaseObservable.flatMap { getAllPurchases() })
    }

    private fun changeCourse(pack: QuestionsPack) {
        view?.showProgress()
        compositeDisposable addDisposable api.joinCourse(pack.courseId).doOnComplete {
            questionsPacksManager.switchPack(pack)
        }
        .observeOn(mainScheduler)
        .subscribeOn(backgroundScheduler)
        .subscribe({
            adapter.selection = pack.ordinal
            view?.hideProgress()
        }, {
            view?.hideProgress()
        })
    }

    override fun attachView(view: QuestionsPacksView) {
        super.attachView(view)

        view.onAdapter(adapter)

        if (isPacksLoaded) {
            view.hideContentProgress()
        } else {
            loadContent()
        }
    }

    override fun destroy() {
        compositeDisposable.dispose()
    }
}