package com.hidra.launcher.shizuku

import kotlin.system.exitProcess

/** Instantiated by Shizuku in its own shell-privileged process; see [ShizukuController]. */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): String {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val out = process.inputStream.bufferedReader().readText()
        val err = process.errorStream.bufferedReader().readText()
        val code = process.waitFor()
        return "$code\n$out$err"
    }
}
