package `in`.getdownfoundation.sahusales.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.theme.Primary

@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    onRegisterSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var orgName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val loading by viewModel.loading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Primary)
        Spacer(Modifier.height(32.dp))

        listOf(
            Triple("Full Name *", fullName, { v: String -> fullName = v }),
            Triple("Email *", email, { v: String -> email = v }),
            Triple("Mobile", mobile, { v: String -> mobile = v }),
            Triple("Organisation", orgName, { v: String -> orgName = v })
        ).forEach { (label, value, onChange) ->
            OutlinedTextField(
                value = value, onValueChange = onChange,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (label.contains("Email")) KeyboardType.Email
                    else if (label.contains("Mobile")) KeyboardType.Phone
                    else KeyboardType.Text
                )
            )
            Spacer(Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password *") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFEF4444), fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                errorMsg = null
                viewModel.register(
                    email.trim(), password, fullName.trim(),
                    orgName.ifBlank { null }, mobile.ifBlank { null },
                    onSuccess = onRegisterSuccess,
                    onError = { errorMsg = it }
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            else Text("REGISTER", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onGoLogin) {
            Text("Already have an account? Login")
        }
    }
}
