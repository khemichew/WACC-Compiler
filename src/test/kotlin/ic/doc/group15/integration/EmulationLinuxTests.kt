package ic.doc.group15.integration

import ic.doc.group15.utils.EmulationUtils
import ic.doc.group15.utils.EmulationUtils.Companion.exitCodeAndOutputMatchesLinux
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ic.doc.group15.utils.TIMEOUT


class EmulationLinuxTests {
    private val validFolderPath = EmulationUtils.validFolderPath
    private val validModelOutputFolderPath = EmulationUtils.validModelOutputFolderPath

    // @Disabled
    @Nested
    inner class ArrayValidFiles {
        private val arrayFolderPath = "$validFolderPath/array"
        private val arrayResultFolderPath = "$validModelOutputFolderPath/array"

        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "array", "arrayBasic", "arrayEmpty", "arrayLength", "arrayLookup", "arrayNested",
                "arrayPrint", "arraySimple", "modifyString", "printRef"
            ]
        )
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$arrayFolderPath/$fileName.wacc"
            val resultPath = "$arrayResultFolderPath/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }
    }

    @Nested
    inner class BasicValidFiles {
        private val basicFolderPath = "$validFolderPath/basic"
        private val basicResultFolderPath = "$validModelOutputFolderPath/basic"

        @Nested
        inner class ExitValidFiles {
            private val exitFolderPath = "$basicFolderPath/exit"
            private val exitResultFolderPath = "$basicResultFolderPath/exit"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["exit-1", "exitBasic", "exitBasic2", "exitWrap"])
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$exitFolderPath/$fileName.wacc"
                val resultPath = "$exitResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }

        @Nested
        inner class SkipValidFiles {
            private val skipFolderPath = "$basicFolderPath/skip"
            private val skipResultFolderPath = "$basicResultFolderPath/skip"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["comment", "commentInLine", "skip"])
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$skipFolderPath/$fileName.wacc"
                val resultPath = "$skipResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }
    }

    // @Disabled
    @Nested
    inner class ExpressionValidFiles {
        private val expressionsFolderPath = "$validFolderPath/expressions"
        private val expressionsResultFolderPath =
            "$validModelOutputFolderPath/expressions"

        @Timeout(TIMEOUT)
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
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$expressionsFolderPath/$fileName.wacc"
            val resultPath = "$expressionsResultFolderPath/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }
    }

    // @Disabled
    @Nested
    inner class FunctionValidFiles {
        private val functionsFolderPath = "$validFolderPath/function"
        private val functionsResultFolderPath =
            "$validModelOutputFolderPath/function"

        @Nested
        inner class NestedFunctionValidFiles {
            private val nestedFunctionsFolderPath =
                "$functionsFolderPath/nested_functions"
            private val nestedFunctionsResultFolderPath =
                "$functionsResultFolderPath/nested_functions"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "fibonacciFullRec", "fibonacciRecursive", "fixedPointRealArithmetic",
                    "functionConditionalReturn", "mutualRecursion", "printInputTriangle", "printTriangle",
                    "simpleRecursion"
                ]
            )
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$nestedFunctionsFolderPath/$fileName.wacc"
                val resultPath = "$nestedFunctionsResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }

        @Nested
        inner class SimpleFunctionValidFiles {
            private val simpleFunctionsFolderPath =
                "$functionsFolderPath/simple_functions"
            private val simpleFunctionsResultFolderPath =
                "$functionsResultFolderPath/simple_functions"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "asciiTable", "functionDeclaration", "functionDoubleReturn", "functionIfReturns",
                    "functionManyArguments", "functionMultiReturns", "functionReturnPair", "functionSimple",
                    "functionSimpleLoop", "functionUpdateParameter", "incFunction", "negFunction", "sameArgName",
                    "sameArgName2", "sameNameAsVar"
                ]
            )
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$simpleFunctionsFolderPath/$fileName.wacc"
                val resultPath = "$simpleFunctionsResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }
    }

    // @Disabled
    @Nested
    inner class IfValidFiles {
        private val ifFolderPath = "$validFolderPath/if"
        private val ifResultFolderPath = "$validModelOutputFolderPath/if"

        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["if1", "if2", "if3", "if4", "if5", "if6", "ifBasic", "ifFalse", "ifTrue", "whitespace"])
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$ifFolderPath/$fileName.wacc"
            val resultPath = "$ifResultFolderPath/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }
    }

    // @Disabled
    @Nested
    inner class IOValidFiles {
        private val ioFolderPath = "$validFolderPath/IO"
        private val ioResultFolderPath = "$validModelOutputFolderPath/IO"

        // Note: IOLoop is left out until a separate function is written to
        // handle interactive input using "echo input | refCompile/command"
        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = ["IOSequence"])
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$ioFolderPath/$fileName.wacc"
            val resultPath = "$ioResultFolderPath/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }

        @Nested
        inner class PrintValidFiles {
            private val printFolderPath = "$ioFolderPath/print"
            private val printResultFolderPath = "$ioResultFolderPath/print"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "hashInProgram", "multipleStringsAssignment", "print", "print-backspace",
                    "print-carridge-return", "printBool", "printChar", "printCharArray", "printCharAsString", "printEscChar",
                    "printInt", "println"
                ]
            )
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$printFolderPath/$fileName.wacc"
                val resultPath = "$printResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }

        @Nested
        inner class ReadValidFiles {
            private val readFolderPath = "$ioFolderPath/read"
            private val readResultFolderPath = "$ioResultFolderPath/read"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "echoBigInt", "echoBigNegInt", "echoChar", "echoInt", "echoNegInt", "echoPuncChar",
                    "read"
                ]
            )
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$readFolderPath/$fileName.wacc"
                val resultPath = "$readResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }
    }

    // @Disabled
    @Nested
    inner class PairValidFiles {
        private val pairsFolderPath = "$validFolderPath/pairs"
        private val pairsResultFolderPah = "$validModelOutputFolderPath/pairs"

        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "checkRefPair", "createPair", "createPair02", "createPair03", "createRefPair", "free",
                "linkedList", "nestedPair", "null", "printNull", "printNullPair", "printPair", "printPairOfNulls", "readPair",
                "writeFst", "writeSnd"
            ]
        )
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$pairsFolderPath/$fileName.wacc"
            val resultPath = "$pairsResultFolderPah/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }
    }

    // @Disabled
    @Nested
    inner class RuntimeErrorValidFiles {
        private val runtimeErrorFolderPath = "$validFolderPath/runtimeErr"
        private val runtimeErrorResultFolderPath =
            "$validModelOutputFolderPath/runtimeErr"

        @Nested
        inner class ArrayOutOfBoundsValidFiles {
            private val arrayOutOfBoundsFolderPath =
                "$runtimeErrorFolderPath/arrayOutOfBounds"
            private val arrayOutOfBoundsResultFolderPath =
                "$runtimeErrorResultFolderPath/arrayOutOfBounds"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["arrayNegBounds", "arrayOutOfBounds", "arrayOutOfBoundsWrite"])
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$arrayOutOfBoundsFolderPath/$fileName.wacc"
                val resultPath = "$arrayOutOfBoundsResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }

        @Nested
        inner class DivideByZeroValidFiles {
            private val divideByZeroFolderPath =
                "$runtimeErrorFolderPath/divideByZero"
            private val divideByZeroResultFolderPath =
                "$runtimeErrorResultFolderPath/divideByZero"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["divideByZero", "divZero", "modByZero"])
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$divideByZeroFolderPath/$fileName.wacc"
                val resultPath = "$divideByZeroResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }

        @Nested
        inner class IntegerOverflowValidFiles {
            private val integerOverflowFolderPath =
                "$runtimeErrorFolderPath/integerOverflow"
            private val integerOverflowResultFolderPath =
                "$runtimeErrorResultFolderPath/integerOverflow"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(
                strings = [
                    "intJustOverflow", "intmultOverflow", "intnegateOverflow", "intnegateOverflow2",
                    "intnegateOverflow3", "intnegateOverflow4", "intUnderflow", "intWayOverflow"
                ]
            )
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$integerOverflowFolderPath/$fileName.wacc"
                val resultPath = "$integerOverflowResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }

        @Nested
        inner class NullDereferenceValidFiles {
            private val nullDereferenceFolderPath =
                "$runtimeErrorFolderPath/nullDereference"
            private val nullDereferenceResultFolderPath =
                "$runtimeErrorResultFolderPath/nullDereference"

            @Timeout(TIMEOUT)
            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = ["freeNull", "readNull1", "readNull2", "setNull1", "setNull2", "useNull1", "useNull2"])
            fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
                val filePath = "$nullDereferenceFolderPath/$fileName.wacc"
                val resultPath = "$nullDereferenceResultFolderPath/$fileName.txt"

                exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
            }
        }
    }

    // @Disabled
    @Nested
    inner class ScopeValidFiles {
        private val scopeFolderPath = "$validFolderPath/scope"
        private val scopeResultFolderPath = "$validModelOutputFolderPath/scope"

        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "ifNested1", "ifNested2", "indentationNotImportant", "intsAndKeywords", "printAllTypes",
                "scope", "scopeBasic", "scopeIfRedefine", "scopeRedefine", "scopeSimpleRedefine", "scopeVars",
                "scopeWhileNested", "scopeWhileRedefine"
            ]
        )
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$scopeFolderPath/$fileName.wacc"
            val resultPath = "$scopeResultFolderPath/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }
    }

    // @Disabled
    @Nested
    inner class SequenceValidFiles {
        private val sequenceFolderPath = "$validFolderPath/sequence"
        private val sequenceResultFolderPath =
            "$validModelOutputFolderPath/sequence"

        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "basicSeq", "basicSeq2", "boolAssignment", "charAssignment", "exitSimple",
                "intAssignment", "intLeadingZeros", "stringAssignment"
            ]
        )
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$sequenceFolderPath/$fileName.wacc"
            val resultPath = "$sequenceResultFolderPath/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }
    }

    @Nested
    inner class VariableValidFiles {
        private val variablesFolderPath = "$validFolderPath/variables"
        private val variablesResultFolderPath =
            "$validModelOutputFolderPath/variables"

        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "_VarNames", "boolDeclaration", "boolDeclaration2", "capCharDeclaration",
                "charDeclaration", "charDeclaration2", "emptyStringDeclaration", "intDeclaration", "longVarNames",
                "manyVariables", "negIntDeclaration", "puncCharDeclaration", "stringDeclaration", "zeroIntDeclaration"
            ]
        )
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$variablesFolderPath/$fileName.wacc"
            val resultPath = "$variablesResultFolderPath/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }
    }

    //@Disabled
    @Nested
    inner class WhileValidFiles {
        private val whileFolderPath = "$validFolderPath/while"
        private val whileResultFolderPath = "$validModelOutputFolderPath/while"

        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "fibonacciFullIt", "fibonacciIterative", "loopCharCondition", "loopIntCondition", "max",
                "min", "rmStyleAdd", "rmStyleAddIO", "whileBasic", "whileBoolFlip", "whileCount", "whileFalse"
            ]
        )
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$whileFolderPath/$fileName.wacc"
            val resultPath = "$whileResultFolderPath/$fileName.txt"

            exitCodeAndOutputMatchesLinux(fileName, filePath, resultPath)
        }
    }
}
