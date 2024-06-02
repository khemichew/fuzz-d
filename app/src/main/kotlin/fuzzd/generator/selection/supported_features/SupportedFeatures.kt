package fuzzd.generator.selection.supported_features

import fuzzd.generator.selection.probability_manager.ProbabilityManager
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions

abstract class SupportedFeatures {
    abstract fun GetBackendSupportedFeatures(): Set<KFunction<*>>

    abstract fun GetBackendNonSupportedFeatures(): Set<KFunction<*>>

    protected fun ComputeNonSupportedFeatures(supportedFeatures: Set<KFunction<*>>): Set<KFunction<*>> {
        return ProbabilityManager::class.declaredFunctions.toSet().minus(supportedFeatures)
    }
}