package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.DnsBenchmarkResult
import com.example.ui.PingViewModel
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberElectric
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPink
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceCardElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberYellow

@Composable
fun FastBenchmarkScreen(
    viewModel: PingViewModel,
    onSelectHostForPing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val benchmarks by viewModel.dnsBenchmarks.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()

    val fastest = benchmarks.firstOrNull { it.isSuccess && it.latencyMs > 0 }
    val maxLatency = benchmarks.filter { it.isSuccess }.map { it.latencyMs }.maxOrNull() ?: 150f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Benchmark Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "FAST CONNECTION BENCHMARK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberTextPrimary
                        )
                    }
                }

                Text(
                    text = "Benchmark response time and jitter across top global Anycast DNS & Edge nodes to identify the lowest latency route for your network.",
                    fontSize = 12.sp,
                    color = CyberTextSecondary,
                    lineHeight = 17.sp
                )

                // Recommended Fastest Server Highlight
                if (fastest != null && !isBenchmarking) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberGreen.copy(alpha = 0.12f))
                            .border(1.dp, CyberGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "FASTEST DETECTED SERVER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberGreen
                                    )
                                    Text(
                                        text = "${fastest.providerName} (${fastest.primaryIp})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyberTextPrimary
                                    )
                                }
                            }
                            Text(
                                text = "${"%.1f".format(fastest.latencyMs)} ms",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberGreen
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.startDnsBenchmark() },
                    enabled = !isBenchmarking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = com.example.ui.theme.CyberBg
                    )
                ) {
                    if (isBenchmarking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = com.example.ui.theme.CyberBg
                            )
                            Text("BENCHMARKING SERVERS...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("RUN FULL NETWORK SPEED TEST", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Benchmark Results List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(benchmarks) { index, item ->
                BenchmarkItemCard(
                    item = item,
                    rank = index + 1,
                    isFastest = index == 0 && item.isSuccess && item.latencyMs > 0,
                    maxLatency = maxLatency,
                    onSelect = { onSelectHostForPing(item.primaryIp) }
                )
            }
        }
    }
}

@Composable
fun BenchmarkItemCard(
    item: DnsBenchmarkResult,
    rank: Int,
    isFastest: Boolean,
    maxLatency: Float,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurfaceCard)
            .border(
                1.dp,
                if (isFastest) CyberGreen else CyberBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isFastest -> CyberGreen
                                    rank <= 3 && item.isSuccess -> CyberCyan
                                    else -> CyberSurfaceCardElevated
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$rank",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFastest || (rank <= 3 && item.isSuccess)) com.example.ui.theme.CyberBg else CyberTextSecondary
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.providerName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary
                            )
                            if (isFastest) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("FASTEST", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = CyberGreen)
                                }
                            }
                        }
                        Text(
                            text = "${item.primaryIp} • ${item.description}",
                            fontSize = 11.sp,
                            color = CyberTextSecondary
                        )
                    }
                }

                // Latency Indicator / Spinner
                if (item.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CyberCyan)
                } else if (item.isSuccess && item.latencyMs > 0) {
                    Text(
                        text = "${"%.1f".format(item.latencyMs)} ms",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            item.latencyMs < 30 -> CyberGreen
                            item.latencyMs < 80 -> CyberCyan
                            item.latencyMs < 140 -> CyberYellow
                            else -> CyberRed
                        }
                    )
                } else if (item.error != null) {
                    Text(
                        text = "Unreachable",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberRed
                    )
                } else {
                    Text(
                        text = "--",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted
                    )
                }
            }

            // Latency Visual Progress Bar
            if (item.isSuccess && item.latencyMs > 0) {
                val progress = (item.latencyMs / maxLatency).coerceIn(0.05f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when {
                        item.latencyMs < 30 -> CyberGreen
                        item.latencyMs < 80 -> CyberCyan
                        item.latencyMs < 140 -> CyberYellow
                        else -> CyberRed
                    },
                    trackColor = CyberSurfaceCardElevated
                )
            }
        }
    }
}
