package `in`.getdownfoundation.sahusales.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    val events by viewModel.events.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val isAdmin = currentUser?.role == "admin"
    val tabs = if (isAdmin) listOf("MY", "TEAM", "ACTIVITY") else listOf("MY", "ACTIVITY")

    // Refresh reminders & events whenever tab changes
    LaunchedEffect(selectedTab) {
        viewModel.loadReminders()
        viewModel.loadEvents()
        viewModel.loadActivity()
    }

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
            "MY" -> {
                val myId = currentUser?.id
                val myReminders = reminders.filter { r ->
                    // Show all pending/snoozed reminders for current user's events
                    r.status in listOf("pending", "snoozed")
                }
                MyRemindersTab(myReminders)
            }
            "TEAM" -> {
                // Admin sees all reminders grouped by event/contact
                TeamRemindersTab(reminders)
            }
            "ACTIVITY" -> ActivityTab(activity)
        }
    }
}

@Composable
fun MyRemindersTab(reminders: List<ReminderFeedItem>) {
    if (reminders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔔", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("No upcoming reminders", color = TextSecondary, fontSize = 15.sp)
                Text("Create an event with a reminder to see it here", color = TextSecondary, fontSize = 12.sp)
            }
        }
        return
    }

    val now = System.currentTimeMillis()
    val overdue = reminders.filter { parseMillis(it.effectiveTime) < now }
    val upcoming = reminders.filter { parseMillis(it.effectiveTime) >= now }

    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (overdue.isNotEmpty()) {
            item {
                Text("OVERDUE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusOverdue,
                    modifier = Modifier.padding(vertical = 4.dp))
            }
            items(overdue) { r -> ReminderCard(r) }
        }
        if (upcoming.isNotEmpty()) {
            item {
                Text("UPCOMING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusUpcoming,
                    modifier = Modifier.padding(top = if (overdue.isNotEmpty()) 8.dp else 4.dp, bottom = 4.dp))
            }
            items(upcoming) { r -> ReminderCard(r) }
        }
    }
}

@Composable
fun TeamRemindersTab(reminders: List<ReminderFeedItem>) {
    if (reminders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No team reminders", color = TextSecondary)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(reminders.sortedBy { parseMillis(it.effectiveTime) }) { r ->
            ReminderCard(r)
        }
    }
}

@Composable
fun ReminderCard(r: ReminderFeedItem) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(r.tagColor ?: "#1565C0"))
    } catch (e: Exception) { Primary }

    val effectiveMs = parseMillis(r.effectiveTime)
    val isPast = effectiveMs < System.currentTimeMillis()
    val borderColor = when {
        isPast -> StatusOverdue
        r.status == "snoozed" -> StatusSnoozed
        else -> StatusUpcoming
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier.width(5.dp).fillMaxHeight()
                    .background(borderColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(Modifier.padding(12.dp).weight(1f)) {
                // Tag badge
                r.tagName?.let { tag ->
                    Surface(color = tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                        Text(
                            tag.uppercase(), fontSize = 10.sp, color = tagColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Contact name (primary)
                Text(
                    r.contactName ?: r.eventTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                r.contactOrganisation?.let {
                    Text(it, fontSize = 13.sp, color = TextSecondary)
                }

                // Event title
                if (r.contactName != null) {
                    Text("📋 ${r.eventTitle}", fontSize = 13.sp, color = TextPrimary)
                }

                r.eventNotes?.let {
                    Text(it, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                }

                Spacer(Modifier.height(4.dp))

                // Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⏰ ${formatTime(r.effectiveTime)}",
                        fontSize = 12.sp,
                        color = if (isPast) StatusOverdue else TextSecondary,
                        fontWeight = if (isPast) FontWeight.Medium else FontWeight.Normal
                    )
                    if (r.status == "snoozed") {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = StatusSnoozed.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                            Text("💤 SNOOZED", fontSize = 9.sp, color = StatusSnoozed,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (isPast && r.status == "pending") {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = StatusOverdue.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                            Text("OVERDUE", fontSize = 9.sp, color = StatusOverdue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityTab(items: List<ActivityItem>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No activity yet", color = TextSecondary)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item -> ActivityRow(item) }
    }
}

@Composable
fun ActivityRow(item: ActivityItem) {
    val icon = when (item.action) {
        "triggered" -> "🔔"
        "snoozed" -> "💤"
        "ended" -> "✅"
        "delegated" -> "👤"
        "created" -> "➕"
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
                Text(
                    "${item.eventTitle ?: ""} — ${item.contactName ?: ""}",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${item.actorName ?: "System"} · ${formatTime(item.createdAt)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

fun parseMillis(iso: String): Long {
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = if (fmt.endsWith("'Z'")) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
                val d = sdf.parse(iso)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        0L
    } catch (_: Exception) { 0L }
}

fun formatTime(iso: String): String {
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
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
