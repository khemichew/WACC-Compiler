package ic.doc.group15.visitor

import ic.doc.group15.SymbolTable
import ic.doc.group15.assembly.* // ktlint-disable no-unused-imports
import ic.doc.group15.assembly.LibraryFunction.Companion.AEABI_IDIV
import ic.doc.group15.assembly.LibraryFunction.Companion.AEABI_IDIVMOD
import ic.doc.group15.assembly.LibraryFunction.Companion.EXIT
import ic.doc.group15.assembly.LibraryFunction.Companion.MALLOC
import ic.doc.group15.assembly.LibraryFunction.Companion.PUTCHAR
import ic.doc.group15.assembly.UtilFunction.* // ktlint-disable no-unused-imports
import ic.doc.group15.assembly.instruction.* // ktlint-disable no-unused-imports
import ic.doc.group15.assembly.instruction.ConditionCode.*
import ic.doc.group15.assembly.instruction.ConditionCode.GT
import ic.doc.group15.assembly.instruction.ConditionCode.LT
import ic.doc.group15.assembly.instruction.Directive.Companion.LTORG
import ic.doc.group15.assembly.operand.* // ktlint-disable no-unused-imports
import ic.doc.group15.assembly.operand.Register.*
import ic.doc.group15.ast.*
import ic.doc.group15.ast.BinaryOp.*
import ic.doc.group15.type.*
import ic.doc.group15.type.BasicType.Companion.BoolType
import ic.doc.group15.type.BasicType.Companion.CharType
import ic.doc.group15.type.BasicType.Companion.IntType
import ic.doc.group15.type.BasicType.Companion.StringType
import ic.doc.group15.util.WORD
import java.lang.IllegalArgumentException
import java.util.*

private const val MAX_STACK_CHANGE = 1024

