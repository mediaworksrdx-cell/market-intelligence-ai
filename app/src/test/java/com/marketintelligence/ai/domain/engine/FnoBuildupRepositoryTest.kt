package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.domain.model.BuildupType
import com.marketintelligence.ai.domain.repository.FnoBuildupRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FnoBuildupRepositoryTest {

    private lateinit var repository: FnoBuildupRepository

    @Before
    fun setUp() {
        repository = FnoBuildupRepository()
    }

    @Test
    fun testClassifyBuildup_LongBuildup() {
        // Price Up, OI Up -> Long Buildup
        val result = repository.classifyBuildup(priceChange = 25.0, oiChange = 500000L)
        assertEquals(BuildupType.LONG_BUILDUP, result)
    }

    @Test
    fun testClassifyBuildup_ShortBuildup() {
        // Price Down, OI Up -> Short Buildup
        val result = repository.classifyBuildup(priceChange = -30.0, oiChange = 750000L)
        assertEquals(BuildupType.SHORT_BUILDUP, result)
    }

    @Test
    fun testClassifyBuildup_ShortCovering() {
        // Price Up, OI Down -> Short Covering
        val result = repository.classifyBuildup(priceChange = 18.0, oiChange = -250000L)
        assertEquals(BuildupType.SHORT_COVERING, result)
    }

    @Test
    fun testClassifyBuildup_LongUnwinding() {
        // Price Down, OI Down -> Long Unwinding
        val result = repository.classifyBuildup(priceChange = -15.0, oiChange = -400000L)
        assertEquals(BuildupType.LONG_UNWINDING, result)
    }

    @Test
    fun testClassifyBuildup_Neutral() {
        val result = repository.classifyBuildup(priceChange = 0.0, oiChange = 0L)
        assertEquals(BuildupType.NEUTRAL, result)
    }

    @Test
    fun testGetInitialBuildupList_ContainsAllRegimesAndAssetTypes() {
        val list = repository.getInitialBuildupList()
        assertTrue("Catalog should contain at least 20 constituents", list.size >= 20)

        // Check for indices and equities
        val indices = list.filter { it.isIndex }
        val equities = list.filter { !it.isIndex }
        assertTrue("Must have indices", indices.isNotEmpty())
        assertTrue("Must have equities", equities.isNotEmpty())

        // Check that all 4 regimes are represented
        val longBuildup = list.filter { it.buildupType == BuildupType.LONG_BUILDUP }
        val shortBuildup = list.filter { it.buildupType == BuildupType.SHORT_BUILDUP }
        val shortCovering = list.filter { it.buildupType == BuildupType.SHORT_COVERING }
        val longUnwinding = list.filter { it.buildupType == BuildupType.LONG_UNWINDING }

        assertTrue("Must have Long Buildup stocks", longBuildup.isNotEmpty())
        assertTrue("Must have Short Buildup stocks", shortBuildup.isNotEmpty())
        assertTrue("Must have Short Covering stocks", shortCovering.isNotEmpty())
        assertTrue("Must have Long Unwinding stocks", longUnwinding.isNotEmpty())

        // Check specific key assets
        val nifty = list.find { it.symbol == "NIFTY" }
        assertNotNull("NIFTY must be present", nifty)
        assertEquals("Indices", nifty?.sector)

        val reliance = list.find { it.symbol == "RELIANCE" }
        assertNotNull("RELIANCE must be present", reliance)
        assertEquals("Energy", reliance?.sector)
    }
}
