package fuzzd.generator.selection.probability_manager

import kotlin.reflect.KFunction

class FeatureSupportedProbabilityManager(
    val excludedFeatures: Set<KFunction<*>> = setOf(),
    val maxTypeDepth: Int? = null
) : ProbabilityManager {
    private val baseProbabilityManager = BaseProbabilityManager()

    private fun getProbability(function: KFunction<*>): Double =
        if (function in excludedFeatures) 0.0 else function.call(baseProbabilityManager) as Double

    private fun getStatementCount(function: KFunction<*>): Int =
        if (function in excludedFeatures) 0 else function.call(baseProbabilityManager) as Int

    // type selection
    override fun classType(): Double = getProbability(ProbabilityManager::classType)
    override fun traitType(): Double = getProbability(ProbabilityManager::traitType)
    override fun datatype(): Double = getProbability(ProbabilityManager::datatype)
    override fun arrayType(): Double = getProbability(ProbabilityManager::arrayType)
    override fun datatstructureType(): Double =
        getProbability(ProbabilityManager::datatstructureType)

    override fun literalType(): Double = getProbability(ProbabilityManager::literalType)
    override fun setType(): Double = getProbability(ProbabilityManager::setType)
    override fun multisetType(): Double = getProbability(ProbabilityManager::multisetType)
    override fun mapType(): Double = getProbability(ProbabilityManager::mapType)
    override fun sequenceType(): Double = getProbability(ProbabilityManager::sequenceType)
    override fun stringType(): Double = getProbability(ProbabilityManager::stringType)
    override fun intType(): Double = getProbability(ProbabilityManager::intType)
    override fun boolType(): Double = getProbability(ProbabilityManager::boolType)
    override fun charType(): Double = getProbability(ProbabilityManager::charType)


    // statements
    override fun assertStatement(): Double = 0.0
    override fun ifStatement(): Double = getProbability(ProbabilityManager::ifStatement)
    override fun matchStatement(): Double = getProbability(ProbabilityManager::matchStatement)
    override fun forallStatement(): Double = getProbability(ProbabilityManager::forallStatement)
    override fun forLoopStatement(): Double = getProbability(ProbabilityManager::forLoopStatement)
    override fun whileStatement(): Double = getProbability(ProbabilityManager::whileStatement)
    override fun methodCall(): Double = getProbability(ProbabilityManager::methodCall)
    override fun mapAssign(): Double = getProbability(ProbabilityManager::mapAssign)
    override fun assignStatement(): Double =
        getProbability(ProbabilityManager::assignStatement) - multiAssignStatement()

    override fun multiAssignStatement(): Double =
        getProbability(ProbabilityManager::multiAssignStatement)

    override fun classInstantiation(): Double =
        getProbability(ProbabilityManager::classInstantiation)

    // decl info
    override fun constField(): Double = getProbability(ProbabilityManager::constField)

    // expressions
    override fun assignIdentifier(): Double = getProbability(ProbabilityManager::assignIdentifier)
    override fun assignArrayIndex(): Double = getProbability(ProbabilityManager::assignArrayIndex)
    override fun binaryExpression(): Double = getProbability(ProbabilityManager::binaryExpression)
    override fun unaryExpression(): Double = getProbability(ProbabilityManager::unaryExpression)
    override fun modulusExpression(): Double = getProbability(ProbabilityManager::modulusExpression)
    override fun multisetConversion(): Double =
        getProbability(ProbabilityManager::multisetConversion)

    override fun functionCall(): Double = getProbability(ProbabilityManager::functionCall)
    override fun ternary(): Double = getProbability(ProbabilityManager::ternary)
    override fun matchExpression(): Double = getProbability(ProbabilityManager::matchExpression)
    override fun assignExpression(): Double = getProbability(ProbabilityManager::assignExpression)
    override fun indexExpression(): Double = getProbability(ProbabilityManager::indexExpression)
    override fun identifier(): Double = getProbability(ProbabilityManager::identifier)
    override fun literal(): Double = getProbability(ProbabilityManager::literal)
    override fun constructor(): Double = getProbability(ProbabilityManager::constructor)
    override fun comprehension(): Double = getProbability(ProbabilityManager::comprehension)

    override fun comprehensionConditionIntRange(): Double =
        getProbability(ProbabilityManager::comprehensionConditionIntRange)

    // index types
    override fun arrayIndexType(): Double = getProbability(ProbabilityManager::arrayIndexType)
    override fun mapIndexType(): Double = getProbability(ProbabilityManager::mapIndexType)
    override fun multisetIndexType(): Double = getProbability(ProbabilityManager::multisetIndexType)
    override fun sequenceIndexType(): Double = getProbability(ProbabilityManager::sequenceIndexType)
    override fun stringIndexType(): Double = getProbability(ProbabilityManager::stringIndexType)
    override fun datatypeIndexType(): Double = getProbability(ProbabilityManager::datatypeIndexType)

    // array init types
    override fun arrayInitValues(): Double = getProbability(ProbabilityManager::arrayInitValues)
    override fun arrayInitComprehension(): Double =
        getProbability(ProbabilityManager::arrayInitComprehension)

    override fun arrayInitDefault(): Double = getProbability(ProbabilityManager::arrayInitDefault)

    // other info
    override fun methodStatements(): Int = getStatementCount(ProbabilityManager::methodStatements)
    override fun ifBranchStatements(): Int =
        getStatementCount(ProbabilityManager::ifBranchStatements)

    override fun forLoopBodyStatements(): Int =
        getStatementCount(ProbabilityManager::forLoopBodyStatements)

    override fun whileBodyStatements(): Int =
        getStatementCount(ProbabilityManager::whileBodyStatements)

    override fun mainFunctionStatements(): Int =
        getStatementCount(ProbabilityManager::mainFunctionStatements)

    override fun matchStatements(): Int = getStatementCount(ProbabilityManager::matchStatements)
    override fun comprehensionIdentifiers(): Int =
        getStatementCount(ProbabilityManager::comprehensionIdentifiers)

    override fun numberOfTraits(): Int = getStatementCount(ProbabilityManager::numberOfTraits)
    override fun maxNumberOfAssigns(): Int =
        getStatementCount(ProbabilityManager::maxNumberOfAssigns)

    override fun maxTypeDepth(): Int = maxTypeDepth ?: baseProbabilityManager.maxTypeDepth()

    // Verification mutation
    override fun mutateVerificationCondition(): Double = 0.0
    override fun mutateAssertFalse(): Double = 0.0
}
