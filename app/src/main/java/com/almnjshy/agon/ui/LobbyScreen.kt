package com.almnjshy.agon.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.almnjshy.agon.engine.AIDifficulty
import com.almnjshy.agon.network.*
import com.almnjshy.agon.ui.theme.AgonColors

private sealed interface LobbyStep {
    data object ChooseMode : LobbyStep
    data object HotspotHostSetup : LobbyStep
    data object HotspotHostWaiting : LobbyStep
    data object HotspotJoin : LobbyStep
    data object WifiDirectPeers : LobbyStep
    data object WifiDirectHostSetup : LobbyStep
    data object Waiting : LobbyStep
}

/**
 * Local-network multiplayer lobby: pick WiFi Direct or Hotspot, then host or join.
 * `vm` is kept alive by the caller and reused for [GameScreen]'s network variant once
 * the round actually starts.
 */
@Composable
fun LobbyScreen(
    vm: NetworkGameViewModel,
    onGameStarting: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf<LobbyStep>(LobbyStep.ChooseMode) }
    var playerCount by remember { mutableStateOf(4) }
    var localName by remember { mutableStateOf("Host") }
    var difficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }

    val wifiDirect = remember { WifiDirectManager(context) }
    val hotspot = remember { HotspotManager(context) }
    val nsd = remember { NsdHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            wifiDirect.stop()
            hotspot.stop()
            nsd.stop()
        }
    }

    val permissionsNeeded = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }.toTypedArray()
    }
    var pendingPermissionAction by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            when (pendingPermissionAction) {
                "wifi_direct" -> {
                    wifiDirect.start()
                    wifiDirect.startDiscovery()
                    step = LobbyStep.WifiDirectPeers
                }
                "nsd_join" -> nsd.discoverService()
            }
        }
        pendingPermissionAction = null
    }

    val connectedNames by vm.connectedPlayerNames.collectAsState()
    val clientReady by vm.clientReady.collectAsState()
    val clientError by vm.clientError.collectAsState()
    val hotspotCreds by hotspot.credentials.collectAsState()
    val discoveredHost by nsd.discoveredHost.collectAsState()
    val p2pPeers by wifiDirect.peers.collectAsState()
    val p2pInfo by wifiDirect.connectionInfo.collectAsState()

    // Once a WiFi Direct connection forms, whoever is the negotiated group owner hosts.
    LaunchedEffect(p2pInfo) {
        val info = p2pInfo ?: return@LaunchedEffect
        if (!info.groupFormed) return@LaunchedEffect
        if (info.isGroupOwner) {
            step = LobbyStep.WifiDirectHostSetup
        } else {
            val hostIp = info.groupOwnerAddress?.hostAddress
            if (hostIp != null) {
                vm.joinGame(hostIp, localName)
                step = LobbyStep.Waiting
            }
        }
    }

    LaunchedEffect(discoveredHost) {
        val ip = discoveredHost ?: return@LaunchedEffect
        vm.joinGame(ip, localName)
        step = LobbyStep.Waiting
    }

    LaunchedEffect(clientReady) {
        if (clientReady) onGameStarting()
    }

    Box(Modifier.fillMaxSize().background(AgonColors.FeltGreenDark)) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .background(AgonColors.FeltGreenMid.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(24.dp)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                LobbyStep.ChooseMode -> ChooseModeContent(
                    onWifiDirect = {
                        pendingPermissionAction = "wifi_direct"
                        permissionLauncher.launch(permissionsNeeded)
                    },
                    onHotspotHost = { step = LobbyStep.HotspotHostSetup },
                    onHotspotJoin = { step = LobbyStep.HotspotJoin },
                    onCancel = onCancel
                )

                LobbyStep.WifiDirectPeers -> WifiDirectPeersContent(
                    peers = p2pPeers,
                    onRefresh = { wifiDirect.startDiscovery() },
                    onSelect = { device -> wifiDirect.connectTo(device) {} },
                    onBack = { step = LobbyStep.ChooseMode }
                )

                LobbyStep.WifiDirectHostSetup -> PlayerCountAndNameSetup(
                    playerCount = playerCount,
                    onPlayerCountChange = { playerCount = it },
                    name = localName,
                    onNameChange = { localName = it },
                    difficulty = difficulty,
                    onDifficultyChange = { difficulty = it },
                    onConfirm = {
                        vm.startHosting(playerCount, localName, difficulty)
                        step = LobbyStep.Waiting
                    }
                )

                LobbyStep.HotspotHostSetup -> PlayerCountAndNameSetup(
                    playerCount = playerCount,
                    onPlayerCountChange = { playerCount = it },
                    name = localName,
                    onNameChange = { localName = it },
                    difficulty = difficulty,
                    onDifficultyChange = { difficulty = it },
                    onConfirm = {
                        hotspot.start()
                        nsd.registerService()
                        vm.startHosting(playerCount, localName, difficulty)
                        step = LobbyStep.HotspotHostWaiting
                    }
                )

                LobbyStep.HotspotHostWaiting -> HotspotHostWaitingContent(
                    credentials = hotspotCreds,
                    connectedNames = connectedNames,
                    onStart = { vm.hostStartNewRound(); onGameStarting() }
                )

                LobbyStep.HotspotJoin -> HotspotJoinContent(
                    name = localName,
                    onNameChange = { localName = it },
                    onReady = {
                        pendingPermissionAction = "nsd_join"
                        permissionLauncher.launch(permissionsNeeded)
                    },
                    error = clientError,
                    onBack = { step = LobbyStep.ChooseMode }
                )

                LobbyStep.Waiting -> WaitingContent(isHost = vm.isHost, connectedNames = connectedNames)
            }
        }
    }
}

