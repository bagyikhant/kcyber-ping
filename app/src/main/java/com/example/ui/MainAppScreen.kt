package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.FastBenchmarkScreen
import com.example.ui.screens.HistorySavedScreen
import com.example.ui.screens.IpToolsScreen
import com.example.ui.screens.MainPingScreen
import com.example.ui.screens.NetworkInfoScreen
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

enum class AppNavDestination(
    val title: String,
    val icon: ImageVector
) {
    PING("Ping", Icons.Default.Radar),
    NETWORK("Network", Icons.Default.Wifi),
    FAST_DNS("Fast DNS", Icons.Default.RocketLaunch),
    IP_TOOLS("IP Tools", Icons.Default.Lan),
    HISTORY("History", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: PingViewModel) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(AppNavDestination.PING) }
    val networkStatus by viewModel.networkStatus.collectAsState()

    // Explicit Toast "AungKyawKhant(Lawksawk)" requirement on launch
    LaunchedEffect(Unit) {
        Toast.makeText(context, "AungKyawKhant(Lawksawk)", Toast.LENGTH_LONG).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (networkStatus.isConnected) CyberGreen else CyberCyan)
                        )
                        Text(
                            text = " PING KCYBER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = " // NETWORK TOOLKIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "AungKyawKhant(Lawksawk)", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Author Info",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBg,
                    titleContentColor = CyberTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                contentColor = CyberCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                AppNavDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberBg,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberCyan,
                            unselectedIconColor = CyberTextSecondary,
                            unselectedTextColor = CyberTextSecondary
                        )
                    )
                }
            }
        },
        containerColor = CyberBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentDestination) {
                AppNavDestination.PING -> MainPingScreen(viewModel = viewModel)
                AppNavDestination.NETWORK -> NetworkInfoScreen(viewModel = viewModel)
                AppNavDestination.FAST_DNS -> FastBenchmarkScreen(
                    viewModel = viewModel,
                    onSelectHostForPing = { host ->
                        viewModel.setHost(host)
                        currentDestination = AppNavDestination.PING
                        viewModel.startPing()
                    }
                )
                AppNavDestination.IP_TOOLS -> IpToolsScreen(
                    viewModel = viewModel,
                    onSelectHostForPing = { host ->
                        viewModel.setHost(host)
                        currentDestination = AppNavDestination.PING
                    }
                )
                AppNavDestination.HISTORY -> HistorySavedScreen(
                    viewModel = viewModel,
                    onSelectHostForPing = { host ->
                        viewModel.setHost(host)
                        currentDestination = AppNavDestination.PING
                        viewModel.startPing()
                    }
                )
            }
        }
    }
}
