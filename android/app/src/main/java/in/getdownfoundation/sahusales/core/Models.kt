package `in`.getdownfoundation.sahusales.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("organisation_name") val organisationName: String? = null,
    val mobile: String? = null,
    val role: String = "member",
    val permissions: Map<String, Boolean> = emptyMap(),
    @SerialName("is_disabled") val isDisabled: Boolean = false,
    val vibrate: Boolean = true
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val organisation: String? = null,
    val mobile: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class EventTag(
    val id: String,
    val name: String,
    val color: String
)

@Serializable
data class Reminder(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("remind_at") val remindAt: String,
    val status: String = "pending",
    @SerialName("snoozed_until") val snoozedUntil: String? = null
)

@Serializable
data class Event(
    val id: String,
    @SerialName("contact_id") val contactId: String? = null,
    @SerialName("tag_id") val tagId: String? = null,
    val title: String,
    val notes: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    val status: String = "upcoming",
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("contact_organisation") val contactOrganisation: String? = null,
    @SerialName("contact_mobile") val contactMobile: String? = null,
    @SerialName("contact_whatsapp") val contactWhatsapp: String? = null,
    @SerialName("tag_name") val tagName: String? = null,
    @SerialName("tag_color") val tagColor: String? = null,
    @SerialName("assignee_name") val assigneeName: String? = null,
    val reminders: List<Reminder> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ReminderFeedItem(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("remind_at") val remindAt: String,
    val status: String,
    @SerialName("snoozed_until") val snoozedUntil: String? = null,
    @SerialName("effective_time") val effectiveTime: String,
    @SerialName("event_title") val eventTitle: String,
    @SerialName("event_notes") val eventNotes: String? = null,
    @SerialName("event_status") val eventStatus: String? = null,
    @SerialName("tag_name") val tagName: String? = null,
    @SerialName("tag_color") val tagColor: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("contact_organisation") val contactOrganisation: String? = null,
    @SerialName("contact_mobile") val contactMobile: String? = null,
    @SerialName("contact_whatsapp") val contactWhatsapp: String? = null
)

@Serializable
data class ActivityItem(
    val id: String,
    @SerialName("reminder_id") val reminderId: String? = null,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("actor_id") val actorId: String? = null,
    val action: String,
    val detail: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("event_title") val eventTitle: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("actor_name") val actorName: String? = null,
    @SerialName("tag_name") val tagName: String? = null,
    @SerialName("tag_color") val tagColor: String? = null
)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double? = null,
    @SerialName("gst_percent") val gstPercent: Double? = null
)

@Serializable
data class InvoiceItem(
    val name: String,
    val qty: Double,
    val rate: Double,
    @SerialName("gst_percent") val gstPercent: Double = 0.0,
    @SerialName("line_total") val lineTotal: Double? = null,
    @SerialName("line_tax") val lineTax: Double? = null
)

@Serializable
data class Invoice(
    val id: String,
    @SerialName("invoice_number") val invoiceNumber: String,
    @SerialName("contact_id") val contactId: String? = null,
    val items: List<InvoiceItem>,
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val status: String = "draft",
    @SerialName("financial_year") val financialYear: String,
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("contact_organisation") val contactOrganisation: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ApiError(val error: String)

// Request bodies
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("organisation_name") val organisationName: String? = null,
    val mobile: String? = null
)

@Serializable
data class UpdateReminderRequest(
    val status: String,
    @SerialName("snoozed_until") val snoozedUntil: String? = null
)
