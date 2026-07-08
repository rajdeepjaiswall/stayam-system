package `in`.getdownfoundation.sahusales.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit,
    onGoRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val loading by viewModel.loading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sahu Sales", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary)
        Text("Solution", fontSize = 18.sp, color = Color(0xFF475569))
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFEF4444), fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                errorMsg = null
                viewModel.login(email.trim(), password,
                    onSuccess = onLoginSuccess,
                    onError = { errorMsg = it }
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            else Text("LOGIN", fontWeight = FontWeight.Bold)
        }

        // Registration is admin-only via Team section — no self-register button
    }
}
