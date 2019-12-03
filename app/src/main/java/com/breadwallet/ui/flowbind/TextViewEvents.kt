package com.breadwallet.ui.flowbind

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

@UseExperimental(ExperimentalCoroutinesApi::class)
fun TextView.editorActions(): Flow<Int> =
    callbackFlow {
        setOnEditorActionListener { _, actionId, _ ->
            offer(actionId)
        }
        awaitClose { setOnEditorActionListener(null) }
    }.flowOn(Dispatchers.Main)

@UseExperimental(ExperimentalCoroutinesApi::class)
fun TextView.textChanges(): Flow<String> =
    callbackFlow {
        val listener = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                offer(text.toString())
            }
        }
        addTextChangedListener(listener)
        awaitClose { removeTextChangedListener(listener) }
    }.flowOn(Dispatchers.Main)
