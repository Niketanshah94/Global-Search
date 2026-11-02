package com.quickfind.core.sources.di

import com.quickfind.core.sources.NicknameSource
import com.quickfind.core.sources.SearchSource
import com.quickfind.core.sources.calendar.CalendarSource
import com.quickfind.core.sources.notifications.NotificationSource
import com.quickfind.core.sources.semantic.SemanticSource
import com.quickfind.core.sources.instant.CurrencySource
import com.quickfind.core.sources.instant.SmartPasteSource
import com.quickfind.core.sources.instant.ToolSource
import com.quickfind.core.sources.apps.AppSource
import com.quickfind.core.sources.contacts.ContactSource
import com.quickfind.core.sources.files.FileSource
import com.quickfind.core.sources.instant.CalculatorSource
import com.quickfind.core.sources.instant.PhoneNumberSource
import com.quickfind.core.sources.instant.UrlSource
import com.quickfind.core.sources.instant.WebSearchSource
import com.quickfind.core.sources.settings.SettingsSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** One binding per data source; the engine receives them as a Set (spec 4.2). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SourcesModule {
    @Binds @IntoSet abstract fun apps(s: AppSource): SearchSource
    @Binds @IntoSet abstract fun contacts(s: ContactSource): SearchSource
    @Binds @IntoSet abstract fun files(s: FileSource): SearchSource
    @Binds @IntoSet abstract fun settings(s: SettingsSource): SearchSource
    @Binds @IntoSet abstract fun calculator(s: CalculatorSource): SearchSource
    @Binds @IntoSet abstract fun phone(s: PhoneNumberSource): SearchSource
    @Binds @IntoSet abstract fun url(s: UrlSource): SearchSource
    @Binds @IntoSet abstract fun web(s: WebSearchSource): SearchSource
    @Binds @IntoSet abstract fun tools(s: ToolSource): SearchSource
    @Binds @IntoSet abstract fun currency(s: CurrencySource): SearchSource
    @Binds @IntoSet abstract fun smartPaste(s: SmartPasteSource): SearchSource
    @Binds @IntoSet abstract fun nicknames(s: NicknameSource): SearchSource
    @Binds @IntoSet abstract fun calendar(s: CalendarSource): SearchSource
    @Binds @IntoSet abstract fun notifications(s: NotificationSource): SearchSource
    @Binds @IntoSet abstract fun semantic(s: SemanticSource): SearchSource
}
