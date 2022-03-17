package ic.doc.group15.assembly.instruction

import ic.doc.group15.assembly.Instruction
import ic.doc.group15.assembly.operand.AddressOperand
import ic.doc.group15.assembly.operand.Register
import ic.doc.group15.util.BYTE

abstract class StoreInstruction protected constructor(
    instr: String,
    conditionCode: ConditionCode?,
    val src: Register,
    val addr: AddressOperand
) : Instruction(instr, conditionCode, src, addr)

/**
 * Returns an instance of LoadInstruction specific to the size of the data being loaded.
 */
fun Store(
    size: Int,
    dest: Register,
    addr: AddressOperand
): StoreInstruction {
    return Store(size, null, dest, addr)
}

fun Store(
    size: Int,
    conditionCode: ConditionCode?,
    dest: Register,
    addr: AddressOperand
): StoreInstruction {
    return if (size == BYTE) {
        StoreByte(conditionCode, dest, addr)
    } else {
        StoreWord(conditionCode, dest, addr)
    }
}

/**
 * STR is a store register instruction that stores the particular value
 * of word size (4 bytes) at the register into memory at the specified
 * address.
 *
 * @param src The source register
 * @param addr The destination address
 */
class StoreWord(
    conditionCode: ConditionCode?,
    src: Register,
    addr: AddressOperand
) : StoreInstruction("STR", conditionCode, src, addr) {

    constructor(src: Register, addr: AddressOperand) : this(null, src, addr)
}

/**
 * STRB is a store register instruction that stores the particular value
 * of byte size (1 byte) at the register into memory at the specified
 * address.
 *
 * @param src The source register
 * @param addr The destination address
 */
class StoreByte(
    conditionCode: ConditionCode?,
    src: Register,
    addr: AddressOperand
) : StoreInstruction("STRB", conditionCode, src, addr) {

    constructor(src: Register, addr: AddressOperand) : this(null, src, addr)
}
