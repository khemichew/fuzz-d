package fuzzd.generator.selection.probability_manager

class BaseProbabilityManager : ProbabilityManager {
    // Types
    override fun classType() = 0.06
    override fun traitType() = 0.03
    override fun datatype() = 0.05
    override fun arrayType() = 0.1
    override fun datatstructureType() = 0.24
    override fun literalType() = 0.75

    // Datastructure types
    override fun setType() = 0.125
    override fun multisetType() = 0.125
    override fun mapType() = 0.25
    override fun sequenceType() = 0.25
    override fun stringType() = 0.25

    // literal types
    override fun intType() = 0.4
    override fun boolType() = 0.4
    override fun charType() = 0.05

    // statements
    override fun assertStatement(): Double = 0.0
    override fun ifStatement() = 0.13
    override fun matchStatement() = 0.05
    override fun forallStatement(): Double = 0.05
    override fun forLoopStatement(): Double = 0.1
    override fun whileStatement() = 0.1
    override fun methodCall() = 0.07
    override fun mapAssign() = 0.05
    override fun assignStatement() = 0.5
    override fun multiAssignStatement(): Double = 0.1
    override fun classInstantiation() = 0.05

    // decl info
    override fun constField(): Double = 0.3

    // assign type
    override fun assignIdentifier() = 0.8
    override fun assignArrayIndex() = 0.2

    // expressions
    override fun binaryExpression() = 0.4
    override fun unaryExpression() = 0.15
    override fun modulusExpression() = 0.15
    override fun multisetConversion() = 0.05
    override fun functionCall() = 0.07
    override fun ternary() = 0.05
    override fun matchExpression() = 0.05
    override fun assignExpression() = 0.1
    override fun indexExpression() = 0.1
    override fun identifier() = 0.5
    override fun literal() = 0.2
    override fun constructor() = 0.3
    override fun comprehension() = 0.06

    override fun comprehensionConditionIntRange(): Double = 0.6

    // index type
    override fun arrayIndexType() = 0.2
    override fun mapIndexType() = 0.2
    override fun multisetIndexType() = 0.2
    override fun sequenceIndexType() = 0.2
    override fun stringIndexType() = 0.2
    override fun datatypeIndexType() = 0.2

    // array init type
    override fun arrayInitDefault(): Double = 0.4
    override fun arrayInitValues(): Double = 0.3
    override fun arrayInitComprehension(): Double = 0.3

    // other data
    override fun methodStatements(): Int = 6
    override fun ifBranchStatements(): Int = 5
    override fun forLoopBodyStatements(): Int = 4
    override fun whileBodyStatements(): Int = 4
    override fun mainFunctionStatements(): Int = 16
    override fun matchStatements(): Int = 4
    override fun comprehensionIdentifiers(): Int = 3

    override fun numberOfTraits(): Int = 3

    override fun maxTypeDepth(): Int = 3
    override fun maxNumberOfAssigns(): Int = 5

    // verification mutation
    override fun mutateVerificationCondition(): Double = 0.0
    override fun mutateAssertFalse(): Double = 0.0
}
