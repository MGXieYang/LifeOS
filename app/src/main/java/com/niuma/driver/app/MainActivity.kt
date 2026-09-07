package com.niuma.driver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.niuma.driver.core.ui.NiuMaTheme
import com.niuma.driver.feature.home.HomeScreen
import com.niuma.driver.feature.settings.SettingsScreen
import com.niuma.driver.feature.statistics.StatisticsScreen
import com.niuma.driver.feature.wish.WishScreen

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NiuMaTheme {
                val vm: AppViewModel = viewModel(factory=object: ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T = AppViewModel(application as NiuMaApplication) as T
                })
                val state by vm.state.collectAsStateWithLifecycle()
                when(val data=state) {
                    ScreenState.Loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator()}
                    is ScreenState.Failed -> Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),verticalArrangement=Arrangement.Center) {
                        Text("暂时无法读取本地数据")
                        Text(data.message)
                        Button(onClick=vm::retry) {Text("重试")}
                    }
                    is ScreenState.Ready -> if(!data.settings.onboarded) {
                        Surface(Modifier.fillMaxSize().systemBarsPadding()) {
                            SettingsScreen(data.settings,data.calendarDescription,vm::saveSettings,vm::updateCalendar,onboarding=true)
                        }
                    } else MainNavigation(data,vm)
                }
            }
        }
    }
}
@Composable private fun MainNavigation(data: ScreenState.Ready,vm: AppViewModel) {
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route ?: "home"
    val pages=listOf(Triple("home","首页",Icons.Outlined.Home),Triple("wish","心愿",Icons.Outlined.CardGiftcard),
        Triple("statistics","统计",Icons.Outlined.BarChart),Triple("settings","设置",Icons.Outlined.Settings))
    fun navigate(target: String) { nav.navigate(target) {popUpTo("home") {saveState=true};launchSingleTop=true;restoreState=true} }
    Scaffold(bottomBar={NavigationBar { pages.forEach { (target,label,icon) ->
        NavigationBarItem(selected=route==target,onClick={navigate(target)},icon={Icon(icon,contentDescription=label)},label={Text(label)})
    }}}) { padding ->
        NavHost(navController=nav,startDestination="home",modifier=Modifier.padding(padding)) {
            composable("home") {HomeScreen(data,{navigate("wish")},{navigate("settings")})}
            composable("wish") {WishScreen(data,vm::saveWish,vm::deleteWish)}
            composable("statistics") {StatisticsScreen(data)}
            composable("settings") {SettingsScreen(data.settings,data.calendarDescription,vm::saveSettings,vm::updateCalendar)}
        }
    }
}
