package ic.doc.group15.error

enum class ErrorType(val type: String, val code: Int) {
    SYNTAX("syntax", 100),
    SEMANTICS("semantics", 200)
}
