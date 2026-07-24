package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.ext.integrity.IntegritySpoofStore
import android.ext.settings.app.AswSpoofTelephonyRegion
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterSpoofTelephonyRegion : AswAdapter<AswSpoofTelephonyRegion>() {
    override fun getAppSwitch() = AswSpoofTelephonyRegion.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.app_spoof_telephony_region)
    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.app_spoof_telephony_region_on)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.app_spoof_telephony_region_off)

    override fun getDetailFragmentClass() = AppManageSpoofTelephonyRegionFragment::class

    override fun shouldIncludeInAppListPage(app: ApplicationInfo, gosPs: GosPackageState): Boolean {
        return true
    }
}

@Composable
fun AppManageSpoofTelephonyRegionPreference(app: ApplicationInfo) {
    AswPreference(LocalContext.current, app, AswAdapterSpoofTelephonyRegion)
}

class AppManageSpoofTelephonyRegionFragment : AppInfoWithHeader() {
    private lateinit var spoofSwitch: SwitchPreferenceCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.app_spoof_telephony_region)

        val prefCtx = prefContext
        val screen: PreferenceScreen = preferenceManager.createPreferenceScreen(prefCtx)

        FooterPreference(prefCtx).apply {
            setOrder(-100)
            setTitle(R.string.app_spoof_telephony_region_footer)
            screen.addPreference(this)
        }
        spoofSwitch = SwitchPreferenceCompat(prefCtx).apply {
            setTitle(R.string.app_spoof_telephony_region_switch)
            setSummary(R.string.app_spoof_telephony_region_switch_summary)
            setOnPreferenceChangeListener { _, value ->
                val ed = GosPackageState.edit(mPackageName, mUserId)
                AswSpoofTelephonyRegion.I.set(ed, value as Boolean)
                if (!ed.apply()) {
                    return@setOnPreferenceChangeListener false
                }
                IntegritySpoofStore.syncPolicy(requireContext())
                true
            }
            screen.addPreference(this)
        }
        AswAdapterSpoofTelephonyRegion.addAppListPageLink(
            screen, getText(R.string.app_spoof_telephony_region_see_all_apps)
        )
        setPreferenceScreen(screen)
    }

    override fun refreshUi(): Boolean {
        val appInfo = mPackageInfo?.applicationInfo ?: return false
        val gosPs = GosPackageState.get(mPackageName, mUserId)
        spoofSwitch.isChecked = AswSpoofTelephonyRegion.I
            .get(requireContext(), mUserId, appInfo, gosPs)
        return true
    }

    override fun createDialog(id: Int, errorCode: Int): AlertDialog? = null

    override fun getMetricsCategory(): Int = METRICS_CATEGORY_UNKNOWN
}
