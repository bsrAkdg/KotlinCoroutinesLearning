package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.*
import kotlin.concurrent.thread

/** https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html
 *  This section covers coroutine cancellation and timeouts.
 */

fun main() {

    // TODO Cancelling coroutine execution
    cancellingCoroutineExecution()
}

fun cancellingCoroutineExecution() {
    /*
        In a long-running application you might need fine-grained control on your background coroutines.
        For example, a user might have closed the page that launched a coroutine and now its result
        is no longer needed and its operation can be cancelled. The launch function returns a Job
        that can be used to cancel the running coroutine:
     */
    println("cancellingCoroutineExecution start")
    cancellingCoroutineExecutionSample()
    println("cancellingCoroutineExecution end")
}

fun cancellingCoroutineExecutionSample() = runBlocking {
    val job = launch {
        repeat(1000) { i ->
            println("job: I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancel() // cancels the job
    job.join() // waits for job's completion
    println("main: Now I can quit.")
}