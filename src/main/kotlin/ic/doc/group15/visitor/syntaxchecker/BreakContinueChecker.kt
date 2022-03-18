package ic.doc.group15.visitor.syntaxchecker

import ic.doc.group15.antlr.WaccParser
import ic.doc.group15.antlr.WaccParserBaseVisitor
import ic.doc.group15.error.SyntacticErrorList
import ic.doc.group15.error.syntactic.BreakNotInLoopError
import ic.doc.group15.error.syntactic.ContinueNotInLoopError
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Used to check whether break and continue statements are within loop blocks.
 *
 * Should be used before the AST generation stage.
 */
class BreakContinueChecker(
    private val syntacticErrors: SyntacticErrorList,
    private val enableLogging: Boolean = false
) : WaccParserBaseVisitor<Unit>() {

    private var inLoop = false

    override fun visitWhileStat(ctx: WaccParser.WhileStatContext) {
        log("Inside a while loop.")
        visitLoop(ctx)
    }

    override fun visitFor_stat(ctx: WaccParser.For_statContext) {
        log("Inside a for loop.")
        visitLoop(ctx)
    }

    override fun visitFor_range_stat(ctx: WaccParser.For_range_statContext) {
        log("Inside a for range loop.")
        visitLoop(ctx)
    }

    override fun visitBreakStat(ctx: WaccParser.BreakStatContext) {
        log("Found a break statement.")
        if (!inLoop) {
            syntacticErrors.addError(
                BreakNotInLoopError(ctx.start)
            )
        }
    }

    override fun visitContinueStat(ctx: WaccParser.ContinueStatContext) {
        log("Found a continue statement.")
        if (!inLoop) {
            syntacticErrors.addError(
                ContinueNotInLoopError(ctx.start)
            )
        }
    }

    private fun visitLoop(ctx: ParserRuleContext) {
        val oldInLoop = inLoop
        inLoop = true

        visitChildren(ctx)

        inLoop = oldInLoop
    }

    private fun log(message: String) {
        if (enableLogging) {
            println(message)
        }
    }
}
