package com.example.gocab
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gocab.MainScreenNavigationHelper.currentScreen
import com.example.gocab.network.RetrofitInstance
import com.example.gocab.network.UserRequest
import com.example.gocab.ui.theme.GoCabTheme
import com.example.gocab.viewmodel.DriverProfileViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.gocab.MyProfileScreenS
import com.example.gocab.ScheduledRidesScreen
import com.example.gocab.ui.student.StudentMyProfile


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            GoCabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

enum class Screen {
    SPLASH,
    ROLE_SELECTION,
    LOGIN,
    SIGNUP,
    PERSONAL_DETAILS,
    BOOK_RIDE,
    HOME,
    RIDE_HISTORY,
    SEARCH_RIDE,
    SEARCH_RIDE_J,
    DRIVER_PERSONAL_DETAILS,
    DRIVER_HOME,
    DRIVER_PROFILE,
    RIDE_REQUESTS,
    CONFIRMED_RIDES,
    MONTHLY_EARNINGS,
    DRIVER_DETAILS,
    // ✅ add these new ones
    MAINTENANCE_HOME,
    MAINTENANCE_STUDENTS,
    MAINTENANCE_DRIVERS,
    MAINTENANCE_COMPLAINTS,
    MAINTENANCE_PROFILE,
    ADMIN_HOME,MY_PROFILE,
    SCHEDULED_RIDES,
    STUDENT_PROFILE
}
val driverScreens = setOf(
    Screen.DRIVER_HOME,
    Screen.DRIVER_PROFILE,
    Screen.RIDE_REQUESTS,
    Screen.CONFIRMED_RIDES,
    Screen.MONTHLY_EARNINGS
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    /*val currentScreenState = MainScreenNavigationHelper.currentScreen
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current*/
    val currentScreenState = currentScreen
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current


    // Splash delay
    /*LaunchedEffect(Unit) {
        delay(3000)
        currentScreenState.value = if (auth.currentUser == null) {
            val role = Prefs.getUserRole(context)
            val detailsFilled = Prefs.isDetailsFilled(context)
            when {
                role == "Driver" && !detailsFilled -> Screen.DRIVER_PERSONAL_DETAILS
                role == "Driver" && detailsFilled -> Screen.DRIVER_HOME
                role == "Student" && !detailsFilled -> Screen.PERSONAL_DETAILS
                else -> Screen.HOME
            }
        } else {
            Screen.ROLE_SELECTION
        }
    }*/
    LaunchedEffect(Unit) {
        delay(3000)
        val user = auth.currentUser
        val role = Prefs.getUserRole(context)
        val detailsFilled = Prefs.isDetailsFilled(context)

        // 🔍 Strict validation
        currentScreenState.value = when {
            user == null -> {
                // No Firebase user → fresh app start
                Screen.ROLE_SELECTION
            }
            role.isNullOrEmpty() -> {
                // Logged in but role missing → reset
                FirebaseAuth.getInstance().signOut()
                Prefs.clear(context)
                Screen.ROLE_SELECTION
            }
            role == "Driver" && !detailsFilled -> Screen.DRIVER_PERSONAL_DETAILS
            role == "Driver" && detailsFilled -> Screen.DRIVER_HOME
            role == "Student" && !detailsFilled -> Screen.PERSONAL_DETAILS
            role == "Student" && detailsFilled -> Screen.HOME
            else -> {
                // Fallback if prefs are corrupted
                FirebaseAuth.getInstance().signOut()
                Prefs.clear(context)
                Screen.ROLE_SELECTION
            }
        }
    }
    when (currentScreenState.value) {
        Screen.SPLASH -> SplashScreen()
        Screen.ROLE_SELECTION -> RoleSelectionScreen { role ->
            currentScreenState.value = when (role) {
                "Student", "Driver" -> Screen.SIGNUP
                "Others" -> Screen.LOGIN
                else -> Screen.LOGIN
            }
        }
        Screen.LOGIN -> LoginScreen(
            onSignUpClicked = { currentScreenState.value = Screen.SIGNUP },
            onLoginSuccess = {
                val role = Prefs.getUserRole(context)
                val detailsFilled = Prefs.isDetailsFilled(context)
                currentScreenState.value = when {
                    role == "Driver" && !detailsFilled -> Screen.DRIVER_PERSONAL_DETAILS
                    role == "Driver" && detailsFilled -> Screen.DRIVER_HOME
                    role == "Student" && !detailsFilled -> Screen.PERSONAL_DETAILS
                    else -> Screen.HOME
                }
            }
        )
        Screen.SIGNUP -> SignupScreen(
            onSignupSuccess = {
                Toast.makeText(context, "Please verify your email", Toast.LENGTH_LONG).show()
                currentScreenState.value = Screen.LOGIN
            },
            onBackToLogin = { currentScreenState.value = Screen.LOGIN }
        )

        Screen.PERSONAL_DETAILS -> PersonalDetailsScreen(
            onFinished = {
                Prefs.setDetailsFilled(context, true)
                Toast.makeText(context, "Details saved!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.HOME
            }
        )

        Screen.STUDENT_PROFILE -> {
            var isStudentEditing by remember { mutableStateOf(false) }
            StudentMyProfile(
                firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                isEditing = isStudentEditing,
                onEditToggle = { isStudentEditing = it },
                onLogout = {
                    auth.signOut()
                    currentScreen.value = Screen.LOGIN
                },
                onBack = {
                    currentScreen.value = Screen.HOME   // 🔥 last screen
                }
            )
        }
        Screen.BOOK_RIDE -> BookRideScreen(
            onInitiateRide = { currentScreenState.value = Screen.SEARCH_RIDE },
            onJoinRide = { currentScreenState.value = Screen.SEARCH_RIDE_J }
        )
        /*Screen.HOME -> HomeScreen(
            onBookRide = { currentScreenState.value = Screen.BOOK_RIDE },
            onViewRides = { currentScreenState.value = Screen.RIDE_HISTORY },
            onLogout = {
                auth.signOut()
                Toast.makeText(context, "Logged out!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.LOGIN
            },
            onProfile = { currentScreenState.value = Screen.MY_PROFILE },
            onHistory = { currentScreenState.value = Screen.RIDE_HISTORY },
            onScheduledRides = { currentScreenState.value = Screen.SCHEDULED_RIDES }
        )*/
        Screen.HOME -> HomeScreen(
            onBookRide = { currentScreenState.value = Screen.BOOK_RIDE },
            onViewRides = { currentScreenState.value = Screen.RIDE_HISTORY },
            onLogout = {
                auth.signOut()
                Toast.makeText(context, "Logged out!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.LOGIN
            },
            onProfile = { currentScreenState.value = Screen.STUDENT_PROFILE },
            onHistory = { currentScreenState.value = Screen.RIDE_HISTORY },
            currentScreen = { currentScreenState.value },
            onScheduledRides = { currentScreenState.value = Screen.SCHEDULED_RIDES      },

            //onScheduledRides = { /* //DO */ }
        )

        Screen.RIDE_HISTORY -> RideHistoryScreen(
            onLogout = {
                auth.signOut()
                Toast.makeText(context, "Logged out!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.LOGIN
            },
            onProfile = { currentScreenState.value = Screen.MY_PROFILE },
            onScheduledRides = { currentScreenState.value = Screen.SCHEDULED_RIDES },
            onHome = { currentScreenState.value = Screen.HOME }
        )
        Screen.SEARCH_RIDE -> SearchRideNavScreen(
            onBackToHome = { currentScreenState.value = Screen.BOOK_RIDE }
        )
        Screen.SEARCH_RIDE_J -> JoinRideSearchNav1(
            onBackToHome = { currentScreenState.value = Screen.BOOK_RIDE }
        )
        Screen.DRIVER_PERSONAL_DETAILS -> DriverPersonalDetailsScreen {
            currentScreenState.value = Screen.DRIVER_HOME
        }
        in driverScreens -> DriverAppContainer(
            currentScreen = currentScreenState.value,
            onScreenChange = { newScreen -> currentScreenState.value = newScreen },
            onLogout = {
                auth.signOut()
                Toast.makeText(context, "Driver logged out!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.ROLE_SELECTION
            }
        )
        Screen.MAINTENANCE_HOME -> MaintenanceHomeScreens(
            //onViewStudents = { currentScreenState.value = Screen.MAINTENANCE_STUDENTS },
            //onViewDrivers = { currentScreenState.value = Screen.MAINTENANCE_DRIVERS },
            onViewComplaints = { currentScreenState.value = Screen.MAINTENANCE_COMPLAINTS },
            //onProfile = { currentScreenState.value = Screen.MAINTENANCE_PROFILE },
            onLogout = {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.LOGIN
            }
        )
        Screen.ADMIN_HOME -> AdminApp(
            onViewStudents = {},
            onViewDrivers = {},
            onScheduledRides = {},
            onRideAlerts = {},
            onLogout = {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.LOGIN
            }
        )
        Screen.MY_PROFILE -> MyProfileScreenS(
            onLogout = {
                auth.signOut()
                Toast.makeText(context, "Logged out!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.LOGIN
            },
            onHome = { currentScreenState.value = Screen.HOME },
            onRideHistory = { currentScreenState.value = Screen.RIDE_HISTORY },
            onScheduledRides = { currentScreenState.value = Screen.SCHEDULED_RIDES }
        )

        Screen.SCHEDULED_RIDES -> ScheduledRidesScreen(
            onLogout = {
                auth.signOut()
                Toast.makeText(context, "Logged out!", Toast.LENGTH_SHORT).show()
                currentScreenState.value = Screen.LOGIN
            },
            onHome = { currentScreenState.value = Screen.HOME },
            onProfile = { currentScreenState.value = Screen.MY_PROFILE },
            onRideHistory = { currentScreenState.value = Screen.RIDE_HISTORY }
        )

        else -> {}

    }

}
/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverAppContainer(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onNavigate = { newScreen ->
                    onScreenChange(newScreen)
                    scope.launch { drawerState.close() }
                },
                onLogout = {
                    scope.launch {
                        drawerState.close()
                        onLogout()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                val title = when (currentScreen) {
                    Screen.DRIVER_HOME -> "Driver Dashboard"
                    Screen.DRIVER_PROFILE -> "My Profile"
                    Screen.RIDE_REQUESTS -> "New Ride Requests"
                    Screen.CONFIRMED_RIDES -> "My Rides"
                    Screen.MONTHLY_EARNINGS -> "My Earnings"
                    else -> "GoCab Driver"
                }
                CenterAlignedTopAppBar(
                    title = { Text(title, color = Color.White, fontSize = 22.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open()
                                else drawerState.close()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (currentScreen == Screen.DRIVER_PROFILE) {
                            IconButton(onClick = { onScreenChange(Screen.DRIVER_DETAILS) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF3F51B5)
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (currentScreen) {
                    /*Screen.DRIVER_HOME -> DriverHomeScreenContent()*/
                    Screen.DRIVER_HOME -> DriverHomeScreen(
                        onRideRequests = { onScreenChange(Screen.RIDE_REQUESTS) },
                        onMonthlyEarnings = { onScreenChange(Screen.MONTHLY_EARNINGS) }
                    )



                    Screen.DRIVER_PROFILE -> {
                        BackHandler { onScreenChange(Screen.DRIVER_HOME) }

                        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val driverProfileViewModel: DriverProfileViewModel = viewModel()

                        DriverProfileScreen(
                            onLogout = onLogout,
                            firebaseUid = firebaseUid,
                            viewModel = driverProfileViewModel
                        )
                    }
                    Screen.RIDE_REQUESTS -> {
                        BackHandler { onScreenChange(Screen.DRIVER_HOME) }
                        RideRequestScreen(
                            onAccept = {
                                Toast.makeText(context, "Ride Accepted!", Toast.LENGTH_SHORT).show()
                                onScreenChange(Screen.DRIVER_HOME)
                            },
                            onReject = {
                                Toast.makeText(context, "Ride Rejected!", Toast.LENGTH_SHORT).show()
                                onScreenChange(Screen.DRIVER_HOME)
                            }
                        )
                    }
                    Screen.MONTHLY_EARNINGS -> {
                        BackHandler { onScreenChange(Screen.DRIVER_HOME) }
                        DriverHomeScreenContent()
                    }

                    else -> DriverHomeScreenContent()
                }
            }
        }
    }
}*/
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image (keeps original brightness)
        Image(
            painter = painterResource(id = R.drawable.signup_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Optional subtle overlay — remove or reduce alpha if image is too dull
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f)) // reduce alpha to keep image bright
        )
        // Form positioned a bit lower to match your background design
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp)
                .padding(top = 300.dp), // adjust this value to align fields precisely
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sign Up",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(                 // <- Material3 API
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    disabledBorderColor = Color.Transparent,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    disabledBorderColor = Color.Transparent,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    disabledBorderColor = Color.Transparent,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val e = email.trim()
                    val p = password.trim()
                    val c = confirmPassword.trim()
                    if (e.isBlank() || p.isBlank() || c.isBlank()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                        Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (p != c) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    auth.createUserWithEmailAndPassword(e, p)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val firebaseUid = user?.uid ?: ""
                                val emailId = user?.email ?: ""
                                val role = Prefs.getUserRole(context) ?: ""
                                // ✅ Save first login flag
                                val sharedPreferences = context.getSharedPreferences("GoCabPrefs", Context.MODE_PRIVATE)
                                sharedPreferences.edit().putBoolean("firstLogin_${firebaseUid}", true).apply()
                                // ✅ Save user info to Azure SQL via backend
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val selectedRole = Prefs.getUserRole(context) ?: "Student"
                                        Log.d("RegisterDebug", "➡ Selected role during signup: $selectedRole")
                                        val userResponse = RetrofitInstance.api.registerUser(
                                            UserRequest(firebaseUid, emailId, selectedRole)
                                        )
                                        Log.d("RegisterDebug", "Response code: ${userResponse.code()}")
                                        Log.d("RegisterDebug", "Response body: ${userResponse.body()}")
                                        Log.d("RegisterDebug", "Response error body: ${userResponse.errorBody()?.string()}")
                                        if (userResponse.isSuccessful) {
                                            Log.d("RegisterDebug", "✅ Request succeeded")
                                        } else {
                                            Log.e("RegisterDebug", "❌ Request failed")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("RegisterDebug", "🔥 Exception while calling backend: ${e.message}", e)
                                    }
                                }
                                auth.currentUser?.sendEmailVerification()
                                Prefs.setDetailsFilled(context, false)
                                onSignupSuccess()
                            } else {
                                Toast.makeText(context, "Signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                Text("Sign Up", color = Color.White, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onBackToLogin) {
                Text("Already have an account? Login", color = Color.White)
            }
        }
    }
}
@Composable
fun LoginScreen(
    onSignUpClicked: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.signup_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp)
                .padding(top = 300.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val e = email.trim()
                    val p = password.trim()
                    if (e.isBlank() || p.isBlank()) {
                        Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    auth.signInWithEmailAndPassword(e, p)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    val firebaseUid = user.uid
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val response =
                                                RetrofitInstance.api.getUserRole(mapOf("firebase_uid" to firebaseUid))
                                            withContext(Dispatchers.Main) {
                                                if (response.isSuccessful) {
                                                    val actualRole =
                                                        response.body()?.user_type ?: ""
                                                    Prefs.setUserRole(context, actualRole)
                                                    val requiresVerification =
                                                        (actualRole == "Student" || actualRole == "Driver")
                                                    val verifiedOnce =
                                                        Prefs.getBoolean(context, "verified_$firebaseUid", false)

                                                    if (requiresVerification && !user.isEmailVerified && !verifiedOnce) {
                                                        Toast.makeText(
                                                            context,
                                                            "Please verify your email before logging in.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        FirebaseAuth.getInstance().signOut()
                                                        return@withContext
                                                    }

                                                    // ✅ Remember verified status
                                                    if (requiresVerification && user.isEmailVerified && !verifiedOnce) {
                                                        Prefs.setBoolean(context, "verified_$firebaseUid", true)
                                                    }

                                                    // ✅ Navigate by role
                                                    when (actualRole) {

                                                        "Student" -> {
                                                            Toast.makeText(context, "Welcome Student!", Toast.LENGTH_SHORT).show()

                                                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                                                            val sharedPreferences = context.getSharedPreferences("GoCabPrefs", Context.MODE_PRIVATE)
                                                            val isFirstLogin = sharedPreferences.getBoolean("firstLogin_${userId}", false)

                                                            if (isFirstLogin) {
                                                                // Navigate to personal details only once
                                                                (context as ComponentActivity).runOnUiThread {
                                                                    MainScreenNavigationHelper.navigateTo(Screen.PERSONAL_DETAILS)
                                                                }
                                                                // Mark it as completed
                                                                sharedPreferences.edit().putBoolean("firstLogin_${userId}", false).apply()
                                                            } else {
                                                                // From 2nd login onwards → home screen
                                                                (context as ComponentActivity).runOnUiThread {
                                                                    MainScreenNavigationHelper.navigateTo(Screen.HOME)
                                                                }
                                                            }
                                                        }

                                                        "Driver" -> {
                                                            Toast.makeText(context, "Welcome Driver!", Toast.LENGTH_SHORT).show()

                                                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                                                            val sharedPreferences = context.getSharedPreferences("GoCabPrefs", Context.MODE_PRIVATE)
                                                            val isFirstLogin = sharedPreferences.getBoolean("firstLogin_${userId}", false)

                                                            if (isFirstLogin) {
                                                                // ✅ Show DriverPersonalScreen only once
                                                                (context as ComponentActivity).runOnUiThread {
                                                                    MainScreenNavigationHelper.navigateTo(Screen.DRIVER_PERSONAL_DETAILS)
                                                                }
                                                                sharedPreferences.edit().putBoolean("firstLogin_${userId}", false).apply()
                                                            } else {
                                                                // ✅ From next time onwards → Driver Home
                                                                (context as ComponentActivity).runOnUiThread {
                                                                    MainScreenNavigationHelper.navigateTo(Screen.DRIVER_HOME)
                                                                }
                                                            }
                                                        }

                                                        "MaintenanceTeam" -> {
                                                            Toast.makeText(
                                                                context,
                                                                "Welcome Maintenance Team!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            Prefs.setDetailsFilled(context, true)
                                                            (context as ComponentActivity).runOnUiThread {
                                                                MainScreenNavigationHelper.navigateTo(Screen.MAINTENANCE_HOME)
                                                            }
                                                        }

                                                        "Administration" -> {
                                                            Toast.makeText(
                                                                context,
                                                                "Welcome Administration!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            Prefs.setDetailsFilled(context, true)
                                                            (context as ComponentActivity).runOnUiThread {
                                                                MainScreenNavigationHelper.navigateTo(Screen.ADMIN_HOME)
                                                            }
                                                        }

                                                        else -> {
                                                            Toast.makeText(
                                                                context,
                                                                "Invalid role access!",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            FirebaseAuth.getInstance().signOut()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Error fetching user role",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("RoleCheck", "Error fetching user role", e)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to verify role",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Login Failed: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                Text("Login", color = Color.White, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSignUpClicked) {
                Text("Don't have an account? Sign Up", color = Color.White)
            }
        }
    }
}
/*@Composable
fun DriverHomeScreenContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_5),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Text(
                text = "Welcome, Driver 👋",
                fontSize = 34.sp,
                color = Color.Black
            )
        }
    }
}*/
/*@Composable
fun DrawerContent(
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(250.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF3F51B5))
        ) {
            Column {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "🚖 GoCab Driver",
                    fontSize = 26.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 20.dp, bottom = 30.dp)
                )

                DrawerItem("My Profile", Icons.Filled.Person) { onNavigate(Screen.DRIVER_PROFILE) }
                DrawerItem("Ride Requests", Icons.Filled.DirectionsCar) { onNavigate(Screen.RIDE_REQUESTS) }
                DrawerItem("Confirmed Rides", Icons.Filled.CheckCircle) { onNavigate(Screen.CONFIRMED_RIDES) }
                DrawerItem("Monthly Earnings", Icons.Filled.Money) { onNavigate(Screen.MONTHLY_EARNINGS) }
                DrawerItem("Logout", Icons.AutoMirrored.Filled.ExitToApp) { onLogout() }
            }
        }
    }
}
@Composable
fun DrawerItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = Color.White)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp)
    }
}*/
@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = "Splash Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // Center content vertically too
    ) {
        // Background Image (img_4)
        Image(
            painter = painterResource(id = R.drawable.img_6),
            contentDescription = "Role Selection Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Slight dark overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )
        // Foreground content (Text and Buttons)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center, // Center buttons vertically
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp) // Add padding on sides
        ) {
            Text(
                text = "Enter as",
                fontSize = 42.sp, // Bada font size
                fontWeight = FontWeight.Bold,
                color = Color.White, // Safed rang background par dikhne ke liye
                modifier = Modifier.padding(bottom = 48.dp) // Buttons se thoda neeche
            )
            // Student Button
            RoleSelectionButtonWide( // Using the new reusable button
                text = "Student",
                icon = Icons.Filled.School,
                buttonColor = Color(0xFFFFFFFF), // Blue
                onClick = {
                    Prefs.setUserRole(context, "Student")
                    onRoleSelected("Student")
                }
            )
            Spacer(modifier = Modifier.height(30.dp)) // Spacing between buttons
            // Driver Button
            RoleSelectionButtonWide( // Using the new reusable button
                text = "Driver",
                icon = Icons.Filled.DirectionsCar,
                buttonColor = Color(0xFFFFFFFF), // Green
                onClick = {
                    Prefs.setUserRole(context, "Driver")
                    onRoleSelected("Driver")
                }
            )
            Spacer(modifier = Modifier.height(30.dp)) // Spacing between buttons
            // Others Button (Now Filled)
            RoleSelectionButtonWide( // Using the new reusable button
                text = "Others",
                icon = Icons.Filled.Person,
                buttonColor = Color(0xFFFFFFFF), // Neutral color
                onClick = {
                    Prefs.setUserRole(context, "Others")
                    MainScreenNavigationHelper.currentScreen.value = Screen.LOGIN
                }
            )
        }
    }
}
// Reusable Composable for WIDER filled buttons with image background
@Composable
fun RoleSelectionButtonWide(text: String, icon: ImageVector, buttonColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp), // Rounded corners
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color(0xFF3D2486) // Text/Icon color on button
        ),
        modifier = Modifier
            .fillMaxWidth(0.96f) // ✅ WIDER BUTTON (80% of screen width)
            .height(64.dp)      // ✅ Slightly Taller Button
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start // Center icon and text
        ) {
            Spacer(modifier = Modifier.width(12.dp)) // Add padding before icon
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(23.dp)) // ✅ Increase space between icon and text
            // Center the text horizontally within the remaining space
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { // CenterStart will align text nicely
                Text(
                    text = text,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp)) // Add padding after text
        }
    }
}
@Composable
fun MaintenanceHomePlaceholder(onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome to Maintenance Team Home", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onLogout) {
                Text("Logout")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administration Dashboard") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome, Administration!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("This is your Admin Panel — features coming soon.")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceStudentsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),  // ✅ use Scaffold padding here
            contentAlignment = Alignment.Center
        ) {
            Text("Student Management Page (Coming Soon)")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceDriversScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drivers List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),  // ✅ apply padding
            contentAlignment = Alignment.Center
        ) {
            Text("Driver Management Page (Coming Soon)")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceComplaintsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complaints") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),  // ✅ apply padding
            contentAlignment = Alignment.Center
        ) {
            Text("Complaints Page (Coming Soon)")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceProfileScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),  // ✅ apply padding
            contentAlignment = Alignment.Center
        ) {
            Text("Profile Page (Coming Soon)")
        }
    }
}
