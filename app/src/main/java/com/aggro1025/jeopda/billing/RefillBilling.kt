package com.aggro1025.jeopda.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.aggro1025.jeopda.data.AppPrefs
import com.aggro1025.jeopda.session.SmartSession
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * 구글 플레이 결제 (인앱 상품 두 종류를 한 곳에서 처리한다).
 * 1) 500원 결제 = 오늘 무료 해제를 다시 채우기 (소모성 인앱 상품).
 * - 결제가 끝나면 무료 횟수를 채우고 상품을 "소비"해서 다시 살 수 있게 한다
 * - 결제는 됐는데 앱이 중간에 꺼진 경우, 다음에 앱을 열 때 남은 결제를 찾아 마저 처리한다 (요구사항 7장)
 * - 같은 결제를 두 번 채워 주지 않도록 처리한 결제 토큰을 기기에 기록한다
 * 2) 유료 테마 = 한 번 사면 계속 쓰는 상품. 소비하지 않고 "확인(acknowledge)"만 한다.
 *    앱을 열 때마다 플레이의 구매 기록으로 가진 테마 목록을 다시 맞춘다 (재설치·환불 반영).
 * 카드 정보는 구글 플레이가 처리하고, 접다는 결제 토큰만 기기 안에서 쓴다.
 */
object RefillBilling {
    const val PRODUCT_ID = "refill_free_unlocks"

    enum class Result { REFILLED, THEME_OWNED, CANCELLED, PENDING, UNAVAILABLE, ERROR }

    /** 계속 쓰는 상품(테마) ID 목록 */
    val THEME_PRODUCTS: Set<String> = com.aggro1025.jeopda.ui.theme.FolderTheme.entries.mapNotNull { it.productId }.toSet()

    private enum class Granted { NONE, REFILL, THEME }

    /** 결제 창(플레이스토어 화면)이 떠 있는 동안: 다른 앱 막기가 결제 창을 닫지 않게 (부팅 후 경과 시간 ms) */
    @Volatile
    var purchaseUntil = 0L
        private set

    fun isPurchasing(): Boolean = android.os.SystemClock.elapsedRealtime() < purchaseUntil

    private var client: BillingClient? = null
    private var appContext: Context? = null
    private var onResult: ((Result) -> Unit)? = null
    private val main = Handler(Looper.getMainLooper())

    private fun client(context: Context): BillingClient {
        appContext = context.applicationContext
        client?.let { return it }
        return BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
            .also { client = it }
    }

    private fun withConnection(context: Context, block: (BillingClient) -> Unit, onFail: () -> Unit) {
        val c = client(context)
        if (c.isReady) {
            block(c)
            return
        }
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                main.post { if (result.responseCode == BillingClient.BillingResponseCode.OK) block(c) else onFail() }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun details(c: BillingClient, productId: String, cb: (ProductDetails?) -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        c.queryProductDetailsAsync(params) { result, list ->
            main.post { cb(if (result.responseCode == BillingClient.BillingResponseCode.OK) list.productDetailsList.firstOrNull() else null) }
        }
    }

    /** 스토어에 등록된 가격 문구 (예: ₩4,900). 못 가져오면 null */
    fun price(context: Context, productId: String, cb: (String?) -> Unit) {
        withConnection(context, { c -> details(c, productId) { cb(it?.oneTimePurchaseOfferDetails?.formattedPrice) } }, onFail = { cb(null) })
    }

    /** 결제 창 띄우기. 결과는 [done]으로 한 번 온다 (메인 스레드) */
    fun buy(activity: Activity, productId: String = PRODUCT_ID, done: (Result) -> Unit) {
        onResult = done
        purchaseUntil = android.os.SystemClock.elapsedRealtime() + 5 * 60_000L
        withConnection(activity, { c ->
            details(c, productId) { product ->
                if (product == null) {
                    finish(Result.UNAVAILABLE)
                    return@details
                }
                val flow = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
                    )
                    .build()
                val launch = c.launchBillingFlow(activity, flow)
                when (launch.responseCode) {
                    BillingClient.BillingResponseCode.OK -> Unit
                    // 이미 산 테마: 구매 기록을 다시 맞추고 가진 것으로 처리
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                        restorePending(activity, onOwnedChanged = {}, done = { finish(if (productId in AppPrefs(activity).ownedProducts) Result.THEME_OWNED else Result.ERROR) })
                    else -> finish(Result.UNAVAILABLE)
                }
            }
        }, onFail = { finish(Result.UNAVAILABLE) })
    }

    /**
     * 앱을 열 때: 결제는 끝났는데 아직 채워 주지 못한 것이 있으면 처리하고,
     * 가진 테마 목록을 플레이 구매 기록과 맞춘다 (다시 설치했거나 환불된 경우).
     */
    fun restorePending(context: Context, onRefilled: () -> Unit = {}, onOwnedChanged: () -> Unit = {}, done: () -> Unit = {}) {
        appContext = context.applicationContext
        withConnection(context, { c ->
            c.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            ) { result, purchases ->
                main.post {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        val granted = purchases.map { handle(it) }
                        if (Granted.REFILL in granted) onRefilled()
                        val prefs = AppPrefs(context)
                        val owned = purchases
                            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                            .flatMap { it.products }
                            .filter { it in THEME_PRODUCTS }
                            .toSet()
                        if (owned != prefs.ownedProducts) {
                            prefs.ownedProducts = owned
                            onOwnedChanged()
                        }
                    }
                    done()
                }
            }
        }, onFail = done)
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        main.post {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val list = purchases.orEmpty()
                    val granted = list.map { handle(it) }
                    when {
                        Granted.THEME in granted -> finish(Result.THEME_OWNED)
                        Granted.REFILL in granted -> finish(Result.REFILLED)
                        list.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> finish(Result.PENDING)
                        else -> finish(Result.ERROR)
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> finish(Result.CANCELLED)
                else -> finish(Result.ERROR)
            }
        }
    }

    /** 결제 하나 처리: 테마면 가진 목록에 넣고 확인, 무료 채우기면 처음 보는 결제일 때 채우고 소비 */
    private fun handle(purchase: Purchase): Granted {
        val ctx = appContext ?: return Granted.NONE
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return Granted.NONE
        val prefs = AppPrefs(ctx)
        val theme = purchase.products.firstOrNull { it in THEME_PRODUCTS }
        if (theme != null) {
            val isNew = theme !in prefs.ownedProducts
            if (isNew) prefs.ownedProducts = prefs.ownedProducts + theme
            // 3일 안에 확인하지 않으면 구글 플레이가 자동 환불한다
            if (!purchase.isAcknowledged) {
                client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { _ -> }
            }
            return if (isNew) Granted.THEME else Granted.NONE
        }
        if (PRODUCT_ID !in purchase.products) return Granted.NONE
        val token = purchase.purchaseToken
        val isNew = token !in prefs.grantedPurchaseTokens
        if (isNew) {
            SmartSession.refillFreeToday(ctx)
            prefs.grantedPurchaseTokens = prefs.grantedPurchaseTokens + token
        }
        // 소비해야 다시 살 수 있다 (실패하면 다음에 앱을 열 때 다시 시도, 이미 채운 토큰은 다시 채우지 않음)
        client?.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(token).build()) { _, _ -> }
        return if (isNew) Granted.REFILL else Granted.NONE
    }

    private fun finish(r: Result) {
        purchaseUntil = 0L
        val cb = onResult ?: return
        onResult = null
        cb(r)
    }
}
