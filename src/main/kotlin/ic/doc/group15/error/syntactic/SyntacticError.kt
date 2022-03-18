package ic.doc.group15.error.syntactic

import ic.doc.group15.error.CompilationError
import ic.doc.group15.error.SYNTACTIC_ERROR_CODE
import org.antlr.v4.runtime.Token

open class SyntacticError(
    line: Int,
    column: Int,
    message: String
) : CompilationError(SYNTACTIC_ERROR_CODE, line, column, message) {
    override fun toString(): String {
        return "Syntactic error (line $line, column $column): $message"
    }
}

class MissingReturnError(
    token: Token,
    funcName: String
) : SyntacticError(
    token.line,
    token.charPositionInLine,
    "Function $funcName is not ended with a return or an exit statement."
)

class BreakNotInLoopError(
    token: Token
) : SyntacticError(
    token.line,
    token.charPositionInLine,
    "Break statement must be inside a loop."
)

class ContinueNotInLoopError(
    token: Token
) : SyntacticError(
    token.line,
    token.charPositionInLine,
    "Continue statement must be inside a loop."
)
