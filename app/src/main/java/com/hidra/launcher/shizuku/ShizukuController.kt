package com.hidra.launcher.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.hidra.launcher.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Bridges to Shizuku so HidR-A can disable ("freeze") and re-enable other apps.
 * A disabled app is removed from every launcher and app drawer on the device;
 * re-enabling it brings it back. Needs Shizuku installed, running and allowed.
 */
object ShizukuController {

    enum class Status { NotInstalled, NotRunning, NeedsPermission, Connecting, Ready }

    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 1001
    private val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")

    private val _status = MutableStateFlow(Status.NotInstalled)
    val status: StateFlow<Status> = _status.asStateFlow()
    val isReady: Boolean get() = _status.value == Status.Ready

    private var appContext: Context? = null
    private var initialized = false
    @Volatile private var service: IShellService? = null
    @Volatile private var binding = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IShellService.Stub.asInterface(binder)
            binding = false
            refresh()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            binding = false
            refresh()
        }
    }

    fun init(context: Context) {
        if (initialized) {
            refresh()
            return
        }
        initialized = true
        appContext = context.applicationContext
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            service = null
            binding = false
            refresh()
        }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
        refresh()
    }

    fun refresh() {
        val ctx = appContext ?: return
        _status.value = when {
            !Shizuku.pingBinder() ->
                if (isInstalled(ctx)) Status.NotRunning else Status.NotInstalled
            Shizuku.isPreV11() -> Status.NotRunning
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED ->
                Status.NeedsPermission
            service == null -> {
                bind(ctx)
                Status.Connecting
            }
            else -> Status.Ready
        }
    }

    fun requestPermission() {
        if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    /** Disables the app for the main user. Blocking; call off the main thread. */
    fun freeze(packageName: String): Boolean = runPm("disable-user", packageName)

    /** Re-enables the app for the main user. Blocking; call off the main thread. */
    fun unfreeze(packageName: String): Boolean = runPm("enable", packageName)

    private fun runPm(verb: String, packageName: String): Boolean {
        // The name is interpolated into a shell command, so only accept a real package name.
        if (!packageName.matches(PACKAGE_REGEX)) return false
        return exec("pm $verb --user 0 $packageName")?.first == 0
    }

    private fun exec(command: String): Pair<Int, String>? {
        val svc = service ?: return null
        return try {
            val raw = svc.exec(command)
            val newline = raw.indexOf('\n')
            (raw.substring(0, newline).toIntOrNull() ?: -1) to raw.substring(newline + 1)
        } catch (e: Exception) {
            null
        }
    }

    private fun bind(ctx: Context) {
        if (binding) return
        binding = true
        val args = Shizuku.UserServiceArgs(ComponentName(ctx.packageName, ShellService::class.java.name))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(1)
        Shizuku.bindUserService(args, connection)
    }

    private fun isInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
