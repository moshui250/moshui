package com.codeagent.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankTierTest {

    @Test
    void shouldMapAcCountsToExpectedTiers() {
        assertEquals(RankTier.UNINITIALIZED, RankTier.of(0));
        assertEquals(RankTier.BIT_APPRENTICE, RankTier.of(1));
        assertEquals(RankTier.BIT_APPRENTICE, RankTier.of(9));
        assertEquals(RankTier.BYTE_WALKER, RankTier.of(10));
        assertEquals(RankTier.CACHE_WALKER, RankTier.of(72));
        assertEquals(RankTier.KERNEL_DEACON, RankTier.CACHE_WALKER.next());
        assertEquals(130, RankTier.CACHE_WALKER.next().getMinAc());
        assertEquals(RankTier.ORIGIN_CORE, RankTier.of(1400));
        assertEquals("130 - 249 题", RankTier.KERNEL_DEACON.rangeText());
    }
}
