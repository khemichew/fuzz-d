package fuzzd.generator

import fuzzd.generator.ast.ClassAST
import fuzzd.generator.ast.DafnyAST
import fuzzd.generator.ast.ExpressionAST
import fuzzd.generator.ast.ExpressionAST.ArrayIndexAST
import fuzzd.generator.ast.ExpressionAST.ArrayInitAST
import fuzzd.generator.ast.ExpressionAST.BinaryExpressionAST
import fuzzd.generator.ast.ExpressionAST.BooleanLiteralAST
import fuzzd.generator.ast.ExpressionAST.CharacterLiteralAST
import fuzzd.generator.ast.ExpressionAST.ClassInstanceAST
import fuzzd.generator.ast.ExpressionAST.ClassInstantiationAST
import fuzzd.generator.ast.ExpressionAST.FunctionMethodCallAST
import fuzzd.generator.ast.ExpressionAST.IdentifierAST
import fuzzd.generator.ast.ExpressionAST.IntegerLiteralAST
import fuzzd.generator.ast.ExpressionAST.LiteralAST
import fuzzd.generator.ast.ExpressionAST.MapConstructorAST
import fuzzd.generator.ast.ExpressionAST.MapIndexAST
import fuzzd.generator.ast.ExpressionAST.MapIndexAssignAST
import fuzzd.generator.ast.ExpressionAST.NonVoidMethodCallAST
import fuzzd.generator.ast.ExpressionAST.RealLiteralAST
import fuzzd.generator.ast.ExpressionAST.SetDisplayAST
import fuzzd.generator.ast.ExpressionAST.StringLiteralAST
import fuzzd.generator.ast.ExpressionAST.TernaryExpressionAST
import fuzzd.generator.ast.ExpressionAST.UnaryExpressionAST
import fuzzd.generator.ast.FunctionMethodAST
import fuzzd.generator.ast.FunctionMethodSignatureAST
import fuzzd.generator.ast.MainFunctionAST
import fuzzd.generator.ast.MethodAST
import fuzzd.generator.ast.MethodSignatureAST
import fuzzd.generator.ast.SequenceAST
import fuzzd.generator.ast.StatementAST
import fuzzd.generator.ast.StatementAST.AssignmentAST
import fuzzd.generator.ast.StatementAST.BreakAST
import fuzzd.generator.ast.StatementAST.CounterLimitedWhileLoopAST
import fuzzd.generator.ast.StatementAST.DeclarationAST
import fuzzd.generator.ast.StatementAST.IfStatementAST
import fuzzd.generator.ast.StatementAST.MultiDeclarationAST
import fuzzd.generator.ast.StatementAST.PrintAST
import fuzzd.generator.ast.StatementAST.VoidMethodCallAST
import fuzzd.generator.ast.StatementAST.WhileLoopAST
import fuzzd.generator.ast.TopLevelAST
import fuzzd.generator.ast.TraitAST
import fuzzd.generator.ast.Type
import fuzzd.generator.ast.Type.BoolType
import fuzzd.generator.ast.Type.CharType
import fuzzd.generator.ast.Type.ConstructorType
import fuzzd.generator.ast.Type.ConstructorType.ArrayType
import fuzzd.generator.ast.Type.IntType
import fuzzd.generator.ast.Type.LiteralType
import fuzzd.generator.ast.Type.MapType
import fuzzd.generator.ast.Type.RealType
import fuzzd.generator.ast.Type.SetType
import fuzzd.generator.ast.error.IdentifierOnDemandException
import fuzzd.generator.ast.error.MethodOnDemandException
import fuzzd.generator.ast.identifier_generator.NameGenerator.ClassNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.ControlFlowGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.FieldNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.FunctionMethodNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.MethodNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.ParameterNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.ReturnsNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.TraitNameGenerator
import fuzzd.generator.ast.operators.BinaryOperator.AdditionOperator
import fuzzd.generator.ast.operators.BinaryOperator.GreaterThanEqualOperator
import fuzzd.generator.context.GenerationContext
import fuzzd.generator.selection.ExpressionType
import fuzzd.generator.selection.ExpressionType.BINARY
import fuzzd.generator.selection.ExpressionType.CONSTRUCTOR
import fuzzd.generator.selection.ExpressionType.FUNCTION_METHOD_CALL
import fuzzd.generator.selection.ExpressionType.IDENTIFIER
import fuzzd.generator.selection.ExpressionType.LITERAL
import fuzzd.generator.selection.ExpressionType.TERNARY
import fuzzd.generator.selection.ExpressionType.UNARY
import fuzzd.generator.selection.SelectionManager
import fuzzd.generator.selection.StatementType
import fuzzd.generator.selection.StatementType.ASSIGN
import fuzzd.generator.selection.StatementType.CLASS_INSTANTIATION
import fuzzd.generator.selection.StatementType.DECLARATION
import fuzzd.generator.selection.StatementType.IF
import fuzzd.generator.selection.StatementType.METHOD_CALL
import fuzzd.generator.selection.StatementType.PRINT
import fuzzd.generator.selection.StatementType.WHILE
import fuzzd.generator.symbol_table.DependencyTable
import fuzzd.generator.symbol_table.FunctionSymbolTable
import fuzzd.generator.symbol_table.SymbolTable
import fuzzd.utils.unionAll

