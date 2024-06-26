package fuzzd.generator.selection

import fuzzd.generator.ast.Type
import fuzzd.generator.ast.Type.BoolType
import fuzzd.generator.ast.Type.CharType
import fuzzd.generator.ast.Type.ClassType
import fuzzd.generator.ast.Type.ConstructorType.ArrayType
import fuzzd.generator.ast.Type.DataStructureType
import fuzzd.generator.ast.Type.DataStructureType.MapType
import fuzzd.generator.ast.Type.DataStructureType.MultisetType
import fuzzd.generator.ast.Type.DataStructureType.SequenceType
import fuzzd.generator.ast.Type.DataStructureType.SetType
import fuzzd.generator.ast.Type.DataStructureType.StringType
import fuzzd.generator.ast.Type.DatatypeType
import fuzzd.generator.ast.Type.IntType
import fuzzd.generator.ast.Type.LiteralType
import fuzzd.generator.ast.Type.TopLevelDatatypeType
import fuzzd.generator.ast.Type.TraitType
import fuzzd.generator.ast.error.InvalidInputException
import fuzzd.generator.ast.operators.BinaryOperator
import fuzzd.generator.ast.operators.BinaryOperator.BooleanBinaryOperator
import fuzzd.generator.ast.operators.BinaryOperator.ComparisonBinaryOperator
import fuzzd.generator.ast.operators.BinaryOperator.DataStructureBinaryOperator
import fuzzd.generator.ast.operators.BinaryOperator.DataStructureMathematicalOperator
import fuzzd.generator.ast.operators.BinaryOperator.DataStructureMembershipOperator
import fuzzd.generator.ast.operators.BinaryOperator.UnionOperator
import fuzzd.generator.ast.operators.UnaryOperator
import fuzzd.generator.ast.operators.UnaryOperator.Companion.isUnaryType
import fuzzd.generator.ast.operators.UnaryOperator.NegationOperator
import fuzzd.generator.ast.operators.UnaryOperator.NotOperator
import fuzzd.generator.context.GenerationContext
import fuzzd.generator.selection.ArrayInitType.DEFAULT
import fuzzd.generator.selection.ArrayInitType.VALUE
import fuzzd.generator.selection.AssignType.ARRAY_INDEX
import fuzzd.generator.selection.ExpressionType.BINARY
import fuzzd.generator.selection.ExpressionType.COMPREHENSION
import fuzzd.generator.selection.ExpressionType.CONSTRUCTOR
import fuzzd.generator.selection.ExpressionType.FUNCTION_METHOD_CALL
import fuzzd.generator.selection.ExpressionType.IDENTIFIER
import fuzzd.generator.selection.ExpressionType.INDEX
import fuzzd.generator.selection.ExpressionType.INDEX_ASSIGN
import fuzzd.generator.selection.ExpressionType.LITERAL
import fuzzd.generator.selection.ExpressionType.MATCH
import fuzzd.generator.selection.ExpressionType.MODULUS
import fuzzd.generator.selection.ExpressionType.MULTISET_CONVERSION
import fuzzd.generator.selection.ExpressionType.TERNARY
import fuzzd.generator.selection.ExpressionType.UNARY
import fuzzd.generator.selection.IndexType.ARRAY
import fuzzd.generator.selection.IndexType.DATATYPE
import fuzzd.generator.selection.IndexType.MAP
import fuzzd.generator.selection.IndexType.MULTISET
import fuzzd.generator.selection.IndexType.SEQUENCE
import fuzzd.generator.selection.IndexType.STRING
import fuzzd.generator.selection.StatementType.ASSERT
import fuzzd.generator.selection.StatementType.ASSIGN
import fuzzd.generator.selection.StatementType.CLASS_INSTANTIATION
import fuzzd.generator.selection.StatementType.FORALL
import fuzzd.generator.selection.StatementType.FOR_LOOP
import fuzzd.generator.selection.StatementType.IF
import fuzzd.generator.selection.StatementType.MAP_ASSIGN
import fuzzd.generator.selection.StatementType.METHOD_CALL
import fuzzd.generator.selection.StatementType.MULTI_ASSIGN
import fuzzd.generator.selection.StatementType.WHILE
import fuzzd.generator.selection.probability_manager.BaseProbabilityManager
import fuzzd.generator.selection.probability_manager.FeatureSupportedProbabilityManager
import fuzzd.generator.selection.probability_manager.ProbabilityManager
import fuzzd.utils.unionAll
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class SelectionManager(
    private val random: Random,
    private val probabilityManager: ProbabilityManager = FeatureSupportedProbabilityManager(),
    private val unsupportedFeatures: Set<KFunction<*>> = setOf()
) {
    fun selectType(context: GenerationContext, depth: Int = 1): Type {
        val classTypeProb = if (context.onDemandIdentifiers && context.globalSymbolTable.hasClasses()) probabilityManager.classType() / context.classGenerationDepth else 0.0
        val traitTypeProb =
            if (context.unsupportedFeatures.contains(ProbabilityManager::traitType))
            {
                0.0
            } else if (context.onDemandIdentifiers && context.globalSymbolTable.hasTraits()) {
                probabilityManager.traitType() / context.classGenerationDepth
            } else 0.0

        val datatypeProb = if (context.globalSymbolTable.hasAvailableDatatypes(context.onDemandIdentifiers)) probabilityManager.datatype() / context.expressionDepth else 0.0

        val selection = listOf<Pair<(GenerationContext, Int) -> Type, Double>>(
            this::selectClassType to classTypeProb,
            this::selectTraitType to traitTypeProb,
            this::selectDatatypeType to datatypeProb,
            this::selectArrayType to if (depth < probabilityManager.maxTypeDepth() && context.onDemandIdentifiers) probabilityManager.arrayType() / context.expressionDepth else 0.0,
            this::selectDataStructureType to if (depth < probabilityManager.maxTypeDepth()) probabilityManager.datatstructureType() / context.expressionDepth else 0.0,
            this::selectLiteralType to probabilityManager.literalType(),
        )

        return randomWeightedSelection(normaliseWeights(selection)).invoke(context, depth)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun selectClassType(context: GenerationContext, depth: Int): ClassType =
        ClassType(randomSelection(context.globalSymbolTable.classes().toList()))

    @Suppress("UNUSED_PARAMETER")
    private fun selectTraitType(context: GenerationContext, depth: Int): TraitType =
        TraitType(randomSelection(context.globalSymbolTable.traits().toList()))

    @Suppress("UNUSED_PARAMETER")
    fun selectDatatypeType(context: GenerationContext, depth: Int): DatatypeType {
        return randomSelection(context.globalSymbolTable.availableDatatypes(context.onDemandIdentifiers))
    }

    fun selectDataStructureType(context: GenerationContext, depth: Int): DataStructureType =
        randomWeightedSelection(
            normaliseWeights(
                listOf<Pair<(GenerationContext, Int) -> DataStructureType, Double>>(
                    this::selectSetType to probabilityManager.setType(),
                    this::selectMultisetType to probabilityManager.multisetType(),
                    this::selectMapType to probabilityManager.mapType(),
                    this::selectSequenceType to probabilityManager.sequenceType(),
                    this::selectStringType to probabilityManager.stringType(),
                ),
            ),
        ).invoke(context, depth)

    fun selectDataStructureTypeWithInnerType(innerType: Type, context: GenerationContext): DataStructureType =
        randomWeightedSelection(
            normaliseWeights(
                listOf(
                    SetType(innerType) to probabilityManager.setType(),
                    SequenceType(innerType) to probabilityManager.sequenceType(),
                    MapType(innerType, selectType(context, 2)) to probabilityManager.mapType(),
                    MultisetType(innerType) to probabilityManager.multisetType(),
                ),
            ),
        )

    @Suppress("UNUSED_PARAMETER")
    private fun selectStringType(context: GenerationContext, depth: Int): StringType = StringType

    private fun selectSetType(context: GenerationContext, depth: Int): SetType =
        SetType(selectType(context, depth + 1))

    private fun selectMultisetType(context: GenerationContext, depth: Int): MultisetType =
        MultisetType(selectType(context, depth + 1))

    private fun selectMapType(context: GenerationContext, depth: Int): MapType =
        MapType(selectType(context, depth + 1), selectType(context, depth + 1))

    private fun selectSequenceType(context: GenerationContext, depth: Int): SequenceType =
        SequenceType(selectType(context, depth + 1))

    fun selectArrayType(context: GenerationContext, depth: Int): ArrayType =
        ArrayType(selectType(context, depth + 1))

    @Suppress("UNUSED_PARAMETER")
    fun selectLiteralType(context: GenerationContext, depth: Int): LiteralType =
        randomWeightedSelection(
            normaliseWeights(
                listOf(
                    IntType to probabilityManager.intType(),
                    BoolType to probabilityManager.boolType(),
                    CharType to probabilityManager.charType(),
                ),
            ),
        )

    fun selectMethodReturnType(context: GenerationContext): List<Type> {
        val numberOfReturns = random.nextInt(MAX_RETURNS)
        return (1..numberOfReturns).map { selectType(context) }
    }

    private fun binaryOperators(operator: KClass<out BinaryOperator>): List<KClass<out BinaryOperator>> =
        if (operator.isSealed) operator.sealedSubclasses.map { clazz: KClass<out BinaryOperator> -> binaryOperators(clazz) }.unionAll().toList() else listOf(operator)

    private fun binaryOperators(): List<BinaryOperator> = BinaryOperator::class.sealedSubclasses
        .map { binaryOperators(it) }.unionAll()
        .mapNotNull { it.objectInstance }

    // selects operator, returning the operator and a selected input type for inner expressions
    fun selectBinaryOperator(context: GenerationContext, targetType: Type): Pair<BinaryOperator, Pair<Type, Type>> = when (targetType) {
        BoolType -> {
            val subclasses = binaryOperators().filter { it !is DataStructureMathematicalOperator && it.outputType(targetType, targetType) == BoolType }
            val selectedIndex = random.nextInt(subclasses.size)

            when (val selectedSubclass = subclasses[selectedIndex]) {
                is BooleanBinaryOperator -> Pair(selectedSubclass, Pair(BoolType, BoolType))
                is ComparisonBinaryOperator -> Pair(selectedSubclass, Pair(IntType, IntType))
                is DataStructureMembershipOperator -> {
                    val dataStructureType = selectDataStructureType(context, 1)
                    val keyType = when (dataStructureType) {
                        is SetType -> dataStructureType.innerType
                        is MultisetType -> dataStructureType.innerType
                        is SequenceType -> dataStructureType.innerType
                        is MapType -> dataStructureType.keyType
                    }

                    Pair(selectedSubclass, Pair(keyType, dataStructureType))
                }

                is DataStructureBinaryOperator -> {
                    var dataStructureType: DataStructureType
                    do {
                        dataStructureType = selectDataStructureType(context, 1)
                    } while (!selectedSubclass.supportsInput(dataStructureType, dataStructureType))
                    Pair(selectedSubclass, Pair(dataStructureType, dataStructureType))
                }

                else -> throw UnsupportedOperationException("$selectedSubclass not a valid boolean-type binary operator")
            }
        }

        is SetType, is MultisetType -> {
            val subclassInstances = BinaryOperator.DataStructureMathematicalOperator::class.sealedSubclasses
                .mapNotNull { it.objectInstance }
            val selectedIndex = random.nextInt(subclassInstances.size)
            Pair(subclassInstances[selectedIndex], Pair(targetType, targetType))
        }

        is MapType, is SequenceType -> Pair(UnionOperator, Pair(targetType, targetType))

        IntType -> {
            val subclassInstances = BinaryOperator.MathematicalBinaryOperator::class.sealedSubclasses
                .mapNotNull { it.objectInstance }
                .filter { it.supportsInput(targetType, targetType) }
            val selectedIndex = random.nextInt(subclassInstances.size)
            Pair(subclassInstances[selectedIndex], Pair(targetType, targetType))
        }

        else -> throw UnsupportedOperationException("Target type $targetType not supported for binary operations")
    }

    fun selectUnaryOperator(targetType: Type): UnaryOperator =
        when (targetType) {
            BoolType -> NotOperator
            IntType -> NegationOperator
            else -> throw InvalidInputException("Target type $targetType not supported for unary operations")
        }

    fun selectStatementType(context: GenerationContext, methodCalls: Boolean = true): StatementType {
        val ifStatementProbability = if (context.statementDepth < MAX_STATEMENT_DEPTH) min(probabilityManager.ifStatement() / context.statementDepth, 0.3) else 0.0
        val matchProbability = if (context.statementDepth < MAX_STATEMENT_DEPTH) min(probabilityManager.matchStatement() / context.statementDepth, 0.3) else 0.0
        val whileStatementProbability = if (context.statementDepth == 1) probabilityManager.whileStatement() else 0.0

        val forLoopProbability: Double = if (context.unsupportedFeatures.contains(ProbabilityManager::forLoopStatement)) {
            0.0
        } else if (context.statementDepth == 1) {
            probabilityManager.forLoopStatement()
        } else {
            0.0
        }

        val forallProbability = if (context.unsupportedFeatures.contains(ProbabilityManager::forallStatement))
        {
            0.0
        } else if (context.statementDepth == 1) { // if statement due to DAFNY#3969
            min(probabilityManager.forallStatement(), 0.2)
        }
        else {
            0.0
        }

        val methodCallProbability = if (methodCalls) {
            val methodCallProbability = min(probabilityManager.methodCall(), 0.1)
            if (context.methodContext == null) methodCallProbability else methodCallProbability / 5 // avoid large call chains
        } else {
            0.0
        }

        val selection = listOf(
            ASSERT to probabilityManager.assertStatement(),
            IF to ifStatementProbability,
            FOR_LOOP to forLoopProbability,
            FORALL to forallProbability,
            WHILE to whileStatementProbability,
            METHOD_CALL to methodCallProbability,
            StatementType.MATCH to matchProbability,
            MAP_ASSIGN to min(probabilityManager.mapAssign(), 0.2),
            ASSIGN to probabilityManager.assignStatement(),
            MULTI_ASSIGN to probabilityManager.multiAssignStatement(),
            CLASS_INSTANTIATION to min(probabilityManager.classInstantiation(), 0.1),
        )

        return randomWeightedSelection(normaliseWeights(selection))
    }

    fun selectFieldIsConstant(): Boolean = withProbability(probabilityManager.constField())

    @Suppress("UNUSED_PARAMETER")
    fun selectAssignType(context: GenerationContext): AssignType =
        randomWeightedSelection(
            normaliseWeights(
                listOf(
                    AssignType.IDENTIFIER to probabilityManager.assignIdentifier(),
                    ARRAY_INDEX to probabilityManager.assignArrayIndex(),
                ),
            ),
        )

    private fun isBinaryType(targetType: Type): Boolean =
        targetType !is ArrayType && targetType !is ClassType && targetType !is TraitType &&
            targetType !is TopLevelDatatypeType && targetType != CharType

    private fun isAssignType(targetType: Type): Boolean =
        targetType is MapType || targetType is MultisetType || targetType is SequenceType ||
            (targetType is DatatypeType && targetType.constructor.fields.isNotEmpty())

    fun selectExpressionType(targetType: Type, context: GenerationContext, identifier: Boolean = true): ExpressionType {
        val binaryProbability =
            if (isBinaryType(targetType) && context.expressionDepth < MAX_EXPRESSION_DEPTH) probabilityManager.binaryExpression() / context.expressionDepth else 0.0
        val unaryProbability = if (isUnaryType(targetType)) probabilityManager.unaryExpression() / context.expressionDepth else 0.0
        val modulusProbability = if (targetType == IntType) probabilityManager.modulusExpression() else 0.0
        val multisetConversionProbability =
            if (targetType is MultisetType && targetType.innerType !is ArrayType /* due to resolver errors*/) probabilityManager.multisetConversion() else 0.0
        val functionCallProbability =
            if (!targetType.hasHeapType() && context.onDemandIdentifiers && context.functionCalls) probabilityManager.functionCall() / context.expressionDepth else 0.0
        val ternaryProbability = if (context.expressionDepth < MAX_EXPRESSION_DEPTH) probabilityManager.ternary() / context.expressionDepth else 0.0
        val matchProbability =
            if (context.expressionDepth == 1 && context.statementDepth == 1 /*< MAX_EXPRESSION_DEPTH*/ && context.globalSymbolTable.hasAvailableDatatypes(context.onDemandIdentifiers) && !targetType.hasHeapType()) {
                probabilityManager.matchExpression() / context.expressionDepth
            } else {
                0.0
            }
        val assignProbability =
            if (isAssignType(targetType) && (context.onDemandIdentifiers || context.symbolTable.hasType(targetType))) probabilityManager.assignExpression() / context.expressionDepth else 0.0
        val indexProbability =
            if (identifier && (targetType !is DatatypeType)) probabilityManager.indexExpression() / context.expressionDepth else 0.0

        val identifierProbability = if (identifier) probabilityManager.identifier() else 0.0
        val literalProbability =
            if (isLiteralType(targetType)) {
                if (identifier) probabilityManager.literal() / 3 else probabilityManager.literal()
            } else {
                0.0
            }
        val constructorProbability =
            if (((targetType is ArrayType || targetType is ClassType || targetType is TraitType) && context.effectfulStatements && context.expressionDepth == 1) ||
                (targetType is TopLevelDatatypeType || targetType is DataStructureType)
            ) {
                if (identifier) probabilityManager.constructor() / 3 else probabilityManager.constructor()
            } else {
                0.0
            }
        val comprehensionProbability =
            if (!targetType.hasHeapType() && targetType !is MultisetType && targetType is DataStructureType && targetType.innerType != BoolType) probabilityManager.comprehension() else 0.0

        val selection = listOf(
            COMPREHENSION to comprehensionProbability,
            CONSTRUCTOR to constructorProbability,
            LITERAL to literalProbability,
            TERNARY to ternaryProbability,
            MATCH to matchProbability,
            IDENTIFIER to identifierProbability,
            MULTISET_CONVERSION to multisetConversionProbability,
            FUNCTION_METHOD_CALL to functionCallProbability,
            UNARY to unaryProbability,
            BINARY to binaryProbability,
            MODULUS to modulusProbability,
            INDEX to indexProbability,
            INDEX_ASSIGN to assignProbability,
        )

        return randomWeightedSelection(normaliseWeights(selection))
    }

    fun selectIndexType(context: GenerationContext, targetType: Type): IndexType = randomWeightedSelection(
        normaliseWeights(
            listOf(
                ARRAY to if (context.onDemandIdentifiers) probabilityManager.arrayIndexType() else 0.0,
                MAP to probabilityManager.mapIndexType(),
                MULTISET to if (targetType == IntType) probabilityManager.multisetIndexType() else 0.0,
                SEQUENCE to probabilityManager.sequenceIndexType(),
                STRING to if (targetType == CharType) probabilityManager.stringIndexType() else 0.0,
                DATATYPE to probabilityManager.datatypeIndexType(),
            ),
        ),
    )

    fun selectArrayInitType(context: GenerationContext, targetType: ArrayType): ArrayInitType = randomWeightedSelection(
        normaliseWeights(
            listOf(
                DEFAULT to probabilityManager.arrayInitDefault(),
                VALUE to probabilityManager.arrayInitValues(),
                ArrayInitType.COMPREHENSION to if (!targetType.internalType.hasHeapType()) probabilityManager.arrayInitComprehension() else 0.0,
            ),
        ),
    )

    fun <T> normaliseWeights(items: List<Pair<T, Double>>): List<Pair<T, Double>> {
        var weightSum = 0.0

        items.forEach { item -> weightSum += item.second }
        return items.mapNotNull { item -> if (item.second == 0.0) null else Pair(item.first, item.second / weightSum) }
    }

    fun selectComprehensionConditionWithIntRange() = withProbability(probabilityManager.comprehensionConditionIntRange())

    fun selectNumberOfComprehensionIdentifiers() = random.nextInt(1, probabilityManager.comprehensionIdentifiers())

    fun <T> randomSelection(items: List<T>): T = if (items.size == 1) items[0] else items[random.nextInt(items.size)]

    fun <T> randomWeightedSelection(items: List<Pair<T, Double>>): T {
        val probability = random.nextFloat()
        var wsum = 0.0

        for ((item, w) in items) {
            wsum += w

            if (probability < wsum) return item
        }

        return items[0].first // default
    }

    fun withProbability(probability: Double): Boolean = random.nextFloat() < probability

    fun selectAssertStatementDatastructureType(): Boolean = withProbability(probabilityManager.datatstructureType())

    fun selectNumberOfParameters(): Int = random.nextInt(0, MAX_PARAMETERS)

    fun selectArrayLength(): Int = random.nextInt(MIN_ARRAY_LENGTH, MAX_ARRAY_LENGTH)

    fun selectNumberOfFields() = random.nextInt(0, MAX_FIELDS)

    fun selectNumberOfGlobalFields() = random.nextInt(0, MAX_GLOBAL_FIELDS)

    fun selectNumberOfConstructorFields(context: GenerationContext) =
        randomWeightedSelection(
            normaliseWeights(
                listOf(
                    1 to 0.4,
                    2 to 0.2,
                    3 to 0.2 / context.expressionDepth,
                    4 to 0.1 / context.expressionDepth,
                    5 to 0.1 / context.expressionDepth,
                ),
            ),
        )

    fun selectMakeDatatypeInductive() = withProbability(0.4)

    fun selectNumberOfDatatypeConstructors() =
        randomWeightedSelection(normaliseWeights(listOf(1 to 0.25, 2 to 0.4, 3 to 0.25, 4 to 0.1)))

    fun selectNumberOfDatatypeFields() =
        randomWeightedSelection(normaliseWeights(listOf(0 to 0.1, 1 to 0.2, 2 to 0.2, 3 to 0.2, 4 to 0.1, 5 to 0.1)))

    fun selectNumberOfFunctionMethods() = random.nextInt(0, MAX_FUNCTION_METHODS)

    fun selectNumberOfMethods() = random.nextInt(0, MAX_METHODS)

    fun selectNumberOfTraits() = if (unsupportedFeatures.contains(ProbabilityManager::traitType)) {
        0
    } else {
        randomWeightedSelection(listOf(0 to 0.4, 1 to 0.4, 2 to 0.15, 3 to 0.05))
    }

    fun selectNumberOfTraitInherits() = if (unsupportedFeatures.contains(ProbabilityManager::traitType)) {
        0
    } else{
        random.nextInt(0, MAX_TRAIT_INHERITS)
    }

    fun selectNumberOfAssigns(): Int = random.nextInt(1, probabilityManager.maxNumberOfAssigns() + 1) // include max of probability manager with +1

    fun selectDecimalLiteral(): Int = random.nextInt(0, MAX_INT_VALUE)

    fun selectStringLength(): Int = random.nextInt(1, MAX_STRING_LENGTH)

    fun selectCharacter(): Char = random.nextInt('a'.code, 'z'.code).toChar()

    fun selectBoolean(): Boolean = random.nextBoolean()

    fun selectInt(min: Int, max: Int): Int = if (min == max) min else random.nextInt(min, max)

    fun selectMutateVerificationCondition(): Boolean = withProbability(probabilityManager.mutateVerificationCondition())

    fun selectMutateAssertFalse(): Boolean = withProbability(probabilityManager.mutateAssertFalse())

    fun methodStatements(): Int = probabilityManager.methodStatements()
    fun ifBranchStatements(): Int = probabilityManager.ifBranchStatements()
    fun whileBodyStatements(): Int = probabilityManager.whileBodyStatements()
    fun forLoopBodyStatements(): Int = probabilityManager.forLoopBodyStatements()
    fun mainFunctionStatements(): Int = probabilityManager.mainFunctionStatements()
    fun matchStatements(): Int = probabilityManager.matchStatements()

    companion object {
        private const val MAX_EXPRESSION_DEPTH = 3
        private const val MAX_STATEMENT_DEPTH = 3
        private const val MIN_ARRAY_LENGTH = 1
        private const val MAX_ARRAY_LENGTH = 30
        private const val MAX_INT_VALUE = 1000
        private const val MAX_STRING_LENGTH = 10
        private const val MAX_PARAMETERS = 5
        private const val MAX_RETURNS = 5
        private const val MAX_FIELDS = 3
        private const val MAX_GLOBAL_FIELDS = 30
        private const val MAX_FUNCTION_METHODS = 3
        private const val MAX_METHODS = 3
        private const val MAX_TRAIT_INHERITS = 2

        private val LITERAL_TYPES = listOf(IntType, BoolType, CharType)
        private fun isLiteralType(type: Type) = type in LITERAL_TYPES
    }
}
