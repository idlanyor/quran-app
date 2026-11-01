package com.irengcloud.quran

import com.irengcloud.quran.data.normalizeDegrees
import com.irengcloud.quran.data.qiblaBearing
import com.irengcloud.quran.data.shortestTurn
import org.junit.Assert.*
import org.junit.Test

class QiblaTest {
    @Test fun matchesPublishedMumbaiReference() {
        assertEquals(280.077462, qiblaBearing(19.071017570421, 72.838622286762)!!, 0.1)
    }
    @Test fun sameLongitudeSouthOfKaabaFacesNorth() {
        assertEquals(0.0, qiblaBearing(0.0, 39.8262)!!, 0.0001)
    }
    @Test fun sameLongitudeNorthOfKaabaFacesSouth() {
        assertEquals(180.0, qiblaBearing(40.0, 39.8262)!!, 0.0001)
    }
    @Test fun undefinedBearingAtKaabaDoesNotShowFakeDirection() {
        assertNull(qiblaBearing(21.4225, 39.8262))
    }
    @Test fun arrowTakesShortestTurnAcrossNorth() {
        assertEquals(2.0, shortestTurn(1.0, 359.0), 0.0001)
        assertEquals(-2.0, shortestTurn(359.0, 1.0), 0.0001)
        assertEquals(0.0, shortestTurn(295.0, 295.0), 0.0001)
    }
    @Test fun coordinatesAreValidatedAndOutputIsNormalized() {
        assertTrue(runCatching { qiblaBearing(91.0, 0.0) }.isFailure)
        assertTrue(runCatching { qiblaBearing(Double.NaN, 0.0) }.isFailure)
        assertEquals(359.0, normalizeDegrees(-1.0), 0.0001)
        for ((lat, lon) in listOf(-6.2 to 106.8, 51.5 to -0.1, -33.8 to 151.2, 40.7 to -74.0)) {
            val bearing = qiblaBearing(lat, lon)!!
            assertTrue(bearing >= 0 && bearing < 360)
        }
    }
}
