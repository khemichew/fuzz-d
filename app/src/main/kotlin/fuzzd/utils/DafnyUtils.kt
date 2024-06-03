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
            BackendTarget.RUST to RustBackendFeatures(),
            BackendTarget.CSHARP to CSharpBackendFeatures(),
            BackendTarget.JAVASCRIPT to JavaScriptBackendFeatures(),
            BackendTarget.GO to GoBackendFeatures(),
            BackendTarget.PYTHON to PythonBackendFeatures(),
            BackendTarget.JAVA to JavaBackendFeatures(),
            BackendTarget.ALL to AllBackendFeatures(),
        )

        fun MAX_TYPE_DEPTH(target: BackendTarget): Int? =
            when (target) {
                BackendTarget.RUST -> 1
                else -> null
            }
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
