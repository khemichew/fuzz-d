package fuzzd.generator.selection.supported_features

import fuzzd.generator.selection.probability_manager.ProbabilityManager
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions

class AllSupportedFeatures: SupportedFeatures() {
    private val SUPPORTED_FEATURES: Set<KFunction<*>> =
        ProbabilityManager::class.declaredFunctions.toSet()

    private val UNSUPPORTED_FEATURES: Set<KFunction<*>> =
        ComputeNonSupportedFeatures(SUPPORTED_FEATURES)

    override fun GetBackendNonSupportedFeatures(): Set<KFunction<*>> {
        return SUPPORTED_FEATURES
    }

    override fun GetBackendSupportedFeatures(): Set<KFunction<*>> {
        return UNSUPPORTED_FEATURES
    }
}