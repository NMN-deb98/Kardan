package ir.techima.kardan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection

private val Ink = Color(0xFF101827)
private val Panel = Color(0xFF1B2638)
private val Mint = Color(0xFF55E0BC)
private val Muted = Color(0xFF9EADC2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KardanTheme { KardanApp() }
            }
        }
    }
}

@Composable
private fun KardanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint,
            secondary = Color(0xFF8DAAFF),
            background = Ink,
            surface = Panel,
            onPrimary = Ink,
            onBackground = Color(0xFFF3F6FB),
            onSurface = Color(0xFFF3F6FB)
        ),
        content = content
    )
}

private data class Guide(val title: String, val category: String, val description: String)
private data class TaskItem(val id: Int, val title: String, val done: Boolean)

@Composable
private fun KardanApp() {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedGuide by remember { mutableStateOf<Guide?>(null) }
    var taskText by remember { mutableStateOf("") }
    var nextId by remember { mutableIntStateOf(1) }
    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }

    val guides = listOf(
        Guide("امنیت حساب‌های آنلاین", "امنیت", "برای هر حساب رمز عبور یکتا بسازید، تأیید دومرحله‌ای را فعال کنید و کدهای بازیابی را در محل امن نگه دارید."),
        Guide("آزاد کردن فضای گوشی", "موبایل", "ابتدا حجم پوشه دانلودها و فایل‌های رسانه‌ای را بررسی کنید. پیش از حذف، از فایل‌های مهم نسخه پشتیبان بگیرید."),
        Guide("تشخیص پیامک مشکوک", "امنیت", "روی لینک‌های ناشناس نزنید، اطلاعات بانکی یا رمز یک‌بارمصرف را ارسال نکنید و آدرس فرستنده را جداگانه بررسی کنید."),
        Guide("تهیه نسخه پشتیبان", "بهره‌وری", "فایل‌های مهم را در دو محل جدا نگه دارید و هر چند وقت یک‌بار سالم بودن نسخه پشتیبان را آزمایش کنید."),
        Guide("مدیریت هزینه‌های روزانه", "مالی", "هزینه‌ها را همان روز ثبت کنید و در پایان هفته مجموع هر دسته را بررسی کنید تا الگوهای پرهزینه مشخص شوند.")
    )

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Color.White) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("خانه") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.MenuBook, null) }, label = { Text("راهنماها") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Calculate, null) }, label = { Text("ابزارها") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("کارهای من") })
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    taskCount = tasks.count { !it.done },
                    onGuides = { tab = 1 },
                    onTools = { tab = 2 },
                    onTasks = { tab = 3 }
                )
                1 -> GuidesScreen(guides, query, { query = it }, { selectedGuide = it })
                2 -> ToolsScreen()
                else -> TasksScreen(
                    tasks = tasks,
                    text = taskText,
                    onText = { taskText = it },
                    onAdd = {
                        if (taskText.isNotBlank()) {
                            tasks = tasks + TaskItem(nextId, taskText.trim(), false)
                            nextId += 1
                            taskText = ""
                        }
                    },
                    onToggle = { id ->
                        tasks = tasks.map { if (it.id == id) it.copy(done = !it.done) else it }
                    },
                    onDelete = { id -> tasks = tasks.filterNot { it.id == id } }
                )
            }
            selectedGuide?.let { guide ->
                AlertDialog(
                    onDismissRequest = { selectedGuide = null },
                    title = { Text(guide.title, textAlign = TextAlign.Right) },
                    text = { Text(guide.description, textAlign = TextAlign.Right) },
                    confirmButton = { TextButton(onClick = { selectedGuide = null }) { Text("متوجه شدم") } },
                    containerColor = Panel
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(taskCount: Int, onGuides: () -> Unit, onTools: () -> Unit, onTasks: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).background(Mint, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bolt, null, tint = Ink, modifier = Modifier.size(30.dp))
                }
                Column {
                    Text("کاردان", fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("کارهایت را هوشمندانه انجام بده", color = Muted, fontSize = 13.sp)
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().background(
                    androidx.compose.ui brush = null
                )
            ) {}
        }
        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(Panel, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("یک قدم ساده، یک روز بهتر", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("راهنماهای کاربردی و ابزارهای روزمره، همه در یک جا.", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onGuides, label = { Text("مطالعه راهنماها") }, leadingIcon = { Icon(Icons.Default.MenuBook, null) })
                    AssistChip(onClick = onTools, label = { Text("ابزارها") }, leadingIcon = { Icon(Icons.Default.Build, null) })
                }
            }
        }
        item { Text("دسترسی سریع", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item { QuickTile("راهنماهای دیجیتال", "پاسخ‌های کوتاه برای کارهای روزمره", Icons.Default.MenuBook, onGuides) }
        item { QuickTile("ماشین‌حساب", "محاسبات ساده بدون اینترنت", Icons.Default.Calculate, onTools) }
        item { QuickTile("کارهای من", "$taskCount کار در انتظار انجام", Icons.Default.CheckCircle, onTasks) }
        item {
            Text(
                "نسخهٔ اولیه · اطلاعات کارها فعلاً فقط تا زمان بسته‌شدن برنامه نگه‌داری می‌شود.",
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(46.dp).background(Color(0xFF26384B), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronLeft, null, tint = Muted)
    }
}

@Composable
private fun GuidesScreen(guides: List<Guide>, query: String, onQuery: (String) -> Unit, onSelect: (Guide) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("راهنماهای کاربردی", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("جست‌وجو در راهنماها") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(guides.filter { it.title.contains(query, true) || it.category.contains(query, true) }) { guide ->
                Row(
                    Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).clickable { onSelect(guide) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, null, tint = Mint)
                    Column(Modifier.weight(1f)) {
                        Text(guide.title, fontWeight = FontWeight.SemiBold)
                        Text(guide.category, color = Muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun ToolsScreen() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("نتیجه نمایش داده می‌شود") }
    val keys = listOf("7", "8", "9", "÷", "4", "5", "6", "×", "1", "2", "3", "−", "C", "0", ".", "+", "=")
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ابزارها", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(18.dp)) {
            Text(if (expression.isBlank()) "محاسبه سریع" else expression, fontSize = 25.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(result, color = Mint, fontSize = 15.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
        val rows = keys.chunked(4)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            when (key) {
                                "C" -> { expression = ""; result = "پاک شد" }
                                "=" -> {
                                    val value = safeCalculate(expression)
                                    result = value ?: "عبارت معتبر نیست"
                                    if (value != null) expression = value
                                }
                                else -> expression += when (key) { "×" -> "*"; "÷" -> "/"; "−" -> "-"; else -> key }
                            }
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (key in listOf("+", "−", "×", "÷", "=")) Mint else Panel, contentColor = if (key in listOf("+", "−", "×", "÷", "=")) Ink else Color.White),
                        contentPadding = PaddingValues(2.dp)
                    ) { Text(key, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text("محاسبه در دستگاه انجام می‌شود و نیازی به اینترنت ندارد.", color = Muted, fontSize = 12.sp)
    }
}

private fun safeCalculate(input: String): String? {
    val cleaned = input.replace(" ", "")
    if (cleaned.isEmpty() || !cleaned.matches(Regex("[0-9.+*/()\\-]+"))) return null
    return try {
        val parser = ArithmeticParser(cleaned)
        val value = parser.parse()
        if (!value.isFinite()) null else if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    } catch (_: Exception) { null }
}

private class ArithmeticParser(private val text: String) {
    private var pos = 0
    fun parse(): Double {
        val value = expression()
        if (pos != text.length) throw IllegalArgumentException()
        return value
    }
    private fun expression(): Double {
        var value = term()
        while (pos < text.length && (text[pos] == '+' || text[pos] == '-')) {
            val op = text[pos++]
            val right = term()
            value = if (op == '+') value + right else value - right
        }
        return value
    }
    private fun term(): Double {
        var value = factor()
        while (pos < text.length && (text[pos] == '*' || text[pos] == '/')) {
            val op = text[pos++]
            val right = factor()
            if (op == '/' && right == 0.0) throw ArithmeticException()
            value = if (op == '*') value * right else value / right
        }
        return value
    }
    private fun factor(): Double {
        if (pos < text.length && text[pos] == '-') { pos++; return -factor() }
        if (pos < text.length && text[pos] == '(') {
            pos++
            val value = expression()
            if (pos >= text.length || text[pos++] != ')') throw IllegalArgumentException()
            return value
        }
        val start = pos
        while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) pos++
        if (start == pos) throw IllegalArgumentException()
        return text.substring(start, pos).toDouble()
    }
}

@Composable
private fun TasksScreen(
    tasks: List<TaskItem>,
    text: String,
    onText: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("کارهای من", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = text, onValueChange = onText, modifier = Modifier.weight(1f), placeholder = { Text("کار جدید را بنویس") }, singleLine = true)
            Button(onClick = onAdd, enabled = text.isNotBlank(), modifier = Modifier.height(54.dp)) { Icon(Icons.Default.Add, "افزودن") }
        }
        if (tasks.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.TaskAlt, null, tint = Mint, modifier = Modifier.size(48.dp))
                Text("هنوز کاری اضافه نکرده‌ای", fontWeight = FontWeight.Bold)
                Text("اولین کار کوچک خودت را ثبت کن.", color = Muted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { task ->
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.done, onCheckedChange = { onToggle(task.id) })
                        Text(task.title, modifier = Modifier.weight(1f), color = if (task.done) Muted else Color.White)
                        IconButton(onClick = { onDelete(task.id) }) { Icon(Icons.Default.DeleteOutline, "حذف", tint = Muted) }
                    }
                }
            }
        }
    }
}
