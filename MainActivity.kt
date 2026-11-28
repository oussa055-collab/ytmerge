package com.example.cosmos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cosmos.ui.CosmosTheme
import com.example.cosmos.ui.DetailScreen
import com.example.cosmos.ui.FeedScreen
import com.example.cosmos.ui.FeedViewModel
import com.example.cosmos.ui.LiveScreen

class MainActivity : AppCompatActivity() {

    private var pendingId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingId = intent.getStringExtra(EXTRA_POST_ID)
        setContent {
            CosmosTheme {
                AppNav(pendingId = pendingId, onConsumed = { pendingId = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingId = intent.getStringExtra(EXTRA_POST_ID)
    }

    companion object {
        const val EXTRA_POST_ID = "post_id"
    }
}

@Composable
private fun AppNav(pendingId: String?, onConsumed: () -> Unit) {
    val nav = rememberNavController()
    val vm: FeedViewModel = viewModel()
    val context = LocalContext.current

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(pendingId) {
        if (pendingId != null) {
            nav.navigate("detail/$pendingId")
            onConsumed()
        }
    }

    NavHost(nav, startDestination = "feed") {
        composable("feed") {
            FeedScreen(vm, onOpen = { id -> nav.navigate("detail/$id") }, onLive = { nav.navigate("live") })
        }
        composable("live") { LiveScreen { nav.popBackStack() } }
        composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            DetailScreen(entry.arguments?.getString("id").orEmpty(), vm) { nav.popBackStack() }
        }
    }

    SetupDialog()
}

/** One-time hint about background updates (Xiaomi kills background work unless allowed). */
@Composable
private fun SetupDialog() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cosmos", Context.MODE_PRIVATE) }
    var show by remember { mutableStateOf(!prefs.getBoolean("setup_shown", false)) }
    if (!show) return

    fun close() {
        prefs.edit().putBoolean("setup_shown", true).apply()
        show = false
    }

    AlertDialog(
        onDismissRequest = { close() },
        title = { Text(stringResource(R.string.setup_title)) },
        text = { Text(stringResource(R.string.setup_body)) },
        confirmButton = {
            TextButton(onClick = {
                close()
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                )
            }) { Text(stringResource(R.string.setup_open)) }
        },
        dismissButton = { TextButton(onClick = { close() }) { Text(stringResource(R.string.setup_later)) } },
    )
}
