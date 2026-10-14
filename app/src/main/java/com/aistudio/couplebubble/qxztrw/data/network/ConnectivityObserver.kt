package com.aistudio.couplebubble.qxztrw.data.network

import kotlinx.coroutines.flow.Flow

/** Whether the device currently has a network with internet access. */
interface ConnectivityObserver {
    /** Emits the current state first, then every change. */
    val isOnline: Flow<Boolean>
}
