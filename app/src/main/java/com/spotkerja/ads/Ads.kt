package com.spotkerja.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/** AdMob — semua unit memakai TEST IDs Google (ganti ke ID produksi saat rilis). */
object Ads {
    const val BANNER_TEST = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST = "ca-app-pub-3940256099942544/1033173712"

    private var interstitial: InterstitialAd? = null
    private var lastShowMs = 0L

    fun preloadInterstitial(ctx: Context) {
        if (interstitial != null) return
        InterstitialAd.load(ctx, INTERSTITIAL_TEST, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null }
            })
    }

    /** Tampilkan interstitial maksimal 1x per 2 menit, lalu preload ulang. */
    fun maybeShowInterstitial(activity: Activity, onDone: () -> Unit = {}) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (ad == null || now - lastShowMs < 120_000) { onDone(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                lastShowMs = now
                preloadInterstitial(activity)
                onDone()
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                interstitial = null
                onDone()
            }
        }
        ad.show(activity)
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = Ads.BANNER_TEST
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
