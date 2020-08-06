package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

/** https://kotlinlang.org/docs/reference/coroutines/basics.html
 *  This section covers basic coroutine concepts.
*/

suspend fun main() {

    // TODO Your first coroutine
    firstCoroutine()

    println("\n****************************\n")

    // TODO Bridging blocking and non-blocking worlds
    blockingNonBlockingWorlds()

    println("\n****************************\n")

    // TODO Waiting for a job
    waitingForAJob()

}

fun firstCoroutine() {
    /*
        Essentially, coroutines are light-weight threads. They are launched with launch coroutine
        builder in a context of some CoroutineScope. Here we are launching a new coroutine in
        the GlobalScope, meaning that the lifetime of the new coroutine is limited only by
        the lifetime of the whole application.
     */

    GlobalScope.launch {    // launch a new coroutine in background and continue
        println("Global sleep...")
        delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
        println("Global wake up...")
    }
    println("Main thread sleep...") // main thread continues while coroutine is delayed
    Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive
    println("Main thread wake up...")

    println("\n--------------\n")

    /*
        You can achieve the same result by replacing GlobalScope.launch { ... } with thread { ... },
        and delay(...) with Thread.sleep(...). Try it (don't forget to import kotlin.concurrent.thread).

        If you start by replacing GlobalScope.launch with thread, the compiler produces the following error:
        Error: Kotlin: Suspend functions are only allowed to be called from a coroutine or another suspend function

        That is because delay is a special suspending function that does not block a thread,
        but suspends the coroutine, and it can be only used from a coroutine.
     */

    thread {
        println("Global sleep...")
        Thread.sleep(1000L) // non-blocking delay for 1 second (default time unit is ms)
        println("Global wake up...")
    }
    println("Main thread sleep...")
    Thread.sleep(2000L)
    println("Main thread wake up...")

}

fun blockingNonBlockingWorlds() {
    /*
        The first example mixes non-blocking delay(...) and blocking Thread.sleep(...) in the same code.
        It is easy to lose track of which one is blocking and which one is not.
        Let's be explicit about blocking using the runBlocking coroutine builder:
     */

    GlobalScope.launch { // launch a new coroutine in background and continue
        println("Global sleep...")
        delay(2000L) // non-blocking delay for 1 second (default time unit is ms)
        println("Global wake up...")
    }

    println("Main thread start") // main thread continues here immediately

    runBlocking {     // but this expression blocks the main thread
        println("Run blocking start...")
        delay(2000L)  // ... while we delay for 2 seconds to keep JVM alive
        println("Run blocking finish...")
    }
    println("Main thread finish")

    println("\n--------------\n")

    /*
        This example can be also rewritten in a more idiomatic way,
        using runBlocking to wrap the execution of the main function:

        Here runBlocking<Unit> { ... } works as an adaptor that is used to start the top-level main coroutine.
        We explicitly specify its Unit return type, because a well-formed main function in Kotlin has to return Unit.
     */
    anotherRunBlockingSample()


    /*
        This is also a way to write unit tests for suspending functions:

        class MyTest {
            @Test
            fun testMySuspendingFunction() = runBlocking<Unit> {
                // here we can use suspending functions using any assertion style that we like
            }
        }
     */
}

fun anotherRunBlockingSample() = runBlocking<Unit> { // start main coroutine
    GlobalScope.launch { // launch a new coroutine in background and continue
        println("Global sleep...")
        delay(2000L) // non-blocking delay for 1 second (default time unit is ms)
        println("Global wake up...")
    }
    println("Run blocking sleep...") // main coroutine continues here immediately
    delay(2000L)      // delaying for 2 seconds to keep JVM alive
    println("Run blocking wake up...")
}

suspend fun waitingForAJob() {
    /*
        Delaying for a time while another coroutine is working is not a good approach.
        Let's explicitly wait (in a non-blocking way) until the background Job that we have launched is complete:

        Now the result is still the same, but the code of the main coroutine is not tied to
        the duration of the background job in any way. Much better.
     */

    val job = GlobalScope.launch { // launch a new coroutine and keep a reference to its Job
        println("Global sleep...")
        delay(10000L) // non-blocking delay for 1 second (default time unit is ms)
        println("Global wake up...")
    }
    println("Main thread start")
    job.join() // wait until child coroutine completes
    println("Main thread finish")
}