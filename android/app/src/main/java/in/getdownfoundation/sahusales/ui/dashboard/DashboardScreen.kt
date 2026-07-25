package `in`.getdownfoundation.sahusales.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.core.ActivityItem
import `in`.getdownfoundation.sahusales.core.Event
import `in`.getdownfoundation.sahusales.core.ReminderFeedItem
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    val events by viewModel.events.collectAsState()
    val activity by viewModel.activity.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val isAdmin = currentUser?.role == "admin"

    val tabs = if (isAdmin)
        listOf("REMINDERS", "UPCOMING", "CANCELLED", "TEAM", "ACTIVITY")
    else
        listOf("REMINDERS", "UPCOMING", "CANCELLED", "ACTIVITY")

    // Refresh all data whenever tab changes
    LaunchedEffect(selectedTab) {
        viewModel.loadReminders()
        viewModel.loadEvents()
        viewModel.loadActivity()
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Dashboard",
            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary,
            modifier = Modifier.padding(16.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Surface,
            contentColor = Primary,
            edgePadding = 0.dp) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                    Text(
                        title, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
        }

        when (tabs[selectedTab]) {
            "REMINDERS" -> {
                val myReminders = reminders.filter { it.status in listOf("pending", "snoozed") }
                RemindersTab(myReminders)
            }
            "UPCOMING" -> {
                val upcoming = events.filter { it.status == "upcoming" }
                    .sortedBy { e ->
                        e.reminders
                            .filter { it.status == "pending" || it.status == "snoozed" }
                            .mapNotNull { r -> parseMillis(r.remindAt).takeIf { it > 0 } }
                            .minOrNull() ?: Long.MAX_VALUE
                    }
                UpcomingEventsTab(upcoming)
            }
            "CANCELLED" -> {
                val cancelled = events.filter { it.status == "cancelled" }
                CancelledEventsTab(cancelled)
            }
            "TEAM" -> {
                TeamRemindersTab(reminders)
            }
            "ACTIVITY" -> ActivityTab(activity)
        }
    }
}

// ── Reminders Tab ─────────────────────────────────────────────────────────────

@Composable
fun RemindersTab(reminders: List<ReminderFeedItem>) {
    if (reminders.isEmpty()) {
        EmptyState("🔔", "No reminders", "Set a reminder when creating an event to see it here")
        return
    }
    val now = System.currentTimeMillis()
    val overdue = reminders.filter { parseMillis(it.effectiveTime) < now }.sortedByDescending { parseMillis(it.effectiveTime) }
    val upcoming = reminders.filter { parseMillis(it.effectiveTime) >= now }.sortedBy { parseMillis(it.effectiveTime) }

    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (overdue.isNotEmpty()) {
            item {
                SectionHeader("OVERDUE", StatusOverdue)
            }
            items(overdue) { r -> ReminderCard(r) }
        }
        if (upcoming.isNotEmpty()) {
            item {
                SectionHeader(
                    "UPCOMING",
                    StatusUpcoming,
                    topPad = if (overdue.isNotEmpty()) 12.dp else 0.dp
                )
            }
            items(upcoming) { r -> ReminderCard(r) }
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
                Text(r.contactName ?: r.eventTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                r.contactOrganisation?.let { Text(it, fontSize = 12.sp, color = TextSecondary) }
                if (r.contactName != null) {
                    Text("📋 ${r.eventTitle}", fontSize = 13.sp, color = TextPrimary)
                }
                r.eventNotes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⏰ ${formatTime(r.effectiveTime)}",
                        fontSize = 12.sp,
                        color = if (isPast) StatusOverdue else StatusUpcoming,
                        fontWeight = if (isPast) FontWeight.Bold else FontWeight.Normal
                    )
                    if (r.status == "snoozed") {
                        Spacer(Modifier.width(6.dp))
                        StatusBadge("💤 SNOOZED", StatusSnoozed)
                    }
                    if (isPast && r.status == "pending") {
                        Spacer(Modifier.width(6.dp))
                        StatusBadge("OVERDUE", StatusOverdue)
                    }
                }
            }
        }
    }
}

// ── Upcoming Events Tab ────────────────────────────────────────────────────────

@Composable
fun UpcomingEventsTab(events: List<Event>) {
    if (events.isEmpty()) {
        EmptyState("📅", "No upcoming events", "Create an event to see it here")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(events) { e -> DashboardEventCard(e, Primary) }
    }
}

// ── Cancelled Events Tab ───────────────────────────────────────────────────────

@Composable
fun CancelledEventsTab(events: List<Event>) {
    if (events.isEmpty()) {
        EmptyState("✅", "No cancelled events", "Events you cancel will appear here")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(events) { e -> DashboardEventCard(e, StatusOverdue) }
    }
}

@Composable
fun DashboardEventCard(event: Event, accentColor: Color) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(event.tagColor ?: "#1565C0"))
    } catch (e: Exception) { Primary }

    val now = System.currentTimeMillis()
    val nextReminder = event.reminders
        .filter { it.status == "pending" || it.status == "snoozed" }
        .mapNotNull { r -> parseMillis(r.remindAt).takeIf { it > 0 } }
        .minOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier.width(5.dp).fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(Modifier.padding(12.dp).weight(1f)) {
                // Tag + status row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    event.tagName?.let {
                        Surface(color = tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                            Text(
                                it.uppercase(), fontSize = 10.sp, color = tagColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } ?: Spacer(Modifier.size(1.dp))

                    Surface(color = accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                        Text(
                            event.status.uppercase(), fontSize = 9.sp, color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                event.contactName?.let {
                    Text(it, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
                event.contactOrganisation?.let { Text(it, fontSize = 12.sp, color = TextSecondary) }
                event.assigneeName?.let {
                    Text("👤 $it", fontSize = 12.sp, color = Primary.copy(alpha = 0.8f))
                }
                event.notes?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                }

                // Next reminder
                if (nextReminder != null) {
                    Spacer(Modifier.height(6.dp))
                    val isOverdue = nextReminder < now
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "⏰ ${formatTime(event.reminders.first { it.status == "pending" || it.status == "snoozed" }.remindAt)}",
                            fontSize = 12.sp,
                            color = if (isOverdue) StatusOverdue else StatusUpcoming,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isOverdue) {
                            Spacer(Modifier.width(6.dp))
                            StatusBadge("OVERDUE", StatusOverdue)
                        }
                    }
                } else if (event.status == "upcoming") {
                    Spacer(Modifier.height(4.dp))
                    Text("No reminder set", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

// ── Team Tab ───────────────────────────────────────────────────────────────────

@Composable
fun TeamRemindersTab(reminders: List<ReminderFeedItem>) {
    if (reminders.isEmpty()) {
        EmptyState("👥", "No team reminders", "")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(reminders.sortedBy { parseMillis(it.effectiveTime) }) { r -> ReminderCard(r) }
    }
}

// ── Activity Tab ───────────────────────────────────────────────────────────────

@Composable
fun ActivityTab(items: List<ActivityItem>) {
    if (items.isEmpty()) {
        EmptyState("📊", "No activity yet", "Activity from your events will appear here")
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
                    fontWeight = FontWeight.Medium, fontSize = 14.sp
                )
                Text(
                    "${item.actorName ?: "System"} · ${formatTime(item.createdAt)}",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(label: String, color: Color, topPad: Dp = 0.dp) {
    Text(
        label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier.padding(top = topPad, bottom = 4.dp)
    )
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
        Text(
            text, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

// ── Date/time helpers (used by TeamScreen + EventsScreen imports) ──────────────

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