class AstAssemblyGenerator(
    ast: AST,
    enableLogging: Boolean = true
) : AssemblyGenerator<ASTNode>(ast, enableLogging) {

    /**
     * The current label that the generator is adding instructions to as it translates them.
     */
    private lateinit var currentLabel: BranchLabel

    /**
     * The next available register to write intermediate results to.
     */
    private lateinit var resultRegister: Register

    /**
     * Store the total stack space used to store intermediate results of
     * the current scope. Since we may encounter nested scopes, we want to
     * store the offset when entering a sub-scope and restore it when we exit
     * the sub-scope.
     */
    private val offsetStackStore = LinkedList<Int>()

    /**
     * Set by translation functions for loop blocks, so that continue and break translation
     * functions can use them.
     */
    private lateinit var continueLabel: BranchLabel
    private lateinit var breakLabel: BranchLabel

    /**
     * Per WACC language specification, a program matches the grammar "begin func* stat end".
     * The AST representation decouples the statements from a SequenceAST to a mapping of lists of
     * StatementAST in the symbol table to avoid stack overflow from recursing a huge block of
     * statements.
     */
    @TranslatorMethod
    private fun translateProgram(program: AST) {
        log("Translating program")
        resultRegister = R4

        // Translate all function blocks into assembly
        program.getFuncs().forEach { translate(it) }

        // Translate main instructions into assembly
        mainPrologue(program.symbolTable)
        program.getMain().forEach { translate(it) }
        mainEpilogue(program.symbolTable)
    }

    @TranslatorMethod
    private fun translateFunctionDeclaration(node: FunctionDeclarationAST) {
        log("Translating function declaration")
        // Define label
        val funcLabel = newFunctionLabel(node.funcName)
        currentLabel = funcLabel

        // Translate block statements and add to loop label - we start from register R4
        resultRegister = R4

        node.formals.forEach { it.ident.stackPosition }

        // Sets up the environment for a function
        functionPrologue(node, node.paramSymbolTable)

        node.getStatements().map { translate(it) }

        // Restore the state so that the program can resume from where it left off
        functionEpilogue(node)
    }

    @TranslatorMethod
    private fun translateCall(node: CallAssignAST) {
        translate(node.callStat)

        log("Translating call assign")

        // Move the result from R0 to resultRegister
        addLines(Move(resultRegister, R0))
    }

    @TranslatorMethod
    private fun translateCallStat(node: CallStatementAST) {
        log("Translating call")

        var callStackSize = 0

        val oldOffset = offsetStackStore[0]

        // Set up the stack with the necessary parameters before a function call.
        node.actuals.reversed().forEach {
            // Load variable to resultRegister
            translate(it)
            val size = it.type.size
            callStackSize += size
            addLines(
                // Allocate space in stack
                Sub(SP, SP, IntImmediateOperand(size)),
                // Move value from resultRegister to stack
                Store(size, resultRegister, ZeroOffset(SP))
            )
            offsetStackStore[0] += size
        }

        offsetStackStore[0] = oldOffset

        // Perform the call
        addLines(BranchLink(branchToFunction(node.funcName)))

        // Increment the stack pointer back to where it was
        if (callStackSize > 0) {
            addLines(Add(SP, SP, IntImmediateOperand(callStackSize)))
        }
    }

    @TranslatorMethod
    private fun translateVariableDeclaration(node: VariableDeclarationAST) {
        // Parse the expression whose value is to be stored in the variable
        log("Translating VariableDeclarationAST")
        translate(node.rhs)
        transAssign(node.varIdent, node.symbolTable)
    }

    @TranslatorMethod
    private fun translateAssignToIdent(node: AssignToIdentAST) {
        // Parse the expression whose value is to be stored in the variable
        log("Translating AssignToIdentAST")
        translate(node.rhs)
        val lhs = node.lhs
        transAssign(lhs.ident, lhs.symbolTable)
    }

    @TranslatorMethod
    private fun translateFreeStatement(node: FreeStatementAST) {
        log("Translating FreeStatementAST")
        val utilFunc: UtilFunction = when (node.expr.type) {
            is PairType -> P_FREE_PAIR
            else -> P_FREE_POINTER
        }
        defineUtilFuncs(utilFunc)
        translate(node.expr)
        addLines(
            Move(R0, resultRegister),
            BranchLink(utilFunc)
        )
    }

    /**
     * return statements have the format "return x". It can
     * only exist in a body of a non-main function and is used to return a value from
     * that function.
     */
    @TranslatorMethod
    private fun translateReturnStatement(node: ReturnStatementAST) {
        log("Translating ReturnStatementAST")
        if (node.expr != null) {
            translate(node.expr!!)
            addLines(
                Move(R0, resultRegister)
            )
        }
        unwindStack(node.parent!!.symbolTable)
        addLines(
            Pop(PC)
        )
    }

    /**
     * Per WACC language spec, exit statements have the format "exit x", where x is
     * an exit code of type int in range [0, 255].
     */
    @TranslatorMethod
    private fun translateExitStatement(node: ExitStatementAST) {
        log("Translating ExitStatementAST")
        translate(node.expr)
        addLines(
            Move(R0, resultRegister),
            BranchLink(EXIT)
        )
    }

    @TranslatorMethod
    private fun translatePrintStatement(node: PrintStatementAST) {
        log("Translating PrintStatementAST")
        translate(node.expr)
        addLines(
            Move(R0, resultRegister)
        )

        val expr = node.expr
        when (val type = expr.type) {
            StringType -> {
                defineUtilFuncs(P_PRINT_STRING)
                addLines(BranchLink(P_PRINT_STRING))
            }
            CharType -> {
                addLines(BranchLink(PUTCHAR))
            }
            IntType -> {
                defineUtilFuncs(P_PRINT_INT)
                addLines(BranchLink(P_PRINT_INT))
            }
            BoolType -> {
                defineUtilFuncs(P_PRINT_BOOL)
                addLines(BranchLink(P_PRINT_BOOL))
            }
            is PairType, is PointerType -> {
                defineUtilFuncs(P_PRINT_REFERENCE)
                addLines(BranchLink(P_PRINT_REFERENCE))
            }
            is ArrayType -> {
                if (type.elementType == CharType) {
                    defineUtilFuncs(P_PRINT_STRING)
                    addLines(BranchLink(P_PRINT_STRING))
                } else {
                    defineUtilFuncs(P_PRINT_REFERENCE)
                    addLines(BranchLink(P_PRINT_REFERENCE))
                }
            }
        }
    }

    @TranslatorMethod
    private fun translatePrintlnStatement(node: PrintlnStatementAST) {
        log("Translating PrintlnStatementAST")
        val printStatementAST = PrintStatementAST(node.parent!!, node.symbolTable, node.expr)
        translatePrintStatement(printStatementAST)
        defineUtilFuncs(P_PRINT_LN)
        addLines(BranchLink(P_PRINT_LN))
    }

    /**
     * Per WACC language spec, a read statement follows the grammar "read x", where x
     * is either of character or integer type.
     *
     * If the input is of type int, then it will convert the input string into an integer
     * (truncated to fit within the minimum/maximum range of the int type). If the standard
     * input value is incompatible with the type of the target, then this value will not be
     * consumed from the standard input stream. Instead, the program will continue, leaving
     * the target's value unchanged.
     */
    @TranslatorMethod
    private fun translateReadStatement(node: ReadStatementAST) {
        log("Translating ReadStatementAST")

        val assignTo = node.target
        val readFunc = if (node.target.type == IntType) P_READ_INT else P_READ_CHAR

        defineUtilFuncs(readFunc)
        getAddress(assignTo.lhs)
        addLines(
            Move(R0, resultRegister),
            BranchLink(readFunc)
        )
    }

    /**
     * Per WACC language spec, a if-block matches the grammar "if expr then stat else stat fi"
     * The if-then-else block follows the basic implementation as follows:
     *
     * [Sequence of instructions to test IF condition]
     *
     * B{cond} else
     *
     * [Sequence of instructions in THEN block]
     *
     * B fi
     *
     * else:
     *
     * [Sequence of instructions in ELSE block]
     *
     * fi: ...
     */
    @TranslatorMethod
    private fun translateIfBlock(stat: IfBlockAST) {
        log("Translating IfBlockAST")

        val oldLabel = currentLabel

        // Add condition expression to currentLabel
        translate(stat.condExpr)

        // First, deal with the else label
        val elseLabel = newBranchLabel()

        // compare to set condition flags and branch (can be optimised)
        addLines(
            Compare(resultRegister, IntImmediateOperand(0)),
            Branch(EQ, BranchLabelOperand(elseLabel))
        )

        // add sequence of instructions in THEN block under if label
        blockPrologue(stat)
        stat.getStatements().forEach(::translate)
        blockEpilogue(stat)

        // add sequence of instructions in ELSE block under else label
        currentLabel = elseLabel
        blockPrologue(stat.elseBlock)
        stat.elseBlock.getStatements().forEach(::translate)
        blockEpilogue(stat.elseBlock)

        currentLabel = oldLabel

        // Finally, deal with what comes after the if statement
        val fiLabel = newBranchLabel()

        // add branch to fi label to the original label
        addLines(
            Branch(BranchLabelOperand(fiLabel))
        )
        currentLabel = fiLabel
    }

    @TranslatorMethod
    private fun translateSwitchStat(stat: SwitchStatementAST) {
        log("Translating SwitchStatAST")

        // evaluate the switch case expression
        translate(stat.expr)

        // we create labels corresponding to each case and the fi label at the end
        val labels = HashMap<ExpressionAST, BranchLabel>()
        for (expr in stat.cases.keys) {
            labels[expr] = newBranchLabel()
        }
        val defaultLabel = newBranchLabel()
        val fiLabel = newBranchLabel()

        // now go through the expressions for each case, evaluate each one,
        // check if it is the value we are trying to match, and if so branch
        // to the corresponding label
        resultRegister = resultRegister.nextReg()
        for (expr in stat.cases.keys) {
            translate(expr)
            addLines(
                Compare(resultRegister, resultRegister.prevReg()),
                Branch(EQ, BranchLabelOperand(labels[expr] as BranchLabel))
            )
        }

        // here we evaluate the code for each block of the switch statement under
        // their respective labels and then branch to the fi label as we don't
        // want fall through behaviour
        for (expr in stat.cases.keys) {
            currentLabel = labels[expr] as BranchLabel
            blockPrologue(stat.cases[expr] as BlockAST)
            (stat.cases[expr] as BlockAST).getStatements().forEach(::translate)
            blockEpilogue(stat.cases[expr] as BlockAST)
            addLines(
                Branch(BranchLabelOperand(fiLabel))
            )
        }

        // finally we evaluate the default case
        currentLabel = defaultLabel
        blockPrologue(stat.default as BlockAST)
        (stat.default as BlockAST).getStatements().forEach(::translate)
        blockEpilogue(stat.default as BlockAST)

        currentLabel = fiLabel
    }

    /**
     * Per WACC language spec, a while-block matches the grammar "while expr do stat done"
     * The while block follows the basic implementation as follows:
     *
     * B check
     *
     * loop:
     *
     * {Sequence of instructions in WHILE block}
     *
     * check:
     *
     * {Sequence of instructions to test WHILE condition}
     *
     * CMP resultReg, 1
     * BEQ loop
     */
    @TranslatorMethod
    private fun translateWhileBlock(node: WhileBlockAST) {
        log("Translating WhileBlockAST")

        // Set the state to be used next time we translate break or continue
        val oldBreak = breakLabel
        val oldContinue = continueLabel

        // Create the labels we need, in this precise correct order
        val oldLabel = currentLabel
        val loopLabel = newBranchLabel()
        val checkLabel = newBranchLabel()
        val endLabel = newBranchLabel()

        breakLabel = endLabel
        continueLabel = checkLabel

        // Translate block statements and add to loop label
        currentLabel = loopLabel
        blockPrologue(node)
        node.getStatements().forEach { translate(it) }
        blockEpilogue(node)

        currentLabel = oldLabel

        // Add branch instruction
        addLines(
            Branch(BranchLabelOperand(checkLabel))
        )

        // Translate condition statements and add to check label
        currentLabel = checkLabel
        translate(node.condExpr)

        // Add compare and branch instruction
        addLines(
            Compare(resultRegister, IntImmediateOperand(1)),
            Branch(EQ, BranchLabelOperand(loopLabel))
        )

        // After the loop is over, reset the state and continue translating the program
        currentLabel = endLabel
        breakLabel = oldBreak
        continueLabel = oldContinue
    }

    @TranslatorMethod
    private fun translateForBlock(node: ForBlockAST) {
        log("Translating ForBlockAST")

        // Set the state to be used next time we translate break or continue
        val oldBreak = breakLabel
        val oldContinue = continueLabel

        // Create the labels we need, in this precise correct order
        val oldLabel = currentLabel
        val loopLabel = newBranchLabel()
        val updateLabel = newBranchLabel()
        val checkLabel = newBranchLabel()
        val endLabel = newBranchLabel()

        breakLabel = endLabel
        continueLabel = updateLabel

        // Translate the loop variable declaration and branch to the loop check label
        currentLabel = oldLabel
        blockPrologue(node)
        translate(node.varDecl)
        blockEpilogue(node)
        addLines(
            Branch(BranchLabelOperand(checkLabel))
        )

        // Translate the loop block statements
        currentLabel = loopLabel
        node.getStatements().forEach { translate(it) }

        // Add the loop update statement right after the loop label
        currentLabel = updateLabel
        translate(node.loopUpdate)

        // Translate condition statement, and branch back to the loop label if the condition is met
        currentLabel = checkLabel
        translate(node.condExpr)
        addLines(
            Compare(resultRegister, IntImmediateOperand(1)),
            Branch(EQ, BranchLabelOperand(loopLabel))
        )

        // After the loop is over, reset the state and continue translating the program
        currentLabel = endLabel
        breakLabel = oldBreak
        continueLabel = oldContinue
    }

    @TranslatorMethod
    fun translateContinueStatement(node: ContinueStatementAST) {
        addLines(
            Branch(BranchLabelOperand(continueLabel))
        )
    }

    @TranslatorMethod
    fun translateBreakStatement(node: BreakStatementAST) {
        addLines(
            Branch(BranchLabelOperand(breakLabel))
        )
    }

    @TranslatorMethod
    private fun translateBeginEndBlock(node: BeginEndBlockAST) {
        log("Translating BeginEndBlockAST")
        blockPrologue(node)
        node.getStatements().forEach { translate(it) }
        blockEpilogue(node)
    }

    @TranslatorMethod
    private fun translateNewPair(node: NewPairAST) {
        log("Translating NewPairAST")

        // Allocate two registers for newPair
        var accumulatorState = false
        if (resultRegister == Register.MAX_REG) {
            resultRegister = resultRegister.prevReg()
            addLines(Push(resultRegister))
            offsetStackStore[0] += WORD
            accumulatorState = true
        }

        addLines(
            Load(WORD, R0, PseudoImmediateOperand(2 * WORD)),
            BranchLink(MALLOC),
            Move(resultRegister, R0) // R_n
        )

        listOf(node.fstExpr, node.sndExpr).forEachIndexed { index, expr ->
            // Translate the expression and store it in memory with malloc
            resultRegister = resultRegister.nextReg() // R_n+1
            translate(expr) // result stored in // R_n+1
            addLines(
                Load(WORD, R0, PseudoImmediateOperand(expr.type.size)),
                BranchLink(MALLOC)
            )

            // Store the value of the item of the pair in the address received from malloc
            addLines(
                Store(expr.type.size, resultRegister, ZeroOffset(R0))
            )

            // Store the address of the pair item into the actual pairs memory
            addLines(
                Store(
                    WORD,
                    R0,
                    if (index == 0) {
                        ZeroOffset(resultRegister.prevReg())
                    } else {
                        ImmediateOffset(resultRegister.prevReg(), index * WORD)
                    }
                ),
            )
            // Change back to R_n
            resultRegister = resultRegister.prevReg()
        }

        // result stored in MAX_REG - 1, we move the result to MAX_REG and restore
        // original value in MAX_REG - 1
        if (accumulatorState) {
            addLines(
                Move(resultRegister.nextReg(), ZeroOffset(resultRegister)),
                Pop(resultRegister)
            )
            resultRegister = resultRegister.nextReg()
            offsetStackStore[0] -= WORD
        }
    }

    @TranslatorMethod
    private fun translateAlloc(node: AllocAST) {
        log("Translating AllocAST")

        translate(node.expr)

        addLines(
            Move(R0, resultRegister),
            BranchLink(MALLOC),
            Move(resultRegister, R0)
        )
    }

    //endregion

    //region translateExpr

    @TranslatorMethod
    private fun translateSizeOf(node: SizeOfAST) {
        log("Translating SizeOfAST")
        val size = node.sizeOfType.size
        addLines(
            Load(size, resultRegister, PseudoImmediateOperand(size))
        )
    }

    @TranslatorMethod
    private fun translateReference(node: ReferenceAST) {
        log("Translating ReferenceAST")
        getAddress(node.item.lhs)
    }

    @TranslatorMethod
    private fun translateDeref(node: DerefPointerAST) {
        log("Translating DerefPointerAST")
        // Get the address of the data
        // Essentially, perform n - 1 dereferences
        // e.g. for $$$i, load $$i into R0
        getAddressDeref(node)
        // Perform the final dereference, so find $R0
        addLines(
            Move(R0, resultRegister),
            BranchLink(P_CHECK_NULL_POINTER),
            Load(WORD, resultRegister, ZeroOffset(resultRegister))
        )
    }

    @TranslatorMethod
    private fun translateIntLiteral(node: IntLiteralAST) {
        log("Translating IntLiteralAST")
        addLines(
            Load(IntType.size, resultRegister, PseudoImmediateOperand(node.intValue))
        )
    }

    @TranslatorMethod
    private fun translateBoolLiteral(node: BoolLiteralAST) {
        log("Translating BoolLiteralAST")
        addLines(
            Move(resultRegister, BoolImmediateOperand(node.boolValue))
        )
    }

    @TranslatorMethod
    private fun translateCharLiteral(node: CharLiteralAST) {
        log("Translating CharLiteralAST")
        addLines(
            Move(resultRegister, CharImmediateOperand(node.charValue))
        )
    }

    @TranslatorMethod
    private fun translateStringLiteral(node: StringLiteralAST) {
        log("Translating StringLiteralAST")
        addLines(
            Load(WORD, resultRegister, DataLabelOperand(newStringLabel(node.stringValue)))
        )
    }

    @TranslatorMethod
    private fun translateVariableIdentifier(node: VariableIdentifierAST) {
        log("Translating VariableIdentifierAST")
        transRetrieve(node.ident, node.symbolTable)
    }

    @TranslatorMethod
    @Suppress("UNUSED_PARAMETER")
    private fun translateNullPairLiteralAST(node: NullPairLiteralAST) {
        log("Translating NullPairLiteralAST")
        addLines(
            Load(WORD, resultRegister, PseudoImmediateOperand(0))
        )
    }

    @TranslatorMethod
    private fun translateArrayLiteral(node: ArrayLiteralAST) {
        log("Translating ArrayLiteralAST")
        val elems = node.elems
        val elemSize: Int = if (node.elems.isNotEmpty()) {
            elems[0].type.size
        } else {
            0
        }
        val size = WORD + elems.size * elemSize // calculate bytes need to malloc

        // Allocate two registers for ArrayLiteral
        var accumulatorState = false
        if (resultRegister == Register.MAX_REG) {
            resultRegister = resultRegister.prevReg()
            addLines(Push(resultRegister))
            offsetStackStore[0] += WORD
            accumulatorState = true
        }

        currentLabel.addLines(
            Load(WORD, R0, PseudoImmediateOperand(size)),
            BranchLink(MALLOC),
            Move(resultRegister, R0)
        )
        // offset (in bytes) from the malloced address to store the value at
        // starts at addr + 4 because the first 4 bytes are dedicated to storing tha array's length
        var offset = WORD
        for (expr in elems) {
            val dest = resultRegister.nextReg()
            val src = resultRegister
            resultRegister = dest // R_n+1
            translate(expr)
            resultRegister = src // R_n
            currentLabel.addLines(
                Store(expr.type.size, dest, ImmediateOffset(src, offset))
            )
            offset += expr.type.size
        }

        val dest = resultRegister.nextReg() // R_n+1
        currentLabel.addLines(
            Load(WORD, dest, PseudoImmediateOperand(elems.size)),
            Store(WORD, dest, ZeroOffset(resultRegister))
        )

        // result stored in MAX_REG - 1, we move the result to MAX_REG and restore
        // original value in MAX_REG - 1
        if (accumulatorState) {
            addLines(
                Move(dest, ZeroOffset(resultRegister)),
                Pop(resultRegister)
            )
            resultRegister = dest
            offsetStackStore[0] -= WORD
        }
    }

    @TranslatorMethod
    private fun translateArrayElem(node: ArrayElemAST) {
        log("Translating ArrayElemAST")

        val oldReg = resultRegister

        defineUtilFuncs(
            P_CHECK_ARRAY_BOUNDS
        )

        // Allocate two registers for arrayElem
        var accumulatorState = false
        if (resultRegister == Register.MAX_REG) { // R10
            resultRegister = resultRegister.prevReg() // R9
            addLines(Push(resultRegister)) // Spare R9
            offsetStackStore[0] += WORD
            accumulatorState = true
        }

        // load address of value into resultRegister
        getAddressArrayElem(node)

        addLines(
            Load(node.type.size, resultRegister, ZeroOffset(resultRegister))
        )

        // result stored in MAX_REG - 1, we move the result to MAX_REG and restore
        // original value in MAX_REG - 1
        if (accumulatorState) {
            addLines(
                Move(resultRegister.nextReg(), ZeroOffset(resultRegister)),
                Pop(resultRegister)
            )
            resultRegister = resultRegister.nextReg()
            offsetStackStore[0] -= WORD
        }

        resultRegister = oldReg
    }

    @TranslatorMethod
    private fun translateAssignToArrayElem(node: AssignToArrayElemAST) {
        log("Translating AssignToArrayElemAST")

        val oldReg = resultRegister

        defineUtilFuncs(
            P_CHECK_ARRAY_BOUNDS
        )

        val arrayElemAST = node.lhs
        val arrayVariable = node.lhs.arrayVar
        val typeSize = (arrayVariable.type as ArrayType).elementType.size
        val stackPointerOffset = arrayVariable.ident.stackPosition

        addLines(
            Add(resultRegister.nextReg(), SP, IntImmediateOperand(stackPointerOffset))
        )

        translate(node.rhs)

        resultRegister = resultRegister.nextReg()
        for (i in arrayElemAST.indexExpr.indices) {
            resultRegister = resultRegister.nextReg()
            translate(arrayElemAST.indexExpr[i])
            resultRegister = resultRegister.prevReg()
            addLines(
                Load(WORD, resultRegister, ZeroOffset(resultRegister)),
                Move(R0, resultRegister.nextReg()),
                Move(R1, resultRegister)
            )
            if (node.lhs.noBoundCheckRequired == null ||
                !node.lhs.noBoundCheckRequired!![i]) {
                addLines(BranchLink(P_CHECK_ARRAY_BOUNDS))
            }
            addLines(
                Add(
                    resultRegister,
                    resultRegister,
                    IntImmediateOperand(4)
                ),
                if (typeSize == WORD) {
                    Add(
                        resultRegister,
                        resultRegister,
                        LogicalShiftLeft(resultRegister.nextReg(), 2)
                    )
                } else {
                    Add(resultRegister, resultRegister, resultRegister.nextReg())
                }
            )
        }
        resultRegister = oldReg
        val dest = ZeroOffset(resultRegister.nextReg())
        addLines(
            Store(typeSize, resultRegister, dest)
        )
    }

    @TranslatorMethod
    private fun translateFstPairElem(node: FstPairElemAST) {
        log("Translating FstPairElemAST")
        translatePairElem(node, node.elemType)
    }

    @TranslatorMethod
    private fun translateSndPairElem(node: SndPairElemAST) {
        log("Translating SndPairElemAST")
        translatePairElem(node, node.elemType)
    }

    private fun translatePairElem(node: PairElemAST, elemType: VariableType) {
        getAddressPairElem(node)
        assert(node.expr.type.size == WORD)
        addLines(
            Load(elemType.size, resultRegister, ZeroOffset(resultRegister))
        )
    }

    @TranslatorMethod
    private fun translateAssignToPairElem(node: AssignToPairElemAST) {
        log("Translating AssignToPairElemAST")
        translateAssignToPointer(node)
    }

    @TranslatorMethod
    private fun translateAssignToDeref(node: AssignToDerefAST) {
        log("Translating AssignToDerefAST")
        translateAssignToPointer(node)
    }

    private fun translateAssignToPointer(node: AssignToLhsAST<*>) {
        val oldResultRegister = resultRegister

        // Translate the expression to assign
        translate(node.rhs)
        val rhsResultRegister = resultRegister

        resultRegister = resultRegister.nextReg()

        // Get the address in memory at which we will store the rhs
        getAddress(node.lhs)

        // Write the expression result to the address
        addLines(
            Store(node.rhs.type.size, rhsResultRegister, ZeroOffset(resultRegister))
        )

        resultRegister = oldResultRegister
    }

    @TranslatorMethod
    private fun translateUnOp(unOpExpr: UnaryOpExprAST) {
        log("Translating UnaryOpExprAST")
        translate(unOpExpr.expr)

        when (unOpExpr.operator) {
            UnaryOp.BANG -> {
                addLines(
                    Xor(resultRegister, resultRegister, IntImmediateOperand(1))
                )
            }
            UnaryOp.MINUS -> {
                defineUtilFuncs(
                    P_CHECK_DIVIDE_BY_ZERO,
                    P_THROW_RUNTIME_ERROR,
                    P_THROW_OVERFLOW_ERROR,
                    P_PRINT_STRING
                )
                addLines(
                    ReverseSub(
                        updateFlags = true, resultRegister, resultRegister,
                        IntImmediateOperand(0)
                    ),
                    BranchLink(VS, P_THROW_OVERFLOW_ERROR)
                )
            }
            UnaryOp.LEN -> {
                addLines(
                    Load(WORD, resultRegister, ZeroOffset(resultRegister))
                )
            }
            else -> { }
        }
    }

    @TranslatorMethod
    private fun translateBinOp(expr: BinaryOpExprAST) {
        log("Translating BinaryOpExprAST")

        // Allocate two registers for BinOp
        var accumulatorState = false
        if (resultRegister == Register.MAX_REG) {
            resultRegister = resultRegister.prevReg()
            addLines(Push(resultRegister))
            offsetStackStore[0] += WORD
            accumulatorState = true
        }

        translate(expr.expr1)

        resultRegister = resultRegister.nextReg()
        translate(expr.expr2)
        resultRegister = resultRegister.prevReg()
        when (expr.operator) {
            PLUS, MINUS, MULT, DIV, MOD -> {
                defineUtilFuncs(
                    P_THROW_RUNTIME_ERROR,
                    P_THROW_OVERFLOW_ERROR,
                    P_PRINT_STRING
                )

                when (expr.operator) {
                    MULT -> {
                        addLines(
                            LongMult(
                                updateFlags = true,
                                resultRegister,
                                resultRegister.nextReg(),
                                resultRegister,
                                resultRegister.nextReg()
                            ),
                            Compare(
                                resultRegister.nextReg(),
                                ArithmeticShiftRight(resultRegister, 31)
                            ),
                            BranchLink(NE, P_THROW_OVERFLOW_ERROR)
                        )
                    }
                    DIV -> {
                        defineUtilFuncs(P_CHECK_DIVIDE_BY_ZERO)
                        addLines(
                            Move(R0, resultRegister),
                            Move(R1, resultRegister.nextReg()),
                            BranchLink(P_CHECK_DIVIDE_BY_ZERO),
                            BranchLink(AEABI_IDIV),
                            Move(resultRegister, R0)
                        )
                    }
                    MOD -> {
                        defineUtilFuncs(P_CHECK_DIVIDE_BY_ZERO)
                        addLines(
                            Move(R0, resultRegister),
                            Move(R1, resultRegister.nextReg()),
                            BranchLink(P_CHECK_DIVIDE_BY_ZERO),
                            BranchLink(AEABI_IDIVMOD),
                            Move(resultRegister, R1)
                        )
                    }
                    PLUS -> {
                        addLines(
                            Add(true, resultRegister, resultRegister, resultRegister.nextReg()),
                            BranchLink(VS, P_THROW_OVERFLOW_ERROR)
                        )
                    }
                    MINUS -> {
                        addLines(
                            Sub(true, resultRegister, resultRegister, resultRegister.nextReg()),
                            BranchLink(VS, P_THROW_OVERFLOW_ERROR)
                        )
                    }
                }
            }
            BinaryOp.GT, GTE, BinaryOp.LT, LTE, EQUALS, NOT_EQUALS -> {
                addLines(
                    Compare(resultRegister, resultRegister.nextReg())
                )
                when (expr.operator) {
                    BinaryOp.GT -> {
                        addLines(
                            Move(GT, resultRegister, IntImmediateOperand(1)),
                            Move(LE, resultRegister, IntImmediateOperand(0))
                        )
                    }
                    GTE -> {
                        addLines(
                            Move(GE, resultRegister, IntImmediateOperand(1)),
                            Move(LT, resultRegister, IntImmediateOperand(0))
                        )
                    }
                    BinaryOp.LT -> {
                        addLines(
                            Move(LT, resultRegister, IntImmediateOperand(1)),
                            Move(GE, resultRegister, IntImmediateOperand(0))
                        )
                    }
                    LTE -> {
                        addLines(
                            Move(LE, resultRegister, IntImmediateOperand(1)),
                            Move(GT, resultRegister, IntImmediateOperand(0))
                        )
                    }
                    EQUALS -> {
                        addLines(
                            Move(EQ, resultRegister, IntImmediateOperand(1)),
                            Move(NE, resultRegister, IntImmediateOperand(0))
                        )
                    }
                    NOT_EQUALS -> {
                        addLines(
                            Move(NE, resultRegister, IntImmediateOperand(1)),
                            Move(EQ, resultRegister, IntImmediateOperand(0))
                        )
                    }
                }
            }
            AND -> {
                addLines(And(resultRegister, resultRegister, resultRegister.nextReg()))
            }
            OR -> {
                addLines(Or(resultRegister, resultRegister, resultRegister.nextReg()))
            }
        }

        // result stored in MAX_REG - 1, we move the result to MAX_REG and restore
        // original value in MAX_REG - 1
        if (accumulatorState) {
            resultRegister = resultRegister.nextReg()
            addLines(
                Pop(resultRegister)
            )
            offsetStackStore[0] -= WORD
        }
    }

    //endregion

    //region decorators

    /**
     * Sets up the environment for the block of statements, specifically by initialising the stack variables
     */
    private fun blockPrologue(node: BlockAST) {
        blockPrologue(node.symbolTable)
    }

    private fun blockPrologue(symbolTable: SymbolTable) {
        log("Calling blockPrologue")
        // Push the value that tracks the stack space used by intermediate values
        // by the current block
        offsetStackStore.addFirst(0)

        // Calculate how much space to be allocated (and modify each variable to include its position on the stack)
        val stackSpaceUsed = symbolTable.getStackSize()
        if (stackSpaceUsed > 0) {
            var currentStackPosition = stackSpaceUsed
            var subtractLeft = stackSpaceUsed

            // Setup stack
            while (subtractLeft > 0) {
                val subtractNow = if (subtractLeft <= MAX_STACK_CHANGE) {
                    subtractLeft
                } else {
                    MAX_STACK_CHANGE
                }
                addLines(Sub(SP, SP, IntImmediateOperand(subtractNow)))
                subtractLeft -= subtractNow
            }

            // Calculate the stack position for each variable
            val variables = symbolTable.getValuesByType(Variable::class)
            for (v in variables) {
                currentStackPosition -= v.type.size
                v.stackPosition = currentStackPosition
            }
        }
    }

    /**
     * Restores the state so that the program can resume from where it left off before entering the block
     */
    private fun blockEpilogue(node: BlockAST) {
        blockEpilogue(node.symbolTable)
    }

    private fun blockEpilogue(symbolTable: SymbolTable) {
        log("Calling blockEpilogue")
        // Pop the value that tracks the stack space used by intermediate values
        // by the current block
        offsetStackStore.removeFirst()

        unwindStack(symbolTable)
    }

    private fun unwindStack(symbolTable: SymbolTable) {
        val stackSpaceUsed = symbolTable.getStackSize()
        if (stackSpaceUsed > 0) {
            var addLeft = stackSpaceUsed

            val subList = mutableListOf<Instruction>()

            // Setup stack
            while (addLeft > 0) {
                val addNow = if (addLeft <= MAX_STACK_CHANGE) {
                    addLeft
                } else {
                    MAX_STACK_CHANGE
                }
                subList.add(Add(SP, SP, IntImmediateOperand(addNow)))
                addLeft -= addNow
            }

            addLines(subList.reversed())
        }
    }

    private fun functionPrologue(
        node: FunctionDeclarationAST,
        paramSymbolTable: SymbolTable? = null
    ) {
        functionPrologue(node.symbolTable, paramSymbolTable)
    }

    private fun functionPrologue(symbolTable: SymbolTable, paramSymbolTable: SymbolTable? = null) {
        log("Calling functionPrologue")
        addLines(
            Push(LR)
        )
        if (paramSymbolTable != null) {
            // Params are pushed in reverse order to the stack
            // After the last param is pushed, the value of LR is pushed, which is a word
            // So the stack position of the first parameter is WORD
            var currentStackPos = WORD
            val params = paramSymbolTable.getValuesByType(Param::class)
            params.forEach {
                it.stackPosition = currentStackPos
                currentStackPos += it.type.size
            }
            assert(currentStackPos == paramSymbolTable.getStackSize() + WORD)
        }
        blockPrologue(symbolTable)
    }

    private fun functionEpilogue(node: FunctionDeclarationAST) {
        functionEpilogue(node.symbolTable)
    }

    private fun functionEpilogue(symbolTable: SymbolTable) {
        log("Calling functionEpilogue")
        blockEpilogue(symbolTable)
        addLines(
            Pop(PC),
            LTORG
        )
    }

    private fun mainPrologue(symbolTable: SymbolTable) {
        val mainLabel = newBranchLabel("main")
        currentLabel = mainLabel
        functionPrologue(symbolTable)
    }

    private fun mainEpilogue(symbolTable: SymbolTable) {
        blockEpilogue(symbolTable)
        addLines(
            Load(WORD, R0, PseudoImmediateOperand(0)),
            Pop(PC),
            LTORG
        )
    }

    //endregion

    //region helpers

    private fun addLines(vararg lines: Instruction) {
        currentLabel.addLines(*lines)
    }

    private fun addLines(lines: Collection<Instruction>) {
        currentLabel.addLines(lines)
    }

    private fun transAssign(variable: Variable, currentScope: SymbolTable) {
        log("Calling transAssign")
        variableAction(variable, currentScope, store = true)
    }

    private fun transRetrieve(variable: Variable, currentScope: SymbolTable) {
        log("Calling transRetrieve")
        variableAction(variable, currentScope, store = false)
    }

    private fun variableAction(
        variable: Variable,
        currentScope: SymbolTable,
        store: Boolean
    ) {
        val scopeOffset = currentScope.calcScopeOffset(variable)
        val size = variable.type.size

        // Consider the case where intermediate results are pushed on the stack
        val pos = variable.stackPosition + offsetStackStore[0] + scopeOffset
        val addressOperand = if (pos == 0) {
            ZeroOffset(SP)
        } else {
            ImmediateOffset(SP, pos)
        }

        if (store) {
            addLines(
                Store(size, resultRegister, addressOperand)
            )
        } else {
            addLines(
                Load(size, resultRegister, addressOperand)
            )
        }
    }

    private fun getAddress(assignTo: AssignRhsAST) {
        when (assignTo) {
            is VariableIdentifierAST -> {
                getAddressIdent(assignTo)
            }
            is ArrayElemAST -> {
                getAddressArrayElem(assignTo)
            }
            is PairElemAST -> {
                getAddressPairElem(assignTo)
            }
            is DerefPointerAST -> {
                getAddressDeref(assignTo)
            }
            is BinaryOpExprAST -> {
                assert(assignTo.type is PointerType)
                translate(assignTo)
            }
            else -> { throw IllegalArgumentException("cannot get address of this type") }
        }
    }

    private fun getAddressIdent(variable: VariableIdentifierAST) {
        addLines(
            Add(resultRegister, SP, IntImmediateOperand(variable.ident.stackPosition))
        )
    }

    private fun getAddressArrayElem(arrayElem: ArrayElemAST) {
        val variable = arrayElem.arrayVar.ident

        addLines(Add(resultRegister, SP, IntImmediateOperand(variable.stackPosition)))

        for (i in arrayElem.indexExpr.indices) {
            val index = arrayElem.indexExpr[i]

            resultRegister = resultRegister.nextReg()
            translate(index)
            resultRegister = resultRegister.prevReg()

            addLines(
                Load(WORD, resultRegister, ZeroOffset(resultRegister)),
                // check bounds of array
                Move(R0, resultRegister.nextReg()),
                Move(R1, resultRegister)
            )

            if (arrayElem.noBoundCheckRequired == null ||
                !arrayElem.noBoundCheckRequired!![i]) {
                addLines(BranchLink(P_CHECK_ARRAY_BOUNDS))
            }
            addLines(
                Add(resultRegister, resultRegister, IntImmediateOperand(WORD)),
                // get address of desired index into result reg
                Add(
                    resultRegister,
                    resultRegister,
                    LogicalShiftLeft(resultRegister.nextReg(), 2)
                )
            )
        }
    }

    private fun getAddressPairElem(pairElemAST: PairElemAST) {
        defineUtilFuncs(
            P_CHECK_NULL_POINTER
        )
        translate(pairElemAST.expr)
        val offset = if (pairElemAST is FstPairElemAST) {
            ZeroOffset(resultRegister)
        } else {
            ImmediateOffset(resultRegister, WORD)
        }
        addLines(
            Move(R0, resultRegister),
            BranchLink(P_CHECK_NULL_POINTER),
            Load(WORD, resultRegister, offset)
        )
    }

    private fun getAddressDeref(derefPointerAST: DerefPointerAST) {
        defineUtilFuncs(
            P_CHECK_NULL_POINTER
        )
        translate(derefPointerAST.expr)
        for (i in 0 until derefPointerAST.numDerefs - 1) {
            addLines(
                Move(R0, resultRegister),
                BranchLink(P_CHECK_NULL_POINTER),
                Load(WORD, resultRegister, ZeroOffset(resultRegister))
            )
        }
    }

    //endregion
}
