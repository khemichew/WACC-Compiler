package ic.doc.group15.codegen

import ic.doc.group15.SymbolTable
import ic.doc.group15.ast.*
import ic.doc.group15.codegen.assembly.*
import ic.doc.group15.codegen.assembly.LibraryFunction.Companion.AEABI_IDIV
import ic.doc.group15.codegen.assembly.LibraryFunction.Companion.AEABI_IDIVMOD
import ic.doc.group15.codegen.assembly.LibraryFunction.Companion.MALLOC
import ic.doc.group15.codegen.assembly.UtilFunction.*
import ic.doc.group15.codegen.assembly.instruction.*
import ic.doc.group15.codegen.assembly.instruction.ConditionCode.*
import ic.doc.group15.codegen.assembly.operand.*
import ic.doc.group15.codegen.assembly.operand.Register.*
import ic.doc.group15.type.BasicType.*
import ic.doc.group15.type.FunctionType

const val START_VAL = 0

class AssemblyGenerator(private val ast: AST, private val st: SymbolTable) {

    private val state : State = State()

    private var sp: Int = START_VAL - 1

    /**
     * Represents the ".dataLabel" section of the assembly code.
     *
     * Contains info for raw dataLabel in memory, such as string literals.
     */
    private val data: MutableMap<String, DataLabel> = mutableMapOf()
    private val utilData: MutableMap<String, DataLabel> = mutableMapOf()

    /**
     * Represents the ".text" section of the assembly code.
     *
     * Contains labels that can be branched to, and the main function.
     */
    private val text: MutableMap<String, BranchLabel> = mutableMapOf()
    private val utilText: MutableMap<String, BranchLabel> = mutableMapOf()

    private val stringLabelGenerator = UniqueStringLabelGenerator()
    private val branchLabelGenerator = UniqueBranchLabelGenerator()

    fun generate(): String {
        var asm = ""
        if (data.isNotEmpty() || utilData.isNotEmpty()) {
            asm += ".data\n\n"
        }
        asm += joinAsm(data.values) +
               joinAsm(utilData.values) +
               ".text\n\n.global main\n" +
               joinAsm(text.values) +
               joinAsm(utilText.values)
        return asm
    }

    private fun joinAsm(asm: Collection<Assembly>): String {
        return asm.joinToString(separator = "\n", postfix = if (asm.isNotEmpty()) "\n" else "")
    }

    // when we enter a block, we will prematurely calculate how much stack space that block will need by summing the
    // size in bytes of each of the variables in its symbol table. then we will be able to decrement the stack pointer
    // by this amount in one go leaving enough space for the execution of the entire block. the below function takes
    // in a block and returns the amount of stack space it will require
    fun requiredStackSpace(node: BlockAST): Int {
        return node.symbolTable.getStackSize()
    }

    /**
     * Per WACC language specification, a program matches the grammar "begin func* stat end".
     * The AST representation decouples the statements from a SequenceAST to a mapping of lists of StatementAST
     * in the symbol table to avoid stack overflow from recursing a huge block of statements.
     *
     * transProgram dissolves the program into a list of functions, pass the FunctionDeclarationAST
     * into transFunction, and add the list of statements of the main program into a FunctionDeclarationAST,
     * thereby processing it via transFunction as well.
     */
    @Translator(AST::class)
    private fun transProgram(program: AST) {
        val functionASTs = program.statements.filterIsInstance<FunctionDeclarationAST>()

        // translate all function blocks into assembly
        functionASTs.map { f -> f as FunctionDeclarationAST }
            .forEach { f -> transFunctionDeclaration(f) }

        // Translate main instructions into assembly
        // We create a new FunctionDeclarationAST to store statements in the main function
        // and let transFunction add the entries to the text attribute

        // The symbol table contains the statements AND the FunctionDeclarationAST
        // For future proofing, we might want to remove all FunctionDeclarationASTs when
        // nested function definition is introduced in the extension task
        val mainAST = FunctionDeclarationAST(program, program.symbolTable, IntType, "main")
        mainAST.funcIdent = FunctionType(IntType, emptyList(), program.symbolTable)
        mainAST.returnStat = ReturnStatementAST(mainAST, mainAST.symbolTable.subScope(), IntLiteralAST(0))
        transFunctionDeclaration(mainAST)
    }

