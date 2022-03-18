package ic.doc.group15.error.semantic

import ic.doc.group15.ast.BinaryOp
import ic.doc.group15.ast.UnaryOp
import ic.doc.group15.type.*
import ic.doc.group15.type.BasicType.*
import ic.doc.group15.type.BasicType.Companion.BoolType
import ic.doc.group15.type.BasicType.Companion.CharType
import ic.doc.group15.type.BasicType.Companion.IntType
import ic.doc.group15.type.BasicType.Companion.StringType
import org.antlr.v4.runtime.Token

abstract class TypeError protected constructor(
    token: Token,
    message: String,
    expectedTypes: List<Type>,
    actualType: Type
) : SemanticError(
    token,
    "$message (expected type${if (expectedTypes.size > 1) "s" else ""}:" +
        " ${expectedTypes.joinToString()} | actual type: $actualType)"
) {
    constructor(token: Token, message: String, expectedType: Type, actualType: Type) : this(
        token,
        message,
        listOf(expectedType),
        actualType
    )
}

class CondTypeError(
    condExprToken: Token,
    actualType: Type
) : TypeError(
    condExprToken,
    "Type of conditional expression should be $BoolType",
    BoolType,
    actualType
)

class ExitTypeError(
    exitExprToken: Token,
    actualType: Type
) : TypeError(
    exitExprToken,
    "Expression passed to exit statement must be of type $IntType",
    IntType,
    actualType
)

class ReturnTypeError(
    returnExprToken: Token,
    expectedType: Type,
    actualType: Type
) : TypeError(
    returnExprToken,
    "Incompatible return type",
    expectedType,
    actualType
)

class AllocTypeError(
    allocExprToken: Token,
    actualType: Type
) : TypeError(
    allocExprToken,
    "malloc can only take integer expressions",
    IntType,
    actualType
)

class FreeTypeError(
    freeExprToken: Token,
    actualType: Type
) : TypeError(
    freeExprToken,
    "free statement can only take pairs, arrays and pointers as arguments",
    listOf(PairType.ANY_PAIR, ArrayType.ANY_ARRAY, PointerType.ANY_POINTER),
    actualType
)

class ReadTypeError(
    readExprToken: Token,
    actualType: Type
) : TypeError(
    readExprToken,
    "Cannot read user input into target of type $actualType",
    listOf(IntType, CharType, StringType),
    actualType
)

class AssignTypeError(
    lhsToken: Token,
    expectedType: Type,
    actualType: Type
) : TypeError(
    lhsToken,
    "Expression type does not match assignment type",
    expectedType,
    actualType
)

class IndexingNonArrayTypeError(
    arrayExprToken: Token,
    actualType: Type
) : TypeError(
    arrayExprToken,
    "Array indexing cannot be performed on $actualType type",
    ArrayType.ANY_ARRAY,
    actualType
)

class DereferencingNonPointerTypeError(
    derefToken: Token,
    actualType: Type
) : TypeError(
    derefToken,
    "Cannot dereference non-pointer type",
    PointerType.ANY_POINTER,
    actualType
)

class ArrayIndexTypeError(
    indexExprToken: Token,
    actualType: Type
) : TypeError(
    indexExprToken,
    "Array index must be of type $IntType",
    IntType,
    actualType
)

class FstTypeError(
    fstExprToken: Token,
    actualType: Type
) : TypeError(
    fstExprToken,
    "fst can only be called on pairs",
    PairType.ANY_PAIR,
    actualType
)

class SndTypeError(
    sndExprToken: Token,
    actualType: Type
) : TypeError(
    sndExprToken,
    "snd can only be called on pairs",
    PairType.ANY_PAIR,
    actualType
)

class SizeOfTypeError(
    sizeOfToken: Token,
    actualType: Type
) : TypeError(
    sizeOfToken,
    "sizeof can only take variable types",
    listOf(IntType, CharType, StringType, BoolType, PairType.ANY_PAIR, ArrayType.ANY_ARRAY,
        PointerType.ANY_POINTER),
    actualType
)

class ParameterTypeError(
    paramExprToken: Token,
    funcName: String,
    paramPosition: Int,
    expectedType: Type,
    actualType: Type
) : TypeError(
    paramExprToken,
    "Type of parameter $paramPosition not compatible with function definition of $funcName",
    expectedType,
    actualType
)

class ArrayLiteralElemsTypeError(
    literalExprToken: Token,
    expectedType: Type,
    actualType: Type
) : TypeError(
    literalExprToken,
    "Element type does not match array literal type",
    expectedType,
    actualType
)

class UnaryOpTypeError(
    unaryExprToken: Token,
    unOp: UnaryOp,
    actualType: Type
) : TypeError(
    unaryExprToken,
    "Cannot apply unary operator ${unOp.str} to type $actualType",
    unOp.expectedType,
    actualType
)

class BinaryOpTypeError(
    binaryExprToken: Token,
    binOp: BinaryOp,
    actualType: Type
) : TypeError(
    binaryExprToken,
    "Cannot apply binary operator ${binOp.str} to type $actualType",
    binOp.allowedTypes?.toList() ?: listOf(Type.ANY),
    actualType
)

class BinaryOpIncompatibleTypesError(
    binaryExprToken: Token,
    expectedType: Type,
    actualType: Type
) : TypeError(
    binaryExprToken,
    "Operands of binary expression must be compatible",
    expectedType,
    actualType
)

class ComparingPointerWithNonPointerError(
    comparingBinaryOpToken: Token,
    expectedType: Type,
    actualType: Type
) : TypeError(
    comparingBinaryOpToken,
    "Cannot compare a pointer with a non-pointer value",
    expectedType,
    actualType
)
