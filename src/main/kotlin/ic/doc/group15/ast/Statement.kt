package ic.doc.group15.ast

import ic.doc.group15.SymbolTable
import ic.doc.group15.type.*

abstract class StatementAST protected constructor(
    val parent: BlockAST?,
    symbolTable: SymbolTable = SymbolTable.emptyTable
) : ASTNode(symbolTable)

open class VariableDeclarationAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    val varName: String,
    val rhs: AssignRhsAST,
    val varIdent: Variable
) : StatementAST(parent, symbolTable)

class ParameterAST(
    symbolTable: SymbolTable,
    paramName: String,
    override val ident: Param
) : VariableIdentifierAST(symbolTable, paramName, ident)

class CallStatementAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    val funcName: String,
    val funcIdent: FunctionType,
    val actuals: List<ExpressionAST>
) : StatementAST(parent, symbolTable)

class SkipStatementAST(
    parent: BlockAST,
) : StatementAST(parent)

class ReadStatementAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    val target: AssignToLhsAST<*>
) : StatementAST(parent, symbolTable)

class ContinueStatementAST(
    parent: BlockAST,
) : StatementAST(parent)

class BreakStatementAST(
    parent: BlockAST,
) : StatementAST(parent)

class FreeStatementAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    val expr: AssignRhsAST
) : StatementAST(parent, symbolTable)

class PrintStatementAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    val expr: ExpressionAST
) : StatementAST(parent, symbolTable)

class PrintlnStatementAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    val expr: ExpressionAST
) : StatementAST(parent, symbolTable)

open class ReturnStatementAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    open val expr: ExpressionAST?,
    val type: ReturnableType
) : StatementAST(parent, symbolTable)

class ExitStatementAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    override val expr: ExpressionAST
) : ReturnStatementAST(parent, symbolTable, expr, BasicType.IntType)
