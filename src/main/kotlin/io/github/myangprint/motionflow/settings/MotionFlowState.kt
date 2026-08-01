package io.github.myangprint.motionflow.settings

const val CURRENT_SCHEMA_VERSION = 4

enum class MotionProfileId {
    SUBTLE,
    BALANCED,
    EXPRESSIVE,
    CUSTOM,
}

enum class PowerSaveBehavior {
    DISABLE_ALL,
    ESSENTIAL_ONLY,
    IGNORE,
}

enum class CaretTrailStyle {
    SILKY_TRAIL,
    SPEED_LINES,
    ROUNDED_RIBBON,
}

enum class CaretTrailCoverage {
    ALL_CARETS,
    PRIMARY_ONLY,
}

enum class CaretTrailVisibility(
    val alphaScale: Double,
    val widthScale: Float,
) {
    SOFT(0.78, 0.86f),
    BALANCED(1.00, 1.00f),
    CLEAR(1.25, 1.10f),
    BRIGHT(1.50, 1.20f),
}

data class MotionFlowState(
    var schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    var enabled: Boolean = true,
    var profile: String = MotionProfileId.BALANCED.name,
    var reducedMotion: Boolean = false,
    var durationScale: Double = 1.0,
    var intensityScale: Double = 1.0,
    var powerSaveBehavior: String = PowerSaveBehavior.DISABLE_ALL.name,
    var caretTrailEnabled: Boolean = true,
    var caretTrailLength: Int = 7,
    var caretTrailStyle: String = CaretTrailStyle.SILKY_TRAIL.name,
    var caretTrailCoverage: String = CaretTrailCoverage.ALL_CARETS.name,
    var caretTrailVisibility: String = CaretTrailVisibility.CLEAR.name,
    var clickRippleEnabled: Boolean = true,
    var navigationPulseEnabled: Boolean = true,
    var navigationJumpThreshold: Int = 8,
    var editorContextCueEnabled: Boolean = true,
    var toolWindowCueEnabled: Boolean = true,
    var inactiveFrameDimEnabled: Boolean = false,
    var inactiveFrameDimAmount: Double = 0.10,
    var diagnosticsDebugMode: Boolean = false,
)

data class NormalizedState(
    val state: MotionFlowState,
    val correctedFields: Set<String>,
)

object MotionStateNormalizer {
    fun normalize(source: MotionFlowState): NormalizedState {
        val corrected = linkedSetOf<String>()
        fun doubleValue(name: String, value: Double, range: ClosedFloatingPointRange<Double>, fallback: Double): Double {
            if (!value.isFinite()) {
                corrected += name
                return fallback
            }
            val result = value.coerceIn(range)
            if (result != value) corrected += name
            return result
        }

        fun intValue(name: String, value: Int, range: IntRange): Int {
            val result = value.coerceIn(range)
            if (result != value) corrected += name
            return result
        }

        val profile = MotionProfileId.entries.firstOrNull { it.name == source.profile }
            ?: MotionProfileId.BALANCED.also { corrected += "profile" }
        val powerSave = PowerSaveBehavior.entries.firstOrNull { it.name == source.powerSaveBehavior }
            ?: PowerSaveBehavior.DISABLE_ALL.also { corrected += "powerSaveBehavior" }
        val requestedTrailStyle = CaretTrailStyle.entries.firstOrNull { it.name == source.caretTrailStyle }
        val caretTrailStyle = when {
            source.schemaVersion < 3 && requestedTrailStyle == CaretTrailStyle.SPEED_LINES -> {
                corrected += "caretTrailStyle"
                CaretTrailStyle.SILKY_TRAIL
            }
            requestedTrailStyle != null -> requestedTrailStyle
            else -> CaretTrailStyle.SILKY_TRAIL.also { corrected += "caretTrailStyle" }
        }
        val caretTrailCoverage = CaretTrailCoverage.entries.firstOrNull { it.name == source.caretTrailCoverage }
            ?: CaretTrailCoverage.ALL_CARETS.also { corrected += "caretTrailCoverage" }
        val caretTrailVisibility = CaretTrailVisibility.entries.firstOrNull { it.name == source.caretTrailVisibility }
            ?: CaretTrailVisibility.CLEAR.also { corrected += "caretTrailVisibility" }
        if (source.schemaVersion != CURRENT_SCHEMA_VERSION) corrected += "schemaVersion"

        return NormalizedState(
            source.copy(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                profile = profile.name,
                powerSaveBehavior = powerSave.name,
                caretTrailStyle = caretTrailStyle.name,
                caretTrailCoverage = caretTrailCoverage.name,
                caretTrailVisibility = caretTrailVisibility.name,
                durationScale = doubleValue("durationScale", source.durationScale, 0.25..2.0, 1.0),
                intensityScale = doubleValue("intensityScale", source.intensityScale, 0.25..1.5, 1.0),
                caretTrailLength = intValue("caretTrailLength", source.caretTrailLength, 2..12),
                navigationJumpThreshold = intValue("navigationJumpThreshold", source.navigationJumpThreshold, 3..50),
                inactiveFrameDimAmount = doubleValue(
                    "inactiveFrameDimAmount",
                    source.inactiveFrameDimAmount,
                    0.04..0.25,
                    0.10,
                ),
            ),
            corrected,
        )
    }

    fun defaults(profile: MotionProfileId = MotionProfileId.BALANCED): MotionFlowState = when (profile) {
        MotionProfileId.SUBTLE -> MotionFlowState(
            profile = profile.name,
            durationScale = 0.75,
            intensityScale = 0.65,
            caretTrailLength = 4,
            caretTrailCoverage = CaretTrailCoverage.PRIMARY_ONLY.name,
            caretTrailVisibility = CaretTrailVisibility.SOFT.name,
            inactiveFrameDimEnabled = false,
        )
        MotionProfileId.BALANCED -> MotionFlowState(profile = profile.name)
        MotionProfileId.EXPRESSIVE -> MotionFlowState(
            profile = profile.name,
            durationScale = 1.20,
            intensityScale = 1.25,
            caretTrailLength = 10,
            caretTrailCoverage = CaretTrailCoverage.ALL_CARETS.name,
            caretTrailVisibility = CaretTrailVisibility.BRIGHT.name,
            inactiveFrameDimEnabled = true,
        )
        MotionProfileId.CUSTOM -> MotionFlowState(profile = profile.name)
    }
}
