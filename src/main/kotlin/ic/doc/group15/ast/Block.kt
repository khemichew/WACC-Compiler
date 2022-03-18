package ic.doc.group15.ast

import ic.doc.group15.SymbolTable
import ic.doc.group15.type.*
import java.util.*

abstract class BlockAST protected constructor(
    parent: BlockAST?,
    symbolTable: SymbolTable = SymbolTable.emptyTable
) : StatementAST(parent, symbolTable) {

    private val statements: MutableList<StatementAST> = LinkedList()

    fun getStatements(): List<StatementAST> = statements

    open fun addStatement(stat: StatementAST): StatementAST {
        statements.add(stat)
        return stat
    }
}

class FunctionDeclarationAST(
    parent: BlockAST,
    val paramSymbolTable: SymbolTable,
    symbolTable: SymbolTable,
    val returnType: Type,
    val funcName: String
) : BlockAST(parent, symbolTable) {

    lateinit var funcIdent: FunctionType

    val formals: MutableList<ParameterAST> = mutableListOf()
}

class IfBlockAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
    val condExpr: ExpressionAST
) : BlockAST(parent, symbolTable) {

    lateinit var elseBlock: ElseBlockAST
}

class ElseBlockAST(
    parent: IfBlockAST,
    symbolTable: SymbolTable
) : BlockAST(parent, symbolTable)

abstract class LoopBlockAST protected constructor(
    parent: BlockAST,
    symbolTable: SymbolTable
) : BlockAST(parent, symbolTable) {
    lateinit var condExpr: ExpressionAST

    constructor(
        parent: BlockAST,
        symbolTable: SymbolTable,
        condExpr: ExpressionAST
    ) : this(parent, symbolTable) {
        this.condExpr = condExpr
    }
}

class WhileBlockAST(
    parent: BlockAST,
    symbolTable: SymbolTable
) : LoopBlockAST(parent, symbolTable) {

    constructor(
        parent: BlockAST,
        symbolTable: SymbolTable,
        condExpr: ExpressionAST
    ) : this(parent, symbolTable) {
        this.condExpr = condExpr
    }
}

class ForBlockAST(
    parent: BlockAST,
    symbolTable: SymbolTable,
) : LoopBlockAST(parent, symbolTable) {
    lateinit var varDecl: VariableDeclarationAST
    lateinit var loopUpdate: StatementAST

    constructor(
        parent: BlockAST,
        symbolTable: SymbolTable,
        condExpr: ExpressionAST
    ) : this(parent, symbolTable) {
        this.condExpr = condExpr
    }
}

open class BeginEndBlockAST(
    parent: BlockAST?,
    symbolTable: SymbolTable
) : BlockAST(parent, symbolTable)
