package ir.techima.kardan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

class ToolInfo(val route: AppRoute, val title: String, val desc: String, val icon: ImageVector, val group: String, private val k: Int = 0) {
    fun tint(c: KColors): Color = listOf(c.primary, c.accent, c.warn, c.ok, c.danger)[k % 5]
}

object Tools {
    const val G_DAILY = "کارهای روزمره"
    const val G_PHONE = "گوشی و امنیت"
    const val G_DATA = "داده و ارتباط"

    val all = listOf(
        ToolInfo(AppRoute.CALC, "ماشین‌حساب", "محاسبهٔ سریع با ارقام فارسی", Icons.Filled.Calculate, G_DAILY, 0),
        ToolInfo(AppRoute.CONVERTER, "تبدیل واحد", "طول، وزن، حجم، داده، دما و…", Icons.Filled.SwapHoriz, G_DAILY, 1),
        ToolInfo(AppRoute.TIMER, "تایمر", "حتی با بستن برنامه اعلان می‌دهد", Icons.Filled.Timer, G_DAILY, 2),
        ToolInfo(AppRoute.STOPWATCH, "کرنومتر", "زمان‌سنج با ثبت دور", Icons.Filled.Timelapse, G_DAILY, 3),
        ToolInfo(AppRoute.WORLD, "ساعت جهانی", "اختلاف ساعت شهرها با تهران", Icons.Filled.Public, G_DAILY, 1),
        ToolInfo(AppRoute.DATECONV, "تبدیل تاریخ", "شمسی ⇄ میلادی و فاصلهٔ تاریخ‌ها", Icons.Filled.CalendarMonth, G_DAILY, 0),
        ToolInfo(AppRoute.TEXTTOOLS, "ابزار متن", "شمارش، تبدیل ارقام، پاکسازی متن", Icons.Filled.TextFields, G_DAILY, 2),
        ToolInfo(AppRoute.PACKAGE, "مقایسهٔ بستهٔ اینترنت", "هزینهٔ هر گیگ و هر روز را حساب کن", Icons.Filled.DataUsage, G_DATA, 3),
        ToolInfo(AppRoute.TORCH, "چراغ‌قوه", "روشن و خاموش با یک لمس", Icons.Filled.Lightbulb, G_PHONE, 2),
        ToolInfo(AppRoute.PASSGEN, "رمزساز", "رمز تصادفی و قوی، آفلاین", Icons.Filled.Key, G_PHONE, 4),
        ToolInfo(AppRoute.VAULT, "گاوصندوق رمزها", "رمزهایت را رمزگذاری‌شده نگه دار", Icons.Filled.Lock, G_PHONE, 4),
        ToolInfo(AppRoute.DEVICE, "اطلاعات دستگاه", "حافظه، باتری، شبکه و نسخه", Icons.Filled.PhoneAndroid, G_PHONE, 1),
        ToolInfo(AppRoute.MOVIES, "فهرست تماشا", "فیلم و سریال‌هایی که می‌خواهی ببینی", Icons.Filled.Movie, G_DATA, 2),
        ToolInfo(AppRoute.CONTACTS, "تماس سریع", "شماره‌های مهم، با یک لمس", Icons.Filled.Contacts, G_DATA, 3),
        ToolInfo(AppRoute.BACKUP, "پشتیبان‌گیری", "خروجی و بازیابی اطلاعات برنامه", Icons.Filled.Backup, G_DATA, 0),
        ToolInfo(AppRoute.SETTINGS, "تنظیمات", "تم، حالت سالمندان، اندازهٔ نوشته", Icons.Filled.Settings, G_DATA, 1)
    )
    fun info(r: AppRoute): ToolInfo? = all.firstOrNull { it.route == r }
}

@Composable
fun ToolsScreen(state: AppState) {
    val c = K.c
    Page("ابزارها", "همه‌چیز آفلاین و روی خود گوشی") {
        listOf(Tools.G_DAILY, Tools.G_PHONE, Tools.G_DATA).forEach { g ->
            SectionTitle(g)
            Tools.all.filter { it.group == g }.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { t ->
                        Box(Modifier.weight(1f)) {
                            ToolTile(t, Modifier.fillMaxWidth()) { state.openRoute(t.route) }
                            val key = "tool:" + t.route.name
                            val fav = state.favorites.contains(key)
                            IconButton(onClick = { state.toggleFavorite(key) }, modifier = Modifier.align(Alignment.TopStart)) {
                                Icon(if (fav) Icons.Filled.Star else Icons.Filled.StarBorder, if (fav) "حذف از محبوب‌ها" else "افزودن به محبوب‌ها", tint = if (fav) c.warn else c.muted)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ToolScreen(state: AppState, route: AppRoute) {
    val info = Tools.info(route)
    val title = info?.title ?: "ابزار"
    val back = { state.back(); Unit }
    when (route) {
        AppRoute.CALC -> CalcTool(title, back)
        AppRoute.CONVERTER -> ConverterTool(title, back)
        AppRoute.TIMER -> TimerTool(state, title, back)
        AppRoute.STOPWATCH -> StopwatchTool(title, back)
        AppRoute.WORLD -> WorldTool(state, title, back)
        AppRoute.PASSGEN -> PassGenTool(title, back)
        AppRoute.DATECONV -> DateConvTool(title, back)
        AppRoute.TEXTTOOLS -> TextTool(title, back)
        AppRoute.PACKAGE -> PackageTool(title, back)
        AppRoute.TORCH -> TorchTool(state, title, back)
        AppRoute.VAULT -> VaultScreen(state, title, back)
        AppRoute.DEVICE -> DeviceScreen(state, title, back)
        AppRoute.SETTINGS -> SettingsScreen(state, title, back)
        AppRoute.MOVIES -> MoviesScreen(state, title, back)
        AppRoute.BACKUP -> BackupScreen(state, title, back)
        AppRoute.CONTACTS -> ContactsScreen(state, title, back)
        AppRoute.PLANNER -> { state.goTab(3) }
    }
}
