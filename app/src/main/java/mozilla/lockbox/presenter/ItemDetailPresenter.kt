/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.annotation.StringRes
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.support.FeatureFlags
import mozilla.lockbox.support.pushError

interface ItemDetailView {
    val usernameCopyClicks: Observable<Unit>
    val passwordCopyClicks: Observable<Unit>
    val togglePasswordClicks: Observable<Unit>
    val hostnameClicks: Observable<Unit>
    val kebabMenuClicks: Observable<Unit>
    val editClicks: BehaviorRelay<Unit>
    val deleteClicks: BehaviorRelay<Unit>
    var isPasswordVisible: Boolean
    fun showKebabMenu()
    fun hideKebabMenu()
    fun updateItem(item: ItemDetailViewModel)
    fun showPopup()
    fun showToastNotification(@StringRes strId: Int)
    fun handleNetworkError(networkErrorVisibility: Boolean)
//    val retryNetworkConnectionClicks: Observable<Unit>
}

@ExperimentalCoroutinesApi
class ItemDetailPresenter(
    private val view: ItemDetailView,
    val itemId: String?,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val networkStore: NetworkStore = NetworkStore.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : Presenter() {

    private var credentials: ServerPassword? = null

    override fun onPause() {
        super.onPause()
        dispatcher.dispatch(ItemDetailAction.TogglePassword)
    }

    override fun onViewReady() {
        val itemId = this.itemId ?: return
        dataStore.get(itemId)
            .observeOn(mainThread())
            .filterNotNull()
            .doOnNext { credentials = it }
            .map { it.toDetailViewModel() }
            .subscribe(view::updateItem)
            .addTo(compositeDisposable)

        if (FeatureFlags.CRUD_UPDATE_AND_DELETE) {
            view.showKebabMenu()
        } else {
            view.hideKebabMenu()
        }

        handleClicks(view.usernameCopyClicks) {
            if (!it.username.isNullOrBlank()) {
                dispatcher.dispatch(ClipboardAction.CopyUsername(it.username.toString()))
                dispatcher.dispatch(DataStoreAction.Touch(it.id))
                view.showToastNotification(R.string.toast_username_copied)
            }
        }

        handleClicks(view.passwordCopyClicks) {
            if (it.password.isNotEmpty()) {
                dispatcher.dispatch(ClipboardAction.CopyPassword(it.password))
                dispatcher.dispatch(DataStoreAction.Touch(it.id))
                view.showToastNotification(R.string.toast_password_copied)
            }
        }

        handleClicks(view.hostnameClicks) {
            if (it.hostname.isNotEmpty()) {
                dispatcher.dispatch(RouteAction.OpenWebsite(it.hostname))
            }
        }

        view.togglePasswordClicks
            .subscribe { dispatcher.dispatch(ItemDetailAction.TogglePassword) }
            .addTo(compositeDisposable)

        view.isPasswordVisible = false

        networkStore.isConnected
            .subscribe(view::handleNetworkError)
            .addTo(compositeDisposable)

        itemDetailStore.isPasswordVisible
            .subscribe { view.isPasswordVisible = it }
            .addTo(compositeDisposable)

        view.editClicks
            .subscribe {
                dispatcher.dispatch(RouteAction.EditItemDetail(credentials?.id.toString()))
            }
            .addTo(compositeDisposable)

        view.deleteClicks
            .subscribe {
                if (credentials != null) {
                    dispatcher.dispatch(DialogAction.DeleteConfirmationDialog(credentials!!))
                } else {
                    pushError(
                        NullPointerException("Credentials are null."),
                        "Error accessing credential with id ${credentials?.id}"
                    )
                }
            }
            .addTo(compositeDisposable)

        view.kebabMenuClicks
            .subscribe {
                view.showPopup()
            }
            .addTo(compositeDisposable)
    }

    private fun handleClicks(clicks: Observable<Unit>, withServerPassword: (ServerPassword) -> Unit) {
        clicks.subscribe {
            this.credentials?.let { password -> withServerPassword(password) }
        }
            .addTo(compositeDisposable)
    }
}