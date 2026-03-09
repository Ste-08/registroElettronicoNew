package com.riccardocalligaro.registro_elettronico

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.riccardocalligaro.registro_elettronico/multi-account"
    private val WIDGET_CHANNEL = "com.riccardocalligaro.registro_elettronico/agenda_widget"
    private val GRADES_WIDGET_CHANNEL = "com.riccardocalligaro.registro_elettronico/grades_widget"
    
    companion object {
        var pendingWidgetNavigation: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "OPEN_AGENDA" -> pendingWidgetNavigation = "agenda"
            "OPEN_GRADES" -> pendingWidgetNavigation = "grades"
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "restartApp") {
                val packageManager: PackageManager = context.packageManager
                val intent: Intent? = packageManager.getLaunchIntentForPackage(context.packageName)
                val componentName: ComponentName? = intent?.component
                val mainIntent: Intent = Intent.makeRestartActivityTask(componentName)
                context.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, WIDGET_CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "updateWidget") {
                AgendaWidgetProvider.refreshWidget(context)
                result.success(true)
            } else if (call.method == "getPendingNavigation") {
                val nav = pendingWidgetNavigation
                pendingWidgetNavigation = null
                result.success(nav)
            } else {
                result.notImplemented()
            }
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, GRADES_WIDGET_CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "updateGradesWidget") {
                GradesWidgetProvider.refreshWidget(context)
                result.success(true)
            } else {
                result.notImplemented()
            }
        }
    }
}
