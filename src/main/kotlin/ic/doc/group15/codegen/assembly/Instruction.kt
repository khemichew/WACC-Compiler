package ic.doc.group15.codegen.assembly

import ic.doc.group15.instructions.Operand2
import ic.doc.group15.instructions.Register

class BranchLabel : Label<Instruction> {
    constructor(name: String, lines: List<Instruction>) : super(name, lines)
    constructor(name: String) : super(name)
}

class LDRimmInt(val reg: Int, val loadVal: Int) : Instruction() // LDR r4, =5
class LDRimmString(val reg: Int, val loadVal: String) : Instruction() // LDR r4, =msg_0
class LDRsp(val reg: Int, val spOffset: Int) : Instruction() // LDR r4, [sp, #4]
class LDRSBsp(val reg: Int, val spOffset: Int) : Instruction() // LDRSB r4, [sp, #1]
class LDR(val reg1: Int, val reg2: Int) : Instruction() // LDR reg1, [reg2]
class MOVimmBool(val reg: Int, val movVal: Boolean) : Instruction() // MOV r4, #1
class MOVimmChar(val reg: Int, val movVal: Char) : Instruction() // e.g. MOV r4, #'c'
class MOVreg(val reg1: Int, val reg2: Int) : Instruction()
class MOVEQ(val reg: Int, val value: Boolean) : Instruction()
class MOVNE(val reg: Int, val value: Boolean) : Instruction()
class STRsp(val reg: Int, val spPos: Int) : Instruction() // STR r4, [sp]
class STRBsp(val reg: Int, val spPos: Int) : Instruction()
class ADDS(val reg1: Int, val reg2: Int, val re3: Int) : Instruction()
class ADD(val reg1: Int, val reg2: Int, val value: Int) : Instruction()
class ADDspImm(val reg1: Int, val value: Int) : Instruction() // stack pointer is always the second argument e.g. this translates to the assembly: ADD reg1, sp, value
class ADDspSpImm(val value: Int) : Instruction() // only for incrementing the sp e.g. ADD sp, sp, #4
class SUBspSpImm(val value: Int) : Instruction() // only for decrementing the sp e.g. SUB sp, sp, #4
class RSB(val reg1: Int, val reg2: Int, val operand2: Operand2): Instruction()
class AND(val reg1: Int, val reg2: Int, val re3: Int) : Instruction()
class EOR(val dest: Int, val base: Int, val op: Operand2): Instruction()
class SMULL(val reg1: Int, val reg2: Int, val reg3: Int, val reg4: Int) : Instruction()
class CMPreg(val reg1: Int, val reg2: Int) : Instruction()
class CMPimm(val reg1: Int, val value: Int) : Instruction()
class BEQ(val label: String) : Instruction()
class B(val label: String) : Instruction()
class BL(val label: String) : Instruction()
class PUSHlr : Instruction() // so far i have only seen PUSH used to push lr so for now PUSH is hardcoded to only push lr
class POPpc : Instruction() // so far i have only seen POP used to pop pc so for now POP is hardcoded to only pop pc
