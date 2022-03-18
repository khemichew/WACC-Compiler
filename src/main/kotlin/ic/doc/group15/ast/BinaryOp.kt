package ic.doc.group15.ast

import ic.doc.group15.type.BasicType.Companion.BoolType
import ic.doc.group15.type.BasicType.Companion.CharType
import ic.doc.group15.type.BasicType.Companion.IntType
import ic.doc.group15.type.PointerType
import ic.doc.group15.type.VariableType

private val intOnly = setOf<VariableType>(IntType)
private val addables = intOnly + PointerType.ANY_POINTER
private val comparables = addables + CharType
private val boolOnly = setOf<VariableType>(BoolType)

/**
 * Describes all the binary operations supported in WACC.
 *
 * If allowedTypes = null, all types are allowed.
 */
enum class BinaryOp(
    val str: String,
    val allowedTypes: Set<VariableType>? = null,
    val returnType: VariableType = BoolType
) {
    MULT("*", intOnly, returnType = IntType),
    DIV("/", intOnly, returnType = IntType),
    MOD("%", intOnly, returnType = IntType),
    PLUS("+", addables, returnType = IntType),
    MINUS("-", addables, returnType = IntType),
    GT(">", comparables),
    GTE(">=", comparables),
    LT("<", comparables),
    LTE("<=", comparables),
    EQUALS("="),
    NOT_EQUALS("!="),
    AND("&&", boolOnly),
    OR("||", boolOnly);
}
