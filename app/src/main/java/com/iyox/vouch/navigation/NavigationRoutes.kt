package com.iyox.vouch.navigation

import kotlinx.serialization.Serializable

@Serializable
object OnboardingRoute

@Serializable
object VouchesRoute

@Serializable
object PersonsRoute

@Serializable
object SettingsRoute

@Serializable
data class SettledSpaceRoute(
    val personName: String? = null
)

@Serializable
data class SingleVouchRoute(
    val vouchId: Long? = null,
    val personName: String? = null
)

@Serializable
data class GroupVouchRoute(
    val vouchId: Long? = null
)

@Serializable
data class ViewPersonRoute(
    val personName: String
)

@Serializable
data class VouchViewRoute(
    val vouchId: Long
)

@Serializable
data object CurrencySearchRoute

@Serializable
data object OpenSourceLicensesRoute
