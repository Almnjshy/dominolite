package com.almnjshy.agon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almnjshy.agon.engine.AIDifficulty
import com.almnjshy.agon.rendering.TableSurface
import com.almnjshy.agon.rendering.TableTheme
import com.almnjshy.agon.ui.theme.AgonColors

@Composable
fun MainMenuScreen(
    onStart: (playerCount: Int, difficulty: AIDifficulty) -> Unit,
    onPlayWithFriends: () -> Unit
) {
    var playerCount by remember { mutableStateOf(4) }
    var difficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }

    Box(Modifier.fillMaxSize()) {
        TableSurface(theme = TableTheme.CLASSIC_FELT)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .background(AgonColors.FeltGreenDark.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),  // ← Scroll للتأكد من ظهور كل شيء
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "AGON",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = AgonColors.GoldBright
            )
            Text("لعبة الدومينو الإيزومترية", color = AgonColors.TileIvory)

            Text("عدد اللاعبين", color = AgonColors.TileIvory, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(2, 3, 4).forEach { n ->
                    SelectableChip(label = "$n", selected = playerCount == n) { playerCount = n }
                }
            }

            Text("مستوى الذكاء الاصطناعي", color = AgonColors.TileIvory, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AIDifficulty.entries.forEach { d ->
                    SelectableChip(label = labelFor(d), selected = difficulty == d) { difficulty = d }
                }
            }

            Button(
                onClick = { onStart(playerCount, difficulty) },
                colors = ButtonDefaults.buttonColors(containerColor = AgonColors.Gold),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("ابدأ المباراة", color = AgonColors.WoodDark, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onPlayWithFriends) {
                Text("اللعب مع الأصدقاء (WiFi Direct / Hotspot)", color = AgonColors.GoldBright)
            }
        }
    }
}

private fun labelFor(d: AIDifficulty) = when (d) {
    AIDifficulty.EASY -> "سهل"
    AIDifficulty.MEDIUM -> "متوسط"
    AIDifficulty.HARD -> "صعب"
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AgonColors.Gold else AgonColors.FeltGreenMid
    val fg = if (selected) AgonColors.WoodDark else AgonColors.TileIvory
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontWeight = FontWeight.Medium)
    }
}
