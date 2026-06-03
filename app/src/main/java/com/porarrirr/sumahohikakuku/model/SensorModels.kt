package com.porarrirr.sumahohikakuku.model

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import java.util.Locale

const val MANUAL_INPUT_SENSOR_VALUE = "_MANUAL_INPUT_"
const val INCH_TO_MM = 25.4
// Optical format approximation: 0.63 ≈ 16 mm per nominal inch.
const val SENSOR_DIAG_FACTOR = 16.0 / INCH_TO_MM
const val FF_DIAGONAL_MM = 43.2666
const val MAX_DEVICES = 5
const val MAX_LENSES_PER_DEVICE = 4

val DEFAULT_DEVICE_COLORS = listOf(
    "#2563EB",
    "#7C3AED",
    "#DC2626",
    "#F97316",
    "#059669",
    "#0EA5E9",
    "#F59E0B",
    "#EC4899",
    "#10B981",
    "#6366F1",
    "#14B8A6",
    "#4ADE80"
)

private val manufacturerOrder = listOf("Sony", "OmniVision", "Samsung", "GalaxyCore", "SmartSens", "Toshiba", "Other")
private val trailingAlphaRegex = Regex("[A-Z]+$", RegexOption.IGNORE_CASE)
private val firstDigitsRegex = Regex("\\d+")
private val manualDescriptorFractionRegex = Regex("^([0-9]+(?:\\.[0-9]+)?)/([0-9]+(?:\\.[0-9]+)?)$")

private data class NumericPattern(
    val regex: Regex,
    val groupIndex: Int
)

private val numericPatterns = listOf(
    NumericPattern(
        regex = Regex(
            "(?:LYT-T?|IMX|S5K[A-Z]{0,2}|OV(?:[A-Z0-9]{2,3})?[A-Z]?|GC|SC|HES|CK|ISOCELL\\s[A-Z]{0,2})(\\d+[A-Z0-9]*)",
            RegexOption.IGNORE_CASE
        ),
        groupIndex = 1
    ),
    NumericPattern(regex = Regex("(\\d+)MP", RegexOption.IGNORE_CASE), groupIndex = 1),
    NumericPattern(regex = Regex("[A-Z]+(\\d+)", RegexOption.IGNORE_CASE), groupIndex = 1),
    NumericPattern(regex = Regex("(\\d+)"), groupIndex = 1)
)

enum class SensorSource {
    DATABASE,
    MANUAL
}

data class SensorSpec(
    val name: String,
    val value: String,
    val megapixels: Double,
    val pixelSizeUm: Double,
    val binningType: String,
    val manufacturer: String,
    val source: SensorSource,
    val isManual: Boolean = false
)

data class SensorMetrics(
    val diagonalMm: Double,
    val widthMm: Double,
    val heightMm: Double,
    val areaSqMm: Double,
    val sensorName: String,
    val binningType: String,
    val nativePixelSizeUm: Double,
    val source: SensorSource
)

data class LensProcessed(
    val nativeFocalLength35mm: Double,
    val fNumber: Double,
    val actualFocalLengthMm: Double,
    val sensorMetrics: SensorMetrics,
    val opticalEndFocalLength35mm: Double = nativeFocalLength35mm,
    val endFNumber: Double = fNumber
) {
    val isVariableOptical: Boolean
        get() = opticalEndFocalLength35mm > nativeFocalLength35mm

    fun containsOptical(focal35mm: Double): Boolean {
        return focal35mm >= nativeFocalLength35mm &&
                focal35mm <= opticalEndFocalLength35mm
    }

    fun fNumberAt(focal35mm: Double): Double {
        val span = opticalEndFocalLength35mm - nativeFocalLength35mm
        if (span <= 0.0) return fNumber
        val t = ((focal35mm - nativeFocalLength35mm) / span)
            .coerceIn(0.0, 1.0)
        return fNumber + (endFNumber - fNumber) * t
    }
}

data class FocalLengthMetrics(
    val focalLength35mm: Double,
    val effectiveWidthMm: Double,
    val effectiveHeightMm: Double,
    val effectiveAreaSqMm: Double,
    val zoomRatio: Double,
    val apertureDiameterMm: Double,
    val apertureAreaSqMm: Double,
    val totalLightIntake: Double,
    val baseLens: LensProcessed,
    val opticalFocalLength35mm: Double = baseLens.nativeFocalLength35mm,
    val opticalZoomRatio: Double = 1.0,
    val digitalCropRatio: Double = zoomRatio,
    val effectiveFNumber: Double = baseLens.fNumber,
    val opticalActualFocalLengthMm: Double = baseLens.actualFocalLengthMm
)

