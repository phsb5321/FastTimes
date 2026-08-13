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
package com.tmdev.fasttimes.ui.dashboard.components

import com.tmdev.fasttimes.data.profile.FastingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The goal picker offers exactly one profile first, and that choice must be a pure function of the
 * profile list — the dashboard used to derive its geometry from [kotlin.random.Random], so the
 * regression this guards is "the home screen changes between launches with identical data".
 */
class GoalPickerTest {
    private fun profile(
        id: Long,
        name: String,
        durationMillis: Long?,
        favorite: Boolean = false
    ) = FastingProfile(id, name, durationMillis, "desc for $name", favorite)

    private val open = profile(1, "Open Fast", null)
    private val sixteen = profile(2, "16-Hour Fast", 16 * 3_600_000L)
    private val twelve = profile(3, "12-Hour Fast", 12 * 3_600_000L)

    @Test
    fun recommendation_prefers_the_favorite() {
        val favorite = profile(4, "18-Hour Fast", 18 * 3_600_000L, favorite = true)
        assertEquals(favorite, recommendedProfile(listOf(open, sixteen, favorite, twelve)))
    }

    @Test
    fun recommendation_falls_back_to_the_first_timed_profile_not_the_open_one() {
        assertEquals(sixteen, recommendedProfile(listOf(open, sixteen, twelve)))
    }

    @Test
    fun recommendation_uses_whatever_exists_when_every_profile_is_open_ended() {
        assertEquals(open, recommendedProfile(listOf(open)))
    }

    @Test
    fun recommendation_is_null_only_when_there_are_no_profiles() {
        assertNull(recommendedProfile(emptyList()))
    }

    @Test
    fun recommendation_is_stable_across_repeated_calls() {
        val profiles = listOf(open, sixteen, twelve)
        val first = recommendedProfile(profiles)
        repeat(50) { assertEquals(first, recommendedProfile(profiles)) }
    }

    @Test
    fun duration_label_renders_hours_and_keeps_the_open_fast_glyph() {
        assertEquals("16h", sixteen.durationLabel())
        assertEquals("∞", open.durationLabel())
        // Preserves the pre-extraction arithmetic: sub-hour values floor to "0h", never to "∞".
        assertEquals("0h", profile(5, "Test Timer", 1_000L).durationLabel())
    }
}
