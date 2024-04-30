package org.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

fun CoroutineScope.launchRepeatingTask(minutesInterval: Long, block: suspend () -> Unit) {
    launch {
        while (true) {
            block()
            delay(TimeUnit.MINUTES.toMillis(minutesInterval))
        }
    }
}