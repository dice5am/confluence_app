package com.cavin.confluence.navigation

/**
 * Deep-linkable route stubs (MOB-1.3).
 *
 * - home
 * - chart?tf=&alertId=
 * - alerts
 * - settings
 */
object Routes {
    const val HOME = "home"
    const val CHART = "chart"
    const val ALERTS = "alerts"
    const val SETTINGS = "settings"

    const val CHART_TF_ARG = "tf"
    const val CHART_ALERT_ID_ARG = "alertId"

    /**
     * Nav graph pattern with optional query args.
     * Always navigate via [chart] so both keys are present for matching.
     */
    const val CHART_ROUTE_PATTERN = "chart?tf={tf}&alertId={alertId}"

    fun chart(tf: String? = null, alertId: String? = null): String {
        val tfVal = tf.orEmpty()
        val alertVal = alertId.orEmpty()
        return "chart?tf=$tfVal&alertId=$alertVal"
    }

    const val DEEP_LINK_SCHEME = "confluence"
    const val DEEP_LINK_HOST = "app"

    fun deepLinkUri(path: String): String = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$path"
}
