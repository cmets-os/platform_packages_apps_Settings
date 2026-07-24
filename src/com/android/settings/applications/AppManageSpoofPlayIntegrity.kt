package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.ext.integrity.IntegritySpoofStore
import android.ext.settings.app.AswSpoofPlayIntegrity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterSpoofPlayIntegrity : AswAdapter<AswSpoofPlayIntegrity>() {
    override fun getAppSwitch() = AswSpoofPlayIntegrity.I

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.app_spoof_play_integrity)
    override fun getOnTitle(ctx: Context) = ctx.getText(R.string.app_spoof_play_integrity_on)
    override fun getOffTitle(ctx: Context) = ctx.getText(R.string.app_spoof_play_integrity_off)

    override fun getDetailFragmentClass() = AppManageSpoofPlayIntegrityFragment::class

    override fun shouldIncludeInAppListPage(app: ApplicationInfo, gosPs: GosPackageState): Boolean {
        return true
    }
}

@Composable
fun AppManageSpoofPlayIntegrityPreference(app: ApplicationInfo) {
    AswPreference(LocalContext.current, app, AswAdapterSpoofPlayIntegrity)
}

class AppManageSpoofPlayIntegrityFragment : AppInfoWithHeader() {
    private lateinit var spoofSwitch: SwitchPreferenceCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.app_spoof_play_integrity)

        val prefCtx = prefContext
        val screen: PreferenceScreen = preferenceManager.createPreferenceScreen(prefCtx)

        FooterPreference(prefCtx).apply {
            setOrder(-100)
            setTitle(R.string.app_spoof_play_integrity_footer)
            screen.addPreference(this)
        }
        spoofSwitch = SwitchPreferenceCompat(prefCtx).apply {
            setTitle(R.string.app_spoof_play_integrity_switch)
            setSummary(R.string.app_spoof_play_integrity_switch_summary)
            setOnPreferenceChangeListener { _, value ->
                val ed = GosPackageState.edit(mPackageName, mUserId)
                AswSpoofPlayIntegrity.I.set(ed, value as Boolean)
                if (!ed.apply()) {
                    return@setOnPreferenceChangeListener false
                }
                IntegritySpoofStore.syncPolicy(requireContext())
                true
            }
            screen.addPreference(this)
        }
        AswAdapterSpoofPlayIntegrity.addAppListPageLink(
            screen, getText(R.string.app_spoof_play_integrity_see_all_apps)
        )
        setPreferenceScreen(screen)
    }

    override fun refreshUi(): Boolean {
        val appInfo = mPackageInfo?.applicationInfo ?: return false
        val gosPs = GosPackageState.get(mPackageName, mUserId)
        spoofSwitch.isChecked = AswSpoofPlayIntegrity.I
            .get(requireContext(), mUserId, appInfo, gosPs)
        return true
    }

    override fun createDialog(id: Int, errorCode: Int): AlertDialog? = null

    override fun getMetricsCategory(): Int = METRICS_CATEGORY_UNKNOWN
}
