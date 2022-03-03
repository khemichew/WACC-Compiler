package ic.doc.group15.codegen.assembly

import ic.doc.group15.codegen.assembly.LibraryFunction.Companion.EXIT
import ic.doc.group15.codegen.assembly.LibraryFunction.Companion.FFLUSH
import ic.doc.group15.codegen.assembly.LibraryFunction.Companion.PRINTF
import ic.doc.group15.codegen.assembly.LibraryFunction.Companion.SCANF
import ic.doc.group15.codegen.assembly.instruction.* // ktlint-disable no-unused-imports
import ic.doc.group15.codegen.assembly.instruction.ConditionCode.*
import ic.doc.group15.codegen.assembly.operand.DataLabelOperand
import ic.doc.group15.codegen.assembly.operand.ImmediateOperand
import ic.doc.group15.codegen.assembly.operand.Register.*
import java.util.*

enum class UtilFunction {

    P_READ_INT {
        override val assembly by lazy {
            listOf(
                Push(LR),
                Move(R1, R0),
                LoadWord(R0, generateStringData("%d")),
                Add(R0, R0, ImmediateOperand(4)),
                BranchLink(SCANF),
                Pop(PC)
            )
        }
    },
    P_CHECK_DIVIDE_BY_ZERO {
        override val assembly by lazy {
            listOf(
                Push(LR),
                Compare(R1, ImmediateOperand(0)),
                LoadWord(EQ, R0, generateStringData("DivideByZeroError: divide or modulo by zero\n")),
                BranchLink(EQ, P_THROW_RUNTIME_ERROR),
                Pop(PC)
            )
        }
    },
    P_THROW_RUNTIME_ERROR {
        override val assembly by lazy {
            listOf(
                BranchLink(P_PRINT_STRING),
                Move(R0, ImmediateOperand(-1)),
                BranchLink(EXIT)
            )
        }
    },
    P_PRINT_STRING {
        override val assembly by lazy {
            listOf(
                Push(LR),
                LoadWord(R1, R0),
                Add(R2, R0, ImmediateOperand(4)),
                LoadWord(R0, generateStringData("%.*s")),
                Add(R0, R0, ImmediateOperand(4)),
                BranchLink(PRINTF),
                Move(R0, ImmediateOperand(0)),
                BranchLink(FFLUSH),
                Pop(PC)
            )
        }
    }
    ;

    val labelName = name.lowercase()

    val dataBlocks: MutableList<DataLabel> = LinkedList()
    val labelBlock: BranchLabel by lazy { BranchLabel(name.lowercase(), assembly) }

    protected abstract val assembly: List<Instruction>

    private val stringLabel: UniqueLabelGenerator = UniqueLabelGenerator(labelName + "_msg_")

    protected fun generateStringData(str: String): DataLabelOperand {
        val label = StringData(stringLabel, str)
        dataBlocks.add(label)
        return DataLabelOperand(label)
    }
}
