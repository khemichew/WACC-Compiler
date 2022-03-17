package ic.doc.group15.error

import ic.doc.group15.error.optimization.BCEError
import ic.doc.group15.error.semantic.SemanticError
import ic.doc.group15.error.syntactic.SyntacticError
import java.util.*
import kotlin.system.exitProcess

abstract class ErrorList<T : WaccError> protected constructor(private val errorCode:
                                                      Int) {

    private val errors: MutableList<T> = LinkedList()

    fun addError(error: T) {
        errors.add(error)
    }

    fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }

    fun printErrors() {
        if (errors.isNotEmpty()) {
            println("${errors.size} error(s) detected during compilation! " +
                    "Exit code $errorCode returned.")
            for (e in errors) {
                println(e)
            }
        }
    }

    fun checkErrors() {
        if (hasErrors()) {
            printErrors()
            exitProcess(errorCode)
        }
    }
}

class SyntacticErrorList : ErrorList<SyntacticError>(SYNTACTIC_ERROR_CODE)

class SemanticErrorList : ErrorList<SemanticError>(SEMANTIC_ERROR_CODE)

class BCEErrorList : ErrorList<BCEError>(SEMANTIC_ERROR_CODE)
