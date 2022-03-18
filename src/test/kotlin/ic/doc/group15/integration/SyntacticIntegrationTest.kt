package ic.doc.group15.integration

import ic.doc.group15.antlr.WaccLexer
import ic.doc.group15.antlr.WaccParser
import ic.doc.group15.error.SyntacticErrorList
import ic.doc.group15.visitor.syntaxchecker.BreakContinueChecker
import ic.doc.group15.visitor.syntaxchecker.ReturnChecker
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

// Verify that parsing works properly
class SyntacticIntegrationTest {

    private val validFolderPath = "wacc_examples/valid"
    private val invalidFolderPath = "wacc_examples/invalid"

    @Nested
    inner class AdvancedValidFiles {
        private val advancedValidFileFolderPath = "$validFolderPath/advanced"

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["binarySortTree", "hashTable", "ticTacToe"])
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$advancedValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class ArrayValidFiles {
        private val arrayFolderPath = "$validFolderPath/array"

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "array", "arrayBasic", "arrayEmpty", "arrayLength", "arrayLookup", "arrayNested",
                "arrayPrint", "arraySimple", "modifyString", "printRef"
            ]
        )
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$arrayFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class BasicValidFiles {
        private val basicFolderPath = "$validFolderPath/basic"

        @Nested
        inner class ExitValidFiles {
            private val exitFolderPath = "$basicFolderPath/exit"

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["exit-1", "exitBasic", "exitBasic2", "exitWrap"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$exitFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class SkipValidFiles {
            private val skipFolderPath = "$basicFolderPath/skip"

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["comment", "commentInLine", "skip"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$skipFolderPath/$fileName.wacc"))
            }
        }
    }

    @Nested
    inner class ExpressionValidFiles {
        private val expressionsFolderPath = "$validFolderPath/expressions"

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "andExpr", "andOverOrExpr", "boolCalc", "boolExpr1", "charComparisonExpr", "divExpr",
                "equalsExpr", "equalsOverAnd", "equalsOverBool", "equalsOverOr", "greaterEqExpr", "greaterExpr", "intCalc",
                "intExpr1", "lessCharExpr", "lessEqExpr", "lessExpr", "longExpr", "longExpr2", "longExpr3", "longSplitExpr",
                "longSplitExpr2", "minusExpr", "minusMinusExpr", "minusNoWhitespaceExpr", "minusPlusExpr", "modExpr",
                "multExpr", "multNoWhitespaceExpr", "negBothDiv", "negBothMod", "negDividendDiv", "negDividendMod",
                "negDivisorDiv", "negDivisorMod", "negExpr", "notequalsExpr", "notExpr", "ordAndchrExpr", "orExpr",
                "plusExpr", "plusMinusExpr", "plusNoWhitespaceExpr", "plusPlusExpr", "sequentialCount", "stringEqualsExpr"
            ]
        )
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$expressionsFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class FunctionValidFiles {
        private val functionsFolderPath = "$validFolderPath/function"

        @Nested
        inner class NestedFunctionValidFiles {
            private val nestedFunctionsFolderPath = "$functionsFolderPath/nested_functions"

            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "fibonacciFullRec", "fibonacciRecursive", "fixedPointRealArithmetic",
                    "functionConditionalReturn", "mutualRecursion", "printInputTriangle", "printTriangle",
                    "simpleRecursion"
                ]
            )
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$nestedFunctionsFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class SimpleFunctionValidFiles {
            private val simpleFunctionsFolderPath = "$functionsFolderPath/simple_functions"

            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "asciiTable", "functionDeclaration", "functionDoubleReturn", "functionIfReturns",
                    "functionManyArguments", "functionMultiReturns", "functionReturnPair", "functionSimple",
                    "functionSimpleLoop", "functionUpdateParameter", "incFunction", "negFunction", "sameArgName",
                    "sameArgName2", "sameNameAsVar"
                ]
            )
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$simpleFunctionsFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class VoidFunctionValidFiles {
            private val voidFunctionFolderPath = "$functionsFolderPath/void_functions"

            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "voidSimple", "voidReturn", "voidCall", "voidExit"
                ]
            )
            fun testSemanticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$voidFunctionFolderPath/$fileName.wacc"))
            }
        }
    }

    @Nested
    inner class HeapValidFiles {
        private val heapFolderPath = "$validFolderPath/heap"

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "allocBasicTypes", "allocAndFree", "dereferenceAssign", "allocToPointerOffset",
                "pointerFuncParam", "pointerFuncParamReference"
            ]
        )
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$heapFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class IfValidFiles {
        private val ifValidFileFolderPath = "$validFolderPath/if"

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["if1", "if2", "if3", "if4", "if5", "if6", "ifBasic", "ifFalse", "ifTrue", "whitespace"])
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$ifValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class IOValidFiles {
        private val ioValidFileFolderPath = "$validFolderPath/IO"

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["IOLoop", "IOSequence"])
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$ioValidFileFolderPath/$fileName.wacc"))
        }

        @Nested
        inner class PrintValidFiles {
            private val printValidFileFolderPath = "$ioValidFileFolderPath/print"

            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "hashInProgram", "multipleStringsAssignment", "print", "print-backspace",
                    "print-carridge-return", "printBool", "printChar", "printCharArray", "printCharAsString", "printEscChar",
                    "printInt", "println"
                ]
            )
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$printValidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class ReadValidFiles {
            private val readValidFileFolderPath = "$ioValidFileFolderPath/read"

            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "echoBigInt", "echoBigNegInt", "echoChar", "echoInt", "echoNegInt", "echoPuncChar",
                    "read"
                ]
            )
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$readValidFileFolderPath/$fileName.wacc"))
            }
        }
    }

    @Nested
    inner class PairValidFiles {
        private val pairsValidFileFolderPath = "$validFolderPath/pairs"

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "checkRefPair", "createPair", "createPair02", "createPair03", "createRefPair", "free",
                "linkedList", "nestedPair", "null", "printNull", "printNullPair", "printPair", "printPairOfNulls", "readPair",
                "writeFst", "writeSnd"
            ]
        )
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$pairsValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class RuntimeErrorValidFiles {
        private val runtimeErrorValidFileFolderPath = "$validFolderPath/runtimeErr"

        @Nested
        inner class ArrayOutOfBoundsValidFiles {
            private val arrayOutOfBoundsValidFileFolderPath = "$runtimeErrorValidFileFolderPath/arrayOutOfBounds"

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["arrayNegBounds", "arrayOutOfBounds", "arrayOutOfBoundsWrite"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$arrayOutOfBoundsValidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class DivideByZeroValidFiles {
            private val divideByZeroValidFileFolderPath = "$runtimeErrorValidFileFolderPath/divideByZero"

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["divideByZero", "divZero", "modByZero"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$divideByZeroValidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class IntegerOverflowValidFiles {
            private val integerOverflowValidFileFolderPath = "$runtimeErrorValidFileFolderPath/integerOverflow"

            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "intJustOverflow", "intmultOverflow", "intnegateOverflow", "intnegateOverflow2",
                    "intnegateOverflow3", "intnegateOverflow4", "intUnderflow", "intWayOverflow"
                ]
            )
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$integerOverflowValidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class NullDereferenceValidFiles {
            private val nullDereferenceValidFileFolderPath = "$runtimeErrorValidFileFolderPath/nullDereference"

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["freeNull", "readNull1", "readNull2", "setNull1", "setNull2", "useNull1", "useNull2"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyValid("$nullDereferenceValidFileFolderPath/$fileName.wacc"))
            }
        }
    }

    @Nested
    inner class ScopeValidFiles {
        private val scopeValidFileFolderPath = "$validFolderPath/scope"

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "ifNested1", "ifNested2", "indentationNotImportant", "intsAndKeywords", "printAllTypes",
                "scope", "scopeBasic", "scopeIfRedefine", "scopeRedefine", "scopeSimpleRedefine", "scopeVars",
                "scopeWhileNested", "scopeWhileRedefine"
            ]
        )
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$scopeValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class SequenceValidFiles {
        private val sequenceValidFileFolderPath = "$validFolderPath/sequence"

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "basicSeq", "basicSeq2", "boolAssignment", "charAssignment", "exitSimple",
                "intAssignment", "intLeadingZeros", "stringAssignment"
            ]
        )
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$sequenceValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class VariableValidFiles {
        private val variablesValidFileFolderPath = "$validFolderPath/variables"

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "_VarNames", "boolDeclaration", "boolDeclaration2", "capCharDeclaration",
                "charDeclaration", "charDeclaration2", "emptyStringDeclaration", "intDeclaration", "longVarNames",
                "manyVariables", "negIntDeclaration", "puncCharDeclaration", "stringDeclaration", "zeroIntDeclaration"
            ]
        )
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$variablesValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class WhileValidFiles {
        private val whileValidFileFolderPath = "$validFolderPath/while"

        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "fibonacciFullIt", "fibonacciIterative", "loopCharCondition", "loopIntCondition", "max",
                "min", "rmStyleAdd", "rmStyleAddIO", "whileBasic", "whileBoolFlip", "whileCount", "whileFalse"
            ]
        )
        fun testSyntacticallyValid(fileName: String) {
            assertTrue(isSyntacticallyValid("$whileValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class SyntaxErrorInvalidFiles {
        private val syntaxErrorFileFolderPath = "$invalidFolderPath/syntaxErr"

        @Nested
        inner class ArrayInvalidFiles {
            private val arrayInvalidFileFolderPath = "$syntaxErrorFileFolderPath/array"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(strings = ["arrayExpr"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$arrayInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class BasicInvalidFiles {
            private val basicInvalidFileFolderPath = "$syntaxErrorFileFolderPath/basic"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(
                strings = [
                    "badComment", "badComment2", "badEscape", "beginNoend", "bgnErr", "multipleBegins",
                    "noBody", "skpErr", "unescapedChar"
                ]
            )
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$basicInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class ExpressionInvalidFiles {
            private val expressionsInvalidFileFolderPath = "$syntaxErrorFileFolderPath/expressions"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(strings = ["missingOperand1", "missingOperand2", "printlnConcat"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$expressionsInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class FunctionInvalidFiles {
            private val functionInvalidFileFolderPath = "$syntaxErrorFileFolderPath/function"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(
                strings = [
                    "badlyNamed", "badlyPlaced", "funcExpr", "funcExpr2", "functionConditionalNoReturn",
                    "functionEndingNotReturn", "functionLateDefine", "functionMissingCall", "functionMissingParam",
                    "functionMissingPType", "functionNoReturn", "functionReturnInLoop", "functionScopeDef",
                    "mutualRecursionNoReturn", "noBodyAfterFuncs", "thisIsNotC"
                ]
            )
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$functionInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class IfInvalidFiles {
            private val ifInvalidFileFolderPath = "$syntaxErrorFileFolderPath/if"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(strings = ["ifiErr", "ifNoelse", "ifNofi", "ifNothen"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$ifInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class PairInvalidFiles {
            private val pairsInvalidFileFolderPath = "$syntaxErrorFileFolderPath/pairs"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(strings = ["badLookup01", "badLookup02"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$pairsInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class PrintInvalidFiles {
            private val printInvalidFileFolderPath = "$syntaxErrorFileFolderPath/print"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(strings = ["printlnCharArry"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$printInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class SequenceInvalidFiles {
            private val sequenceInvalidFileFolderPath = "$syntaxErrorFileFolderPath/sequence"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(strings = ["doubleSeq", "emptySeq", "endSeq", "extraSeq", "missingSeq"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$sequenceInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class VariableInvalidFiles {
            private val variableInvalidFileFolderPath = "$syntaxErrorFileFolderPath/variables"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(
                strings = [
                    "badintAssignments", "badintAssignments1", "badintAssignments2", "bigIntAssignment",
                    "varNoName"
                ]
            )
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$variableInvalidFileFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class WhileInvalidFiles {
            private val whileInvalidFileFolderPath = "$syntaxErrorFileFolderPath/while"

            @ParameterizedTest(name = "check {0} source code is not syntactically valid")
            @ValueSource(strings = ["donoErr", "dooErr", "whileNodo", "whileNodone", "whilErr"])
            fun testSyntacticallyValid(fileName: String) {
                assertTrue(isSyntacticallyInvalid("$whileInvalidFileFolderPath/$fileName.wacc"))
            }
        }
    }

    private fun isSyntacticallyValid(path: String): Boolean {
        val input = CharStreams.fromFileName(path)
        val lexer = WaccLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = WaccParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(DummyErrorListener())
        val program = parser.program()
        val errors = SyntacticErrorList()
        ReturnChecker(errors, true).visit(program)
        BreakContinueChecker(errors, true).visit(program)
        if (errors.hasErrors()) {
            errors.printErrors()
            return false
        }
        program.toStringTree(parser)
        return true
    }

    class DummyErrorListener : BaseErrorListener() {
        override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
            throw ParseCancellationException("Syntax error at line $line:$charPositionInLine: $msg")
        }
    }

    private fun isSyntacticallyInvalid(path: String): Boolean {
        val result: Boolean
        try {
            result = !isSyntacticallyValid(path)
        } catch (e: ParseCancellationException) {
            return true
        }
        return result
    }
}
