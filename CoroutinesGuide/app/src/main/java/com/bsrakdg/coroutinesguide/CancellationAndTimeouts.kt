package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.*

/** https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html
 *  This section covers coroutine cancellation and timeouts.
 */

fun main() {

    // TODO Cancelling coroutine execution
    cancellingCoroutineExecution()

    println("\n****************************\n")

    // TODO Cancellation is cooperative
    cancellationIsCooperative()

    println("\n****************************\n")

    // TODO Making computation code cancellable
    makingComputationCodeCancellable()

    println("\n****************************\n")

    // TODO Closing resources with finally
    closingResourcesWithFinally()

    println("\n****************************\n")

    // TODO Run non-cancellable block
    runNonCancellableBlock()

    println("\n****************************\n")

    // TODO Timeout
    timeout()
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

    /*
        As soon as main invokes job.cancel, we don't see any output from the other coroutine
        because it was cancelled. There is also a Job extension function cancelAndJoin
        that combines cancel and join invocations.
     */
}

fun cancellationIsCooperative() {
    /*
        Coroutine cancellation is cooperative. A coroutine code has to cooperate to be cancellable.
        All the suspending functions in kotlinx.coroutines are cancellable.
        They check for cancellation of coroutine and throw CancellationException when cancelled.
        However, if a coroutine is working in a computation and does not check for cancellation,
        then it cannot be cancelled, like the following example shows:
     */
    cancellationIsCooperativeSample()
}

fun cancellationIsCooperativeSample() = runBlocking {
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) { // computation loop, just wastes CPU
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")

    /*
        Run it to see that it continues to print "I'm sleeping" even after cancellation until
        the job completes by itself after five iterations.
     */
}

fun makingComputationCodeCancellable() {
    /*
        There are two approaches to making computation code cancellable.
        The first one is to periodically invoke a suspending function that checks for cancellation.
        There is a yield function that is a good choice for that purpose.
        The other one is to explicitly check the cancellation status.
        Let us try the latter approach.

        Replace while (i < 5) in the previous example with while (isActive) and rerun it.
     */

    makingComputationCodeCancellableSample()
}

fun makingComputationCodeCancellableSample() = runBlocking {
    println("makingComputationCodeCancellable start")

    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (isActive) { // cancellable computation loop
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")

    println("makingComputationCodeCancellable end")

    /*
        As you can see, now this loop is cancelled. isActive is an extension property available
        inside the coroutine via the CoroutineScope object.
     */
}

fun closingResourcesWithFinally() {
    /*
        Cancellable suspending functions throw CancellationException on cancellation which can be
        handled in the usual way. For example, try {...} finally {...} expression and Kotlin use
        function execute their finalization actions normally when a coroutine is cancelled:
     */

    closingResourcesWithFinallySample()
}

fun closingResourcesWithFinallySample() = runBlocking {
    println("closingResourcesWithFinallySample start")

    val job = launch {
        try {
            repeat(1000) { i ->
                println("job: I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            println("job: I'm running finally")
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")

    println("closingResourcesWithFinallySample end")

    /*
        Both join and cancelAndJoin wait for all finalization actions to complete,
         so the example above produces the following output:
     */
}

fun runNonCancellableBlock() {

    /*
        Any attempt to use a suspending function in the finally block of the previous example causes
        CancellationException, because the coroutine running this code is cancelled.
        Usually, this is not a problem, since all well-behaving closing operations
        (closing a file, cancelling a job, or closing any kind of a communication channel) are
        usually non-blocking and do not involve any suspending functions. However,
        in the rare case when you need to suspend in a cancelled coroutine you can wrap
        the corresponding code in withContext(NonCancellable) {...} using withContext function and
        NonCancellable context as the following example shows:
     */

    runNonCancellableBlockSample()
}
fun runNonCancellableBlockSample() = runBlocking {
    println("runNonCancellableBlockSample start")

    val job = launch {
        try {
            repeat(1000) { i ->
                println("job: I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            withContext(NonCancellable) {
                println("job: I'm running finally")
                delay(1000L)
                println("job: And I've just delayed for 1 sec because I'm non-cancellable")
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")

    println("runNonCancellableBlockSample end")
}

fun timeout() {
    /*
        The most obvious practical reason to cancel execution of a coroutine is because its execution
        time has exceeded some timeout. While you can manually track the reference to the corresponding
        Job and launch a separate coroutine to cancel the tracked one after delay,
        there is a ready to use withTimeout function that does it. Look at the following example:

         Look at timeoutSample1() (throws TimeoutCancellationException)

        The TimeoutCancellationException that is thrown by withTimeout is a subclass of CancellationException.
        We have not seen its stack trace printed on the console before.
        That is because inside a cancelled coroutine CancellationException is considered to be a normal reason for coroutine completion.
        However, in this example we have used withTimeout right inside the function.

        Since cancellation is just an exception, all resources are closed in the usual way.
        You can wrap the code with timeout in a try {...} catch (e: TimeoutCancellationException) {...} block
        if you need to do some additional action specifically on any kind of timeout or use the withTimeoutOrNull
        function that is similar to withTimeout but returns null on timeout instead of throwing an exception:
     */

    timeoutSample2()
}

fun timeoutSample1() = runBlocking {
    println("timeoutSample1 start")

    withTimeout(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
    println("timeoutSample1 end")
}

fun timeoutSample2() = runBlocking {
    println("timeoutSample2 start")

    val result = withTimeoutOrNull(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
        "Done" // will get cancelled before it produces this result
    }
    println("Result is $result")

    println("timeoutSample2 end")

}