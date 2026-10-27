package ir.techima.kardan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Standard scrolling page with the shared header. */
@Composable
fun Page(
    title: String, subtitle: String? = null, onBack: (() -> Unit)? = null,
    scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, subtitle, onBack)
        val m = if (scroll) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier.weight(1f)
        Column(m.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Navigator(state: AppState) {
    when (val s = state.current) {
        Screen.Home -> HomeScreen(state)
        Screen.Guides -> GuidesScreen(state)
        Screen.Tools -> ToolsScreen(state)
        Screen.Planner -> PlannerScreen(state)
        Screen.More -> MoreScreen(state)
        is Screen.GuideDetail -> GuideDetailScreen(state, s.id)
        is Screen.Tool -> ToolScreen(state, s.route)
        is Screen.TaskEdit -> TaskEditScreen(state, s.id, s.prefillTitle)
    }
}

@Composable
fun BottomBar(state: AppState) {
    val c = K.c
    val items = listOf(
        Triple("خانه", Icons.Filled.Home, 0),
        Triple("راهنما", Icons.Filled.MenuBook, 1),
        Triple("ابزارها", Icons.Filled.Build, 2),
        Triple("برنامه", Icons.Filled.CheckCircle, 3),
        Triple("بیشتر", Icons.Filled.MoreHoriz, 4)
    )
    NavigationBar(containerColor = c.surface, contentColor = c.text) {
        items.forEach { (label, icon, idx) ->
            NavigationBarItem(
                selected = state.stack.isEmpty() && state.tab == idx,
                onClick = { state.goTab(idx) },
                icon = { Icon(icon, label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.onPrimary, selectedTextColor = c.primary,
                    indicatorColor = c.primary, unselectedIconColor = c.muted, unselectedTextColor = c.muted
                )
            )
        }
    }
}
