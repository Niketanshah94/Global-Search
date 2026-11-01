package com.quickfind.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceType
import com.quickfind.core.ui.QuickFindTheme
import com.quickfind.feature.search.ui.SearchActions
import com.quickfind.feature.search.ui.SearchScreen
import com.quickfind.feature.search.ui.SearchTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun app(title: String) = SearchResult(
        id = "app:$title", type = SourceType.APP, title = title, subtitle = null, matchScore = 100,
        matchedRanges = listOf(0..1), primaryAction = ResultAction.CopyText(title),
    )

    private class Recorder : SearchActions {
        val events = mutableListOf<String>()
        override fun onQueryChange(text: String) { events += "query:$text" }
        override fun onSubmit() { events += "submit" }
        override fun onResultClick(result: SearchResult) { events += "click:${result.title}" }
        override fun onResultLongClick(result: SearchResult) { events += "long:${result.title}" }
        override fun onAction(result: SearchResult, action: ResultAction) { events += "action" }
        override fun onToggleSection(section: ResultSection) { events += "toggle:$section" }
        override fun onClearScope() { events += "clearScope" }
        override fun onPasteClipboard() { events += "paste" }
        override fun onDismissClipboard() { events += "dismissClip" }
        override fun onOpenSettings() { events += "settings" }
        override fun onDismiss() { events += "dismiss" }
    }

    private fun setContent(state: SearchUiState, recorder: Recorder) {
        rule.setContent {
            QuickFindTheme(dynamicColor = false) {
                SearchScreen(
                    state = state,
                    barAtBottom = false,
                    actions = recorder,
                    focusRequester = remember { FocusRequester() },
                    icon = { _, m -> Box(m) },
                )
            }
        }
    }

    @Test
    fun showsTopHitAndSectionsAndShowMore() {
        val state = SearchUiState(
            query = "wh",
            topHit = app("WhatsApp"),
            sections = listOf(SectionUi(ResultSection.APPS, listOf(app("Weather")), moreCount = 3, expanded = false)),
        )
        val r = Recorder()
        setContent(state, r)
        rule.onNodeWithTag(SearchTestTags.TOP_HIT).assertIsDisplayed()
        rule.onNodeWithText("WhatsApp").assertIsDisplayed()
        rule.onNodeWithText("Weather").performClick()
        rule.onNodeWithText("Show 3 more").performClick()
        assertEquals(listOf("click:Weather", "toggle:APPS"), r.events)
    }

    @Test
    fun enterSubmitsAndTypingReportsQuery() {
        val r = Recorder()
        setContent(SearchUiState(), r)
        rule.onNodeWithTag(SearchTestTags.FIELD).performTextInput("cam")
        rule.onNodeWithTag(SearchTestTags.FIELD).performImeAction()
        assertEquals(listOf("query:cam", "submit"), r.events)
    }

    @Test
    fun noResultsMessage() {
        setContent(SearchUiState(query = "zzzz"), Recorder())
        rule.onNodeWithText("Nothing found for “zzzz”").assertIsDisplayed()
    }
}
