package ic.doc.group15.visitor

import ic.doc.group15.SymbolTable
import ic.doc.group15.antlr.WaccParser.*
import ic.doc.group15.antlr.WaccParserBaseVisitor
import ic.doc.group15.ast.* // ktlint-disable no-unused-imports
import ic.doc.group15.ast.BinaryOp
import ic.doc.group15.error.SemanticErrorList
import ic.doc.group15.error.SyntacticErrorList
import ic.doc.group15.error.semantic.*
import ic.doc.group15.error.syntactic.SyntacticError
import ic.doc.group15.type.* // ktlint-disable no-unused-imports
import ic.doc.group15.type.BasicType.Companion.IntType
import ic.doc.group15.util.EscapeChar
import org.antlr.v4.runtime.ParserRuleContext
import java.util.*

class ParseTreeVisitor(
    private val topAst: AST,
    private val topSymbolTable: SymbolTable,
    private val syntacticErrors: SyntacticErrorList,
    private val semanticErrors: SemanticErrorList,
    private val enableLogging: Boolean = false
) : WaccParserBaseVisitor<ASTNode>() {

    private var scopeAST: BlockAST = topAst
    private var symbolTable = topSymbolTable

    private val functionsToVisit: MutableMap<String, FuncContext> = mutableMapOf()
    private val declaredFunctions: MutableMap<String, FunctionDeclarationAST> = mutableMapOf()

    //region statements_and_blocks

    override fun visitProgram(ctx: ProgramContext): ASTNode {
        assert(symbolTable.isTopLevel())

        log(
            """----------------
               |Begin program semantic analysis
            """
        )
        val program = scopeAST as AST

        log("Getting function names")
        for (func in ctx.func()) {
            functionsToVisit[func.ident().text] = func
        }
        log("Function names noted")

        log("Visiting function definitions")
        for (func in ctx.func()) {
            visit(func)
        }

        log("Visiting main program")
        visit(ctx.stat())

        log(
            """Semantic analysis complete!
               |----------------
               |
            """
        )

        return program
    }

    override fun visitFunc(ctx: FuncContext): ASTNode {
        val ident = ctx.ident()
        val funcName = ident.text

        if (!symbolTable.isTopLevel()) {
            addError(FunctionDeclarationInWrongScopeError(ident.start))
        }

        val oldScope = scopeAST
        val oldSt = symbolTable

        val paramSt = oldSt.subScope()
        val funcSt = paramSt.subScope()

        if (!functionsToVisit.containsKey(funcName)) {
            assert(declaredFunctions.containsKey(funcName))
            log("Function $funcName already declared!")
            return declaredFunctions[funcName]!!
        }

        log(
            """Visiting function declaration
                || Function name: $funcName
            """
        )

        val returnTypeName = ctx.return_type().text

        log(""" || Return type: $returnTypeName""")

        var t = TypeParser.parse(symbolTable, ctx.return_type())
        val f = oldSt.lookupAll(funcName)

        when {
            t !is ReturnableType -> {
                addError(NotReturnableError(ctx.return_type().start, t))
                t = Type.ANY
            }
            f != null -> {
                addError(FunctionAlreadyDeclaredError(ident.start, funcName))
            }
        }

        val func = FunctionDeclarationAST(scopeAST, paramSt, funcSt, t, funcName)
        declaredFunctions[funcName] = func

        log(
            """Visiting parameters of function ${func.funcName}"""
        )

        // Add function identifier to symbol table so that it can be recursively accessed

        symbolTable = paramSt
        scopeAST = func
        for (param in ctx.param()) {
            func.formals.add(visitParam(param) as ParameterAST)
        }
        symbolTable = oldSt

        func.funcIdent = FunctionType(
            t as ReturnableType,
            func.formals.map { it.ident },
            funcSt
        )
        oldSt.add(funcName, func.funcIdent)

        symbolTable = funcSt
        log("|| Visiting $funcName function body")
        visit(ctx.stat())
        symbolTable = oldSt
        scopeAST = oldScope

        return scopeAST.addStatement(func)
    }

    override fun visitParam(ctx: ParamContext): ASTNode {
        val type = ctx.type()
        val typeName = type.text
        val param = ctx.ident()
        val paramName = param.text

        log(
            """Visiting parameter
                || Type name: $typeName
                || Param name: $paramName
            """
        )

        val parameterAST: ParameterAST

        val t = TypeParser.parse(symbolTable, type)
        val p = symbolTable.lookup(paramName)

        when {
            t !is VariableType -> {
                addError(CannotBeParameterError(type.start, t))
            }
            p != null -> {
                if (p is Param) {
                    addError(ParameterAlreadyDeclaredError(param.start, paramName))
                }
            }
        }

        parameterAST = ParameterAST(symbolTable, paramName, Param(t as VariableType))

        symbolTable.add(paramName, parameterAST.ident)

        return parameterAST
    }

    override fun visitPrintStat(ctx: PrintStatContext): ASTNode {
        log("Visiting print statement")

        val expr = visit(ctx.expr()) as ExpressionAST

        return scopeAST.addStatement(PrintStatementAST(scopeAST, symbolTable, expr))
    }

    override fun visitPrintlnStat(ctx: PrintlnStatContext): ASTNode {
        log("Visiting println statement")

        val expr = visit(ctx.expr()) as ExpressionAST

        return scopeAST.addStatement(PrintlnStatementAST(scopeAST, symbolTable, expr))
    }

    override fun visitExitStat(ctx: ExitStatContext): ASTNode {
        log("Visiting exit statement")

        val expr = visit(ctx.expr()) as ExpressionAST
        if (expr.type != IntType) {
            addError(ExitTypeError(ctx.expr().start, expr.type))
        }

        return scopeAST.addStatement(ExitStatementAST(scopeAST, symbolTable, expr))
    }

    override fun visitFreeStat(ctx: FreeStatContext): ASTNode {
        log("Visiting free statement")

        val ctxExpr = ctx.expr()
        val expr = visit(ctxExpr) as ExpressionAST
        if (expr.type !is HeapAllocatedType) {
            addError(FreeTypeError(ctxExpr.start, expr.type))
        }

        return scopeAST.addStatement(FreeStatementAST(scopeAST, symbolTable, expr))
    }

    override fun visitSkipStat(ctx: SkipStatContext?): ASTNode {
        log("Visiting skip statement")

        return scopeAST.addStatement(SkipStatementAST(scopeAST))
    }

    override fun visitCallStat(ctx: CallStatContext): ASTNode {
        return visitCall(ctx.call(), true)
    }

    private fun visitCall(ctx: CallContext, statement: Boolean = true): ASTNode {
        val ident = ctx.ident()
        val funcName = ident.text
        log("Visiting $funcName function call")
        var f = symbolTable.lookupAll(funcName)

        val args: MutableList<ExprContext> =
            if (ctx.arg_list() != null) ctx.arg_list().expr() else mutableListOf()

        if (f == null) {
            if (functionsToVisit.containsKey(funcName)) {
                log("Function $funcName called before it was defined!")
                val oldAst = scopeAST
                val oldSt = symbolTable
                // Go back to the top scope as that is the only place where functions can be
                // defined
                scopeAST = topAst
                symbolTable = topSymbolTable
                f = (visitFunc(functionsToVisit[funcName]!!) as FunctionDeclarationAST).funcIdent
                scopeAST = oldAst
                symbolTable = oldSt
                functionsToVisit.remove(funcName)
            } else {
                addError(IdentifierNotDefinedError(ident.start, funcName))
            }
        }

        if (f !is FunctionType) {
            addError(NotAFunctionError(ident.start, funcName))
        }
        f = f as FunctionType

        val actuals: MutableList<ExpressionAST> = LinkedList()

        if (f.formals.size != args.size) {
            addError(
                NumArgumentsError(ctx.arg_list().start, funcName, f.formals.size, args.size)
            )
        }

        for (k in args.indices) {
            if (k >= f.formals.size) {
                break
            }
            val arg = args[k]
            val argExpr = visit(arg) as ExpressionAST
            if (!f.formals[k].type.compatible(argExpr.type)) {
                addError(
                    ParameterTypeError(arg.start, funcName, k, f.formals[k].type, argExpr.type)
                )
            }
            actuals.add(argExpr)
        }

        val stat = CallStatementAST(scopeAST, symbolTable, funcName, f, actuals)
        if (statement) {
            scopeAST.addStatement(stat)
        }
        return stat
    }

    override fun visitReadStat(ctx: ReadStatContext): ASTNode {
        log("Visiting read statement")

        val assignLhs = ctx.assign_lhs()
        val target = visit(assignLhs) as AssignmentAST<*>
        if (target.type !is BasicType || target.type == BasicType.BoolType) {
            addError(ReadTypeError(assignLhs.start, target.type))
        }

        log("|| Target type: ${target.type}")

        return scopeAST.addStatement(ReadStatementAST(scopeAST, symbolTable, target))
    }

    override fun visitReturnStat(ctx: ReturnStatContext): ASTNode {
        log("Visiting return statement")

        var enclosingAST: BlockAST? = scopeAST

        while (enclosingAST != null && enclosingAST !is FunctionDeclarationAST) {
            enclosingAST = enclosingAST.parent
        }

        if (enclosingAST == null) {
            addError(IllegalReturnStatementError(ctx.start))
            return SkipStatementAST(scopeAST)
        }

        val func = enclosingAST as FunctionDeclarationAST
        val funcReturnType = func.returnType

        log(" || return statement is under function ${func.funcName}")

        val expr: ExpressionAST?
        val returnType: ReturnableType
        if (ctx.expr() != null) {
            expr = visit(ctx.expr()) as ExpressionAST
            returnType = expr.type
        } else {
            expr = null
            returnType = ReturnableType.VOID
        }

        log("we're in return stat")
        log("asserted return type: $funcReturnType")
        log("actual return type: $returnType")

        if (!funcReturnType.compatible(returnType)) {
            addError(ReturnTypeError(ctx.expr()?.start ?: ctx.start, funcReturnType, returnType))
        }

        val returnStat = ReturnStatementAST(func, symbolTable, expr, returnType)
        func.returnStat = returnStat

        return scopeAST.addStatement(returnStat)
    }

    override fun visitBeginEndStat(ctx: BeginEndStatContext): ASTNode {
        return visitBeginEnd(ctx.stat())
    }

    private fun visitBeginEnd(statCtx: ParserRuleContext): ASTNode {
        val oldScope = scopeAST
        val oldSt = symbolTable

        val beginEndSt = symbolTable.subScope()
        val node = BeginEndBlockAST(scopeAST, beginEndSt)

        scopeAST = node
        symbolTable = beginEndSt
        visit(statCtx)
        symbolTable = oldSt
        scopeAST = oldScope

        return scopeAST.addStatement(node)
    }

    override fun visitIfStat(ctx: IfStatContext): ASTNode {
        log("Visiting if statement")

        val oldScope = scopeAST
        val oldSt = symbolTable

        val thenSt = oldSt.subScope()
        val elseSt = oldSt.subScope()

        log("Visiting if statement condition expression")
        val condExpr = visit(ctx.expr()) as ExpressionAST

        if (condExpr.type != BasicType.BoolType) {
            addError(CondTypeError(ctx.expr().start, condExpr.type))
        }

        val ifBlock = IfBlockAST(scopeAST, thenSt, condExpr)

        scopeAST = ifBlock
        symbolTable = thenSt
        log("|| Visiting then block")
        visit(ctx.stat(0))
        symbolTable = oldSt
        scopeAST = oldScope

        val elseBlock = ElseBlockAST(ifBlock, elseSt)

        scopeAST = elseBlock
        symbolTable = elseSt
        log("|| Visiting else block")
        visit(ctx.stat(1))
        symbolTable = oldSt
        scopeAST = oldScope

        ifBlock.elseBlock = elseBlock

        return scopeAST.addStatement(ifBlock)
    }

    override fun visitWhileStat(ctx: WhileStatContext): ASTNode {
        log("Visiting while statement")

        val oldScope = scopeAST
        val oldSt = symbolTable

        val whileSt = oldSt.subScope()

        log("|| Visiting while condition expression")
        val condExpr = visit(ctx.expr()) as ExpressionAST

        if (condExpr.type != BasicType.BoolType) {
            addError(CondTypeError(ctx.expr().start, condExpr.type))
        }

        val whileBlock = WhileBlockAST(scopeAST, whileSt, condExpr)

        scopeAST = whileBlock
        symbolTable = whileSt
        log("|| Visiting while block")
        visit(ctx.stat()) as StatementAST
        symbolTable = oldSt
        scopeAST = oldScope

        return scopeAST.addStatement(whileBlock)
    }

    override fun visitForStat(ctx: ForStatContext): ASTNode {
        log("Visiting for statement")

        val oldScope = scopeAST
        val oldSt = symbolTable

        log("|| Visiting for block")

        val forBlock = ForBlockAST(scopeAST, symbolTable)

        symbolTable = symbolTable.subScope()
        scopeAST = forBlock

        val loopVariable = VariableIdentifierAST(symbolTable, ctx.IDENT().text, Variable(IntType))
        val loopVarVal = IntLiteralAST(Integer.parseInt("0"))
        val varDecl = VariableDeclarationAST(scopeAST, symbolTable, ctx.IDENT().text, loopVarVal, Variable(IntType))
        val loopVarIncrement = AssignToIdentAST(scopeAST, loopVariable)
        loopVarIncrement.rhs = BinaryOpExprAST(symbolTable, loopVarVal, IntLiteralAST(1), BinaryOp.PLUS)
        symbolTable.add(ctx.IDENT().text, Variable(IntType))
        val expr = BinaryOpExprAST(symbolTable, loopVariable, IntLiteralAST(Integer.parseInt(ctx.POSITIVE_OR_NEGATIVE_INTEGER().text)), BinaryOp.LT)

        forBlock.varDecl = varDecl
        forBlock.condExpr = expr
        forBlock.incrementStat = loopVarIncrement

        visit(ctx.stat()) as StatementAST

        symbolTable = oldSt
        scopeAST = oldScope

        return scopeAST.addStatement(forBlock)
    }

    //endregion

    //region assign_and_declare

    override fun visitDeclarationStat(ctx: DeclarationStatContext): ASTNode {
        val type = ctx.type()
        val typeName = type.text
        val ident = ctx.ident()
        val varName = ident.text

        log(
            """Visiting variable declaration 
                || Type name: $typeName
                || Var name: $varName
            """
        )

        val t = TypeParser.parse(symbolTable, type)
        val v = symbolTable.lookup(varName)

        val exprRhs = ctx.assign_rhs()
        val assignRhs = visit(exprRhs) as AssignRhsAST

        when {
            v != null -> {
                if (v !is FunctionType) {
                    addError(IdentifierAlreadyDeclaredError(ident.start, varName))
                }
            }
            !t.compatible(assignRhs.type) -> {
                addError(AssignTypeError(exprRhs.start, t, assignRhs.type))
            }
        }

        val varDecl = VariableDeclarationAST(
            scopeAST,
            symbolTable,
            varName,
            assignRhs,
            Variable(t as VariableType)
        )
        symbolTable.add(varName, varDecl.varIdent)

        return scopeAST.addStatement(varDecl)
    }

    override fun visitAssignmentStat(ctx: AssignmentStatContext): ASTNode {
        log("Visiting assignment to ${ctx.assign_lhs().text}")

        val exprRhs = ctx.assign_rhs()
        val assignLhs = visit(ctx.assign_lhs()) as AssignmentAST<*>
        val assignRhs = visit(exprRhs) as AssignRhsAST

        if (!assignLhs.type.compatible(assignRhs.type)) {
            addError(AssignTypeError(exprRhs.start, assignLhs.type, assignRhs.type))
        }

        log("assignRhs: $assignRhs")

        assignLhs.rhs = assignRhs

        return scopeAST.addStatement(assignLhs)
    }

    override fun visitIdentAssignLhs(ctx: IdentAssignLhsContext): ASTNode {
        val ident = visitIdent(ctx.ident()) as VariableIdentifierAST

        return AssignToIdentAST(scopeAST, ident)
    }

    override fun visitExprAssignRhs(ctx: ExprAssignRhsContext): ASTNode {
        return visit(ctx.expr())
    }

    override fun visitArrayLiterAssignRhs(ctx: ArrayLiterAssignRhsContext): ASTNode {
        return visit(ctx.array_liter())
    }

    override fun visitPairElemAssignRhs(ctx: PairElemAssignRhsContext): ASTNode {
        return visit(ctx.pair_elem())
    }

    override fun visitAllocAssignRhs(ctx: AllocAssignRhsContext): ASTNode {
        log("Visiting alloc")

        val expr = visit(ctx.expr()) as ExpressionAST

        if (expr.type != IntType) {
            addError(AllocTypeError(ctx.expr().start, expr.type))
        }

        return AllocAST(symbolTable, expr)
    }

    override fun visitCallAssignRhs(ctx: CallAssignRhsContext): ASTNode {
        log("Visiting call assign rhs")

        val call = visitCall(ctx.call(), false) as CallStatementAST

        return CallAssignAST(symbolTable, call)
    }

    override fun visitArrayElemAssignLhs(ctx: ArrayElemAssignLhsContext): ASTNode {
        return AssignToArrayElemAST(scopeAST, visitArray_elem(ctx.array_elem()) as ArrayElemAST)
    }

    override fun visitPairElemAssignLhs(ctx: PairElemAssignLhsContext): ASTNode {
        return AssignToPairElemAST(scopeAST, visit(ctx.pair_elem()) as PairElemAST)
    }

    override fun visitDerefAssignLhs(ctx: DerefAssignLhsContext): ASTNode {
        return AssignToDerefAST(scopeAST, visitPointer_deref(ctx.pointer_deref()) as DerefPointerAST)
    }

    override fun visitArray_elem(ctx: Array_elemContext): ASTNode {
        val identCtx = ctx.ident()
        val ident = visitIdent(identCtx) as VariableIdentifierAST
        val arr = if (ident.type !is ArrayType) {
            addError(IndexingNonArrayTypeError(identCtx.start, ident.type))
            ArrayType.ANY_ARRAY
        } else {
            (symbolTable.lookupAll(ident.varName) as Variable).type as ArrayType
        }
        val indexList = mutableListOf<ExpressionAST>()

        for (expr in ctx.expr()) {
            val indexExpr = visit(expr) as ExpressionAST
            if (indexExpr.type != IntType) {
                addError(ArrayIndexTypeError(expr.start, indexExpr.type))
            }
            indexList.add(indexExpr)
        }

        return ArrayElemAST(symbolTable, ident, indexList, arr.elementType)
    }

    override fun visitPointer_deref(ctx: Pointer_derefContext): ASTNode {
        log("Visiting pointer dereference")

        // Every $ results in one dereference
        val numDerefs = ctx.DEREF().size

        val exprCtx = ctx.expr()
        val expr = visit(exprCtx) as ExpressionAST

        // Get the type at the end of the pointer chain
        val elementType = if (expr.type !is PointerType) {
            addError(DereferencingNonPointerTypeError(exprCtx.start, expr.type))
            Type.ANY
        } else {
            expr.type.elementType
        }

        // Dereferencing too many times means that at one point we're dereferencing a non-pointer
        expr.type as PointerType
        if (numDerefs > expr.type.depth) {
            addError(DereferencingNonPointerTypeError(ctx.start, elementType))
        }

        return DerefPointerAST(symbolTable, expr, numDerefs, elementType)
    }

    override fun visitNewPairAssignRhs(ctx: NewPairAssignRhsContext): ASTNode {
        val expr1 = visit(ctx.expr(0)) as ExpressionAST
        val expr2 = visit(ctx.expr(1)) as ExpressionAST
        return NewPairAST(symbolTable, expr1, expr2)
    }

    override fun visitFstPair(ctx: FstPairContext): ASTNode {
        val exprCtx = ctx.expr()
        val expr = visit(exprCtx) as ExpressionAST
        if (expr.type !is PairType) {
            addError(FstTypeError(exprCtx.start, expr.type))
        } else if (expr is NullPairLiteralAST) {
            addError(PairElemNullError(exprCtx.start))
        }

        return FstPairElemAST(symbolTable, expr)
    }

    override fun visitSndPair(ctx: SndPairContext): ASTNode {
        val exprCtx = ctx.expr()
        val expr = visit(exprCtx) as ExpressionAST
        if (expr.type !is PairType) {
            addError(SndTypeError(exprCtx.start, expr.type))
        } else if (expr is NullPairLiteralAST) {
            addError(PairElemNullError(exprCtx.start))
        }

        return SndPairElemAST(symbolTable, expr)
    }

    //endregion

    //region expressions

    override fun visitSizeofExpr(ctx: SizeofExprContext): ASTNode {
        log("Visiting sizeof ${ctx.type().text}")

        // Get the type whose size we want
        var t = TypeParser.parse(symbolTable, ctx.type())
        if (t !is VariableType) {
            addError(SizeOfTypeError(ctx.type().start, t))
            t = Type.ANY
        }

        return SizeOfAST(symbolTable, t)
    }

    override fun visitIdent(ctx: IdentContext): ASTNode {
        log("Visiting identifier with name ${ctx.text}")
        var ident = symbolTable.lookupAll(ctx.text)
        if (ident == null) {
            addError(IdentifierNotDefinedError(ctx.start, ctx.text))
            ident = Variable.ANY_VAR
        } else if (ident !is Variable) {
            addError(IdentifierNotAVariableError(ctx.start, ctx.text))
            ident = Variable.ANY_VAR
        }
        log("Identifier is variable with type ${ident.type}")
        return VariableIdentifierAST(symbolTable, ctx.text, ident)
    }

    override fun visitInt_liter(ctx: Int_literContext): ASTNode {
        return visit(ctx.children[0])
    }

    override fun visitInt_liter_positive(ctx: Int_liter_positiveContext): ASTNode {
        val i = Integer.parseInt(ctx.POSITIVE_OR_NEGATIVE_INTEGER().text)
        assert(i in 0..Int.MAX_VALUE)

        return IntLiteralAST(i)
    }

    override fun visitInt_liter_negative(ctx: Int_liter_negativeContext): ASTNode {
        val i = Integer.parseInt(ctx.text)
        assert(i in Int.MIN_VALUE..0)

        return IntLiteralAST(i)
    }

    override fun visitTBool(ctx: TBoolContext?): ASTNode {
        return BoolLiteralAST(true)
    }

    override fun visitFBool(ctx: FBoolContext?): ASTNode {
        return BoolLiteralAST(false)
    }

    override fun visitChar_liter(ctx: Char_literContext): ASTNode {
        val char: Char = if (ctx.CHAR_LITER_TOKEN() != null) {
            ctx.CHAR_LITER_TOKEN().text[1]
        } else {
            EscapeChar.fromLetter(ctx.ESC_CHAR_LITER().text[2])!!.char
        }
        return CharLiteralAST(char)
    }

    override fun visitStr_liter(ctx: Str_literContext): ASTNode {
        var str = ctx.text.trim('\"')
        EscapeChar.values().forEach {
            str = str.replace(it.string, "${it.char}")
        }
        return StringLiteralAST(str)
    }

    override fun visitPair_liter(ctx: Pair_literContext): ASTNode {
        return NullPairLiteralAST()
    }

    override fun visitArray_liter(ctx: Array_literContext): ASTNode {

        val elems: MutableList<ExpressionAST> = LinkedList()

        var elemType = Type.ANY
        for (expr in ctx.expr()) {
            val elem = visit(expr) as ExpressionAST
            if (!elemType.compatible(elem.type)) {
                addError(ArrayLiteralElemsTypeError(expr.start, elemType, elem.type))
            }
            elemType = elem.type
            elems.add(elem)
        }

        return ArrayLiteralAST(symbolTable, elemType, elems)
    }

    override fun visitUnaryExpr(ctx: UnaryExprContext): ASTNode {
        log("Visiting unary operator expression")

        val unOp: UnaryOp = when {
            ctx.BANG() != null -> UnaryOp.BANG
            ctx.MINUS() != null -> UnaryOp.MINUS
            ctx.LEN() != null -> UnaryOp.LEN
            ctx.ORD() != null -> UnaryOp.ORD
            ctx.CHR() != null -> UnaryOp.CHR
            else -> null
        }!!
        log("Found unary operator $unOp")

        val expr = visit(ctx.expr()) as ExpressionAST

        if (!unOp.expectedType.compatible(expr.type)) {
            addError(UnaryOpTypeError(ctx.start, unOp, expr.type))
        }

        return UnaryOpExprAST(symbolTable, expr, unOp)
    }

    override fun visitBinaryExpr(ctx: BinaryExprContext): ASTNode {
        log("Visiting binary operator expression")
        val binOp: BinaryOp = when {
            ctx.MULT() != null -> BinaryOp.MULT
            ctx.DIV() != null -> BinaryOp.DIV
            ctx.MOD() != null -> BinaryOp.MOD
            ctx.PLUS() != null -> BinaryOp.PLUS
            ctx.MINUS() != null -> BinaryOp.MINUS
            ctx.GT() != null -> BinaryOp.GT
            ctx.GTE() != null -> BinaryOp.GTE
            ctx.LT() != null -> BinaryOp.LT
            ctx.LTE() != null -> BinaryOp.LTE
            ctx.EQUALS() != null -> BinaryOp.EQUALS
            ctx.NOT_EQUALS() != null -> BinaryOp.NOT_EQUALS
            ctx.AND() != null -> BinaryOp.AND
            ctx.OR() != null -> BinaryOp.OR
            else -> null
        }!!
        log("Found binary operator $binOp")
        val expr1 = visit(ctx.expr(0)) as ExpressionAST
        val expr2 = visit(ctx.expr(1)) as ExpressionAST

        if (binOp.allowedTypes != null) {
            if (!binOp.allowedTypes.contains(expr1.type)) {
                addError(BinaryOpTypeError(ctx.start, binOp, expr1.type))
            }
            if (!binOp.allowedTypes.contains(expr1.type)) {
                addError(BinaryOpTypeError(ctx.start, binOp, expr1.type))
            }
        }

        if (!expr1.type.compatible(expr2.type)) {
            addError(BinaryOpDifferentTypesError(ctx.start, expr1.type, expr2.type))
        }

//        if (arrayOf(BinaryOp.AND, BinaryOp.OR).any { it == node.operator }) {
//            if (!node.expr1.type.compatible(BasicType.BoolType) ||
//                !node.expr2.type.compatible(BasicType.BoolType)
//            ) {
//                throw TypeError(
//                    "boolean operands expected in expression but " +
//                        "found operands of type ${node.expr1.type} and ${node.expr2.type} instead"
//                )
//            }
//        }
//
//        if (!node.expr1.type.compatible(node.expr2.type)) {
//            throw TypeError(
//                "left operand of type ${node.expr1.type} is incompatible " +
//                    "with right operand of type ${node.expr2.type}"
//            )
//        }

        return BinaryOpExprAST(symbolTable, expr1, expr2, binOp)
    }

    override fun visitBracketExpr(ctx: BracketExprContext): ASTNode {
        return visit(ctx.expr())
    }

    //endregion

    //region helpers

    private fun log(message: String) {
        if (enableLogging) {
            println(message.trimMargin())
        }
    }

    private fun addError(error: SemanticError) {
        semanticErrors.addError(error)
        if (enableLogging) {
            println(error.toString())
        }
    }

    private fun addError(error: SyntacticError) {
        syntacticErrors.addError(error)
        if (enableLogging) {
            println(error.toString())
        }
    }

    //endregion
}
