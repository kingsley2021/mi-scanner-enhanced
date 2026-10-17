package io.github.lcebot.miscannerenhanced

import android.util.Log
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Method

class MainHook : XposedModule() {

    companion object {
        private const val TAG = "MiScannerEnhanced"
        private const val TARGET_PACKAGE = "com.xiaomi.scanner"
        private const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    }

    private fun log(msg: String, t: Throwable? = null) {
        try {
            if (t != null) {
                Log.i(TAG, msg, t)
                log(Log.INFO, TAG, msg, t)
            } else {
                Log.i(TAG, msg)
                log(Log.INFO, TAG, msg)
            }
        } catch (_: Throwable) {
        }
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        if (param.packageName != TARGET_PACKAGE) return
        log("Loaded in $TARGET_PACKAGE")

        val cl = param.defaultClassLoader
        hookCensorship(cl)
        hookLocationPermission(cl)
    }

    private fun hookCensorship(cl: ClassLoader) {
        try {
            val dialog = cl.loadClass("com.xiaomi.scanner.dialog.CodeTextDialog")
            var showUI: Method? = null
            var checkContent: Method? = null
            for (m in dialog.declaredMethods) {
                when (m.name) {
                    "showUI" -> if (m.parameterCount == 1 && m.parameterTypes[0] == String::class.java) showUI = m
                    "checkContent" -> if (m.parameterCount == 1 && m.parameterTypes[0] == String::class.java) checkContent = m
                }
            }
            if (checkContent == null || showUI == null) {
                log("Could not find checkContent or showUI")
                return
            }
            showUI.isAccessible = true
            val show = showUI
            hook(checkContent).setId("censor").setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    try {
                        val text = chain.getArg(0) as? String ?: return@intercept null
                        show.invoke(chain.getThisObject(), text)
                    } catch (t: Throwable) {
                        log("showUI fallback to proceed", t)
                        chain.proceed()
                    }
                    null
                }
            log("Hooked checkContent -> showUI")
        } catch (t: Throwable) {
            log("Censorship hook failed", t)
        }
    }

    private fun hookLocationPermission(cl: ClassLoader) {
        try {
            val utils = cl.loadClass("com.xiaomi.scanner.app.utils.PermissionsUtils")
            for (m in utils.declaredMethods) {
                if (m.name != "hasPermission") continue
                val params = m.parameterTypes
                if (params.size != 2 || params[1] != Array<String>::class.java) continue

                hook(m).setId("locperm").setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        val perms = try {
                            @Suppress("UNCHECKED_CAST")
                            chain.getArg(1) as? Array<String>
                        } catch (_: Throwable) { null }
                        if (perms != null && perms.isNotEmpty() && perms.all { it == FINE_LOCATION }) {
                            java.lang.Boolean.TRUE
                        } else {
                            chain.proceed()
                        }
                    }
                log("Hooked hasPermission for location bypass")
                return
            }
            log("hasPermission(Activity, String[]) not found")
        } catch (t: Throwable) {
            log("Location permission hook failed", t)
        }
    }
}