data class LensProcessingInput(
    val nativeFocalLength35mm: Double,
    val fNumber: Double,
    val sensorMetrics: SensorMetrics,
    val opticalEndFocalLength35mm: Double = nativeFocalLength35mm,
    val endFNumber: Double = fNumber
)

data class ProcessedDevice(
    val name: String,
    val colorHex: String,
    val lenses: List<LensProcessed>,
    val metricsByFocalLength: List<FocalLengthMetrics>
) {
    private val metricsLookup: Map<Double, FocalLengthMetrics> =
        metricsByFocalLength.associateBy { it.focalLength35mm }

    fun metricsAt(focalLength: Double): FocalLengthMetrics? = metricsLookup[focalLength]
}

data class ComparisonResults(
    val focalLengths: List<Double>,
    val devices: List<ProcessedDevice>
)

fun parseSensorCsv(raw: String): List<SensorSpec> {
    val sensors = raw.lineSequence()
        .map { it.trim().trimStart('\uFEFF') }
        .filter { it.isNotEmpty() && !it.startsWith('#') }
        .mapNotNull { line ->
            val parts = parseCsvLine(line).map { part -> part.trim() }
            if (parts.size != 4) return@mapNotNull null
            val name = parts[0]
            val megapixelsValue = parts[1].toDoubleOrNull()?.div(100.0) ?: return@mapNotNull null
            val pixelSizeUm = parts[2].toDoubleOrNull() ?: return@mapNotNull null
            val binningType = normalizeBinning(parts[3])
            val manufacturer = detectManufacturer(name)
            SensorSpec(
                name = name,
                value = name,
                megapixels = megapixelsValue,
                pixelSizeUm = pixelSizeUm,
                binningType = binningType,
                manufacturer = manufacturer,
                source = SensorSource.DATABASE
            )
        }
        .sortedWith(sensorComparator())
        .toMutableList()

    sensors.add(0, SensorSpec(
        name = "手動入力",
        value = MANUAL_INPUT_SENSOR_VALUE,
        megapixels = 0.0,
        pixelSizeUm = 0.0,
        binningType = "Manual",
        manufacturer = "Manual",
        source = SensorSource.MANUAL,
        isManual = true
    ))

    return sensors
}

private fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false

    var index = 0
    while (index < line.length) {
        val char = line[index]
        when (char) {
            '"' -> {
                val nextIndex = index + 1
                if (inQuotes && nextIndex < line.length && line[nextIndex] == '"') {
                    current.append('"')
                    index = nextIndex
                } else {
                    inQuotes = !inQuotes
                }
            }
            ',' -> {
                if (inQuotes) {
                    current.append(char)
                } else {
                    fields.add(current.toString())
                    current.setLength(0)
                }
            }
            else -> current.append(char)
        }
        index++
    }

    fields.add(current.toString())
    return fields
}

fun calculateNativeSensorMetrics(sensorSpec: SensorSpec?, manualDescriptor: String?): SensorMetrics {
    if (sensorSpec != null && !sensorSpec.isManual) {
        if (sensorSpec.megapixels <= 0.0 || sensorSpec.pixelSizeUm <= 0.0) {
            return SensorMetrics(0.0, 0.0, 0.0, 0.0, sensorSpec.name, sensorSpec.binningType, 0.0, SensorSource.DATABASE)
        }
        val totalPixels = sensorSpec.megapixels * 1_000_000.0
        val heightPx = sqrt(totalPixels * 3.0 / 4.0)
        val widthPx = heightPx * 4.0 / 3.0
        val widthMm = widthPx * sensorSpec.pixelSizeUm / 1000.0
        val heightMm = heightPx * sensorSpec.pixelSizeUm / 1000.0
        val diagonalMm = sqrt(widthMm * widthMm + heightMm * heightMm)
        val areaSqMm = widthMm * heightMm
        return SensorMetrics(
            diagonalMm = diagonalMm,
            widthMm = widthMm,
            heightMm = heightMm,
            areaSqMm = areaSqMm,
            sensorName = sensorSpec.name,
            binningType = sensorSpec.binningType,
            nativePixelSizeUm = sensorSpec.pixelSizeUm,
            source = SensorSource.DATABASE
        )
    }

    val descriptor = manualDescriptor?.trim().orEmpty()
    val diagonalInchNominal = manualDescriptorToDiagonalInches(descriptor)
        ?: return SensorMetrics(0.0, 0.0, 0.0, 0.0, descriptor.ifEmpty { "N/A" }, "Manual", 0.0, SensorSource.MANUAL)
    val diagonalMmOptical = diagonalInchNominal * INCH_TO_MM * SENSOR_DIAG_FACTOR
    val widthMm = (4.0 / 5.0) * diagonalMmOptical
    val heightMm = (3.0 / 5.0) * diagonalMmOptical
    val areaSqMm = widthMm * heightMm

    return SensorMetrics(
        diagonalMm = diagonalMmOptical,
        widthMm = widthMm,
        heightMm = heightMm,
        areaSqMm = areaSqMm,
        sensorName = descriptor,
        binningType = "Manual",
        nativePixelSizeUm = 0.0,
        source = SensorSource.MANUAL
    )
}