// import fuzzd.utils.SAFE_ADDITION_REAL
// import fuzzd.utils.SAFE_DIVISION_REAL
// import fuzzd.utils.SAFE_MULTIPLY_REAL
// import fuzzd.utils.SAFE_SUBTRACT_CHAR
// import fuzzd.utils.SAFE_SUBTRACT_REAL

class Generator(
    private val selectionManager: SelectionManager,
    private val instrument: Boolean = false,
) : ASTGenerator {
    private val classNameGenerator = ClassNameGenerator()
    private val fieldNameGenerator = FieldNameGenerator()
    private val functionMethodNameGenerator = FunctionMethodNameGenerator()
    private val methodNameGenerator = MethodNameGenerator()
    private val traitNameGenerator = TraitNameGenerator()
    private val methodCallTable = DependencyTable<MethodSignatureAST>()

    private val controlFlowGenerator = ControlFlowGenerator()

    /* ==================================== TOP LEVEL ==================================== */

    override fun generate(): DafnyAST {
        val context = GenerationContext(FunctionSymbolTable())

        val globalFields = (1..selectionManager.selectNumberOfGlobalFields()).map { generateField(context) }.toSet()
        val globalStateClass = ClassAST.builder().withName(GLOBAL_STATE).withFields(globalFields).build()
        context.setGlobalState(globalStateClass)

        val mainFunction = generateMainFunction(context)
        val ast = mutableListOf<TopLevelAST>()

        val methodBodyQueue = context.functionSymbolTable.methods().toMutableList()
        val visitedMethods = mutableSetOf<MethodAST>()

        while (methodBodyQueue.isNotEmpty()) {
            val method = methodBodyQueue.removeFirst()

            if (method in visitedMethods) continue
            visitedMethods.add(method)

            val functionContext = GenerationContext(
                context.functionSymbolTable,
                methodContext = method.signature,
            ).setGlobalState(context.globalState())
            val body = generateMethodBody(functionContext, method)
            method.setBody(body)

            methodBodyQueue.addAll(context.functionSymbolTable.methods() subtract methodBodyQueue.toSet() subtract visitedMethods)
        }

        ast.add(context.globalState())
        ast.addAll(context.functionSymbolTable.functionMethods())
        ast.addAll(context.functionSymbolTable.methods())
        ast.addAll(context.functionSymbolTable.traits())
        ast.addAll(context.functionSymbolTable.classes())
        ast.add(mainFunction)

        return DafnyAST(ast)
    }

    override fun generateMainFunction(context: GenerationContext): MainFunctionAST {
        val (globalStateParams, globalStateDeps) = context.globalState().fields.map {
            val exprContext = context.increaseExpressionDepth().disableOnDemand()
            if (it.type() is LiteralType) {
                Pair(generateLiteralForType(exprContext, it.type() as LiteralType), emptyList<StatementAST>())
            } else {
                val ident = IdentifierAST(context.identifierNameGenerator.newValue(), it.type())
                val decl = DeclarationAST(
                    ident,
                    generateExpressionFromType(CONSTRUCTOR, context.disableOnDemand(), it.type()),
                )
                context.symbolTable.add(ident)
                Pair(ident, listOf(decl))
            }
        }.fold(Pair(emptyList<ExpressionAST>(), emptyList<StatementAST>())) { acc, l ->
            Pair(acc.first + l.first, acc.second + l.second)
        }

        val globalStateIdentifier = ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE)
        val globalStateDecl =
            DeclarationAST(globalStateIdentifier, ClassInstantiationAST(context.globalState(), globalStateParams))

        context.symbolTable.add(globalStateIdentifier)

        val body = generateSequence(context)
        val prints = generateChecksum(context)
        return MainFunctionAST(SequenceAST(globalStateDeps + globalStateDecl + body.statements + prints))
    }

    override fun generateChecksum(context: GenerationContext): List<PrintAST> {
        val fields = context.symbolTable.withType(IntType) + context.symbolTable.withType(BoolType)
        return fields.map { field -> PrintAST(field) }
    }

    override fun generateTrait(context: GenerationContext): TraitAST {
        val fields = (1..selectionManager.selectNumberOfFields()).map { generateField(context) }.toSet()

        val functionMethods =
            (1..selectionManager.selectNumberOfFunctionMethods()).map { generateFunctionMethodSignature(context) }
                .toSet()

        val methods = (1..selectionManager.selectNumberOfMethods()).map { generateMethodSignature(context) }.toSet()

        val numberOfInherits = selectionManager.selectNumberOfTraitInherits()
        val selectedTraits = selectTraits(context, numberOfInherits)

        val trait = TraitAST(traitNameGenerator.newValue(), selectedTraits, functionMethods, methods, fields)

        context.functionSymbolTable.addTrait(trait)

        return trait
    }

    private fun selectTraits(context: GenerationContext, n: Int): Set<TraitAST> {
        val traits = context.functionSymbolTable.traits().toMutableList()
        val selectedTraits = mutableSetOf<TraitAST>()

        for (i in 1..n) {
            if (traits.isEmpty()) {
                traits.add(generateTrait(context))
            }

            val selected = selectionManager.randomSelection(traits)
            traits.remove(selected)
            selectedTraits.add(selected)
        }

        return selectedTraits
    }

    override fun generateClass(context: GenerationContext): ClassAST {
        val classContext =
            GenerationContext(FunctionSymbolTable(context.functionSymbolTable.topLevel())).setGlobalState(context.globalState())

        // get traits
        val numberOfTraits = selectionManager.selectNumberOfTraits()
        val selectedTraits = selectTraits(context, numberOfTraits)

        // generate fields
        val additionalFields = (1..selectionManager.selectNumberOfFields()).map { generateField(classContext) }.toSet()
        val requiredFields = selectedTraits.map { it.fields() }.unionAll()

        classContext.symbolTable.addAll(additionalFields union requiredFields)

        // generate function methods
        val additionalFunctionMethods =
            (1..selectionManager.selectNumberOfFunctionMethods()).map { generateFunctionMethod(classContext) }.toSet()
        val requiredFunctionMethods = selectedTraits.map { it.functionMethods() }.unionAll()
            .map { signature -> generateFunctionMethod(classContext, signature) }.toSet()
        val functionMethods = requiredFunctionMethods union additionalFunctionMethods

        // generate methods
        val additionalMethods =
            (1..selectionManager.selectNumberOfMethods()).map { generateMethod(classContext) }.toSet()
        val requiredMethods =
            selectedTraits.map { it.methods() }.unionAll().map { signature -> generateMethod(signature) }.toSet()
        val methods = requiredMethods union additionalMethods
        methods.forEach { method ->
            method.setBody(
                generateMethodBody(
                    GenerationContext(
                        classContext.functionSymbolTable,
                        symbolTable = SymbolTable(classContext.symbolTable),
                        methodContext = method.signature,
                    ).setGlobalState(context.globalState()),
                    method,
                ),
            )
        }

        val clazz = ClassAST(
            classNameGenerator.newValue(),
            selectedTraits,
            functionMethods,
            methods,
            additionalFields,
            requiredFields,
        )

        // update symbol table with on-demand methods & classes generated into local global context
        context.functionSymbolTable.addFunctionMethods(classContext.functionSymbolTable.functionMethods() subtract functionMethods)
        context.functionSymbolTable.addMethods(classContext.functionSymbolTable.methods() subtract methods)
        context.functionSymbolTable.addClass(clazz)

        return clazz
    }

    override fun generateField(context: GenerationContext) =
        IdentifierAST(fieldNameGenerator.newValue(), selectionManager.selectType(context), initialised = true)

    override fun generateFunctionMethod(context: GenerationContext, targetType: Type?): FunctionMethodAST {
        val functionMethodSignature = generateFunctionMethodSignature(context, targetType)
        return generateFunctionMethod(context, functionMethodSignature)
    }

    private fun generateFunctionMethod(
        context: GenerationContext,
        signature: FunctionMethodSignatureAST,
    ): FunctionMethodAST {
        val functionContext = GenerationContext(
            context.functionSymbolTable,
            onDemandIdentifiers = false,
        ).setGlobalState(context.globalState())
        signature.params.forEach { param -> functionContext.symbolTable.add(param) }

        val body = generateExpression(functionContext, signature.returnType)
        val functionMethodAST = FunctionMethodAST(signature, body)

        context.functionSymbolTable.addFunctionMethod(functionMethodAST)

        return functionMethodAST
    }

    override fun generateFunctionMethodSignature(
        context: GenerationContext,
        targetType: Type?,
    ): FunctionMethodSignatureAST {
        val name = functionMethodNameGenerator.newValue()
        val numberOfParameters = selectionManager.selectNumberOfParameters()

        val parameterNameGenerator = ParameterNameGenerator()
        val returnType = targetType ?: generateType(context.disableOnDemand())
        var parameters = (1..numberOfParameters).map {
            IdentifierAST(
                parameterNameGenerator.newValue(),
                generateType(context.disableOnDemand()),
                mutable = false,
                initialised = true,
            )
        }

        return FunctionMethodSignatureAST(
            name,
            returnType,
            parameters + listOf(ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE)),
        )
    }

    override fun generateMethod(context: GenerationContext): MethodAST {
        val method = MethodAST(generateMethodSignature(context))
        context.functionSymbolTable.addMethod(method)
        return method
    }

    private fun generateMethod(signature: MethodSignatureAST): MethodAST = MethodAST(signature)

    override fun generateMethodSignature(context: GenerationContext): MethodSignatureAST {
        val name = methodNameGenerator.newValue()
        val returnType = selectionManager.selectMethodReturnType(context)

        val returnsNameGenerator = ReturnsNameGenerator()
        val returns = returnType.map { t -> IdentifierAST(returnsNameGenerator.newValue(), t, initialised = false) }

        val numberOfParameters = selectionManager.selectNumberOfParameters()
        val parameterNameGenerator = ParameterNameGenerator()
        val parameters = (1..numberOfParameters).map {
            IdentifierAST(
                parameterNameGenerator.newValue(),
                generateType(context, literalOnly = true),
                mutable = false,
                initialised = true,
            )
        }

        return MethodSignatureAST(
            name,
            parameters + listOf(ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE)),
            returns,
        )
    }

    private fun generateMethodBody(context: GenerationContext, method: MethodAST): SequenceAST {
        method.params().forEach { param -> context.symbolTable.add(param) }
        method.returns().forEach { r -> context.symbolTable.add(r) }

        val body = generateSequence(context, maxStatements = 10)

        val returnAssigns = method.returns().map { r ->
            val expr = generateExpression(context.disableOnDemand(), r.type())
            AssignmentAST(r, expr)
        }

        return body.addStatements(context.clearDependentStatements() + returnAssigns)
    }

    override fun generateSequence(context: GenerationContext, maxStatements: Int): SequenceAST {
        val n = selectionManager.selectSequenceLength(maxStatements)

        val statements = (1..n).map { generateStatement(context) }

        val allStatements = context.clearDependentStatements() + statements

        return SequenceAST(allStatements)
    }

    /* ==================================== STATEMENTS ==================================== */

    override fun generateStatement(context: GenerationContext): StatementAST = try {
        generateStatementFromType(selectionManager.selectStatementType(context), context)
    } catch (e: MethodOnDemandException) {
        generateStatementFromType(
            selectionManager.selectStatementType(context, methodCalls = false),
            context,
        )
    }

    private fun generateStatementFromType(
        type: StatementType,
        context: GenerationContext,
    ): StatementAST = when (type) {
        ASSIGN -> generateAssignmentStatement(context)
        CLASS_INSTANTIATION -> generateClassInstantiation(context)
        DECLARATION -> generateDeclarationStatement(context)
        IF -> generateIfStatement(context)
        METHOD_CALL -> generateMethodCall(context)
        PRINT -> generatePrintStatement(context)
        WHILE -> generateWhileStatement(context)
    }

    override fun generateIfStatement(context: GenerationContext): IfStatementAST {
        val condition = generateExpression(context, BoolType)

        val ifPrint = PrintAST(StringLiteralAST(controlFlowGenerator.newValue()))
        val elsePrint = PrintAST(StringLiteralAST(controlFlowGenerator.newValue()))

        val ifBranch = generateSequence(context.increaseStatementDepth())
        val elseBranch = generateSequence(context.increaseStatementDepth())

        return IfStatementAST(
            condition,
            SequenceAST((if (instrument) listOf(ifPrint) else emptyList<StatementAST>()) + ifBranch.statements),
            SequenceAST((if (instrument) listOf(elsePrint) else emptyList<StatementAST>()) + elseBranch.statements),
        )
    }

    override fun generateWhileStatement(context: GenerationContext): WhileLoopAST {
        val counterIdentifierName = context.loopCounterGenerator.newValue()
        val counterIdentifier = IdentifierAST(counterIdentifierName, IntType)
        val counterInitialisation = DeclarationAST(counterIdentifier, IntegerLiteralAST(0))

        var condition: ExpressionAST
        do {
            condition = generateExpression(context, BoolType)
        } while (condition == BooleanLiteralAST(false)) // we don't want while(false) explicitly

        val counterTerminationCheck = IfStatementAST(
            BinaryExpressionAST(
                counterIdentifier,
                GreaterThanEqualOperator,
                IntegerLiteralAST(
                    DAFNY_MAX_LOOP_COUNTER,
                ),
            ),
            SequenceAST(listOf(BreakAST)),
            null,
        )

        val counterUpdate = AssignmentAST(
            counterIdentifier,
            BinaryExpressionAST(counterIdentifier, AdditionOperator, IntegerLiteralAST(1)),
        )

        val whileBody = generateSequence(context.increaseStatementDepth())

        return CounterLimitedWhileLoopAST(
            counterInitialisation,
            counterTerminationCheck,
            counterUpdate,
            condition,
            whileBody,
        )
    }

    override fun generatePrintStatement(context: GenerationContext): PrintAST {
        val targetType = generateType(context, literalOnly = true)
        val safeExpr = generateExpression(context, targetType)
        return PrintAST(safeExpr)
    }

    override fun generateDeclarationStatement(context: GenerationContext): DeclarationAST {
        val targetType = generateType(context)
        return generateDeclarationStatementForType(context, targetType, false)
    }

    private fun generateDeclarationStatementForType(
        context: GenerationContext,
        targetType: Type,
        baseExpression: Boolean,
    ): DeclarationAST {
        val expr = if (baseExpression) {
            generateBaseExpressionForType(context, targetType)
        } else {
            generateExpression(
                context,
                targetType,
            )
        }

        val identifierName = context.identifierNameGenerator.newValue()
        val identifier = IdentifierAST(identifierName, targetType, initialised = true)

        context.symbolTable.add(identifier)

        return DeclarationAST(identifier, expr)
    }

    override fun generateAssignmentStatement(context: GenerationContext): AssignmentAST {
        val targetType = generateType(context, literalOnly = true)
        val identifier = if (selectionManager.randomWeightedSelection(listOf(true to 0.8, false to 0.2))) {
            generateIdentifier(context, targetType, mutableConstraint = true, initialisedConstraint = false)
        } else {
            generateArrayIndex(context, targetType)
        }

        context.symbolTable.add(identifier)

        val expr = generateExpression(context, targetType)

        identifier.initialise()

        return AssignmentAST(identifier, expr)
    }

    override fun generateClassInstantiation(context: GenerationContext): DeclarationAST {
        // on demand create class if one doesn't exist
        if (!context.functionSymbolTable.hasClasses() || selectionManager.generateNewClass(context)) {
            generateClass(context)
        }

        val selectedClass = selectionManager.randomSelection(context.functionSymbolTable.classes().toList())
        val requiredFields = selectedClass.constructorFields

        val params = requiredFields.map { field ->
            generateExpression(
                context.increaseExpressionDepth(),
                field.type(),
            )
        }
        val ident = ClassInstanceAST(selectedClass, context.identifierNameGenerator.newValue())

        context.symbolTable.add(ident)

        return DeclarationAST(ident, ClassInstantiationAST(selectedClass, params))
    }

    @Throws(MethodOnDemandException::class)
    override fun generateMethodCall(context: GenerationContext): StatementAST {
        // get callable methods
        val methods = (
            context.functionSymbolTable.methods().map { it.signature } +
                context.symbolTable.classInstances().map { it.methods }.unionAll()
            )
            .filter { method ->
                context.methodContext == null || methodCallTable.canUseDependency(
                    context.methodContext,
                    method,
                )
            }

        // no support for on demand method generation within methods
        if (methods.isEmpty() && context.methodContext != null) {
            throw MethodOnDemandException("Callable method unavailable for context ${context.methodContext.name}")
        }

        // generate new method if a callable method doesn't exist
        val method: MethodSignatureAST
        if (methods.isEmpty() || selectionManager.generateNewMethod(context)) {
            method = generateMethod(context).signature
        } else {
            method = selectionManager.randomSelection(methods)
        }

        val params = method.params.subList(0, method.params.size - 1)
            .map { param -> generateExpression(context, param.type()) }

        // add call for method context
        if (context.methodContext != null) {
            methodCallTable.addDependency(method, context.methodContext)
        }

        val returns = method.returns
        val stateParam = ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE)

        return if (returns.isEmpty()) {
            // void method type
            VoidMethodCallAST(method, params + stateParam)
        } else {
            // non-void method type
            val idents = returns.map { r -> IdentifierAST(context.identifierNameGenerator.newValue(), r.type()) }
            idents.forEach { ident -> context.symbolTable.add(ident) }
            MultiDeclarationAST(idents, listOf(NonVoidMethodCallAST(method, params + stateParam)))
        }
    }

    /* ==================================== EXPRESSIONS ==================================== */

    override fun generateExpression(context: GenerationContext, targetType: Type): ExpressionAST {
        return try {
            generateExpressionFromType(
                selectionManager.selectExpressionType(targetType, context),
                context,
                targetType,
            )
        } catch (e: IdentifierOnDemandException) {
            generateExpressionFromType(
                selectionManager.selectExpressionType(targetType, context, identifier = false),
                context,
                targetType,
            )
        }
    }

    private fun generateExpressionFromType(
        exprType: ExpressionType,
        context: GenerationContext,
        targetType: Type,
    ): ExpressionAST = when (exprType) {
        CONSTRUCTOR -> generateConstructorForType(context, targetType)
        UNARY -> generateUnaryExpression(context, targetType)
        BINARY -> generateBinaryExpression(context, targetType)
        TERNARY -> generateTernaryExpression(context, targetType)
        FUNCTION_METHOD_CALL -> generateFunctionMethodCall(context, targetType)
        IDENTIFIER -> generateIdentifier(context, targetType, initialisedConstraint = true)
        LITERAL -> generateLiteralForType(context, targetType as LiteralType)
    }

    override fun generateFunctionMethodCall(
        context: GenerationContext,
        targetType: Type,
    ): FunctionMethodCallAST {
        val callable = callableFunctionMethods(context, targetType)
        val functionMethod = if (callable.isEmpty()) {
            generateFunctionMethod(context, targetType).signature
        } else {
            selectionManager.randomSelection(callable)
        }

        val arguments = functionMethod.params.subList(0, functionMethod.params.size - 1).map { param ->
            generateExpression(context.increaseExpressionDepth(), param.type())
        }

        return FunctionMethodCallAST(
            functionMethod,
            arguments + ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE),
        )
    }

    private fun callableFunctionMethods(
        context: GenerationContext,
        targetType: Type,
    ): List<FunctionMethodSignatureAST> =
        functionMethodsWithType(context, targetType)
            .filter { fm ->
                !fm.params
                    .map { p -> p.type() }
                    .any { t -> t is ConstructorType && !context.symbolTable.hasType(t) }
            }

    private fun functionMethodsWithType(
        context: GenerationContext,
        targetType: Type,
    ): List<FunctionMethodSignatureAST> =
        context.functionSymbolTable.withFunctionMethodType(targetType).map { it.signature } +
            context.symbolTable.classInstances().map { it.functionMethods }.unionAll()
                .filter { it.returnType == targetType }

    @Throws(IdentifierOnDemandException::class)
    override fun generateIdentifier(
        context: GenerationContext,
        targetType: Type,
        mutableConstraint: Boolean,
        initialisedConstraint: Boolean,
    ): IdentifierAST {
        val withType = context.symbolTable.withType(targetType).filter {
            (!mutableConstraint || it.mutable) && (!initialisedConstraint || it.initialised())
        }

        if (withType.isEmpty()) {
            if (!context.onDemandIdentifiers) throw IdentifierOnDemandException("Identifier of type $targetType not available")

            val decl = generateDeclarationStatementForType(context, targetType, true)
            context.addDependentStatement(decl)
        }

        return selectionManager.randomSelection(
            context.symbolTable.withType(targetType)
                .filter { (!mutableConstraint || it.mutable) && (!initialisedConstraint || it.initialised()) },
        )
    }

    override fun generateArrayIndex(context: GenerationContext, targetType: Type): ArrayIndexAST {
        val identifier = generateIdentifier(context, ArrayType(targetType), initialisedConstraint = true)
        val index = IntegerLiteralAST(generateDecimalLiteralValue(false), false)

        return ArrayIndexAST(identifier, index)
    }

    override fun generateSetDisplay(context: GenerationContext, targetType: Type): SetDisplayAST {
        val setType = targetType as SetType
        val numberOfExpressions = selectionManager.selectNumberOfConstructorFields()
        val exprs =
            (1..numberOfExpressions).map { generateExpression(context.increaseExpressionDepth(), setType.innerType) }
        return SetDisplayAST(setType.innerType, exprs)
    }

    override fun generateMapConstructor(context: GenerationContext, targetType: Type): MapConstructorAST {
        val mapType = targetType as MapType
        val numberOfExpressions = selectionManager.selectNumberOfConstructorFields()
        val assigns = (1..numberOfExpressions).map {
            Pair(
                generateExpression(context.increaseExpressionDepth(), mapType.keyType),
                generateExpression(context.increaseExpressionDepth(), mapType.valueType),
            )
        }

        return MapConstructorAST(mapType.keyType, mapType.valueType, assigns)
    }

    override fun generateMapIndexAssign(context: GenerationContext, targetType: Type): MapIndexAssignAST {
        val mapType = targetType as MapType
        val map = generateIdentifier(context, mapType)
        val index = generateExpression(context.increaseExpressionDepth(), targetType.keyType)
        val newValue = generateExpression(context.increaseExpressionDepth(), targetType.valueType)

        return MapIndexAssignAST(map, index, newValue)
    }

    override fun generateMapIndex(context: GenerationContext, targetType: Type): MapIndexAST {
        val mapType = targetType as MapType
        val map = generateIdentifier(context, mapType)
        val index = generateExpression(context.increaseExpressionDepth(), targetType.keyType)

        return MapIndexAST(map, index)
    }

    override fun generateUnaryExpression(
        context: GenerationContext,
        targetType: Type,
    ): UnaryExpressionAST {
        val expr = generateExpression(context, targetType)
        val operator = selectionManager.selectUnaryOperator(targetType)

        return UnaryExpressionAST(expr, operator)
    }

    override fun generateBinaryExpression(
        context: GenerationContext,
        targetType: Type,
    ): BinaryExpressionAST {
        val nextDepthContext = context.increaseExpressionDepth()
        val (operator, type) = selectionManager.selectBinaryOperator(context, targetType)
        val expr1 = generateExpression(nextDepthContext, type.first)
        val expr2 = generateExpression(nextDepthContext, type.second)

        return BinaryExpressionAST(expr1, operator, expr2)
    }

    override fun generateTernaryExpression(context: GenerationContext, targetType: Type): TernaryExpressionAST {
        val condition = generateExpression(context, BoolType)
        val ifExpr = generateExpression(context.increaseExpressionDepth(), targetType)
        val elseExpr = generateExpression(context.increaseExpressionDepth(), targetType)

        return TernaryExpressionAST(condition, ifExpr, elseExpr)
    }

    override fun generateBaseExpressionForType(
        context: GenerationContext,
        targetType: Type,
    ): ExpressionAST = when (targetType) {
        is LiteralType -> generateLiteralForType(context, targetType)
        is ArrayType -> generateArrayInitialisation(context, targetType)
        is MapType -> generateMapConstructor(context, targetType)
        is SetType -> generateSetDisplay(context, targetType)
        // should not be possible to reach here
        else -> throw UnsupportedOperationException("Trying to generate base for non-base type $targetType")
    }

    private fun generateConstructorForType(context: GenerationContext, targetType: Type): ExpressionAST =
        when (targetType) {
            is ArrayType -> generateArrayInitialisation(context, targetType)
            is MapType -> generateMapConstructor(context, targetType)
            is SetType -> generateSetDisplay(context, targetType)
            else -> throw UnsupportedOperationException("Trying to generate constructor for non-constructor type")
        }

    private fun generateLiteralForType(
        context: GenerationContext,
        targetType: LiteralType,
    ): LiteralAST = when (targetType) {
        BoolType -> generateBooleanLiteral(context)
        CharType -> generateCharLiteral(context)
        IntType -> generateIntegerLiteral(context)
        RealType -> generateRealLiteral(context)
        // should not be possible to reach here
        else -> throw UnsupportedOperationException("Trying to generate literal for non-literal type $targetType")
    }

    override fun generateIntegerLiteral(context: GenerationContext): IntegerLiteralAST {
        val isHex = selectionManager.randomWeightedSelection(listOf(true to 0.7, false to 0.3))
        val value = generateDecimalLiteralValue(negative = true)
        return IntegerLiteralAST(value, isHex)
    }

    override fun generateArrayInitialisation(
        context: GenerationContext,
        targetType: ArrayType,
    ): ArrayInitAST = ArrayInitAST(selectionManager.selectArrayLength(), targetType)

    override fun generateBooleanLiteral(context: GenerationContext): BooleanLiteralAST =
        BooleanLiteralAST(selectionManager.selectBoolean())

    override fun generateRealLiteral(context: GenerationContext): RealLiteralAST {
        val beforePoint = generateDecimalLiteralValue(negative = true)
        val afterPoint = generateDecimalLiteralValue(negative = false)

        return RealLiteralAST("$beforePoint.$afterPoint")
    }

    override fun generateCharLiteral(context: GenerationContext): CharacterLiteralAST {
        val c = selectionManager.selectCharacter()
        return CharacterLiteralAST(c)
    }

    override fun generateType(context: GenerationContext, literalOnly: Boolean): Type =
        if (!context.onDemandIdentifiers) {
            selectionManager.randomSelection(listOf(IntType, BoolType))
        } else {
            selectionManager.selectType(context, literalOnly = literalOnly)
        }

    private fun generateDecimalLiteralValue(negative: Boolean = true): String {
        var value = selectionManager.selectDecimalLiteral()

        if (negative && selectionManager.randomWeightedSelection(
                listOf(
                    true to 0.2,
                    false to 0.8,
                ),
            )
        ) {
            value *= -1
        }

        return value.toString()
    }

    companion object {
        private const val DAFNY_MAX_LOOP_COUNTER = 100
        private const val GLOBAL_STATE = "GlobalState"
        private const val PARAM_GLOBAL_STATE = "globalState"
    }
}
