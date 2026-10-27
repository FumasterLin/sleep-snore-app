package com.mytools.sleepsnore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytools.sleepsnore.data.model.SleepInfluenceItem
import com.mytools.sleepsnore.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluencesScreen(
    selectedFactors: List<String>,
    selectedRemedies: List<String>,
    onToggleFactor: (String) -> Unit,
    onToggleRemedy: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("睡前影响因素设置", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "今夜采取的止鼾对策 (Remedies)",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnoreQuiet,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(SleepInfluenceItem.ALL_REMEDIES) { item ->
                    val isSelected = selectedRemedies.contains(item.id)
                    InfluenceChip(
                        item = item,
                        isSelected = isSelected,
                        activeColor = SnoreQuiet,
                        onClick = { onToggleRemedy(item.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "今日潜在打鼾诱因 (Factors)",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SnoreEpic,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(SleepInfluenceItem.ALL_FACTORS) { item ->
                    val isSelected = selectedFactors.contains(item.id)
                    InfluenceChip(
                        item = item,
                        isSelected = isSelected,
                        activeColor = SnoreEpic,
                        onClick = { onToggleFactor(item.id) }
                    )
                }
            }

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(50.dp)
            ) {
                Text("确定并返回", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
            }
        }
    }
}

@Composable
private fun InfluenceChip(
    item: SleepInfluenceItem,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bg = if (isSelected) activeColor.copy(alpha = 0.2f) else DarkSurface
    val border = if (isSelected) activeColor else DarkBorder

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = item.imageResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
}
