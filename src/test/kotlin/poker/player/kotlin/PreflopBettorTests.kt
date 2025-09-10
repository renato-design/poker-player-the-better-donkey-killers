package poker.player.kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PreflopBettorTests {

    private val bettor = PreflopBettor()

    @Test
    fun `parsePreflopRange parses single pair 22`() {
        val result = bettor.parsePreflopRange("22")
        assertEquals(1, result.size)
        assertEquals("22", result[0].rank)
        assertFalse(result[0].isSuited, "Pairs are not suited")
    }

    @Test
    fun `parsePreflopRange parses 22+ into all pocket pairs 22AA`() {
        val result = bettor.parsePreflopRange("22+")
        val expectedPairs = listOf("22","33","44","55","66","77","88","99","TT","JJ","QQ","KK","AA")
        assertEquals(expectedPairs.size, result.size)
        assertEquals(expectedPairs.toSet(), result.map { it.rank }.toSet())
        assertTrue(result.all { !it.isSuited }, "All pairs should be marked as non-suited")
    }

    @Test
    fun `parsePreflopRange parses AK (no suffix) as both suited and offsuit`() {
        val result = bettor.parsePreflopRange("AK")
        val akSuited = result.count { it.rank == "AK" && it.isSuited }
        val akOffsuit = result.count { it.rank == "AK" && !it.isSuited }
        assertEquals(2, akSuited + akOffsuit)
        assertEquals(1, akSuited, "Expected exactly one suited AK entry")
        assertEquals(1, akOffsuit, "Expected exactly one offsuit AK entry")
    }

    @Test
    fun `parsePreflopRange parses AKs as suited only`() {
        val result = bettor.parsePreflopRange("AKs")
        assertEquals(1, result.size)
        assertEquals("AK", result[0].rank)
        assertTrue(result[0].isSuited, "AKs should be suited only")
    }

    @Test
    fun `parsePreflopRange parses AKo as offsuit only`() {
        val result = bettor.parsePreflopRange("AKo")
        assertEquals(1, result.size)
        assertEquals("AK", result[0].rank)
        assertFalse(result[0].isSuited, "AKo should be offsuit only")
    }

    @Test
    fun `parsePreflopRange parses A2s+ as suited A2AK`() {
        val result = bettor.parsePreflopRange("A2s+")
        val expected = listOf("A2","A3","A4","A5","A6","A7","A8","A9","AT","AJ","AQ","AK")
        assertEquals(expected.size, result.size, "Expected suited ace wheel through AK")
        assertEquals(expected.toSet(), result.map { it.rank }.toSet())
        assertTrue(result.all { it.isSuited }, "All entries should be suited for s+")
    }

    @Test
    fun `parsePreflopRange parses A2o+ as offsuit A2AK`() {
        val result = bettor.parsePreflopRange("A2o+")
        val expected = listOf("A2","A3","A4","A5","A6","A7","A8","A9","AT","AJ","AQ","AK")
        assertEquals(expected.size, result.size, "Expected offsuit ace wheel through AK")
        assertEquals(expected.toSet(), result.map { it.rank }.toSet())
        assertTrue(result.all { !it.isSuited }, "All entries should be offsuit for o+")
    }

    @Test
    fun `parsePreflopRange parses K9+ as both suited and offsuit K9,KT,KJ,KQ`() {
        val result = bettor.parsePreflopRange("K9+")
        val expected = listOf("K9","KT","KJ","KQ")
        val byHand = result.groupBy { it.rank }
        assertEquals(expected.toSet(), byHand.keys)
        expected.forEach { hand ->
            val suited = byHand[hand]?.count { it.isSuited } ?: 0
            val offsuit = byHand[hand]?.count { !it.isSuited } ?: 0
            assertEquals(1, suited, "Expected one suited entry for $hand")
            assertEquals(1, offsuit, "Expected one offsuit entry for $hand")
        }
        assertEquals(expected.size * 2, result.size)
    }

    @Test
    fun `parsePreflopRange handles comma-separated list and whitespace`() {
        val result = bettor.parsePreflopRange(" 77+ ,  AKs,   76o ")
        val hands = result.map { it.rank }
        val expectedPairs = setOf("77","88","99","TT","JJ","QQ","KK","AA")

        // Pairs part
        assertTrue(expectedPairs.all { it in hands })
        assertTrue(result.filter { it.rank in expectedPairs }.all { !it.isSuited })

        // AKs part
        val akEntries = result.filter { it.rank == "AK" }
        assertEquals(1, akEntries.size)
        assertTrue(akEntries[0].isSuited, "AKs should be suited only")

        // 76o part
        val seventySixEntries = result.filter { it.rank == "76" }
        assertEquals(1, seventySixEntries.size)
        assertFalse(seventySixEntries[0].isSuited, "76o should be offsuit only")
    }

    @Test
    fun `parsePreflopRange ignores empty or invalid tokens`() {
        assertTrue(bettor.parsePreflopRange("").isEmpty())
        assertTrue(bettor.parsePreflopRange(" , , ").isEmpty())
        // Invalid tokens like single rank or malformed string should be ignored
        assertTrue(bettor.parsePreflopRange("A").isEmpty())
        assertTrue(bettor.parsePreflopRange("XYZ").isEmpty())
    }
}