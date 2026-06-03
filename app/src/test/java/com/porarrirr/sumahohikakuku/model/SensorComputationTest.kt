package com.porarrirr.sumahohikakuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorComputationTest {

    private val wideSensorMetrics = SensorMetrics(
        diagonalMm = 10.0,
        widthMm = 8.0,
        heightMm = 6.0,
        areaSqMm = 48.0,
        sensorName = "Wide Sensor",
        binningType = "None",
        nativePixelSizeUm = 1.0,
        source = SensorSource.DATABASE
    )

    private val teleSensorMetrics = SensorMetrics(
        diagonalMm = 8.0,
        widthMm = 6.4,
        heightMm = 4.8,
        areaSqMm = 30.72,
        sensorName = "Tele Sensor",
        binningType = "None",
        nativePixelSizeUm = 1.0,
        source = SensorSource.DATABASE
    )

    private val wideLens = LensProcessed(
        nativeFocalLength35mm = 24.0,
        fNumber = 2.0,
        actualFocalLengthMm = 5.5,
        sensorMetrics = wideSensorMetrics
    )

    private val teleLens = LensProcessed(
        nativeFocalLength35mm = 70.0,
        fNumber = 2.8,
        actualFocalLengthMm = 12.9,
        sensorMetrics = teleSensorMetrics
    )

    private val lenses = listOf(wideLens, teleLens)

    private val xiaomi17UltraTeleLens = LensProcessed(
        nativeFocalLength35mm = 75.0,
        fNumber = 2.39,
        actualFocalLengthMm = 20.0,
        sensorMetrics = wideSensorMetrics,
        opticalEndFocalLength35mm = 100.0,
        endFNumber = 2.96
    )

    private val xiaomi17UltraTeleLenses = listOf(xiaomi17UltraTeleLens)

    private val variableWideLens = wideLens.copy(
        opticalEndFocalLength35mm = 70.0,
        endFNumber = 2.8
    )

    @Test
    fun calculateEffectiveMetrics_returnsUnityZoomAtNativeFocal() {
        val metrics = calculateEffectiveMetrics(focalLength35mm = 24.0, lenses = lenses)

        assertNotNull(metrics)
        assertEquals(1.0, metrics!!.zoomRatio, 1e-9)
        assertEquals(48.0, metrics.effectiveAreaSqMm, 1e-9)
        assertEquals(12.0, metrics.totalLightIntake, 1e-9)
    }

    @Test
    fun calculateEffectiveMetrics_appliesInverseSquareAreaForDigitalZoom() {
        val metrics = calculateEffectiveMetrics(focalLength35mm = 48.0, lenses = lenses)

        assertNotNull(metrics)
        assertEquals(2.0, metrics!!.zoomRatio, 1e-9)
        assertEquals(12.0, metrics.effectiveAreaSqMm, 1e-9)
        assertEquals(3.0, metrics.totalLightIntake, 1e-9)
    }

    @Test
    fun calculateEffectiveMetrics_switchesBaseLensAtBoundary() {
        val beforeBoundary = calculateEffectiveMetrics(focalLength35mm = 69.0, lenses = lenses)
        val atBoundary = calculateEffectiveMetrics(focalLength35mm = 70.0, lenses = lenses)

        assertNotNull(beforeBoundary)
        assertNotNull(atBoundary)
        assertEquals(24.0, beforeBoundary!!.baseLens.nativeFocalLength35mm, 0.0)
        assertEquals(70.0, atBoundary!!.baseLens.nativeFocalLength35mm, 0.0)
    }

    @Test
    fun computeProcessedDevice_returnsNullWhenAllLensesAreInvalid() {
        val invalidMetrics = wideSensorMetrics.copy(
            diagonalMm = 0.0,
            widthMm = 0.0,
            heightMm = 0.0,
            areaSqMm = 0.0
        )
        val rawLenses = listOf(
            LensProcessingInput(
                nativeFocalLength35mm = 24.0,
                fNumber = 2.0,
                sensorMetrics = invalidMetrics
            )
        )

        val processed = computeProcessedDevice(
            name = "Device A",
            colorHex = "#2563EB",
            rawLenses = rawLenses,
            focalLengths = listOf(24.0, 35.0)
        )

        assertNull(processed)
    }

    @Test
    fun computeProcessedDevice_generatesMetricsForReachableFocals() {
        val rawLenses = listOf(
            LensProcessingInput(
                nativeFocalLength35mm = 24.0,
                fNumber = 2.0,
                sensorMetrics = wideSensorMetrics
            ),
            LensProcessingInput(
                nativeFocalLength35mm = 70.0,
                fNumber = 2.8,
                sensorMetrics = teleSensorMetrics
            )
        )

        val processed = computeProcessedDevice(
            name = "Device A",
            colorHex = "#2563EB",
            rawLenses = rawLenses,
            focalLengths = listOf(14.0, 24.0, 35.0, 70.0)
        )

        assertNotNull(processed)
        assertEquals(
            listOf(24.0, 35.0, 70.0),
            processed!!.metricsByFocalLength.map { it.focalLength35mm }
        )
    }

    @Test
    fun xiaomi17UltraTelephoto_keepsFullAreaWithin75To100mm() {
        val metrics75 = calculateEffectiveMetrics(75.0, xiaomi17UltraTeleLenses)!!
        val metrics90 = calculateEffectiveMetrics(90.0, xiaomi17UltraTeleLenses)!!
        val metrics100 = calculateEffectiveMetrics(100.0, xiaomi17UltraTeleLenses)!!

        assertEquals(metrics75.effectiveAreaSqMm, metrics90.effectiveAreaSqMm, 0.0001)
        assertEquals(metrics75.effectiveAreaSqMm, metrics100.effectiveAreaSqMm, 0.0001)
        assertEquals(1.0, metrics90.digitalCropRatio, 0.0001)
        assertEquals(90.0, metrics90.opticalFocalLength35mm, 0.0001)
    }

    @Test
    fun xiaomi17UltraTelephoto_cropsAfter100mm() {
        val metrics100 = calculateEffectiveMetrics(100.0, xiaomi17UltraTeleLenses)!!
        val metrics200 = calculateEffectiveMetrics(200.0, xiaomi17UltraTeleLenses)!!
        val metrics400 = calculateEffectiveMetrics(400.0, xiaomi17UltraTeleLenses)!!

        assertEquals(metrics100.effectiveAreaSqMm / 4.0, metrics200.effectiveAreaSqMm, 0.0001)
        assertEquals(metrics100.effectiveAreaSqMm / 16.0, metrics400.effectiveAreaSqMm, 0.0001)
        assertEquals(2.0, metrics200.digitalCropRatio, 0.0001)
        assertEquals(4.0, metrics400.digitalCropRatio, 0.0001)
        assertEquals(100.0, metrics200.opticalFocalLength35mm, 0.0001)
    }

    @Test
    fun xiaomi17UltraTelephoto_interpolatesFNumberWithinRange() {
        val metrics75 = calculateEffectiveMetrics(75.0, xiaomi17UltraTeleLenses)!!
        val metrics100 = calculateEffectiveMetrics(100.0, xiaomi17UltraTeleLenses)!!
        val metrics90 = calculateEffectiveMetrics(90.0, xiaomi17UltraTeleLenses)!!
        val expected90 = 2.39 + (2.96 - 2.39) * ((90.0 - 75.0) / (100.0 - 75.0))

        assertEquals(2.39, metrics75.effectiveFNumber, 0.0001)
        assertEquals(2.96, metrics100.effectiveFNumber, 0.0001)
        assertEquals(expected90, metrics90.effectiveFNumber, 0.0001)
    }

    @Test
    fun xiaomi17UltraTelephoto_actualFocalChangesWithinOpticalRange() {
        val metrics75 = calculateEffectiveMetrics(75.0, xiaomi17UltraTeleLenses)!!
        val metrics100 = calculateEffectiveMetrics(100.0, xiaomi17UltraTeleLenses)!!

        assertTrue(metrics100.opticalActualFocalLengthMm > metrics75.opticalActualFocalLengthMm)
    }

    @Test
    fun calculateEffectiveMetrics_prefersNativeLensAtOpticalRangeBoundary() {
        val metrics = calculateEffectiveMetrics(
            focalLength35mm = 70.0,
            lenses = listOf(variableWideLens, teleLens)
        )

        assertEquals(70.0, metrics!!.baseLens.nativeFocalLength35mm, 0.0001)
    }
}
