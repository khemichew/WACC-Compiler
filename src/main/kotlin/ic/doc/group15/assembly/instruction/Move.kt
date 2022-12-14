package ic.doc.group15.assembly.instruction

import ic.doc.group15.assembly.operand.Operand
import ic.doc.group15.assembly.operand.Register

abstract class MoveInstruction protected constructor(
    instr: String,
    conditionCode: ConditionCode?,
    updateFlags: Boolean,
    val dest: Register,
    val op: Operand
) : UpdateFlagsInstruction(instr, conditionCode, updateFlags, dest, op)

class Move(
    conditionCode: ConditionCode?,
    val dest: Register,
    val op: Operand,
    updateFlags: Boolean = false
) : UpdateFlagsInstruction("MOV", conditionCode, updateFlags, dest, op) {

    constructor(dest: Register, op: Operand, updateFlags: Boolean = false) :
        this(null, dest, op, updateFlags)

    override fun usesSet(): Set<Register> = setOf(op).filterIsInstance<Register>().toSet()
    override fun definesSet(): Set<Register> = setOf(dest)
}

class MoveNot(
    conditionCode: ConditionCode?,
    val dest: Register,
    val op: Operand,
    updateFlags: Boolean = false
) : UpdateFlagsInstruction("MVN", conditionCode, updateFlags, dest, op) {

    constructor(dest: Register, op: Operand, updateFlags: Boolean = false) :
        this(null, dest, op, updateFlags)

    override fun usesSet(): Set<Register> = setOf(op).filterIsInstance<Register>().toSet()
    override fun definesSet(): Set<Register> = setOf(dest)
}
