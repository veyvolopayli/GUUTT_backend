package org.example

import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

suspend fun startRepeatableTimerTask(minutesInterval: Long, block: suspend () -> Unit) {
    while (true) {
        block()
        delay(TimeUnit.MINUTES.toMillis(minutesInterval))
    }
}