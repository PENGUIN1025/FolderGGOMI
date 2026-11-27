package com.aggro1025.jeopda.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.aggro1025.jeopda.data.AppPrefs
import com.aggro1025.jeopda.session.SmartSession
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
 * 500원 결제 = 오늘 무료 해제를 다시 채우기 (소모성 인앱 상품).
 * - 결제가 끝나면 무료 횟수를 채우고 상품을 "소비"해서 다시 살 수 있게 한다
 * - 결제는 됐는데 앱이 중간에 꺼진 경우, 다음에 앱을 열 때 남은 결제를 찾아 마저 처리한다 (요구사항 7장)
 * - 같은 결제를 두 번 채워 주지 않도록 처리한 결제 토큰을 기기에 기록한다
 * 카드 정보는 구글 플레이가 처리하고, 접다는 결제 토큰만 기기 안에서 쓴다.
 */
object RefillBilling {
    const val PRODUCT_ID = "refill_free_unlocks"

    enum class Result { REFILLED, CANCELLED, PENDING, UNAVAILABLE, ERROR }

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

    /** 결제 창 띄우기. 결과는 [done]으로 한 번 온다 (메인 스레드) */
    fun buy(activity: Activity, done: (Result) -> Unit) {
        onResult = done
        purchaseUntil = android.os.SystemClock.elapsedRealtime() + 5 * 60_000L
        withConnection(activity, { c ->
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    ),
                )
                .build()
            c.queryProductDetailsAsync(params) { result, details ->
                main.post {
                    val product: ProductDetails? = details.productDetailsList.firstOrNull()
                    if (result.responseCode != BillingClient.BillingResponseCode.OK || product == null) {
                        finish(Result.UNAVAILABLE)
                        return@post
                    }
                    val flow = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
                        )
                        .build()
                    val launch = c.launchBillingFlow(activity, flow)
                    if (launch.responseCode != BillingClient.BillingResponseCode.OK) finish(Result.UNAVAILABLE)
                }
            }
        }, onFail = { finish(Result.UNAVAILABLE) })
    }

    /** 앱을 열 때: 결제는 끝났는데 아직 채워 주지 못한 것이 있으면 처리 */
    fun restorePending(context: Context, onRefilled: () -> Unit = {}) {
        withConnection(context, { c ->
            c.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            ) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    main.post { if (purchases.any { handle(it) }) onRefilled() }
                }
            }
        }, onFail = {})
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        main.post {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val list = purchases.orEmpty()
                    when {
                        list.any { handle(it) } -> finish(Result.REFILLED)
                        list.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> finish(Result.PENDING)
                        else -> finish(Result.ERROR)
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> finish(Result.CANCELLED)
                else -> finish(Result.ERROR)
            }
        }
    }

    /** 결제 하나 처리: 처음 보는 완료 결제면 무료 횟수를 채우고 소비. 채웠으면 true */
    private fun handle(purchase: Purchase): Boolean {
        val ctx = appContext ?: return false
        if (PRODUCT_ID !in purchase.products) return false
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return false
        val prefs = AppPrefs(ctx)
        val token = purchase.purchaseToken
        val isNew = token !in prefs.grantedPurchaseTokens
        if (isNew) {
            SmartSession.refillFreeToday(ctx)
            prefs.grantedPurchaseTokens = prefs.grantedPurchaseTokens + token
        }
        // 소비해야 다시 살 수 있다 (실패하면 다음에 앱을 열 때 다시 시도, 이미 채운 토큰은 다시 채우지 않음)
        client?.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(token).build()) { _, _ -> }
        return isNew
    }

    private fun finish(r: Result) {
        purchaseUntil = 0L
        val cb = onResult ?: return
        onResult = null
        cb(r)
    }
}