@Composable
private fun ChooseModeContent(
    onWifiDirect: () -> Unit,
    onHotspotHost: () -> Unit,
    onHotspotJoin: () -> Unit,
    onCancel: () -> Unit
) {
    Text("اللعب مع الأصدقاء", color = AgonColors.GoldBright, fontWeight = FontWeight.Bold)
    Text(
        "بدون إنترنت — يشتغل عبر WiFi Direct أو نقطة اتصال محلية.",
        color = AgonColors.TileIvory,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    GoldButton("WiFi Direct (بحث عن أجهزة قريبة)", onWifiDirect)
    GoldButton("استضف عبر Hotspot", onHotspotHost)
    GoldButton("انضم عبر Hotspot", onHotspotJoin)
    TextButton(onClick = onCancel) { Text("رجوع", color = AgonColors.TileIvory) }
}

@Composable
private fun WifiDirectPeersContent(
    peers: List<android.net.wifi.p2p.WifiP2pDevice>,
    onRefresh: () -> Unit,
    onSelect: (android.net.wifi.p2p.WifiP2pDevice) -> Unit,
    onBack: () -> Unit
) {
    Text("الأجهزة القريبة", color = AgonColors.GoldBright, fontWeight = FontWeight.Bold)
    if (peers.isEmpty()) {
        Text("لا توجد أجهزة بعد — تأكد إن أصدقاءك فتحوا نفس الشاشة.", color = AgonColors.TileIvory)
    }
    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
        items(peers) { device ->
            Button(
                onClick = { onSelect(device) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgonColors.FeltGreenLight)
            ) { Text(device.deviceName) }
        }
    }
    GoldButton("تحديث البحث", onRefresh)
    TextButton(onClick = onBack) { Text("رجوع", color = AgonColors.TileIvory) }
}

@Composable
private fun PlayerCountAndNameSetup(
    playerCount: Int,
    onPlayerCountChange: (Int) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    difficulty: AIDifficulty,
    onDifficultyChange: (AIDifficulty) -> Unit,
    onConfirm: () -> Unit
) {
    Text("إعداد الاستضافة", color = AgonColors.GoldBright, fontWeight = FontWeight.Bold)
    OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("اسمك") })
    Text("عدد اللاعبين الكلي (أنت + أصدقاء + AI للمقاعد الشاغرة)", color = AgonColors.TileIvory)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2, 3, 4).forEach { n ->
            Chip(label = "$n", selected = playerCount == n) { onPlayerCountChange(n) }
        }
    }
    Text("صعوبة الذكاء الاصطناعي للمقاعد الشاغرة", color = AgonColors.TileIvory)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AIDifficulty.entries.forEach { d ->
            Chip(label = d.name, selected = difficulty == d) { onDifficultyChange(d) }
        }
    }
    GoldButton("ابدأ الاستضافة", onConfirm)
}

@Composable
private fun HotspotHostWaitingContent(
    credentials: HotspotCredentials?,
    connectedNames: List<String>,
    onStart: () -> Unit
) {
    Text("بانتظار الأصدقاء", color = AgonColors.GoldBright, fontWeight = FontWeight.Bold)
    if (credentials != null) {
        Text("اسم الشبكة: ${credentials.ssid}", color = AgonColors.TileIvory)
        Text("كلمة المرور: ${credentials.password}", color = AgonColors.TileIvory)
        Text(
            "اطلب من أصدقاءك الاتصال بهذه الشبكة من إعدادات WiFi، ثم فتح Agon واختيار \"انضم\".",
            color = AgonColors.TileIvory.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    } else {
        Text("يتم إنشاء نقطة الاتصال...", color = AgonColors.TileIvory)
    }
    Text("المنضمون: ${if (connectedNames.isEmpty()) "لا أحد بعد" else connectedNames.joinToString()}", color = AgonColors.GoldBright)
    GoldButton("ابدأ المباراة", onStart)
}

@Composable
private fun HotspotJoinContent(
    name: String,
    onNameChange: (String) -> Unit,
    onReady: () -> Unit,
    error: String?,
    onBack: () -> Unit
) {
    Text("انضم عبر Hotspot", color = AgonColors.GoldBright, fontWeight = FontWeight.Bold)
    Text(
        "اتصل أولًا بشبكة WiFi المضيف من إعدادات هاتفك، ثم اضغط بحث.",
        color = AgonColors.TileIvory,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("اسمك") })
    error?.let { Text(it, color = AgonColors.PlayerRed) }
    GoldButton("ابحث عن المضيف", onReady)
    TextButton(onClick = onBack) { Text("رجوع", color = AgonColors.TileIvory) }
}

@Composable
private fun WaitingContent(isHost: Boolean, connectedNames: List<String>) {
    CircularProgressIndicator(color = AgonColors.Gold)
    Text(
        if (isHost) "بانتظار انضمام الأصدقاء..." else "بانتظار المضيف...",
        color = AgonColors.TileIvory
    )
    if (isHost && connectedNames.isNotEmpty()) {
        Text("المنضمون: ${connectedNames.joinToString()}", color = AgonColors.GoldBright)
    }
}

@Composable
private fun GoldButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = AgonColors.Gold),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, color = AgonColors.WoodDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AgonColors.Gold else AgonColors.FeltGreenLight
    val fg = if (selected) AgonColors.WoodDark else AgonColors.TileIvory
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg)
    }
}
