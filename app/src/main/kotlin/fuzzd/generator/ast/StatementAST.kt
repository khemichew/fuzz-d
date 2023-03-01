package fuzzd.generator.ast

import fuzzd.generator.ast.ExpressionAST.IdentifierAST
import fuzzd.generator.ast.error.InvalidInputException
import fuzzd.utils.indent

sealed class StatementAST : ASTElement {
    class IfStatementAST(
        private val condition: ExpressionAST,
        private val ifBranch: SequenceAST,
        private val elseBranch: SequenceAST?,
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
        private val counterInitialisation: DeclarationAST,
        private val terminationCheck: IfStatementAST,
        private val condition: ExpressionAST,
        private val body: SequenceAST,
        private val counterUpdate: AssignmentAST,
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
        private val identifiers: List<IdentifierAST>,
        private val exprs: List<ExpressionAST>,
    ) :
        StatementAST() {
        override fun toString(): String = "var ${identifiers.joinToString(", ")} := ${exprs.joinToString(", ")};"
    }

    class DeclarationAST(identifier: IdentifierAST, expr: ExpressionAST) :
        MultiDeclarationAST(listOf(identifier), listOf(expr))

    open class MultiAssignmentAST(
        private val identifiers: List<IdentifierAST>,
        private val exprs: List<ExpressionAST>,
    ) :
        StatementAST() {
        override fun toString(): String = "${identifiers.joinToString(", ")} := ${exprs.joinToString(", ")};"
    }

    class AssignmentAST(identifier: IdentifierAST, expr: ExpressionAST) :
        MultiAssignmentAST(listOf(identifier), listOf(expr))

    class PrintAST(private val expr: ExpressionAST) : StatementAST() {
        override fun toString(): String = "print $expr;"
    }

    class VoidMethodCallAST(private val method: MethodSignatureAST, private val params: List<ExpressionAST>) :
        StatementAST() {
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
