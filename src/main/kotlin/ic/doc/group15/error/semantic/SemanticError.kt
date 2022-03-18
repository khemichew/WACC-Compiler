package ic.doc.group15.error.semantic

import ic.doc.group15.error.CompilationError
import ic.doc.group15.error.SEMANTIC_ERROR_CODE
import ic.doc.group15.type.*
import org.antlr.v4.runtime.Token

abstract class SemanticError protected constructor(
    token: Token,
    message: String
) : CompilationError(SEMANTIC_ERROR_CODE, token, message) {

    override fun toString(): String {
        return "Semantic error (line $line, column $column): $message [${getErrorName(this)}]"
    }

    protected companion object {
        fun getErrorName(obj: SemanticError): String = obj::class.toString().split('.').last()
    }
}

class NotReturnableError(
    token: Token,
    type: Type
) : SemanticError(token, "Cannot return $type type (not a returnable type)")

class CannotBeParameterError(
    token: Token,
    type: Type
) : SemanticError(token, "Type $type cannot be a parameter type")

class NotAFunctionError(
    token: Token,
    name: String
) : SemanticError(token, "$name is not a function")

class IllegalReturnStatementError(
    returnToken: Token
) : SemanticError(returnToken, "return statement only allowed inside function declaration block")

class NumArgumentsError(
    argListToken: Token,
    funcName: String,
    expectedNum: Int,
    actualNum: Int
) : SemanticError(
    argListToken,
    "Wrong number of arguments for call to $funcName: expected $expectedNum, got $actualNum"
)

class PairElemNullError(
    nullExprToken: Token
) : SemanticError(
    nullExprToken,
    "Cannot access element of null pair"
)

class PointerAdditionError(
    pointerAdditionToken: Token
) : SemanticError(
    pointerAdditionToken,
    "Cannot add two pointers together"
)

class PointerNotFirstInBinOpError(
    binOpToken: Token
) : SemanticError(
    binOpToken,
    "Pointer value must be first when performing pointer arithmetic"
)
