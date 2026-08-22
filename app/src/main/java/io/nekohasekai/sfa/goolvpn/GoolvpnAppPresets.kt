package io.nekohasekai.sfa.goolvpn

import androidx.annotation.StringRes
import io.nekohasekai.sfa.R

data class GoolvpnAppPreset(
    @StringRes val titleRes: Int,
    @StringRes val examplesRes: Int,
    val packageNames: Set<String>,
)

/**
 * A deliberately small, reviewed catalog for the optional Android per-app bypass.
 *
 * Do not replace this with a country-prefix heuristic. Package names are part of
 * the product decision: the user can inspect and remove every selected app.
 */
object GoolvpnAppPresets {
    val all: List<GoolvpnAppPreset> = listOf(
        GoolvpnAppPreset(
            titleRes = R.string.goolvpn_app_preset_food,
            examplesRes = R.string.goolvpn_app_preset_food_examples,
            packageNames = setOf(
                "ru.pyaterochka.app",
                "ru.perekrestok.app",
                "ru.rostics.app",
                "com.teremok.teremok",
                "ru.mcdonalds",
                "ru.burgerking",
                "ru.dodopizza",
                "ru.chizhik",
            ),
        ),
        GoolvpnAppPreset(
            titleRes = R.string.goolvpn_app_preset_banking,
            examplesRes = R.string.goolvpn_app_preset_banking_examples,
            packageNames = setOf(
                "ru.sberbankmobile",
                "com.idamob.tinkoff.android",
                "ru.alfabank.mobile.android",
                "ru.vtb24.mobilebanking.android",
                "ru.gazprombank.android.mobilebank.app",
            ),
        ),
        GoolvpnAppPreset(
            titleRes = R.string.goolvpn_app_preset_yandex,
            examplesRes = R.string.goolvpn_app_preset_yandex_examples,
            packageNames = setOf(
                "ru.yandex.yandexmaps",
                "ru.yandex.metro",
                "ru.yandex.rasp",
                "ru.kinopoisk",
                "ru.yandex.music",
                "ru.yandex.quasar",
                "ru.yandex.yandexgo",
                "ru.yandex.shift",
                "ru.yandex.telemost",
                "ru.yandex.weather",
            ),
        ),
        GoolvpnAppPreset(
            titleRes = R.string.goolvpn_app_preset_services,
            examplesRes = R.string.goolvpn_app_preset_services_examples,
            packageNames = setOf(
                "ru.gosuslugi.pgu",
                "ru.vk.store",
                "ru.megafon.mlk",
                "ru.mts.mymts",
                "ru.beeline.services",
                "ru.mosreg.ig",
                "ru.max.messenger",
            ),
        ),
        GoolvpnAppPreset(
            titleRes = R.string.goolvpn_app_preset_shopping,
            examplesRes = R.string.goolvpn_app_preset_shopping_examples,
            packageNames = setOf(
                "ru.ozon.app",
                "ru.ozon.job",
                "com.wildberries.ru",
                "com.avito.android",
                "com.alibaba.aliexpresshd",
            ),
        ),
    )

    fun installedPackageNames(preset: GoolvpnAppPreset, installedPackageNames: Set<String>): Set<String> =
        preset.packageNames.intersect(installedPackageNames)
}
