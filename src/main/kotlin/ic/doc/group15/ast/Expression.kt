package ic.doc.group15.ast

import ic.doc.group15.SymbolTable
import ic.doc.group15.type.*
import kotlin.math.exp

abstract class ExpressionAST protected constructor(
    symbolTable: SymbolTable = SymbolTable.emptyTable,
    type: VariableType
) : AssignRhsAST(symbolTable, type)

class SizeOfAST(
    symbolTable: SymbolTable,
    val sizeOfType: VariableType
) : ExpressionAST(symbolTable, BasicType.IntType)

class IntLiteralAST(val intValue: Int) : ExpressionAST(type = BasicType.IntType)
class BoolLiteralAST(val boolValue: Boolean) : ExpressionAST(type = BasicType.BoolType)
class CharLiteralAST(val charValue: Char) : ExpressionAST(type = BasicType.CharType)
class StringLiteralAST(val stringValue: String) : ExpressionAST(type = BasicType.StringType)
class NullPairLiteralAST : ExpressionAST(type = PairType())

open class VariableIdentifierAST(
    symbolTable: SymbolTable,
    val varName: String,
    open val ident: Variable
) : ExpressionAST(symbolTable, ident.type)

class ReferenceAST(
    symbolTable: SymbolTable,
    val item: AssignToLhsAST<*>
) : ExpressionAST(symbolTable, PointerType(item.type, 1))

class DerefPointerAST(
    symbolTable: SymbolTable,
    val expr: ExpressionAST,
    val numDerefs: Int,
    type: VariableType
) : ExpressionAST(symbolTable, type)

class ArrayElemAST(
    symbolTable: SymbolTable,
    val arrayVar: VariableIdentifierAST,
    val indexExpr: List<ExpressionAST>,
    type: VariableType,
    var noBoundCheckRequired: List<Boolean>? = null
) : ExpressionAST(symbolTable, type)

class UnaryOpExprAST(
    symbolTable: SymbolTable,
    val expr: ExpressionAST,
    val operator: UnaryOp
) : ExpressionAST(symbolTable, operator.returnType)

class BinaryOpExprAST(
    symbolTable: SymbolTable,
    val expr1: ExpressionAST,
    val expr2: ExpressionAST,
    val operator: BinaryOp,
    returnType: VariableType
) : ExpressionAST(symbolTable, returnType) {

    constructor(
        symbolTable: SymbolTable,
        expr1: ExpressionAST,
        expr2: ExpressionAST,
        operator: BinaryOp
    ) : this(symbolTable, expr1, expr2, operator, operator.returnType)
}
