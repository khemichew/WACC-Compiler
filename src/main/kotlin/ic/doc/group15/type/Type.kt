package ic.doc.group15.type

import ic.doc.group15.BYTE
import ic.doc.group15.SymbolTable
import ic.doc.group15.WORD

const val INT_MAX = Int.MAX_VALUE
const val INT_MIN = Int.MIN_VALUE

interface Identifier

interface Type : Identifier {

    fun compatible(type: Type): Boolean

    companion object {
        val ANY: ReturnableType = AnyType()

        private class AnyType : Type, ReturnableType {

            override fun size(): Int = 0

            override fun compatible(type: Type): Boolean = true

            override fun toString(): String {
                return "any"
            }
        }
    }
}

interface ReturnableType : Type {

    fun size(): Int
}

interface HeapAllocatedType : Type

// we assign the stackPos to be Int.MIN_VALUE until stackPos is actually assigned
open class Variable(
    val type: ReturnableType,
) : Identifier {
    companion object {
        val ANY_VAR = Variable(Type.ANY)
    }

    var stackPosition: Int = 0
}

class Param(type: ReturnableType) : Variable(type)

enum class BasicType(private val size: Int) : ReturnableType {
    IntType(WORD) {
        override fun compatible(type: Type): Boolean {
            return type == IntType
        }

        override fun toString(): String {
            return "int"
        }
    },
    BoolType(BYTE) {
        override fun compatible(type: Type): Boolean {
            return type == BoolType
        }

        override fun toString(): String {
            return "bool"
        }
    },
    CharType(BYTE) {
        override fun compatible(type: Type): Boolean {
            return type == CharType
        }

        override fun toString(): String {
            return "char"
        }
    },
    StringType(WORD) {
        override fun compatible(type: Type): Boolean {
            return type == StringType
        }

        override fun toString(): String {
            return "string"
        }
    }
    ;

    override fun size(): Int = this.size
}

class ArrayType(elementType: ReturnableType, dimension: Int) : ReturnableType, HeapAllocatedType {

    val elementType: ReturnableType
    val dimension: Int

    companion object {
        val ANY_ARRAY = ArrayType(Type.ANY, 1)
    }

    init {
        val type: Type
        val dim: Int
        if (elementType is ArrayType) {
            assert(elementType.elementType !is ArrayType)
            type = elementType.elementType
            dim = dimension + 1
        } else {
            type = elementType
            dim = dimension
        }
        this.elementType = type
        this.dimension = dim
    }

    override fun compatible(type: Type): Boolean {
        if (type !is ArrayType) {
            return false
        }
        if (elementType == Type.ANY || type.elementType == Type.ANY) {
            return true
        }
        if (dimension != type.dimension) {
            return false
        }
        assert(elementType !is ArrayType)
        assert(type.elementType !is ArrayType)
        return elementType.compatible(type.elementType)
    }

    override fun size(): Int = 2 * WORD

    override fun toString(): String {
        return elementType.toString() + "[]".repeat(dimension)
    }
}

class PairType(
    fstType: ReturnableType = Type.ANY,
    sndType: ReturnableType = Type.ANY
) : ReturnableType, HeapAllocatedType {

    val fstType: ReturnableType = if (fstType is PairType) PairType() else fstType
    val sndType: ReturnableType = if (sndType is PairType) PairType() else sndType

    companion object {
        val ANY_PAIR = PairType()
    }

    override fun compatible(type: Type): Boolean {
        if (type !is PairType) {
            return false
        }
        return (fstType.compatible(type.fstType) || type.fstType == Type.ANY) &&
            (sndType.compatible(type.sndType) || type.sndType == Type.ANY)
    }

    override fun size(): Int = 2 * WORD

    override fun toString(): String {
        return "pair($fstType, $sndType)"
    }
}

class FunctionType(
    val returnType: ReturnableType,
    val formals: List<Param>,
    val symbolTable: SymbolTable
) : Type {

    override fun compatible(type: Type): Boolean {
        if (type !is FunctionType || type.returnType != returnType) {
            return false
        }
        return true
    }
}
