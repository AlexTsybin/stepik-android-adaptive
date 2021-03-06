package org.stepik.android.adaptive.ui.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_questions_packs.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.state_error.*
import org.solovyev.android.checkout.Billing
import org.solovyev.android.checkout.Checkout
import org.stepik.android.adaptive.App
import org.stepik.android.adaptive.R
import org.stepik.android.adaptive.core.presenter.BasePresenterActivity
import org.stepik.android.adaptive.core.presenter.QuestionsPacksPresenter
import org.stepik.android.adaptive.core.presenter.contracts.QuestionsPacksView
import org.stepik.android.adaptive.ui.adapter.QuestionsPacksAdapter
import org.stepik.android.adaptive.util.changeVisibillity
import javax.inject.Inject
import javax.inject.Provider

class QuestionsPacksActivity : BasePresenterActivity<QuestionsPacksPresenter, QuestionsPacksView>(), QuestionsPacksView {
    companion object {
        const val RESTORE_DIALOG_TAG = "restore_dialog"
    }

    @Inject
    lateinit var questionsPacksPresenterProvider: Provider<QuestionsPacksPresenter>

    @Inject
    lateinit var billing: Billing

    override fun injectComponent() {
        App.componentManager().paidContentComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questions_packs)
        recycler.layoutManager = LinearLayoutManager(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.questions_packs)

        restorePurchases.setOnClickListener {
            presenter?.restorePurchases()
        }

        tryAgainButton.setOnClickListener {
            presenter?.loadContent()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPurchaseError() {
        Snackbar.make(root, R.string.purchase_error, Snackbar.LENGTH_LONG).show()
    }

    override fun onPurchasesNotSupported() {
        recycler.changeVisibillity(false)
        progress.changeVisibillity(false)
        purchasesAreNotSupported.changeVisibillity(true)
        restorePurchases.changeVisibillity(false)
        errorState.changeVisibillity(false)
    }

    override fun showContentProgress() {
        recycler.changeVisibillity(false)
        progress.changeVisibillity(true)
        purchasesAreNotSupported.changeVisibillity(false)
        errorState.changeVisibillity(false)
    }

    override fun hideContentProgress() {
        recycler.changeVisibillity(true)
        progress.changeVisibillity(false)
        purchasesAreNotSupported.changeVisibillity(false)
        errorState.changeVisibillity(false)
    }

    override fun onContentError() {
        recycler.changeVisibillity(false)
        progress.changeVisibillity(false)
        purchasesAreNotSupported.changeVisibillity(false)
        errorState.changeVisibillity(true)
    }

    override fun createCheckout() = Checkout.forActivity(this, billing)

    override fun showProgress() =
            showProgressDialogFragment(RESTORE_DIALOG_TAG, getString(R.string.loading_message), getString(R.string.processing_your_request))

    override fun hideProgress() = hideProgressDialogFragment(RESTORE_DIALOG_TAG)

    override fun onAdapter(adapter: QuestionsPacksAdapter) {
        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        presenter?.attachView(this)
    }

    override fun onStop() {
        presenter?.detachView(this)
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        presenter?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun getPresenterProvider() = questionsPacksPresenterProvider
}