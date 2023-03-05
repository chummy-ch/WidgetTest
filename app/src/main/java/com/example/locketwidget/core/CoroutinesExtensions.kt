package com.example.locketwidget.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend inline fun <T> onIO(crossinline block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO) { block.invoke(this@withContext) }
}

suspend inline fun <T> onDefault(crossinline block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Default) { block.invoke(this@withContext) }
}

suspend inline fun <T> onMain(crossinline block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Main.immediate) { block.invoke(this@withContext) }
}
