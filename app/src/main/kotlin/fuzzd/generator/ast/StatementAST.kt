package fuzzd.generator.ast

import fuzzd.generator.ast.ExpressionAST.IdentifierAST
import fuzzd.generator.ast.error.InvalidInputException
import fuzzd.utils.indent

sealed class StatementAST : ASTElement {
    class IfStatementAST(
        val condition: ExpressionAST,
        val ifBranch: SequenceAST,
        val elseBranch: SequenceAST?,
    ) : StatementAST() {
        init {
            if (condition.type() != Type.BoolType) {
                throw InvalidInputException("If statement condition not bool type")
            }
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.appendLine("if ($condition) {")
            sb.appendLine(ifBranch)
            sb.append("}")

            if (elseBranch != null) {
                sb.append(" else {\n")
                sb.append(elseBranch)
                sb.append("\n}")
            }

            sb.append("\n")
            return sb.toString()
        }
    }

    class WhileLoopAST(
        val counterInitialisation: DeclarationAST,
        val terminationCheck: IfStatementAST,
        val condition: ExpressionAST,
        val body: SequenceAST,
        val counterUpdate: AssignmentAST,
    ) : StatementAST() {
        init {
            if (condition.type() != Type.BoolType) {
                throw InvalidInputException("If statement condition not bool type")
            }
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.appendLine(counterInitialisation)
            sb.appendLine("while ($condition) {")
            sb.appendLine(indent(terminationCheck))
            sb.appendLine(indent(counterUpdate))
            sb.appendLine(body)
            sb.appendLine("}")
            return sb.toString()
        }
    }

    open class MultiDeclarationAST(
        val identifiers: List<IdentifierAST>,
        val exprs: List<ExpressionAST>,
    ) :
        StatementAST() {
        override fun toString(): String = "var ${identifiers.joinToString(", ")} := ${exprs.joinToString(", ")};"
    }

    class DeclarationAST(identifier: IdentifierAST, expr: ExpressionAST) :
        MultiDeclarationAST(listOf(identifier), listOf(expr))

    open class MultiAssignmentAST(
        val identifiers: List<IdentifierAST>,
        val exprs: List<ExpressionAST>,
    ) :
        StatementAST() {
        override fun toString(): String = "${identifiers.joinToString(", ")} := ${exprs.joinToString(", ")};"
    }

    class AssignmentAST(identifier: IdentifierAST, expr: ExpressionAST) :
        MultiAssignmentAST(listOf(identifier), listOf(expr))

    class PrintAST(val expr: ExpressionAST) : StatementAST() {
        override fun toString(): String = "print $expr;"
    }

    class VoidMethodCallAST(val method: MethodSignatureAST, val params: List<ExpressionAST>) : StatementAST() {
        init {
            val methodName = method.name

            if (method.returns.isNotEmpty()) {
                throw InvalidInputException("Generating invalid method call to non-void method $methodName")
            }

            val methodParams = method.params

            if (params.size != methodParams.size) {
                throw InvalidInputException("Generating method call to $methodName with incorrect no. of parameters. Got ${params.size}, expected ${methodParams.size}")
            }

            (params.indices).forEach { i ->
                val paramType = params[i].type()
                val expectedType = methodParams[i].type()
                if (paramType != expectedType) {
                    throw InvalidInputException("Method call parameter type mismatch for parameter $i. Expected $expectedType, got $paramType")
                }
            }
        }

        override fun toString(): String = "${method.name}(${params.joinToString(", ")});"
    }

    object BreakAST : StatementAST() {
        override fun toString(): String = "break;"
    }
}
