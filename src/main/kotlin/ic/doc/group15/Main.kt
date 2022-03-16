package ic.doc.group15

import ic.doc.group15.antlr.WaccLexer
import ic.doc.group15.antlr.WaccParser
import ic.doc.group15.ast.AST
import ic.doc.group15.error.BCEErrorList
import ic.doc.group15.error.SemanticErrorList
import ic.doc.group15.error.syntactic.SyntacticErrorListener
import ic.doc.group15.visitor.AssemblyGenerator
import ic.doc.group15.visitor.BCEOptimizerSeq
import ic.doc.group15.visitor.ParseTreeVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

private const val ENABLE_LOGGING = true

fun main(args: Array<String>) {
    assert(args.size == 1)
    val input = CharStreams.fromStream(System.`in`)

    val lexer = WaccLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = WaccParser(tokens)
    val syntacticErrorListener = SyntacticErrorListener()
    parser.addErrorListener(syntacticErrorListener)

    val program = parser.program()

    syntacticErrorListener.terminateIfSyntacticErrorOccurred()
    parser.removeErrorListener(syntacticErrorListener)

    val st = SymbolTable.topLevel()
    var ast = AST(st)
    val semanticErrors = SemanticErrorList()
    val visitor = ParseTreeVisitor(ast, st, semanticErrors, enableLogging = ENABLE_LOGGING)
    visitor.visit(program)

    semanticErrors.checkErrors()

    val bceErrors = BCEErrorList()
    val astCopy = ast
    val bceOptimizerSeq = BCEOptimizerSeq(ast, bceErrors, enableLogging =
            ENABLE_LOGGING)
    bceOptimizerSeq.removeArrayBoundChecking()

    if (bceErrors.hasErrors()) {
        ast = astCopy
    }

    val assemblyGenerator = AssemblyGenerator(ast, enableLogging = ENABLE_LOGGING)
    val assemblyCode = assemblyGenerator.generate()

    // Create assembly file
    val filename = args[0].split("/").last()
    val asmFilename = filename.substring(0, filename.length - 4) + "s"

    File(asmFilename).writeText(assemblyCode)
}
