package ic.doc.group15.utils

import ic.doc.group15.Option
import org.apache.maven.surefire.shade.org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.nio.charset.StandardCharsets

const val TIMEOUT: Long = 5

enum class ActualSource {
    Linux,
    Online
}

enum class ExpectedSource {
    Cache,
    Online
}

class EmulationUtils {
    companion object {
        const val validFolderPath = "wacc_examples/valid"
        const val validModelOutputFolderPath = "model_output/$validFolderPath"
        private const val ENABLE_LOGGING = true
        private const val bash = "/bin/bash"
        private const val options = "-c"
        private const val saveErrors = "2>&1"

        /**
         * Compile a WACC program into assembly code compatible with ARM1176JZF-S processor,
         * assemble and link it using the gcc cross-compiler with the arm-linux-gnueabi-gcc
         * package, and execute the machine code using the QEMU-ARM emulator targetting
         * the ARM1176JZF-S processor.
         */
        fun exitCodeAndOutputMatchesLinux(
            fileName: String,
            filePath: String,
            resultPath: String
        ) {
            exitCodeAndOutputMatches(fileName, filePath, resultPath = resultPath)
        }

        fun exitCodeAndOutputMatchesOnline(
            fileName: String,
            filePath: String,
            resultPath: String
        ) {
            exitCodeAndOutputMatches(fileName, filePath, resultPath = resultPath,
                actualSource = ActualSource.Online)
        }

        fun exitCodeAndOutputMatchesOptimization(
            fileName: String,
            filePath: String,
            compileOption: Option? = null
        ) {
            exitCodeAndOutputMatches(fileName, filePath, actualSource =
            ActualSource.Online, expectedSource = ExpectedSource.Online,
                compileOption = compileOption)
        }

        private fun exitCodeAndOutputMatches(
            fileName: String,
            filePath: String,
            resultPath: String? = null,
            actualSource: ActualSource = ActualSource.Linux,
            expectedSource: ExpectedSource = ExpectedSource.Cache,
            compileOption: Option? = null
        ) {
            log("Testing: $filePath")
            compile(filePath, compileOption)
            val actualExitCode: Int
            val actualOutput: String
            val actualAssembly: String
            when (actualSource) {
                ActualSource.Online -> {
                    val (fst, snd, third) = emulateOnline(fileName)
                    actualExitCode = fst
                    actualOutput = snd
                    actualAssembly = third
                }
                ActualSource.Linux -> {
                    val (fst, snd, third) = emulateLinux(fileName)
                    actualExitCode = fst
                    actualOutput = snd
                    actualAssembly = third
                }
            }
            val expectedExitCode: Int
            val expectedOutput: String
            val expectedAssembly: String
            when (expectedSource) {
                ExpectedSource.Cache -> {
                    val (fst, snd, third) = getExpectedResult(resultPath!!)
                    expectedExitCode = fst
                    expectedOutput = snd
                    expectedAssembly = third
                }
                ExpectedSource.Online -> {
                    val (fst, snd, third) = getExpectedOnline(filePath)
                    expectedExitCode = fst
                    expectedOutput = snd
                    expectedAssembly = third
                }
            }

            val exitCodeMatches = (expectedExitCode == actualExitCode)
            val outputMatches = (expectedOutput == actualOutput)

            log("Expected exit code: $expectedExitCode, actual: $actualExitCode")
            log("Expected output:\n$expectedOutput\nActual output:\n$actualOutput")
            log("Expected assembly:\n$expectedAssembly\nActual " +
                    "assembly:\n$actualAssembly")
            assertTrue(
                exitCodeMatches && outputMatches,
                "Our emulation produced results different than the reference " +
                        "compiler for file: $filePath.\n" +
                        "Exit code " +
                        "matches: " +
                        "$exitCodeMatches, output " +
                        "matches: " +
                        "$outputMatches.\n"
            )
        }

        private fun getExpectedResult(path: String): Triple<Int, String,
                String> {
            val list = File(path).readLines()
            val exitCode = list[1].trim().toInt()
            val output = list
                .subList(3, list.size)
                .joinToString("\n")
                .split("Assembly:")[0].trim()
            val assembly = list
                .joinToString("\n")
                .split("Assembly:")[1].trim()

            return Triple(exitCode, output, assembly)
        }

        private fun compile(path: String, compileOption: Option? = null) {
            val optionStr = compileOption?.print() ?: ""
            val compilation = ProcessBuilder(
                bash,
                options,
                "./compile $path $optionStr $saveErrors"
            ).start()
            val compilationExitStatus = compilation.waitFor()
            val compilationOutput = IOUtils.toString(
                compilation.inputStream,
                StandardCharsets.UTF_8.name()
            )
            assertTrue(
                setOf(0, 100, 200)
                    .contains(compilationExitStatus), "./compile failed with " +
                        "exit status $compilationExitStatus and produced " +
                        "the following output: \n${compilationOutput}\n"
            )
        }

        private fun emulateLinux(
            fileName: String
        ): Triple<Int, String, String> {
            
            val emulationOutput = emulateLinuxHelper(fileName)

            val list = emulationOutput.split("\n")
            val output =
                list.subList(0, list.size - 1).joinToString("\n")
            val exitCode = list[list.size - 1].trim().toInt()
            val assembly = ""

            return Triple(exitCode, output, assembly)
        }

        private fun emulateOnline(
            fileName: String
        ) : Triple<Int, String, String> {

            val emulationOutput = emulateOnlineHelper(fileName)

            val output = emulationOutput
                .split("-- Emulation Output:\n")[1]
                .split("---------------------------------------------------------------")[0]
                .trim()
            val exitCode = Regex("(?<=The exit code is: )[0-9]+(?=\\.)")
                .find(emulationOutput)?.value!!.toInt()
            val assembly = emulationOutput
            .split("-- Uploaded file:")[1]
            .split("---------------------------------------------------------------\n")[1]
            .split("---------------------------------------------------------------")[0]
            .trim()

            return Triple(exitCode, output, assembly)
        }

        private fun emulateOnlineHelper(fileName: String): String {
            val emulateOnline = "./wacc_examples/refEmulate $fileName.s"
            val emulate =
                ProcessBuilder(
                    bash, options,
                    "echo '' | $emulateOnline $saveErrors"
                ).start()
            val exitCode = emulate.waitFor()
            val output = IOUtils.toString(
                emulate.inputStream,
                StandardCharsets.UTF_8.name()
            ).trim()
            assertTrue(0 == exitCode, "refEmulate failed. Output: $output\n")
            return output
        }

        private fun emulateLinuxHelper(fileName: String): String {
            val createExecutable = "arm-linux-gnueabi-gcc -o $fileName " +
                    "-mcpu=arm1176jzf-s -mtune=arm1176jzf-s $fileName.s"
            val execute =
                "echo '' | qemu-arm -L /usr/arm-linux-gnueabi $fileName"
            val echoExitCode = "echo \$?"
            val emulation = ProcessBuilder(
                bash, options,
                "$createExecutable; $execute; $echoExitCode $saveErrors"
            ).start()
            val emulationExitStatus = emulation.waitFor()
            val emulationOutput = IOUtils.toString(
                emulation.inputStream,
                StandardCharsets.UTF_8.name()
            ).trim()
            assertTrue(
                0 == emulationExitStatus,
                "Emulating using qemu failed. Output: $emulationOutput\n"
            )
            return emulationOutput
        }

        private fun getExpectedOnline(
            path: String
        ) : Triple<Int, String, String> {
            val emulationOutput = compileAndEmulateExpected(path)

            val output = emulationOutput
                .split(
                    "-- Executing...\n" +
                            "===========================================================\n"
                )[1]
                .split("===========================================================")[0]
                .trim()
            val exitCode = Regex("(?<=The exit code is )[0-9]+(?=\\.)")
                .find(emulationOutput)?.value!!.toInt()
            val assemblyIntermediate = emulationOutput
                .split("contents are:\n" +
                        "===========================================================\n")[1]
                .split("===========================================================")[0]
            val assembly = Regex("^[0-9]+\t", RegexOption.MULTILINE)
                .replace(assemblyIntermediate, "").trim()

            return Triple(exitCode, output, assembly)
        }

        private fun compileAndEmulateExpected(waccPath: String): String {
            val modelSolution =
                ProcessBuilder(
                    bash, options,
                    "echo '' | ./wacc_examples/refCompile -ax $waccPath $saveErrors"
                ).start()
            val exitCode = modelSolution.waitFor()
            val output = IOUtils.toString(
                modelSolution.inputStream,
                StandardCharsets.UTF_8.name()
            ).trim()
            assertTrue(0 == exitCode, "refCompile failed\n. Output: $output")
            return output
        }

        private fun log(message: String) {
            if (ENABLE_LOGGING) {
                println(message)
            }
        }
    }
}