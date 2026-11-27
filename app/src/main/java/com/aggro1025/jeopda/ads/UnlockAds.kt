package com.aggro1025.jeopda.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.aggro1025.jeopda.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * 해제용 보상형 광고 (요구사항 4장).
 * - 2개를 연달아 끝까지 봐야 해제
 * - 중간에 닫으면 취소 (폴더폰 모드에 남음)
 * - 불러오지 못하면(인터넷 없음 등) 실패 → 화면에서 1분 기다린 뒤 해제
 */
object UnlockAds {
    const val COUNT = 2
    private const val LOAD_TIMEOUT_MS = 15_000L

    enum class Result { DONE, CANCELLED, FAILED }

    @Volatile
    private var initialized = false

    /** 앱 시작 때 한 번. 광고 내용은 전체이용가(G) 수준만 */
    fun init(context: Context) {
        if (initialized || Build.FINGERPRINT == "robolectric") return
        initialized = true
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        Thread { runCatching { MobileAds.initialize(context.applicationContext) } }.start()
    }

    /**
     * 광고 [COUNT]개를 차례로 보여 준다. 진행 중에는 [onProgress](몇 번째 광고)를,
     * 끝나면 [onResult]를 한 번 부른다. 모두 메인 스레드에서 호출된다.
     */
    fun play(activity: Activity, onProgress: (Int) -> Unit, onResult: (Result) -> Unit) {
        init(activity)
        var finished = false
        val finish: (Result) -> Unit = { r ->
            if (!finished) {
                finished = true
                onResult(r)
            }
        }
        gatherConsent(activity) { canRequest ->
            if (!canRequest) finish(Result.FAILED) else showNext(activity, 1, onProgress, finish)
        }
    }

    /** 유럽·영국 등 동의가 필요한 지역이면 동의 창을 띄운다. 대한민국에서는 보통 바로 통과 */
    private fun gatherConsent(activity: Activity, done: (Boolean) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    done(info.canRequestAds())
                }
            },
            { done(info.canRequestAds()) },
        )
    }

    private fun showNext(activity: Activity, index: Int, onProgress: (Int) -> Unit, finish: (Result) -> Unit) {
        if (index > COUNT) {
            finish(Result.DONE)
            return
        }
        onProgress(index)
        val handler = Handler(Looper.getMainLooper())
        var timedOut = false
        val timeout = Runnable {
            timedOut = true
            finish(Result.FAILED)
        }
        handler.postDelayed(timeout, LOAD_TIMEOUT_MS)

        RewardedAd.load(
            activity,
            BuildConfig.REWARDED_AD_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    handler.removeCallbacks(timeout)
                    if (timedOut || activity.isFinishing) return
                    var earned = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            if (earned) showNext(activity, index + 1, onProgress, finish) else finish(Result.CANCELLED)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            finish(Result.FAILED)
                        }
                    }
                    ad.show(activity) { earned = true }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    handler.removeCallbacks(timeout)
                    if (!timedOut) finish(Result.FAILED)
                }
            },
        )
    }
}
