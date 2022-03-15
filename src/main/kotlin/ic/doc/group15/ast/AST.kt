package ic.doc.group15.ast

import ic.doc.group15.SymbolTable
import ic.doc.group15.visitor.Visitable

abstract class ASTNode protected constructor(
    val symbolTable: SymbolTable = SymbolTable.emptyTable
) : Visitable

class AST(topLevelSymbolTable: SymbolTable) : BeginEndBlockAST(null, topLevelSymbolTable)