fun computeProcessedDevice(
    name: String,
    colorHex: String,
    rawLenses: List<LensProcessingInput>,
    focalLengths: List<Double>
): ProcessedDevice? {
    if (rawLenses.isEmpty()) return null
    val lenses = rawLenses.map { lens ->
        val focalLength35 = lens.nativeFocalLength35mm
        val metrics = lens.sensorMetrics
        val cropFactor = if (metrics.diagonalMm > 0.0) FF_DIAGONAL_MM / metrics.diagonalMm else return@map null
        val actualFocal = focalLength35 / cropFactor
        LensProcessed(
            nativeFocalLength35mm = focalLength35,
            fNumber = lens.fNumber,
            actualFocalLengthMm = actualFocal,
            sensorMetrics = metrics,
            opticalEndFocalLength35mm = lens.opticalEndFocalLength35mm,
            endFNumber = lens.endFNumber
        )
    }.filterNotNull()
        .sortedWith(
            compareBy<LensProcessed> { it.nativeFocalLength35mm }
                .thenBy { it.opticalEndFocalLength35mm }
        )

    if (lenses.isEmpty()) return null

    val metricsByFocalLength = focalLengths.mapNotNull { currentFocal ->
        calculateEffectiveMetrics(currentFocal, lenses)
    }

    return ProcessedDevice(
        name = name,
        colorHex = colorHex,
        lenses = lenses,
        metricsByFocalLength = metricsByFocalLength
    )
}

fun calculateEffectiveMetrics(focalLength35mm: Double, lenses: List<LensProcessed>): FocalLengthMetrics? {
    require(lenses.isNotEmpty())
    val sorted = lenses.sortedWith(
        compareBy<LensProcessed> { it.nativeFocalLength35mm }
            .thenBy { it.opticalEndFocalLength35mm }
    )
    val opticalLens = sorted
        .filter { it.containsOptical(focalLength35mm) }
        .maxWithOrNull(
            compareBy<LensProcessed> { it.nativeFocalLength35mm }
                .thenBy { it.opticalEndFocalLength35mm }
        )
    val baseLens = opticalLens
        ?: sorted
            .filter { focalLength35mm >= it.opticalEndFocalLength35mm }
            .maxWithOrNull(
                compareBy<LensProcessed> { it.opticalEndFocalLength35mm }
                    .thenBy { it.nativeFocalLength35mm }
            )
        ?: return null

    val opticalFocal35mm = if (opticalLens != null) {
        focalLength35mm
    } else {
        baseLens.opticalEndFocalLength35mm
    }
    val digitalCropRatio = max(1.0, focalLength35mm / opticalFocal35mm)
    val effectiveWidthMm = baseLens.sensorMetrics.widthMm / digitalCropRatio
    val effectiveHeightMm = baseLens.sensorMetrics.heightMm / digitalCropRatio
    val effectiveAreaSqMm = baseLens.sensorMetrics.areaSqMm / digitalCropRatio.pow(2)

    val cropFactor = FF_DIAGONAL_MM / baseLens.sensorMetrics.diagonalMm
    val opticalActualFocalMm = opticalFocal35mm / cropFactor
    val effectiveFNumber = baseLens.fNumberAt(opticalFocal35mm)
    val apertureDiameter = if (effectiveFNumber > 0.0) opticalActualFocalMm / effectiveFNumber else 0.0
    val apertureArea = if (apertureDiameter > 0) (PI / 4.0) * apertureDiameter.pow(2) else 0.0
    val totalLightIntake = if (effectiveFNumber > 0.0) effectiveAreaSqMm / effectiveFNumber.pow(2) else 0.0
    val opticalZoomRatio = opticalFocal35mm / baseLens.nativeFocalLength35mm

    return FocalLengthMetrics(
        focalLength35mm = focalLength35mm,
        effectiveWidthMm = effectiveWidthMm,
        effectiveHeightMm = effectiveHeightMm,
        effectiveAreaSqMm = effectiveAreaSqMm,
        zoomRatio = digitalCropRatio,
        apertureDiameterMm = apertureDiameter,
        apertureAreaSqMm = apertureArea,
        totalLightIntake = totalLightIntake,
        baseLens = baseLens,
        opticalFocalLength35mm = opticalFocal35mm,
        opticalZoomRatio = opticalZoomRatio,
        digitalCropRatio = digitalCropRatio,
        effectiveFNumber = effectiveFNumber,
        opticalActualFocalLengthMm = opticalActualFocalMm
    )
}

