package fuzzd.generator.selection.supported_features

import fuzzd.generator.selection.probability_manager.ProbabilityManager
import kotlin.reflect.KFunction

class RustBackendFeatures : BackendFeatures() {
    private val SUPPORTED_FEATURES: Set<KFunction<*>> = setOf(
        ProbabilityManager::classType,
        // ProbabilityManager::traitType,
        ProbabilityManager::datatype,
        ProbabilityManager::arrayType,
        ProbabilityManager::datatstructureType,
        ProbabilityManager::literalType,

        // Datastructure types
        ProbabilityManager::setType,
        // ProbabilityManager::multisetType,
        //  ProbabilityManager::mapType,
        ProbabilityManager::sequenceType,
        ProbabilityManager::stringType,

        // Literal types
        ProbabilityManager::intType,
        ProbabilityManager::boolType,
        ProbabilityManager::charType,

        // Statements
        ProbabilityManager::assertStatement,
        ProbabilityManager::ifStatement,
        ProbabilityManager::matchStatement,
//        ProbabilityManager::forallStatement,
//         ProbabilityManager::forLoopStatement,
        // ProbabilityManager::whileStatement,
        ProbabilityManager::methodCall,
        // ProbabilityManager::mapAssign,
        ProbabilityManager::assignStatement,
        ProbabilityManager::multiAssignStatement,
        ProbabilityManager::classInstantiation,

        // Decl info
        ProbabilityManager::constField,

        // Assign type
        ProbabilityManager::assignIdentifier,
        ProbabilityManager::assignArrayIndex,

        // Expressions
        ProbabilityManager::binaryExpression,
        ProbabilityManager::unaryExpression,
        ProbabilityManager::modulusExpression,
        // ProbabilityManager::multisetConversion,
        ProbabilityManager::functionCall,
        ProbabilityManager::ternary,
        ProbabilityManager::matchExpression,
        ProbabilityManager::assignExpression,
        ProbabilityManager::indexExpression,
        ProbabilityManager::identifier,
        ProbabilityManager::literal,
        ProbabilityManager::constructor,
        // ProbabilityManager::comprehension,

        ProbabilityManager::comprehensionConditionIntRange,

        // Index type
        ProbabilityManager::arrayIndexType,
        // ProbabilityManager::mapIndexType,
//        ProbabilityManager::multisetIndexType,
        ProbabilityManager::sequenceIndexType,
        ProbabilityManager::stringIndexType,
        ProbabilityManager::datatypeIndexType,

        // Array init type
        ProbabilityManager::arrayInitDefault,
        // ProbabilityManager::arrayInitComprehension,
        ProbabilityManager::arrayInitValues,

        // Additional data
        ProbabilityManager::methodStatements,
        ProbabilityManager::ifBranchStatements,
        //  ProbabilityManager::forLoopBodyStatements,
        // ProbabilityManager::whileBodyStatements,
        ProbabilityManager::mainFunctionStatements,
        ProbabilityManager::matchStatements,
        // ProbabilityManager::comprehensionIdentifiers,

//        ProbabilityManager::numberOfTraits,

        ProbabilityManager::maxNumberOfAssigns,
        ProbabilityManager::maxTypeDepth,

        // Verification mutation
        ProbabilityManager::mutateVerificationCondition,
        ProbabilityManager::mutateAssertFalse
    )

    private val UNSUPPORTED_FEATURES: Set<KFunction<*>> =
        ComputeNonSupportedFeatures(SUPPORTED_FEATURES)

    override fun SupportedFeatures(): Set<KFunction<*>> {
        return SUPPORTED_FEATURES
    }

    override fun NonSupportedFeatures(): Set<KFunction<*>> {
        return UNSUPPORTED_FEATURES
    }
}