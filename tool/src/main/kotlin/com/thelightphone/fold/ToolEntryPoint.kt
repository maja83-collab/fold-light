package com.thelightphone.fold

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

@EntryPoint
object ToolEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(
        serverData: StateFlow<LightServerData?>,
    ) {
        // Offline-first tool; no remote registration needed.
    }

    override suspend fun onPushNotification(
        data: ByteArray,
    ) {
        // No push handling.
    }
}
