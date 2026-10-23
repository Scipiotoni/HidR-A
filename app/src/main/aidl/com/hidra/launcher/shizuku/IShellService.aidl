package com.hidra.launcher.shizuku;

// Runs inside the Shizuku-spawned shell process (uid 2000), which is allowed
// to enable/disable other apps with `pm`.
interface IShellService {
    // Transaction id reserved by Shizuku for tearing the service down.
    void destroy() = 16777114;

    // Returns "<exitCode>\n<combined output>".
    String exec(String command) = 1;
}
