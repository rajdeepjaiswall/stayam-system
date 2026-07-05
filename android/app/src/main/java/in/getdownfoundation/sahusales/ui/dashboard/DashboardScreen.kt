package `in`.getdownfoundation.sahusales.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.core.ActivityItem
import `in`.getdownfoundation.sahusales.core.ReminderFeedItem
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    val activity by viewModel.activity.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = if (currentUser?.role == "admin") listOf("MY", "TEAM", "ACTIVITY") else listOf("MY", "ACTIVITY")

    Column(Modifier.fillMaxSize()) {
        Text(
            "Dashboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier.padding(16.dp)
        )
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = Primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Primary
                )
            }
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                    Text(title, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }

        when (tabs[selectedTab]) {
            "MY" -> MyRemindersTab(reminders.filter { it.status in listOf("pending","snoozed") })
            "TEAM" -> MyRemindersTab(reminders)
            "ACTIVITY" -> ActivityTab(activity)
        }
    }
}

@Composable
fun MyRemindersTab(reminders: List<ReminderFeedItem>) {
    if (reminders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No upcoming reminders", color = TextSecondary)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(reminders) { r -> ReminderCard(r) }
    }
}

@Composable
fun ReminderCard(r: ReminderFeedItem) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(r.tagColor ?: "#1565C0"))
    } catch (e: Exception) { Primary }

    val statusColor = when (r.status) {
        "snoozed" -> StatusSnoozed
        else -> StatusUpcoming
    }

    val effectiveMs = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).also { it.timeZone = TimeZone.getTimeZone("UTC") }
            .parse(r.effectiveTime)?.time ?: 0L
    } catch (e: Exception) { 0L }
    val isPast = effectiveMs < System.currentTimeMillis()
    val borderColor = if (isPast) StatusOverdue else statusColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(borderColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))
            Column(Modifier.padding(12.dp).weight(1f)) {
                Row {
                    r.tagName?.let {
                        Surface(color = tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                            Text(it.uppercase(), fontSize = 10.sp, color = tagColor, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(r.contactName ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                r.contactOrganisation?.let { Text(it, fontSize = 13.sp, color = TextSecondary) }
                Text(r.eventTitle, fontSize = 14.sp, color = TextPrimary)
                Text(
                    formatTime(r.effectiveTime),
                    fontSize = 12.sp,
                    color = if (isPast) StatusOverdue else TextSecondary
                )
            }
        }
    }
}

@Composable
fun ActivityTab(items: List<ActivityItem>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No activity yet", color = TextSecondary)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            ActivityRow(item)
        }
    }
}

@Composable
fun ActivityRow(item: ActivityItem) {
    val icon = when (item.action) {
        "triggered" -> "🔔"
        "snoozed" -> "💤"
        "ended" -> "✅"
        "delegated" -> "👤"
        else -> "•"
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(Modifier.padding(12.dp)) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("${item.eventTitle ?: ""} — ${item.contactName ?: ""}", fontWeight = FontWeight.Medium)
                Text("${item.actorName ?: "System"} · ${formatTime(item.createdAt)}", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

fun formatTime(iso: String): String {
    return try {
        val formats = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        var date: Date? = null
        for (fmt in formats) {
            try {
                date = SimpleDateFormat(fmt, Locale.US).also {
                    it.timeZone = if (fmt.endsWith("'Z'")) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
                }.parse(iso)
                if (date != null) break
            } catch (_: Exception) {}
        }
        date?.let { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(it) } ?: iso
    } catch (e: Exception) { iso }
}
