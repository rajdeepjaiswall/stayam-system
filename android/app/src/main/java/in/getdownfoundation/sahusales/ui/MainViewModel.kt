package `in`.getdownfoundation.sahusales.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import `in`.getdownfoundation.sahusales.alarm.ReminderSyncer
import `in`.getdownfoundation.sahusales.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val context: Context) : ViewModel() {
    private val store = SessionStore(context)

    private val _token = MutableStateFlow<String?>(null)
    val token = _token.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()

    private val _reminders = MutableStateFlow<List<ReminderFeedItem>>(emptyList())
    val reminders = _reminders.asStateFlow()

    private val _activity = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activity = _activity.asStateFlow()

    private val _eventTags = MutableStateFlow<List<EventTag>>(emptyList())
    val eventTags = _eventTags.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices = _invoices.asStateFlow()

    private val _team = MutableStateFlow<List<User>>(emptyList())
    val team = _team.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun api(): ApiService? {
        val t = _token.value ?: return null
        return RetrofitClient.create(Config.BASE_URL, t)
    }

    fun apiNoAuth() = RetrofitClient.create(Config.BASE_URL)

    init {
        viewModelScope.launch {
            val t = store.getToken()
            _token.value = t
            if (t != null) {
                _currentUser.value = store.getUser()
                loadAll()
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = withContext(Dispatchers.IO) {
                    apiNoAuth().login(LoginRequest(email, password))
                }
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    store.saveSession(body.token, body.user)
                    _token.value = body.token
                    _currentUser.value = body.user
                    loadAll()
                    onSuccess()
                } else {
                    val msg = resp.errorBody()?.string() ?: "Login failed"
                    onError(msg)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Network error")
            } finally {
                _loading.value = false
            }
        }
    }

    fun register(email: String, password: String, fullName: String, orgName: String?, mobile: String?,
                 onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = withContext(Dispatchers.IO) {
                    apiNoAuth().register(RegisterRequest(email, password, fullName, orgName, mobile))
                }
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    store.saveSession(body.token, body.user)
                    _token.value = body.token
                    _currentUser.value = body.user
                    loadAll()
                    onSuccess()
                } else {
                    val msg = resp.errorBody()?.string() ?: "Registration failed"
                    onError(msg)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Network error")
            } finally {
                _loading.value = false
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            store.clearSession()
            _token.value = null
            _currentUser.value = null
            _contacts.value = emptyList()
            _events.value = emptyList()
            _reminders.value = emptyList()
            onDone()
        }
    }

    fun loadAll() {
        loadContacts()
        loadEvents()
        loadReminders()
        loadActivity()
        loadEventTags()
        loadProducts()
        loadInvoices()
        loadTeam() // all users load team — needed for event assignment + admin detail view
    }

    fun loadContacts(search: String = "") {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api()?.getContacts(search) } ?: return@launch
                if (resp.isSuccessful) _contacts.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun loadEvents(status: String? = null) {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api()?.getEvents(status) } ?: return@launch
                if (resp.isSuccessful) _events.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun loadReminders() {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api()?.getReminders() } ?: return@launch
                if (resp.isSuccessful) {
                    val data = resp.body() ?: emptyList()
                    _reminders.value = data
                    store.saveReminders(data)
                    ReminderSyncer.sync(context)
                }
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun loadActivity() {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api()?.getActivity() } ?: return@launch
                if (resp.isSuccessful) _activity.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun loadEventTags() {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api()?.getEventTags() } ?: return@launch
                if (resp.isSuccessful) _eventTags.value = resp.body() ?: emptyList()
            } catch (e: Exception) { }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api()?.getProducts() } ?: return@launch
                if (resp.isSuccessful) _products.value = resp.body() ?: emptyList()
            } catch (e: Exception) { }
        }
    }

    fun loadInvoices() {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api()?.getInvoices() } ?: return@launch
                if (resp.isSuccessful) _invoices.value = resp.body() ?: emptyList()
            } catch (e: Exception) { }
        }
    }

    fun loadTeam() {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api()?.getTeam() } ?: return@launch
                if (resp.isSuccessful) _team.value = resp.body() ?: emptyList()
            } catch (e: Exception) { }
        }
    }

    fun clearError() { _error.value = null }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(context.applicationContext) as T
    }
}
