package fuzzd.generator.symbol_table

import fuzzd.generator.ast.FunctionMethodAST
import fuzzd.generator.ast.Type

class GlobalSymbolTable {
    private val functionMethods = mutableMapOf<FunctionMethodAST, Type>()

    fun addAllFunctionMethods(allFunctionMethods: List<FunctionMethodAST>) =
        allFunctionMethods.forEach(this::addFunctionMethod)

    fun addFunctionMethod(functionMethod: FunctionMethodAST) {
        functionMethods[functionMethod] = functionMethod.returnType
    }

    fun hasFunctionMethodType(type: Type): Boolean = functionMethods.any { it.value == type }

    fun withFunctionMethodType(type: Type): List<FunctionMethodAST> =
        functionMethods.filter { it.value == type }.keys.toList()

    fun functionMethods(): List<FunctionMethodAST> = functionMethods.keys.toList()
}