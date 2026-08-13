/*
 * Copyright (C) 2025 tom-murphy-development
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tmdev.fasttimes.ui

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tmdev.fasttimes.data.AppTheme
import com.tmdev.fasttimes.data.fast.Fast
import com.tmdev.fasttimes.data.profile.FastingProfile
import com.tmdev.fasttimes.ui.dashboard.DashboardStats
import com.tmdev.fasttimes.ui.dashboard.DashboardUiState
import com.tmdev.fasttimes.ui.dashboard.DashboardViewModel
import com.tmdev.fasttimes.ui.theme.FastTimesTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: DashboardViewModel
    private var observedFontScale = 0f
    private var observedLocale: Locale? = null

    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)
        every { mockViewModel.stats } returns (MutableStateFlow(DashboardStats()))
        every { mockViewModel.profiles } returns (MutableStateFlow(emptyList()))
        every { mockViewModel.favoriteProfile } returns (MutableStateFlow(null))
        every { mockViewModel.showAlarmPermissionRationale } returns (MutableStateFlow(false))
        every { mockViewModel.completedFast } returns (MutableStateFlow(null))
    }

    private fun renderDashboard(
        fontScale: Float = 1f,
        locale: Locale? = null
    ) {
        composeTestRule.setContent {
            val currentConfiguration = LocalConfiguration.current
            val currentDensity = LocalDensity.current
            val configuration = Configuration(currentConfiguration).apply {
                locale?.let(::setLocale)
            }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides Density(currentDensity.density, fontScale)
            ) {
                val providedDensity = LocalDensity.current
                val providedConfiguration = LocalConfiguration.current
                SideEffect {
                    observedFontScale = providedDensity.fontScale
                    observedLocale = providedConfiguration.locales[0]
                }
                FastTimesTheme(
                    theme = AppTheme.LIGHT,
                    seedColor = Color.Blue,
                    accentColor = Color.Blue,
                    useExpressiveTheme = false,
                    useSystemColors = false
                ) {
                    DashboardScreen(
                        onHistoryClick = {},
                        onStatisticsClick = {},
                        onViewFastDetails = {},
                        onManageProfilesClick = {},
                        viewModel = mockViewModel
                    )
                }
            }
        }
    }

    @Test
    fun loading_state_renders_skeleton() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            DashboardUiState.Loading(showSkeleton = true)
        )

        renderDashboard()

        composeTestRule.onNodeWithTag("DashboardSkeleton").assertIsDisplayed()
    }

    @Test
    fun fab_is_visible_when_no_fast_in_progress() {
        every { mockViewModel.uiState } returns (MutableStateFlow(DashboardUiState.NoFast(
            thisWeekFasts = emptyList(),
            lastWeekFasts = emptyList(),
            lastFast = null,
            showFab = true
        )))

        renderDashboard()

        composeTestRule.onNodeWithContentDescription("Add Fast").assertIsDisplayed()
    }

    @Test
    fun end_fast_button_is_visible_when_fast_in_progress() {
        val fast = Fast(
            id = 1,
            startTime = System.currentTimeMillis(),
            endTime = null,
            profileName = "16:8",
            targetDuration = 1000,
            notes = null
        )
        every { mockViewModel.uiState } returns (
            MutableStateFlow(
                DashboardUiState.FastingInProgress(
                    activeFast = fast,
                    remainingTime = 0.milliseconds,
                    progress = 0f,
                    isEditing = false,
                    useWavyIndicator = false
                )
            )
        )

        renderDashboard()

        composeTestRule.onNodeWithText("End Fast").assertIsDisplayed()

    }

    @Test
    fun fasting_goal_reached_state_renders() {
        val fast = Fast(
            id = 2,
            startTime = System.currentTimeMillis() - 4 * 3_600_000L,
            endTime = null,
            targetDuration = 3_600_000L,
            profileName = "16:8",
            notes = null
        )
        every { mockViewModel.uiState } returns MutableStateFlow(
            DashboardUiState.FastingGoalReached(
                activeFast = fast,
                totalElapsedTime = 4.hours,
                showConfetti = false,
                useWavyIndicator = false
            )
        )

        renderDashboard()

        composeTestRule.onNodeWithText("Goal Reached!").assertIsDisplayed()
        composeTestRule.onNodeWithText("End Fast").assertIsDisplayed()
    }

    @Test
    fun open_fasting_state_renders() {
        val fast = Fast(
            id = 3,
            startTime = System.currentTimeMillis() - 3_600_000L,
            endTime = null,
            targetDuration = null,
            profileName = "Open Fast",
            notes = null
        )
        every { mockViewModel.uiState } returns MutableStateFlow(
            DashboardUiState.OpenFasting(
                activeFast = fast,
                elapsedTime = 1.hours,
                useWavyIndicator = false
            )
        )

        renderDashboard()

        composeTestRule.onNodeWithText("Open Fast").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elapsed Time").assertIsDisplayed()
    }

    @Test
    fun no_fast_state_shows_profiles() {
        val profiles = listOf(
            FastingProfile(1, "16:8", 16, "desc", true),
            FastingProfile(2, "18:6", 18, "desc2", false)
        )
        every { mockViewModel.profiles } returns (MutableStateFlow(profiles))
        every { mockViewModel.uiState } returns (MutableStateFlow(DashboardUiState.NoFast(
            thisWeekFasts = emptyList(),
            lastWeekFasts = emptyList(),
            lastFast = null,
            showFab = true
        )))

        renderDashboard()

        composeTestRule.onNodeWithText("16:8", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("18:6", substring = true).assertIsDisplayed()
    }

    @Test
    fun tapping_the_primary_profile_starts_that_profile() {
        val primaryProfile = FastingProfile(1, "16:8", 16, "desc", true)
        every { mockViewModel.profiles } returns MutableStateFlow(listOf(primaryProfile))
        every { mockViewModel.uiState } returns MutableStateFlow(
            DashboardUiState.NoFast(
                thisWeekFasts = emptyList(),
                lastWeekFasts = emptyList(),
                lastFast = null,
                showFab = true
            )
        )

        renderDashboard()
        composeTestRule.onNodeWithText("16:8", substring = true).performClick()

        verify(exactly = 1) { mockViewModel.startProfileFast(primaryProfile) }
    }

    @Test
    fun no_fast_state_renders_at_200_percent_font_and_pseudo_locale() {
        val pseudoLocale = Locale.forLanguageTag("en-XA")
        val profiles = listOf(
            FastingProfile(1, "16:8", 16, "A deliberately long goal description", true),
            FastingProfile(2, "18:6", 18, "Another goal description", false)
        )
        every { mockViewModel.profiles } returns MutableStateFlow(profiles)
        every { mockViewModel.uiState } returns MutableStateFlow(
            DashboardUiState.NoFast(
                thisWeekFasts = emptyList(),
                lastWeekFasts = emptyList(),
                lastFast = null,
                showFab = true
            )
        )

        renderDashboard(
            fontScale = 2f,
            locale = pseudoLocale
        )

        composeTestRule.runOnIdle {
            assertEquals(2f, observedFontScale)
            assertEquals(pseudoLocale, observedLocale)
        }
        composeTestRule.onNodeWithText("Set your goal").assertIsDisplayed()
        composeTestRule.onNodeWithText("16:8", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("18:6", substring = true).assertIsDisplayed()
    }
}