    // TODO: Fix transBlock
    private fun transBlock(block: BlockAST, resultReg: Register, currentLabel: BranchLabel) {
        val instructions = mutableListOf<Instruction>()
        var stackSpace = requiredStackSpace(block)
        sp -= stackSpace
        instructions.add(Sub(SP, SP, ImmediateOperand(stackSpace)))
        val statements: List<StatementAST> = block.statements
        for (stat in statements) {
            instructions.addAll(transStat(stat, resultReg))
        }
        sp += stackSpace
        instructions.add(Add(SP, SP, ImmediateOperand(stackSpace)))
        this.state.popStacks(
            block.symbolTable.getMap().keys.parallelStream().toList()
        ) // read comments in the State class to understand why we do this

        currentLabel.addLines(instructions)
    }

    @Translator(FunctionDeclarationAST::class)
    private fun transFunctionDeclaration(funcDec: FunctionDeclarationAST): BranchLabel {

        // i think im gonna have to push all registers to stack then pop
        // before and after a function call cus if you look at the reference compiler,
        // the function definitions always assume they can use whatever registers
        // they want, so i assume the registers will have been pushed to the stack.

        // however... it seems that there is no situation where a function
        // can be called while there are registers in use actually so maybe
        // i dont need to do that

        val instructions = mutableListOf<Line>()
        var pos = 0
        for (i in funcDec.formals) {
            state.setStackPos(i.varName, sp + pos)
            pos += i.type.sizeInBytes()
        }
        val stackSpace =
            requiredStackSpace(funcDec) - pos // the required stack space function will take into account the parameters as they are part of the symbol table for functions but we have already taken these into account
        sp -= stackSpace
        // functions are missing labels for now as well as .ltorg at the end
        instructions.addAll(
            mutableListOf(
                Push(LR),
                Sub(SP, SP, ImmediateOperand(stackSpace))
            )
        )
        instructions.addAll(transBlock((funcDec) as BlockAST, R4))
        instructions.add(Move(R0, R4))
        instructions.add(Add(SP, SP, ImmediateOperand(stackSpace)))
        instructions.add(Pop(PC))
        instructions.add(Pop(PC))
        return instructions
    }

    @Translator(CallAST::class)
    private fun transCall(call: CallAST, resultReg: Register) {
        val instructions = mutableListOf<Line>()
        // parameters will be put onto the stack in reverse order e.g. f(a, b, c)
        // stack will look like so:
        // c
        // b
        // a
        val revParams = call.actuals.toMutableList().reversed()
        // below we decrement the stack pointer by the amount needed to
        // store the type of that parameter then put that parameter on
        // the stack at that position. we also keep track of spDec (the
        // total amount we had to decrement the stack pointer for the
        // arguments) so that after executing the function we can add
        // the same amount back to the stack pointer
        var spDec = 0
        for (i in revParams) {
            sp -= i.type.sizeInBytes()
            spDec += i.type.sizeInBytes()
            instructions.addAll(transExp(i, resultReg))
            instructions.add(Sub(SP, SP, ImmediateOperand(-i.type.sizeInBytes())))
            instructions.add(StoreWord(SP, ZeroOffset(resultReg)))
        }
        instructions.add(BranchLink("p_" + call.funcName))
        instructions.add(Add(SP, SP, ImmediateOperand(spDec)))
        instructions.add(Move(resultReg, R0))
    }

    @Translator(VariableDeclarationAST::class)
    private fun transVariableDeclaration(
        node: VariableDeclarationAST,
        resultReg: Register
    ): List<Line> {
        val instructions = mutableListOf<Line>()
        sp -= node.varIdent.type.sizeInBytes()
        state.setStackPos(node.varName, sp)
        instructions.addAll(transAssignRhs(node.rhs, resultReg))
        instructions.add(StoreWord(SP, ImmediateOffset(resultReg, state.getStackPos(node.varName))))
        return instructions
    }