fun isValidManualSensorDescriptor(descriptor: String): Boolean {
    return manualDescriptorToDiagonalInches(descriptor.trim()) != null
}

private fun manualDescriptorToDiagonalInches(descriptor: String): Double? {
    if (descriptor.isBlank()) return null
    val normalized = buildString(descriptor.length) {
        descriptor.forEach { char ->
            val ascii = when (char) {
                in '０'..'９' -> (char.code - '０'.code + '0'.code).toChar()
                '／' -> '/'
                '．', '。', '，', ',' -> '.'
                else -> char
            }
            append(ascii)
        }
    }
    val compact = normalized.replace("\\s+".toRegex(), "")
    val match = manualDescriptorFractionRegex.matchEntire(compact) ?: return null
    val numerator = match.groupValues[1].toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    val denominator = match.groupValues[2].toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    return numerator / denominator
}

private fun sensorComparator(): Comparator<SensorSpec> = Comparator { a, b ->   
    val manufacturerRankA = manufacturerOrder.indexOf(a.manufacturer).let { index ->
        if (index == -1) manufacturerOrder.size else index
    }
    val manufacturerRankB = manufacturerOrder.indexOf(b.manufacturer).let { index ->
        if (index == -1) manufacturerOrder.size else index
    }
    if (manufacturerRankA != manufacturerRankB) return@Comparator manufacturerRankA - manufacturerRankB

    if (a.manufacturer == "Sony" && b.manufacturer == "Sony") {
        val (isALyt, isBLyt) = a.name.startsWith("Sony LYT") to b.name.startsWith("Sony LYT")
        val (isAImx, isBImx) = a.name.startsWith("Sony IMX") to b.name.startsWith("Sony IMX")
        if (isALyt && !isBLyt) return@Comparator -1
        if (!isALyt && isBLyt) return@Comparator 1
        if (isALyt && isBLyt) {
            val numA = getNumericPartForSort(a.name.removePrefix("Sony LYT"))
            val numB = getNumericPartForSort(b.name.removePrefix("Sony LYT"))
            if (numA != numB) return@Comparator numB - numA
        }
        if (isAImx && !isBImx && !isBLyt) return@Comparator -1
        if (!isAImx && !isALyt && isBImx) return@Comparator 1
    }

    val numA = getNumericPartForSort(a.name)
    val numB = getNumericPartForSort(b.name)
    if (numA != numB && numA != 0 && numB != 0) {
        return@Comparator numB - numA
    }

    a.name.compareTo(b.name)
}

private fun getNumericPartForSort(name: String): Int {
    for (pattern in numericPatterns) {
        val match = pattern.regex.find(name) ?: continue
        val groupValue = match.groupValues.getOrNull(pattern.groupIndex).orEmpty()
        val withoutSuffix = groupValue.replace(trailingAlphaRegex, "")
        if (withoutSuffix.isNotBlank() && withoutSuffix.all { it.isDigit() }) {
            return withoutSuffix.toInt()
        }
        val digits = firstDigitsRegex.find(groupValue)?.value ?: continue
        return digits.toIntOrNull() ?: continue
    }
    return 0
}

internal fun normalizeBinning(raw: String): String {
    val binningRaw = raw.trim().lowercase(Locale.US)
    return when {
        binningRaw == "yes" -> "Quad Bayer (2x2)"
        "nona" in binningRaw -> "Nona (3x3)"
        "16-cell" in binningRaw || "16-in-1" in binningRaw -> "16-cell (4x4)"   
        binningRaw == "no" -> "None"
        binningRaw == "unknown" -> "Unknown"
        else -> raw.trim()
    }
}

internal fun detectManufacturer(name: String): String {
    val lower = name.lowercase(Locale.US)
    return when {
        "sony" in lower || name.contains("ソニー") -> "Sony"
        "omnivision" in lower -> "OmniVision"
        "samsung" in lower -> "Samsung"
        "galaxycore" in lower -> "GalaxyCore"
        "smartsens" in lower -> "SmartSens"
        "toshiba" in lower || name.contains("東芝") -> "Toshiba"
        else -> "Other"
    }
}
