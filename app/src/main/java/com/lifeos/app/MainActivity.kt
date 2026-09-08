package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeos.core.ui.LifeOsTheme
import com.lifeos.domain.ThemeMode
import com.lifeos.feature.balance.BalanceScreen
import com.lifeos.feature.balance.BalanceHistoryScreen
import com.lifeos.feature.home.NowScreen
import com.lifeos.feature.lifetime.AddLifeNodeScreen
import com.lifeos.feature.lifetime.TimeScreen
import com.lifeos.feature.onboarding.OnboardingScreen
import com.lifeos.feature.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(application as LifeOsApplication) as T
            })
            val state by vm.state.collectAsStateWithLifecycle()
            val theme = (state as? ScreenState.Ready)?.settings?.themeMode ?: ThemeMode.SYSTEM
            LifeOsTheme(theme) {
                Surface(Modifier.fillMaxSize()) {
                    when (val data = state) {
                        ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        is ScreenState.Failed -> Column(
                            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("暂时无法读取本地数据")
                            Text(data.message)
                            Button(onClick = vm::retry) { Text("重试") }
                        }
                        is ScreenState.Ready -> if (!data.settings.onboarded) {
                            OnboardingScreen(data.settings, vm::saveSettings)
                        } else MainNavigation(data, vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNavigation(data: ScreenState.Ready, vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "now"
    val pages = listOf(
        Triple("now", "当下", Icons.Outlined.Home),
        Triple("balance", "天平", Icons.Outlined.Balance),
        Triple("time", "时间", Icons.Outlined.Schedule),
        Triple("settings", "设置", Icons.Outlined.Settings),
    )
    fun navigate(target: String) {
        nav.navigate(target) {
            popUpTo("now") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(
        bottomBar = {
            if (route in pages.map { it.first }) {
                NavigationBar {
                    pages.forEach { (target, label, icon) ->
                        NavigationBarItem(
                            selected = route == target,
                            onClick = { navigate(target) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = "now", modifier = Modifier.padding(padding)) {
            composable("now") { NowScreen(data) }
            composable("balance") { BalanceScreen(data, vm::saveBalance) { nav.navigate("balance/history") } }
            composable("balance/history") {
                BalanceHistoryScreen(data.balances, vm::deleteBalance, nav::popBackStack)
            }
            composable("time") { TimeScreen(data, vm::deleteNode) { nav.navigate("time/add") } }
            composable("time/add") { AddLifeNodeScreen(vm::saveNode, nav::popBackStack) }
            composable("settings") { SettingsScreen(data.settings, data.calendarDescription, vm::saveSettings) }
        }
    }
}