    @Translator(AssignToIdentAST::class)
    private fun transAssignToIdent(node: AssignToIdentAST, resultReg: Register): List<Line> {
        val instructions = mutableListOf<Line>()
        instructions.addAll(transAssignRhs(node.rhs, resultReg))
        instructions.add(
            StoreWord(
                SP,
                ImmediateOffset(resultReg, state.getStackPos((node.lhs as VariableIdentifierAST).varName) - sp)
            )
        ) // dk y but it assumed node.lhs was just an ASTNode so i had to cast it
        return instructions
    }

    @Translator(AssignToArrayElemAST::class)
    private fun transAssignToArrayElem(
        node: AssignToArrayElemAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(AssignToPairElemAST::class)
    private fun transAssignToPairElem(
        node: AssignToPairElemAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(ArrayLiteralAST::class)
    private fun transArrayLiteral(node: ArrayLiteralAST, resultReg: Register): List<Line> {
        val instructions = mutableListOf<Line>()
        val size = 4 + (node.elems.size * (node.elems[0].type.sizeInBytes())) // calculate bytes need to malloc
        instructions.add(LoadWord(R0, ImmediateOperand(size))) // set up to malloc those bytes
        instructions.add(BranchLink(MALLOC))
        instructions.add(Move(resultReg, R0)) // malloc returns us address in R0, we put that in resultReg
        var offset = 0 // now we go in this for loop to put all the items of the array into the memory of the array
        for (expr in node.elems) {
            instructions.addAll(transExp(expr, resultReg.nextReg()))
            if (expr.type.sizeInBytes() == 4) {
                offset += 4
                instructions.add(StoreWord(resultReg.nextReg(), ImmediateOffset(resultReg, offset)))
            } else {
                offset += 1
                instructions.add(StoreByte(resultReg.nextReg(), ImmediateOffset(resultReg, offset)))
            }
        }
        instructions.add(LoadWord(resultReg.nextReg(), ImmediateOperand(node.elems.size))) // storing the size in the first memory space of the array
        instructions.add(StoreWord(resultReg.nextReg(), ZeroOffset(resultReg)))
        return instructions
    }

    @Translator(FstPairElemAST::class)
    fun transFstPairElem(
        node: FstPairElemAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(SndPairElemAST::class)
    fun transSndPairElem(
        node: SndPairElemAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(ElseBlockAST::class)
    fun transElseBlock(
        node: ElseBlockAST, resultReg:
        Register
    ): List<Line> {
//        TODO?
        return emptyList()
    }

    @Translator(WhileBlockAST::class)
    fun transWhileBlock(
        node: WhileBlockAST, resultReg:
        Register
    ): List<Line> {
//        TODO?
        return emptyList()
    }

    @Translator(BeginEndBlockAST::class)
    fun transBeginEndBlock(
        node: BeginEndBlockAST, resultReg:
        Register
    ): List<Line> {
//        TODO?
        return emptyList()
    }

    @Translator(ArrayElemAST::class)
    fun transArrayElem(
        node: ArrayElemAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(ParameterAST::class)
    fun transParameter(
        node: ParameterAST, resultReg:
        Register
    ): List<Line> {
//        TODO?
        return emptyList()
    }

    @Translator(FreeStatementAST::class)
    fun transFreeStatement(
        node: FreeStatementAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(ReturnStatementAST::class)
    fun transReturnStatement(
        node: ReturnStatementAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(ExitStatementAST::class)
    fun transExitStatement(
        node: ExitStatementAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(PrintStatementAST::class)
    fun transPrintStatement(
        node: PrintStatementAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(PrintlnStatementAST::class)
    fun transPrintlnStatement(
        node: PrintlnStatementAST, resultReg:
        Register
    ): List<Line> {
//        TODO
        return emptyList()
    }

    @Translator(ReadStatementAST::class)
    fun transReadStatement(node: ReadStatementAST, resultReg: Register): List<Line> {
        val instructions = mutableListOf<Line>()
        when (node.target.type) {
            IntType -> {
                defineUtilFuncs(P_READ_INT)
                instructions.addAll(transExp(node.target, resultReg))
                instructions.add(Move(R0, resultReg))
                instructions.add(BranchLink(P_READ_INT))
            }
            // complete remaining types...
        }
        return instructions
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
    @Translator(IfBlockAST::class)
    fun transIfBlock(
        stat: IfBlockAST,
        resultReg: Register,
        currentLabel: BranchLabel,
        elseLabel: BranchLabel,
        fiLabel: BranchLabel
    ) {
        // Add instructions to currentLabel
        transExp(stat.condExpr, resultReg, currentLabel)

        // compare to set condition flags and branch (can be optimised)
        currentLabel.addLines(
            Compare(resultReg, ImmediateOperand(0)),
            Branch(EQ, BranchLabelOperand(elseLabel))
        )

        // add sequence of instructions in THEN block under if label
        transBlock(stat, resultReg, currentLabel)

        // add branch to fi label under if label
        currentLabel.addLine(Branch(BranchLabelOperand(fiLabel)))

        // add sequence of instructions in ELSE block under else label
        transBlock(stat.elseBlock, resultReg, elseLabel)

        // add labels to text/instruction segment
        // possible optimisation: check if no instructions contained in label then un-include label
        // fi label not added since it becomes the current label
        text[currentLabel.name] = currentLabel
        text[elseLabel.name] = elseLabel
    }

    @Translator(AssignRhsAST::class)
    fun transAssignRhs(node: AssignRhsAST, resultReg: Register): List<Line> {
        val instructions = mutableListOf<Line>()
        when (node) {
            is ExpressionAST -> instructions.addAll(transExp(node, resultReg))
            is NewPairAST -> instructions.addAll(transNewPair(node, resultReg))
            is ArrayLiteralAST -> instructions.addAll(transArrayLiteral(node, resultReg))
            is PairElemAST -> {
//                TODO
            }
            // complete remaining types...
        }
        return instructions
    }

    @Translator(NewPairAST::class)
    fun transNewPair(node: NewPairAST, resultReg: Register): List<Line> {
        val instructions = mutableListOf<Line>()
        instructions.add(LoadWord(R0, ImmediateOperand(8))) // sets up malloc arg to alloc 8 bytes for the pair
        instructions.add(BranchLink(MALLOC))
        instructions.add(
            Move(
                resultReg,
                R0
            )
        ) // malloc left alloced address in R0, so here we put this address in resultReg
        instructions.addAll(
            transExp(
                node.fstExpr,
                resultReg.nextReg()
            )
        ) // evaluates the first item of the pair and leaves the result in resultReg + 1
        instructions.add(
            LoadWord(
                R0,
                ImmediateOperand(node.fstExpr.type.sizeInBytes())
            )
        ) // sets up malloc arg to allocate sufficient bytes for the type of pair.fst
        instructions.add(BranchLink(MALLOC)) // jumps to malloc
        if (node.fstExpr.type.sizeInBytes() == 4) {
            instructions.add(
                StoreWord(
                    resultReg.nextReg(),
                    ZeroOffset(R0)
                )
            ) // stores the value of the first item of the pair in the address received from malloc
        } else if (node.fstExpr.type.sizeInBytes() == 1) {
            instructions.add(
                StoreByte(
                    resultReg.nextReg(),
                    ZeroOffset(R0)
                )
            ) // same as above but for single byte size types
        }
        instructions.add(
            StoreWord(
                R0,
                ZeroOffset(resultReg)
            )
        ) // stores the address of the pair.fst item into the actual pairs memory
        instructions.addAll(
            transExp(
                node.sndExpr,
                resultReg.nextReg()
            )
        ) // evaluates the second item of the pair and leaves the result in resultReg + 1
        instructions.add(
            LoadWord(
                R0,
                ImmediateOperand(node.sndExpr.type.sizeInBytes())
            )
        ) // sets up malloc arg to allocate sufficient bytes for the type of pair.snd
        instructions.add(BranchLink(MALLOC)) // jumps to malloc
        if (node.sndExpr.type.sizeInBytes() == 4) {
            instructions.add(
                StoreWord(
                    resultReg.nextReg(),
                    ZeroOffset(R0)
                )
            ) // stores the value of the second item of the pair in the address received from malloc
        } else if (node.sndExpr.type.sizeInBytes() == 1) {
            instructions.add(
                StoreByte(
                    resultReg.nextReg(),
                    ZeroOffset(R0)
                )
            ) // same as above but for single byte size types
        }
        instructions.add(
            StoreWord(
                resultReg.nextReg(),
                ImmediateOffset(resultReg, 4)
            )
        ) // stores the address of the pair.snd item into the actual pairs memory
        return instructions
    }

    fun transExp(expr: ExpressionAST, resultReg: Register): List<Line> {
        val instructions: MutableList<Line> = mutableListOf()
        when (expr) {
            is IntLiteralAST -> {
                instructions.add(LoadWord(resultReg, ImmediateOperand(expr.intValue)))
            }
            is BoolLiteralAST -> {
                instructions.add(Move(resultReg, ImmediateOperand(expr.boolValue)))
            }
            is CharLiteralAST -> {
                instructions.add(Move(resultReg, ImmediateOperand(expr.charValue)))
            }
            is StringLiteralAST -> {
                val label: String = stringLabelGenerator.generate()
                data.put(label, StringData(label, expr.stringValue))
                instructions.add(LoadWord(resultReg, BranchLabelOperand(label)))
            }
            is NullPairLiteralAST -> {
//                TODO
            }
            is VariableIdentifierAST -> {
                when (expr.type) {
                    IntType -> instructions.add(
                        LoadWord(
                            SP,
                            ImmediateOffset(resultReg, state.getStackPos(expr.varName) - sp)
                        )
                    )
                    BoolType -> instructions.add(
                        LoadByte(
                            SP,
                            ImmediateOffset(resultReg, state.getStackPos(expr.varName) - sp)
                        )
                    )
                    CharType -> instructions.add(
                        LoadByte(
                            SP,
                            ImmediateOffset(resultReg, state.getStackPos(expr.varName) - sp)
                        )
                    )
                    StringType -> instructions.add(
                        LoadWord(
                            SP,
                            ImmediateOffset(resultReg, state.getStackPos(expr.varName) - sp)
                        )
                    )
                }
            }
            is ArrayElemAST -> {
//                TODO
            }
            is BinaryOpExprAST -> instructions.addAll(transBinOp(expr, resultReg))
            is UnaryOpExprAST -> instructions.addAll(transUnOp(expr, resultReg))
        }
        return instructions
    }

    fun transBinOp(expr: BinaryOpExprAST, resultReg: Register): List<Line> {
        val instructions = mutableListOf<Line>()
        instructions.addAll(transExp(expr.expr1, resultReg))
        instructions.addAll(transExp(expr.expr2, resultReg.nextReg()))
        when {
            setOf(
                BinaryOp.PLUS,
                BinaryOp.MINUS,
                BinaryOp.MULT,
                BinaryOp.DIV,
                BinaryOp.MOD
            ).contains(expr.operator) -> {
                val label1 : String = stringLabelGenerator.generate()
                data[label1] = StringData(label1, "DivideByZeroError: divide " +
                        "or modulo by zero\\n\\0")
                val label2: String = stringLabelGenerator.generate()
                data[label2] = StringData(label2, "%.*s\\0")
                defineUtilFuncs(
                    P_CHECK_DIVIDE_BY_ZERO,
                    P_THROW_RUNTIME_ERROR,
                    P_PRINT_STRING
                )
                when (expr.operator) {
                    BinaryOp.MULT -> {
                        instructions.addAll(listOf(
                            Mult(
                                updateFlags = true,
                                resultReg,
                                resultReg,
                                resultReg.nextReg()
                            )
//                TODO: use SMULL and check for overflow using schema below
//                SMULL resultReg, resultReg.nextReg(), resultReg, resultReg.nextReg()
//                CMP resultReg.next(), resultReg, ASR #31
//                BLNE p_throw_overflow_error
                        ))
                    }
                    BinaryOp.DIV -> {
                        instructions.addAll(listOf(
                            Move(R0, resultReg),
                            Move(R1, resultReg.nextReg()),
                            BranchLink(P_CHECK_DIVIDE_BY_ZERO),
                            BranchLink(AEABI_IDIV),
                            Move(resultReg, R0))
                        )
                    }
                    BinaryOp.MOD -> {
                        instructions.addAll(listOf(
                            Move(R0, resultReg),
                            Move(R1, resultReg.nextReg()),
                            BranchLink(P_CHECK_DIVIDE_BY_ZERO),
                            BranchLink(AEABI_IDIVMOD),
                            Move(resultReg, R1)
                        ))
                    }
                    BinaryOp.PLUS -> {
                        instructions.addAll(listOf(
                            Add(true, resultReg, resultReg, resultReg.nextReg()),
                            BranchLink(V, P_THROW_OVERFLOW_ERROR)
                        ))
                    }
                    BinaryOp.MINUS -> {
                        instructions.addAll(listOf(
                            Sub(true, resultReg, resultReg, resultReg.nextReg()),
                            BranchLink(V, P_THROW_OVERFLOW_ERROR)
                        ))
                    }
                }
            }
            setOf(
                BinaryOp.GTE,
                BinaryOp.LT,
                BinaryOp.LTE,
                BinaryOp.EQUALS,
                BinaryOp.NOT_EQUALS
            ).contains(expr.operator) -> {
                instructions.addAll(listOf(
                    Compare(resultReg, resultReg.nextReg())
                ))
                when (expr.operator) {
                    BinaryOp.GT -> {
                        instructions.addAll(listOf(
                            Move(GT, resultReg, ImmediateOperand(1)),
                            Move(LE, resultReg, ImmediateOperand(0))
                        ))
                    }
                    BinaryOp.GTE -> {
                        instructions.addAll(listOf(
                            Move(GE, resultReg, ImmediateOperand(1)),
                            Move(LT, resultReg, ImmediateOperand(0))
                        ))
                    }
                    BinaryOp.LT -> {
                        instructions.addAll(listOf(
                            Move(LT, resultReg, ImmediateOperand(1)),
                            Move(GE, resultReg, ImmediateOperand(0))
                        ))
                    }
                    BinaryOp.LTE -> {
                        instructions.addAll(listOf(
                            Move(LE, resultReg, ImmediateOperand(1)),
                            Move(GT, resultReg, ImmediateOperand(0))
                        ))
                    }
                    BinaryOp.EQUALS -> {
                        instructions.addAll(listOf(
                            Move(EQ, resultReg, ImmediateOperand(1)),
                            Move(NE, resultReg, ImmediateOperand(0))
                        ))
                    }
                    BinaryOp.NOT_EQUALS -> {
                        instructions.addAll(listOf(
                            Move(NE, resultReg, ImmediateOperand(1)),
                            Move(EQ, resultReg, ImmediateOperand(0))
                        ))
                    }
                }
            }
            expr.operator == BinaryOp.AND -> {
                instructions.add(And(resultReg, resultReg, resultReg.nextReg()))
            }
            expr.operator == BinaryOp.OR -> {
                instructions.add(Or(resultReg, resultReg, resultReg.nextReg()))
            }
        }
        return instructions
    }

    fun transUnOp(unOpExpr: UnaryOpExprAST, resultReg: Register): List<Line> {
        val instructions = mutableListOf<Line>()
        instructions.addAll(transExp(unOpExpr.expr, resultReg))

        when (unOpExpr.operator) {
            UnaryOp.BANG -> {
                instructions.add(Xor(resultReg, resultReg, ImmediateOperand(1)))
            }
            UnaryOp.MINUS -> {
                val label1 : String = stringLabelGenerator.generate()
                data[label1] = StringData(label1, "DivideByZeroError: divide " +
                        "or modulo by zero\\n\\0")
                val label2: String = stringLabelGenerator.generate()
                data[label2] = StringData(label2, "%.*s\\0")
                defineUtilFuncs(
                    P_CHECK_DIVIDE_BY_ZERO,
                    P_THROW_RUNTIME_ERROR,
                    P_PRINT_STRING
                )
                instructions.addAll(listOf(
                    ReverseSub(resultReg, resultReg, ImmediateOperand(0)),
                    BranchLink(P_THROW_OVERFLOW_ERROR)
                ))
            }
            UnaryOp.LEN -> {
                 instructions.add(LoadWord(resultReg, resultReg))
            }
            UnaryOp.ORD -> {
//                No actions needed since int ~ char
            }
            UnaryOp.CHR -> {
//                No actions needed since int ~ char
            }
        }

        return instructions
    }

    private fun defineUtilFuncs(vararg funcs: UtilFunction) {
        funcs.forEach { func ->
            if (!utilText.containsKey(func.labelName)) {
                func.dataBlocks.forEach {
                    utilData[it.name] = it
                }
                utilText[func.labelName] = func.labelBlock
            }
        }
    }
}
