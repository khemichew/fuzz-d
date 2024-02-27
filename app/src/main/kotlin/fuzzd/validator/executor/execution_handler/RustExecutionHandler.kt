package fuzzd.validator.executor.execution_handler

class RustExecutionHandler(fileDir: String, fileName: String) : AbstractExecutionHandler(fileDir, fileName) {
    override fun getCompileTarget(): String = "rs"

    override fun getExecuteCommand(fileDir: String, fileName: String): String =
        "cargo run --quiet --manifest-path $fileDir/$fileName-rust/Cargo.toml"
}
