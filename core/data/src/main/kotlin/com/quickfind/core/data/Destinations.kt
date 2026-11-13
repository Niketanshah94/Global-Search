package com.quickfind.core.data

/**
 * Intent actions used to move between feature modules without compile-time dependencies.
 * Each is handled by an activity filter (with setPackage to stay inside the app).
 */
object Destinations {
    const val ACTION_SEARCH = "com.quickfind.action.SEARCH"
    const val ACTION_SETTINGS = "com.quickfind.action.SETTINGS"
    const val ACTION_ONBOARDING = "com.quickfind.action.ONBOARDING"
    const val ACTION_FOLDER = "com.quickfind.action.FOLDER"
    const val ACTION_PAYWALL = "com.quickfind.action.PAYWALL"

    /** Restricts search to one source ("files", "contacts"); used by static shortcuts. */
    const val EXTRA_SCOPE = "com.quickfind.extra.SCOPE"
    const val EXTRA_FOLDER_PATH = "com.quickfind.extra.FOLDER_PATH"
    const val EXTRA_FOLDER_TITLE = "com.quickfind.extra.FOLDER_TITLE"

    /** A [com.quickfind.core.model.ProFeature] name: why the paywall opened. */
    const val EXTRA_PRO_FEATURE = "com.quickfind.extra.PRO_FEATURE"

    /** Paywall shown at the end of onboarding: its dismiss button reads "Continue with Free". */
    const val EXTRA_FROM_ONBOARDING = "com.quickfind.extra.FROM_ONBOARDING"

    const val SCOPE_FILES = "files"
    const val SCOPE_CONTACTS = "contacts"
    const val SCOPE_APPS = "apps"
}
