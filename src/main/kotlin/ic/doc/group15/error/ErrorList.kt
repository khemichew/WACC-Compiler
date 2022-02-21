package ic.doc.group15.error

import ic.doc.group15.error.semantic.SemanticError
import ic.doc.group15.error.syntactic.SyntacticError
import java.util.*
import kotlin.system.exitProcess

abstract class ErrorList<T : WaccError> protected constructor(private val errorCode: Int) {

    private val errors: MutableList<T> = LinkedList()

    fun addError(error: T) {
        errors.add(error)
    }

    fun checkErrors() {
        if (errors.isEmpty()) {
            return
        }
        for (e in errors) {
            println(e)
        }
        exitProcess(errorCode)
    }
}

class SyntacticErrorList : ErrorList<SyntacticError>(SYNTACTIC_ERROR_CODE)

class SemanticErrorList : ErrorList<SemanticError>(SEMANTIC_ERROR_CODE)
