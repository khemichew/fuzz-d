package fuzzd.utils

import fuzzd.generator.selection.supported_features.*

const val DAFNY_ADVANCED = "advanced"
const val DAFNY_MAIN = "main"
const val DAFNY_TYPE = "dfy"
const val DAFNY_GENERATED = "generated"

enum class BackendTarget {
    RUST,
    CSHARP,
    JAVASCRIPT,
    JAVA,
    GO,
    PYTHON,
    ALL
}

sealed class SupportedFeaturesRetriever {
    companion object {
        val BACKEND_SUPPORTED_FEATURES = mapOf(
            BackendTarget.RUST to RustSupportedFeatures(),
            BackendTarget.CSHARP to CSharpSupportedFeatures(),
            BackendTarget.JAVASCRIPT to JavaScriptSupportedFeatures(),
            BackendTarget.GO to GoSupportedFeatures(),
            BackendTarget.PYTHON to PythonSupportedFeatures(),
            BackendTarget.JAVA to JavaSupportedFeatures(),
            BackendTarget.ALL to AllSupportedFeatures(),
        )
    }
}

fun runCommand(command: String): Process {
    return Runtime.getRuntime().exec(command)
}

fun compileDafny(targetLanguage: String, fileDir: String, fileName: String, timeout: Long): Process {
    val command =
        "timeout $timeout dafny /compileVerbose:0 /noVerify /compile:2 /spillTargetCode:1 /compileTarget:$targetLanguage $fileDir/$fileName.dfy"
    return runCommand(command)
}

fun verifyDafny(fileDir: String, fileName: String, timeout: Long): Process {
    val command = "timeout $timeout dafny /compile:0 $fileDir/$fileName.dfy"
    return runCommand(command)
}

fun Process.readInputStream(): String = String(inputStream.readAllBytes())

fun Process.readErrorStream(): String = String(errorStream.readAllBytes())
