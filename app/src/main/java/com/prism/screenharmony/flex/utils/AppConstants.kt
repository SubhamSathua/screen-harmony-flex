package com.prism.screenharmony.flex.utils

/**
 * App-wide constants and external link configurations.
 * Similar to environment variables / central configuration.
 */
object AppConstants {

    /**
     * The fallback URL used to redirect browsers away from blocked websites.
     * When a user visits a blocked domain, the browser tab is commanded to navigate
     * here to prevent stuck reload loops.
     */
    const val BROWSER_REDIRECT_URL = "https://www.google.com"

    // External Documentation & Policies
    const val PRIVACY_POLICY_URL = "https://github.com/SubhamSathua/screen-harmony-flex/blob/main/PRIVACY.md"
    const val TERMS_OF_SERVICE_URL = "https://github.com/SubhamSathua/screen-harmony-flex/blob/main/TERMS.md"
    const val SOURCE_CODE_URL = "https://github.com/SubhamSathua/screen-harmony-flex"
    const val SUPPORT_EMAIL = "support@screenharmony.prism"

    // Default Configuration Constants
    const val DEFAULT_BLOCK_SCREEN_DELAY_SECONDS = 5
    const val DEFAULT_PAUSE_MINUTES = 5
    const val MIN_PAUSE_MINUTES = 1
    const val MAX_PAUSE_MINUTES = 60
}
