package ic.doc.group15

import ic.doc.group15.Option.* // ktlint-disable no-unused-imports
import ic.doc.group15.antlr.WaccLexer
import ic.doc.group15.antlr.WaccParser
import ic.doc.group15.ast.AST
import ic.doc.group15.error.BCEErrorList
import ic.doc.group15.error.SemanticErrorList
import ic.doc.group15.error.SyntacticErrorList
import ic.doc.group15.error.syntactic.SyntacticErrorListener
import ic.doc.group15.visitor.AstAssemblyGenerator
import ic.doc.group15.visitor.AssemblyGenerator
import ic.doc.group15.visitor.BCEOptimizerSeq
import ic.doc.group15.visitor.ParseTreeVisitor
import ic.doc.group15.visitor.ReturnChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

private const val TAB_CHAR = ' '
private val tab = "$TAB_CHAR".repeat(4)

private var enableLogging = true

fun main(args: ArgsList) {
    // Parse command line arguments
    if (args.hasOption(HELP)) {
        printHelpMessage()
        return
    }
    if (args.isEmpty()) {
        println("Source file name must be specified.\n\nRun ./compile --help for more info.")
        return
    }
    val sourceFilePath: String = args[0]
    enableLogging = enableLogging || args.hasOption(LOGGING)
    val printAssembly = args.hasOption(PRINT_ASM)

    // Read the source file
    val sourceFile = FileInputStream(sourceFilePath)
    log("Reading source file at ${args[0]}...\n")
    val inputStream = BufferedInputStream(sourceFile)
    val input = CharStreams.fromStream(inputStream)

    // Perform syntactic analysis and build the parse tree
    log("\nPerforming syntactic analysis...\n")
    val lexer = WaccLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = WaccParser(tokens)
    val syntacticErrorListener = SyntacticErrorListener()
    parser.addErrorListener(syntacticErrorListener)
    val program = parser.program()
    syntacticErrorListener.terminateIfSyntacticErrorOccurred()
    parser.removeErrorListener(syntacticErrorListener)

    val syntacticErrors = SyntacticErrorList()

    val returnChecker = ReturnChecker(syntacticErrors, enableLogging = enableLogging)
    returnChecker.visit(program)
    syntacticErrors.checkErrors()

    // Perform semantic analysis and build the AST
    log("\nPerforming semantic analysis...\n")
    val st = SymbolTable()
    var ast = AST(st)
    val semanticErrors = SemanticErrorList()
    val visitor = ParseTreeVisitor(
        ast,
        st,
        syntacticErrors,
        semanticErrors,
        enableLogging = enableLogging
    )
    visitor.visit(program)
    syntacticErrors.checkErrors()

    semanticErrors.checkErrors()

    // Close input resources
    inputStream.close()
    sourceFile.close()

    // Run BCE optimization
    val bceErrors = BCEErrorList()
    val astCopy = ast
    val bceOptimizerSeq = BCEOptimizerSeq(ast, bceErrors, enableLogging =
            enableLogging)
    bceOptimizerSeq.removeArrayBoundChecking()
    if (bceErrors.hasErrors()) {
        ast = astCopy
    }

    // Create assembly file
    val filename = sourceFilePath.split("/").last()
    val asmFilename = filename.substring(0, filename.length - 4) + "s"
    log("\nCreating assembly file \"$asmFilename\"...\n")
    val assemblyFile = File(asmFilename)

    // Generate the assembly code and write it to the assembly file
    log("\nGenerating assembly code...\n")
    val writer = assemblyFile.bufferedWriter()
    val assemblyGenerator = AstAssemblyGenerator(ast, enableLogging = enableLogging)
    assemblyGenerator.generate(writer)
    writer.close()
    log("\nCompilation finished.")

    // Print the assembly code if needed
    if (printAssembly) {
        log("\n$asmFilename:")
        assemblyFile.readLines().forEach { println(it) }
    }
}

private fun printHelpMessage() {
    println(
        """
            |
            |Imperial College 2022
            |Group 15's compiler for the WACC language.
            | 
            |Usage:
            |$tab./compile source_file [option...]
            
            |source_file: Path to the WACC source code file.
            
            |Options:
        """.trimMargin()
    )
    val options = Option.values()
    val longestString = options.maxOf { it.string().length }
    val longestAlt = options.maxOf { it.alt().length }
    println(
        options.joinToString(prefix = tab, separator = "\n$tab") {
            "--" + it.string().padEnd(longestString + tab.length, TAB_CHAR) +
                    "-" + it.alt.padEnd(longestAlt + tab.length, TAB_CHAR) +
                    it.description
        }
    )
}

private fun log(string: String) {
    if (enableLogging) println(string)
}
