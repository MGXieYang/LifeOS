package com.lifeos.app

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
import com.lifeos.core.ui.LifeOsTheme
import com.lifeos.feature.settings.SettingsScreen
import com.lifeos.feature.statistics.StatisticsScreen
import com.lifeos.feature.v2.*

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeOsTheme {
                val vm: AppViewModel = viewModel(factory=object: ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T = AppViewModel(application as LifeOsApplication) as T
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
    val route=entry?.destination?.route ?: "now"
    val pages=listOf(Triple("now","当下",Icons.Outlined.Home),Triple("balance","天平",Icons.Outlined.Balance),
        Triple("time","时间",Icons.Outlined.Schedule),Triple("decision","决策",Icons.Outlined.FactCheck))
    fun navigate(target: String) { nav.navigate(target) {popUpTo("now") {saveState=true};launchSingleTop=true;restoreState=true} }
    Scaffold(bottomBar={if(route in pages.map{it.first}) NavigationBar { pages.forEach { (target,label,icon) ->
        NavigationBarItem(selected=route==target,onClick={navigate(target)},icon={Icon(icon,contentDescription=label)},label={Text(label)})
    }}}) { padding ->
        NavHost(navController=nav,startDestination="now",modifier=Modifier.padding(padding)) {
            composable("now") {NowScreen(data,{nav.navigate("work")},{nav.navigate("settings")})}
            composable("balance") {BalanceScreen(data,vm::saveBalance,vm::deleteBalance,vm::pinBalance)}
            composable("time") {TimeScreen(data,vm::saveNode,vm::deleteNode)}
            composable("decision") {DecisionScreen(data,vm::saveDecision,vm::deleteDecision,vm::saveReview)}
            composable("work") {StatisticsScreen(data)}
            composable("settings") {SettingsScreen(data.settings,data.calendarDescription,vm::saveSettings,vm::updateCalendar,vm::exportBackup,vm::importBackup)}
        }
    }
}
