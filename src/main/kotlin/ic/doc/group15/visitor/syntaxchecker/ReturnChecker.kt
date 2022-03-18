package ic.doc.group15.visitor.syntaxchecker

import ic.doc.group15.antlr.WaccParser
import ic.doc.group15.antlr.WaccParserBaseVisitor
import ic.doc.group15.error.SyntacticErrorList
import ic.doc.group15.error.syntactic.MissingReturnError
import ic.doc.group15.type.TypeParser

/**
 * Used to check whether functions have a valid return statement or not.
 *
 * Should be used before the AST generation stage.
 */
class ReturnChecker(
    private val syntacticErrors: SyntacticErrorList,
    private val enableLogging: Boolean = false
) : WaccParserBaseVisitor<Boolean>() {

    override fun visitFunc(ctx: WaccParser.FuncContext): Boolean {
        val funcName = ctx.ident().text
        log("Checking return statements of $funcName...")
        if (TypeParser.returnTypeIsVoid(ctx.return_type())) {
            log("Return type of $funcName is void! No need to check for return statements.")
            return true
        }
        val validReturn = visit(ctx.stat_sequence()) ?: false
        if (!validReturn) {
            log("No valid return statements found for $funcName!")
            syntacticErrors.addError(
                MissingReturnError(ctx.start, ctx.ident().text)
            )
        }
        return validReturn
    }

    override fun visitIfStat(ctx: WaccParser.IfStatContext): Boolean {
        return (visit(ctx.stat_sequence(0)) ?: false) && (visit(ctx.stat_sequence(1)) ?: false)
    }

    override fun visitReturnStat(ctx: WaccParser.ReturnStatContext): Boolean {
        return true
    }

    override fun visitExitStat(ctx: WaccParser.ExitStatContext): Boolean {
        return true
    }

    private fun log(message: String) {
        if (enableLogging) {
            println(message)
        }
    }
}
