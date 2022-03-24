package ic.doc.group15.integration

import ic.doc.group15.Option
import ic.doc.group15.utils.EmulationUtils.Companion.exitCodeAndOutputMatchesOptimization
import ic.doc.group15.utils.TIMEOUT
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BCETest {
    private val optimizationFolder = "optimization_test_cases"

    //@Disabled
    @Nested
    inner class ArrayValidFiles {
        private val bceFolder = "$optimizationFolder/BCE"

        @Timeout(TIMEOUT)
        @ParameterizedTest(name = "{0}")
        @ValueSource(
            strings = [
                "multipleArrays", "cannotEvaluateIndices", "free", "function",
            "function-free",
                "if", "nested",
                "reassignment", "redeclaration", "scope", "simple", "while"
            ]
        )
        fun testExecutionProducesExpectedExitCodeAndOutput(fileName: String) {
            val filePath = "$bceFolder/$fileName.wacc"

            exitCodeAndOutputMatchesOptimization(fileName, filePath,
                compileOption = Option.OPTIMISATION_LEVEL_1)
        }
    }
}