package com.breadwallet.ui.flowbind

import android.widget.Switch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@UseExperimental(ExperimentalCoroutinesApi::class)
fun Switch.checked(): Flow<Boolean> =
    callbackFlow {
        setOnCheckedChangeListener { _, isChecked ->
            offer(isChecked)
        }
        awaitClose {
            setOnCheckedChangeListener(null)
        }
    }
