package ic.doc.group15.type

import ic.doc.group15.SymbolTable
import ic.doc.group15.util.*

interface Type : Identifier {

    fun compatible(type: Type): Boolean

    companion object {
        val ANY: VariableType = AnyType()

        private class AnyType : VariableType(0) {

            override fun compatible(type: Type): Boolean = true

            override fun toString(): String = "any"
        }
    }
}

interface ReturnableType : Type {

    companion object {
        val VOID: ReturnableType = VoidType()

        private class VoidType : ReturnableType {

            override fun compatible(type: Type): Boolean = type == VOID

            override fun toString(): String = "void"
        }
    }
}

abstract class VariableType protected constructor(val size: Int) : ReturnableType

sealed interface HeapAllocatedType : Type

class BasicType private constructor(val str: String, size: Int) : VariableType(size) {

    companion object {
        val IntType = BasicType("int", WORD)
        val BoolType = BasicType("bool", BYTE)
        val CharType = BasicType("char", BYTE)
        val StringType = BasicType("string", WORD)
    }

    override fun compatible(type: Type): Boolean = type == this || type == Type.ANY

    override fun toString(): String = str
}

class PointerType(
    elementType: VariableType,
    references: Int
) : VariableType(WORD), HeapAllocatedType {

    val elementType: VariableType
    val depth: Int

    companion object {
        val ANY_POINTER = PointerType(Type.ANY, 1)
    }

    init {
        val type: Type
        val dim: Int
        if (elementType is PointerType) {
            assert(elementType.elementType !is ArrayType)
            type = elementType.elementType
            dim = references + 1
        } else {
            type = elementType
            dim = references
        }
        this.elementType = type
        this.depth = dim
    }

    override fun compatible(type: Type): Boolean {
        if (type == BasicType.IntType) {
            return true
        }
        if (type !is PointerType) {
            return false
        }
        if (elementType == Type.ANY || type.elementType == Type.ANY) {
            return true
        }
        if (depth != type.depth) {
            return false
        }
        assert(elementType !is PointerType)
        assert(type.elementType !is PointerType)
        return elementType.compatible(type.elementType)
    }

    override fun toString(): String {
        return "pointer(".repeat(depth) + elementType.toString() + ")".repeat(depth)
    }
}

class ArrayType(elementType: VariableType, dimension: Int) : VariableType(WORD), HeapAllocatedType {

    val elementType: VariableType
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

    override fun toString(): String {
        return elementType.toString() + "[]".repeat(dimension)
    }
}

class PairType(
    fstType: VariableType = Type.ANY,
    sndType: VariableType = Type.ANY
) : VariableType(WORD), HeapAllocatedType {

    val fstType: VariableType = if (fstType is PairType) PairType() else fstType
    val sndType: VariableType = if (sndType is PairType) PairType() else sndType

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
