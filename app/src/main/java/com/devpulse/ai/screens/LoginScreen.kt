package com.devpulse.ai.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.R
import com.devpulse.ai.components.BrandBackground
import com.devpulse.ai.components.BrandButton
import com.devpulse.ai.components.GlowCard
import com.devpulse.ai.components.PulseTextField
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.SurfaceDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.viewmodel.ErrorType
import com.devpulse.ai.viewmodel.LoginViewModel
import com.devpulse.ai.viewmodel.ProfileUiState
import com.devpulse.ai.viewmodel.ProfileViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToDashboard: (String) -> Unit
) {
    val username by viewModel.username.collectAsState()
    val isButtonEnabled by viewModel.isButtonEnabled.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // Trigger navigation only AFTER successfully fetching the profile details
    LaunchedEffect(profileUiState) {
        if (profileUiState is ProfileUiState.Success) {
            onNavigateToDashboard(username)
            profileViewModel.resetState() // Reset flow state to Idle
        }
    }

    BrandBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Header
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -50 })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Box
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = "GitHub Logo",
                            modifier = Modifier.size(64.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "DevPulse AI",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "AI-powered Developer Growth Analyzer",
                        fontSize = 15.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Card Container
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })
            ) {
                GlowCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Analyze your GitHub Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter your username to inspect commits, language usage, and calculate your growth metrics.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        PulseTextField(
                            value = username,
                            onValueChange = { viewModel.onUsernameChanged(it) },
                            placeholder = "github-username",
                            leadingIcon = {
                                Text(
                                    text = "@",
                                    color = Color(0xFF64748B),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val isLoading = profileUiState is ProfileUiState.Loading
                        BrandButton(
                            text = if (isLoading) "Analyzing..." else "Analyze Profile",
                            onClick = {
                                profileViewModel.fetchAndAnalyzeProfile(username)
                            },
                            enabled = isButtonEnabled && !isLoading
                        )
                    }
                }
            }
        }

        // Show Material 3 Error Dialog directly on the Login screen if fetch fails
        if (profileUiState is ProfileUiState.Error) {
            val errState = profileUiState as ProfileUiState.Error
            LoginErrorDialog(
                title = when (errState.errorType) {
                    ErrorType.USER_NOT_FOUND -> "User Not Found"
                    ErrorType.RATE_LIMIT -> "API Rate Limit"
                    ErrorType.NETWORK -> "No Internet"
                    ErrorType.EMPTY_REPOS -> "Empty Repositories"
                    else -> "Analysis Error"
                },
                message = errState.message,
                onDismiss = {
                    profileViewModel.clearError()
                }
            )
        }
    }
}

@Composable
fun LoginErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = message,
                color = TextSecondaryDark,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}
