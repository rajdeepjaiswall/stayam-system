package `in`.getdownfoundation.sahusales.core

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<AuthResponse>

    @GET("api/me")
    suspend fun getMe(): Response<User>

    @PATCH("api/me")
    suspend fun updateMe(@Body body: Map<String, String>): Response<User>

    @GET("api/contacts")
    suspend fun getContacts(@Query("search") search: String = ""): Response<List<Contact>>

    @POST("api/contacts")
    suspend fun createContact(@Body body: Map<String, String?>): Response<Contact>

    @GET("api/contacts/{id}")
    suspend fun getContact(@Path("id") id: String): Response<Contact>

    @PATCH("api/contacts/{id}")
    suspend fun updateContact(@Path("id") id: String, @Body body: Map<String, String?>): Response<Contact>

    @DELETE("api/contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String): Response<Map<String, Boolean>>

    @GET("api/events")
    suspend fun getEvents(
        @Query("status") status: String? = null,
        @Query("tag_id") tagId: String? = null,
        @Query("assigned_to") assignedTo: String? = null
    ): Response<List<Event>>

    @POST("api/events")
    suspend fun createEvent(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Event>

    @PATCH("api/events/{id}")
    suspend fun updateEvent(@Path("id") id: String, @Body body: Map<String, String?>): Response<Event>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): Response<Map<String, Boolean>>

    @GET("api/reminders")
    suspend fun getReminders(@Query("due") due: Int = 1): Response<List<ReminderFeedItem>>

    @PATCH("api/reminders/{id}")
    suspend fun updateReminder(@Path("id") id: String, @Body body: UpdateReminderRequest): Response<Reminder>

    @GET("api/activity")
    suspend fun getActivity(): Response<List<ActivityItem>>

    @GET("api/event-tags")
    suspend fun getEventTags(): Response<List<EventTag>>

    @POST("api/event-tags")
    suspend fun createEventTag(@Body body: Map<String, String>): Response<EventTag>

    @PATCH("api/event-tags/{id}")
    suspend fun updateEventTag(@Path("id") id: String, @Body body: Map<String, String>): Response<EventTag>

    @DELETE("api/event-tags/{id}")
    suspend fun deleteEventTag(@Path("id") id: String): Response<Map<String, Boolean>>

    @GET("api/products")
    suspend fun getProducts(): Response<List<Product>>

    @POST("api/products")
    suspend fun createProduct(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Product>

    @PATCH("api/products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Product>

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): Response<Map<String, Boolean>>

    @GET("api/invoices")
    suspend fun getInvoices(): Response<List<Invoice>>

    @POST("api/invoices")
    suspend fun createInvoice(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Invoice>

    @GET("api/invoices/{id}")
    suspend fun getInvoice(@Path("id") id: String): Response<Invoice>

    @PATCH("api/invoices/{id}")
    suspend fun updateInvoice(@Path("id") id: String, @Body body: Map<String, String>): Response<Invoice>

    @GET("api/team")
    suspend fun getTeam(): Response<List<User>>

    @POST("api/team")
    suspend fun createTeamMember(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<AuthResponse>

    @PATCH("api/team/{id}")
    suspend fun updateTeamMember(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<User>
}
