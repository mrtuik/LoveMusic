package com.metrolist.music.taste

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TasteEngineTest {
    @Test
    fun skipUnderTenSecondsIsNegative() {
        assertTrue(TasteEngine.listenScore(4_000, 200_000) < 0)
    }

    @Test
    fun fullListenAndRepeatArePositive() {
        assertEquals(1.0, TasteEngine.listenScore(190_000, 200_000), 0.001)
        assertEquals(1.5, TasteEngine.listenScore(320_000, 200_000), 0.001)
    }

    @Test
    fun partialListenScalesBetweenZeroAndOne() {
        val s = TasteEngine.listenScore(120_000, 200_000)
        assertTrue(s > 0 && s < 1.0)
    }

    @Test
    fun detectsScripts() {
        assertEquals("bn", TasteEngine.detectLanguage("আমি তোমার"))
        assertEquals("hi", TasteEngine.detectLanguage("तेरे बिना"))
        assertEquals("latin", TasteEngine.detectLanguage("Blinding Lights"))
    }
}
