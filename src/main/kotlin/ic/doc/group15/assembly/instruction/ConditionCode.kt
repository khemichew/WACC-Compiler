package ic.doc.group15.assembly.instruction

/**
 * Condition codes present in ARM1176JZF-S, partly implemented.
 */
enum class ConditionCode {
    /**
     * Equal.
     **/
    EQ,

    /**
     * Not equal.
     **/
    NE,

    /**
     * Signed greater or equal.
     **/
    GE,

    /**
     * Signed less than.
     **/
    LT,

    /**
     * Signed greater than.
     **/
    GT,

    /**
     * Signed less than or equal.
     **/
    LE,

    /**
     * Overflow.
     **/
    VS,

    /**
     * Carry.
     **/
    CS
    ;

    override fun toString(): String {
        return name.uppercase()
    }
}
