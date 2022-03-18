package ic.doc.group15.integration

import ic.doc.group15.SymbolTable
import ic.doc.group15.antlr.WaccLexer
import ic.doc.group15.antlr.WaccParser
import ic.doc.group15.ast.AST
import ic.doc.group15.error.SemanticErrorList
import ic.doc.group15.error.SyntacticErrorList
import ic.doc.group15.visitor.AstAssemblyGenerator
import ic.doc.group15.visitor.ParseTreeVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

private const val ENABLE_LOGGING = true

class SemanticIntegrationTest {
    private val validFolderPath = "wacc_examples/valid"
    private val invalidFolderPath = "wacc_examples/invalid"

    @Nested
    inner class AdvancedValidFiles {
        private val advancedValidFileFolderPath = "$validFolderPath/advanced"

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["binarySortTree", "hashTable", "ticTacToe"])
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$advancedValidFileFolderPath/$fileName.wacc"))
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
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$arrayFolderPath/$fileName.wacc"))
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
            fun testSemanticallyValid(fileName: String) {
                assertTrue(isSemanticallyValid("$exitFolderPath/$fileName.wacc"))
            }
        }

        @Nested
        inner class SkipValidFiles {
            private val skipFolderPath = "$basicFolderPath/skip"

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["comment", "commentInLine", "skip"])
            fun testSemanticallyValid(fileName: String) {
                assertTrue(isSemanticallyValid("$skipFolderPath/$fileName.wacc"))
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
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$expressionsFolderPath/$fileName.wacc"))
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
            fun testSemanticallyValid(fileName: String) {
                assertTrue(isSemanticallyValid("$nestedFunctionsFolderPath/$fileName.wacc"))
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
            fun testSemanticallyValid(fileName: String) {
                assertTrue(isSemanticallyValid("$simpleFunctionsFolderPath/$fileName.wacc"))
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
                assertTrue(isSemanticallyValid("$voidFunctionFolderPath/$fileName.wacc"))
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
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$heapFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class IfValidFiles {
        private val ifValidFileFolderPath = "$validFolderPath/if"

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["if1", "if2", "if3", "if4", "if5", "if6", "ifBasic", "ifFalse", "ifTrue", "whitespace"])
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$ifValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class IOValidFiles {
        private val ioValidFileFolderPath = "$validFolderPath/IO"

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["IOLoop", "IOSequence"])
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$ioValidFileFolderPath/$fileName.wacc"))
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
            fun testSemanticallyValid(fileName: String) {
                assertTrue(isSemanticallyValid("$printValidFileFolderPath/$fileName.wacc"))
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
            fun testSemanticallyValid(fileName: String) {
                assertTrue(isSemanticallyValid("$readValidFileFolderPath/$fileName.wacc"))
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
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$pairsValidFileFolderPath/$fileName.wacc"))
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
            fun testSemanticallyValid(fileName: String) {
                assertTrue(
                    isSemanticallyValid(
                        "$arrayOutOfBoundsValidFileFolderPath/$fileName" +
                            ".wacc"
                    )
                )
            }
        }

        @Nested
        inner class DivideByZeroValidFiles {
            private val divideByZeroValidFileFolderPath = "$runtimeErrorValidFileFolderPath/divideByZero"

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["divideByZero", "divZero", "modByZero"])
            fun testSemanticallyValid(fileName: String) {
                assertTrue(isSemanticallyValid("$divideByZeroValidFileFolderPath/$fileName.wacc"))
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
            fun testSemanticallyValid(fileName: String) {
                assertTrue(
                    isSemanticallyValid(
                        "$integerOverflowValidFileFolderPath/$fileName" +
                            ".wacc"
                    )
                )
            }
        }

        @Nested
        inner class NullDereferenceValidFiles {
            private val nullDereferenceValidFileFolderPath = "$runtimeErrorValidFileFolderPath/nullDereference"

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["freeNull", "readNull1", "readNull2", "setNull1", "setNull2", "useNull1", "useNull2"])
            fun testSemanticallyValid(fileName: String) {
                assertTrue(
                    isSemanticallyValid(
                        "$nullDereferenceValidFileFolderPath/$fileName" +
                            ".wacc"
                    )
                )
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
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$scopeValidFileFolderPath/$fileName.wacc"))
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
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$sequenceValidFileFolderPath/$fileName.wacc"))
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
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$variablesValidFileFolderPath/$fileName.wacc"))
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
        fun testSemanticallyValid(fileName: String) {
            assertTrue(isSemanticallyValid("$whileValidFileFolderPath/$fileName.wacc"))
        }
    }

    @Nested
    inner class SemanticErrorInvalidFiles {
        private val semanticErrorFileFolderPath = "$invalidFolderPath/semanticErr"

        @Nested
        inner class ExitInvalidFiles {
            private val exitInvalidFileFolderPath = "$semanticErrorFileFolderPath/exit"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["badCharExit", "exitNonInt", "globalReturn"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$exitInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class ExpressionInvalidFiles {
            private val expressionsInvalidFileFolderPath = "$semanticErrorFileFolderPath/expressions"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(
                strings = [
                    "boolOpTypeErr", "exprTypeErr", "intOpTypeErr", "lessPairExpr", "mixedOpTypeErr",
                    "moreArrExpr", "stringElemErr"
                ]
            )
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$expressionsInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class FunctionInvalidFiles {
            private val functionInvalidFileFolderPath = "$semanticErrorFileFolderPath/function"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(
                strings = [
                    "functionAssign", "functionBadArgUse", "functionBadCall", "functionBadParam",
                    "functionBadReturn", "functionOverArgs", "functionRedefine", "functionSwapArgs", "functionUnderArgs",
                    "funcVarAccess", "functionNonVoidVoidReturn", "functionVoidNonVoidReturn"
                ]
            )
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$functionInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class IfInvalidFiles {
            private val ifInvalidFileFolderPath = "$semanticErrorFileFolderPath/if"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["ifIntCondition"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$ifInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class IOInvalidFiles {
            private val ioInvalidFileFolderPath = "$semanticErrorFileFolderPath/IO"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["readTypeErr"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$ioInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class MultipleInvalidFiles {
            private val multipleInvalidFileFolderPath = "$semanticErrorFileFolderPath/multiple"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["funcMess", "ifAndWhileErrs", "messyExpr", "multiCaseSensitivity", "multiTypeErrs"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$multipleInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class PairInvalidFiles {
            private val pairsInvalidFileFolderPath = "$semanticErrorFileFolderPath/pairs"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["freeNonPair", "fstNull", "sndNull"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$pairsInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class PrintInvalidFiles {
            private val printInvalidFileFolderPath = "$semanticErrorFileFolderPath/print"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["printTypeErr01"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$printInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class ReadInvalidFiles {
            private val readInvalidFileFolderPath = "$semanticErrorFileFolderPath/read"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["readTypeErr01"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$readInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class ScopeInvalidFiles {
            private val scopeInvalidFileFolderPath = "$semanticErrorFileFolderPath/scope"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["badScopeRedefine"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$scopeInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class VariableInvalidFiles {
            private val variableInvalidFileFolderPath = "$semanticErrorFileFolderPath/variables"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(
                strings = [
                    "basicTypeErr01", "basicTypeErr02", "basicTypeErr03", "basicTypeErr04", "basicTypeErr05",
                    "basicTypeErr06", "basicTypeErr07", "basicTypeErr08", "basicTypeErr09", "basicTypeErr10",
                    "basicTypeErr11", "basicTypeErr12", "caseMatters", "doubleDeclare", "undeclaredScopeVar",
                    "undeclaredVar", "undeclaredVarAccess"
                ]
            )
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$variableInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }

        @Nested
        inner class WhileInvalidFiles {
            private val whileInvalidFileFolderPath = "$semanticErrorFileFolderPath/while"

            @ParameterizedTest(name = "check {0} source code is not semantically valid")
            @ValueSource(strings = ["falsErr", "truErr", "whileIntCondition"])
            fun testSemanticallyValid(fileName: String) {
                assertFalse {
                    isSemanticallyValid("$whileInvalidFileFolderPath/$fileName.wacc")
                }
            }
        }
    }

    /**
     * Generate an AST and walk the tree to perform semantic/type checks.
     *
     * @param path Path to the file to be checked semantically.
     * @exception ic.doc.group15.semantics.ast.SemanticError
     */
    private fun isSemanticallyValid(path: String): Boolean {
        val input = CharStreams.fromFileName(path)
        val lexer = WaccLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = WaccParser(tokens)

        val program = parser.program()

        val st = SymbolTable()
        val ast = AST(st)
        val syntacticErrors = SyntacticErrorList()
        val semanticErrors = SemanticErrorList()
        val visitor = ParseTreeVisitor(
            ast,
            st,
            syntacticErrors,
            semanticErrors,
            enableLogging = ENABLE_LOGGING
        )

        visitor.visit(program)

        if (semanticErrors.hasErrors()) {
            println(path)
            semanticErrors.printErrors()
            return false
        }

        val asm = AstAssemblyGenerator(ast, enableLogging = ENABLE_LOGGING)
        val writer = System.out.bufferedWriter()
        asm.generate(writer)
        writer.flush()

        return true
    }
}
