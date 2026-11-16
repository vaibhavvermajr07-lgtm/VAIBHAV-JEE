package com.vaibhav.jee

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Navy = Color(0xFF101B2D)
private val Panel = Color(0xFF1A2940)
private val Blue = Color(0xFF65A9FF)
private val Gold = Color(0xFFE4BD72)
private val Muted = Color(0xFF9AAAC0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderScheduler.createChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val prefs = remember { getSharedPreferences("vaibhav_data", MODE_PRIVATE) }
            VaibhavApp(
                load = { key -> prefs.getString(key, "[]") ?: "[]" },
                save = { key, value -> prefs.edit().putString(key, value).apply() }
            )
        }
    }
}

data class Entry(val title: String, val detail: String, val tag: String = "Pending") {
    fun json() = JSONObject().put("title", title).put("detail", detail).put("tag", tag)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaibhavApp(load: (String) -> String, save: (String, String) -> Unit) {
    val context = LocalContext.current
    val tabs = listOf("Home", "Schedule", "Homework", "Backlogs", "Syllabus", "Planner", "Tests", "Analytics")
    val icons = listOf(Icons.Default.Home, Icons.Default.CalendarMonth, Icons.Default.MenuBook,
        Icons.Default.Flag, Icons.Default.AutoStories, Icons.Default.Today,
        Icons.Default.Quiz, Icons.Default.Insights)
    var selected by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var addKind by remember { mutableStateOf("schedule") }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Physics") }

    fun readEntries(key: String): List<Entry> {
        val arr = JSONArray(load(key))
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { Entry(it.optString("title"), it.optString("detail"), it.optString("tag", "Pending")) }
        }
    }
    fun addEntry(key: String, entry: Entry) {
        val arr = JSONArray(load(key))
        arr.put(entry.json())
        save(key, arr.toString())
    }
    val schedule = remember { mutableStateListOf<Entry>().apply { addAll(readEntries("schedule")) } }
    val homework = remember { mutableStateListOf<Entry>().apply { addAll(readEntries("homework")) } }
    val backlogs = remember { mutableStateListOf<Entry>().apply { addAll(readEntries("backlogs")) } }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Blue, secondary = Gold, background = Navy, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(
            containerColor = Navy,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("VAIBHAV", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                            Text("JEE PREPARATION COMMAND CENTRE", fontSize = 9.sp, color = Muted, letterSpacing = 1.sp)
                        }
                    },
                    actions = { Text("IIT BOMBAY  ", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    val visible = listOf(0,1,2,3,5)
                    visible.forEach { index ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { selected = index },
                            icon = { Icon(icons[index], contentDescription = tabs[index]) },
                            label = { Text(tabs[index], fontSize = 9.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, indicatorColor = Navy)
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selected in listOf(1,2,3)) {
                    FloatingActionButton(onClick = {
                        addKind = when(selected) { 1 -> "schedule"; 2 -> "homework"; else -> "backlogs" }
                        title = ""; detail = ""; showAdd = true
                    }, containerColor = Blue, contentColor = Navy) { Icon(Icons.Default.Add, "Add") }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                when(selected) {
                    0 -> HomeScreen(schedule.size, homework.size, backlogs.size, onNavigate = { selected = it })
                    1 -> EntryScreen("Mission Jeet Schedule", "Add classes manually. PDF/CSV import is planned for the next build.", schedule)
                    2 -> EntryScreen("Homework & DPP", "Track assignments, deadlines and completion.", homework, true) { item ->
                        val idx = homework.indexOf(item)
                        if (idx >= 0) {
                            homework[idx] = item.copy(tag = if(item.tag == "Completed") "Pending" else "Completed")
                            save("homework", JSONArray().apply { homework.forEach { put(it.json()) } }.toString())
                        }
                    }
                    3 -> EntryScreen("Backlog Recovery", "Record pending lectures, chapters, DPPs and revisions.", backlogs)
                    4 -> SyllabusScreen()
                    5 -> PlannerScreen()
                    6 -> TestsScreen()
                    7 -> AnalyticsScreen(schedule.size, homework.count { it.tag == "Completed" }, backlogs.size)
                }
            }
        }

        if(showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                title = { Text(when(addKind) {"schedule" -> "Add Mission Jeet class"; "homework" -> "Add homework"; else -> "Add backlog"}) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(title, { title = it }, label = { Text("Title / chapter") }, singleLine = true)
                        OutlinedTextField(
                            detail, { detail = it },
                            label = { Text("Date/time + notes") },
                            placeholder = { Text("2026-11-16 16:00 • Chapter 3") },
                            supportingText = { Text("For reminders, begin with local date/time: yyyy-MM-dd HH:mm") }
                        )
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                            OutlinedTextField(subject, {}, readOnly = true, label = { Text("Subject") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor())
                            ExposedDropdownMenu(expanded, { expanded = false }) {
                                listOf("Physics","Chemistry","Mathematics","Other").forEach {
                                    DropdownMenuItem(text = { Text(it) }, onClick = { subject = it; expanded = false })
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if(title.isNotBlank()) {
                            val entry = Entry(title, "$subject • $detail")
                            when(addKind) {
                                "schedule" -> { schedule.add(entry); addEntry("schedule", entry) }
                                "homework" -> { homework.add(entry); addEntry("homework", entry) }
                                else -> { backlogs.add(entry); addEntry("backlogs", entry) }
                            }
                            ReminderScheduler.schedule(context, addKind, title, detail)
                        }
                        showAdd = false
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
                containerColor = Panel
            )
        }
    }
}

@Composable
fun HomeScreen(classes: Int, homework: Int, backlogs: Int, onNavigate: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("YOUR MISSION, YOUR PLAN.", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("Good to see you.", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Build consistency. One focused session at a time.", color = Muted, fontSize = 13.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("CLASSES", classes.toString(), Icons.Default.VideoCall, Modifier.weight(1f))
                MetricCard("HOMEWORK", homework.toString(), Icons.Default.Assignment, Modifier.weight(1f))
                MetricCard("BACKLOGS", backlogs.toString(), Icons.Default.Flag, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Today's command centre") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("NEXT BEST ACTION", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Add your class schedule and pending work", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("VAIBHAV can build a realistic daily plan once you enter your classes, available hours and deadlines.", color = Muted, fontSize = 12.sp)
                    Button(onClick = { onNavigate(1) }) { Text("Set up my schedule") }
                }
            }
        }
        item { SectionTitle("Quick access") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickRow("Homework & DPP", "Track due dates and completion", Icons.Default.MenuBook) { onNavigate(2) }
                QuickRow("Backlog recovery", "Organise pending lectures and chapters", Icons.Default.Flag) { onNavigate(3) }
                QuickRow("Daily planner", "Plan focused study blocks and breaks", Icons.Default.Today) { onNavigate(5) }
                QuickRow("Tests & analytics", "Review your practice performance", Icons.Default.Insights) { onNavigate(6) }
            }
        }
        item { Text("Starter build • Local data on this device", color = Muted, fontSize = 10.sp) }
    }
}

@Composable
fun EntryScreen(title: String, subtitle: String, entries: List<Entry>, canComplete: Boolean = false, onToggle: (Entry) -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        if(entries.isEmpty()) {
            EmptyState("Nothing added yet", "Tap + to add your first item.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(entries) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().then(if(canComplete) Modifier.clickable { onToggle(item) } else Modifier)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                Text(item.detail, color = Muted, fontSize = 12.sp)
                            }
                            if(canComplete) {
                                AssistChip(onClick = { onToggle(item) }, label = { Text(item.tag) })
                            } else Text(item.tag, color = Gold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyllabusScreen() {
    val subjects = listOf("Physics" to listOf("Units & Dimensions","Kinematics","Laws of Motion","Work, Energy & Power","Rotational Motion"),
        "Chemistry" to listOf("Mole Concept","Atomic Structure","Chemical Bonding","Thermodynamics","Equilibrium"),
        "Mathematics" to listOf("Sets & Relations","Quadratic Equations","Sequence & Series","Trigonometry","Limits"))
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Syllabus tracker", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Starter chapter list • Expand with your official exam syllabus.", color = Muted, fontSize = 12.sp) }
        items(subjects) { (subject, chapters) ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(14.dp)) {
                    Text(subject, color = Gold, fontWeight = FontWeight.Bold)
                    chapters.forEach { chapter ->
                        var checked by remember { mutableStateOf(false) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked, { checked = it })
                            Text(chapter, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Daily study planner", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Build a balanced day around MJ classes, homework and revision.", color = Muted, fontSize = 12.sp)
        listOf("Morning • Formula revision", "Class block • MJ lecture", "After class • DPP / homework", "Afternoon • Backlog recovery", "Evening • JEE question practice", "Night • Error log + next-day plan").forEachIndexed { index, item ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", color = Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text(item, fontWeight = FontWeight.Medium)
                }
            }
        }
        Text("Add your actual class times and study availability in Schedule to personalise this routine.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun TestsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("JEE Test Centre", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Test engine setup screen", color = Muted, fontSize = 12.sp)
        listOf("JEE Main mock test","JEE Advanced Paper 1","JEE Advanced Paper 2","Chapter-wise practice","Previous-year questions").forEach {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Quiz, null, tint = Blue)
                    Spacer(Modifier.width(12.dp))
                    Text(it, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text("Setup needed", color = Gold, fontSize = 10.sp)
                }
            }
        }
        Text("Add licensed/verified question banks and marking schemes to activate exam simulations.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun AnalyticsScreen(classes: Int, completedHomework: Int, backlogs: Int) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Preparation analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Your recorded activity on this device", color = Muted, fontSize = 12.sp)
        MetricCard("CLASSES ADDED", classes.toString(), Icons.Default.CalendarMonth, Modifier.fillMaxWidth())
        MetricCard("HOMEWORK COMPLETED", completedHomework.toString(), Icons.Default.CheckCircle, Modifier.fillMaxWidth())
        MetricCard("OPEN BACKLOG ITEMS", backlogs.toString(), Icons.Default.Flag, Modifier.fillMaxWidth())
        Text("Detailed trends, test scores and preparation health score will activate as you record study sessions and tests.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, tint = Blue)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 9.sp, color = Muted)
        }
    }
}
@Composable fun SectionTitle(text: String) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
@Composable fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Inbox, null, tint = Muted, modifier = Modifier.size(38.dp))
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}
@Composable fun QuickRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { click() }, colors = CardDefaults.cardColors(containerColor = Panel)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Blue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}
