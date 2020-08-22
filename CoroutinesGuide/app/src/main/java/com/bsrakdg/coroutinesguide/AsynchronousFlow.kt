package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** https://kotlinlang.org/docs/reference/coroutines/flow.html#asynchronous-flow
 *  A suspending function asynchronously returns a single value, but how can we return multiple
 *  asynchronously computed values? This is where Kotlin Flows come in.
 */

fun main() {

    // TODO Representing multiple values
    representingMultipleValues()

    println("\n****************************\n")

    // TODO Flows are cold
    flowsAreCold()

    println("\n****************************\n")

    // TODO Flow cancellation basics
    flowCancellationBasics()
}

fun representingMultipleValues() {
    /*
        Multiple values can be represented in Kotlin using collections.
        For example, we can have a simple function that returns a List of three numbers and then print them all using forEach:
     */

    simple().forEach { value -> println(value) }

    println("\n--------------\n")

    /*  TODO 1. Sequences
        If we are computing the numbers with some CPU-consuming blocking code (each computation taking 100ms),
        then we can represent the numbers using a Sequence:

        This code outputs the same numbers, but it waits 100ms before printing each one.
        However, this computation blocks the main thread that is running the code.
     */
    sequencesSample().forEach { value -> println(value) }

    println("\n--------------\n")

    /*  TODO 2. Suspending functions
        When these values are computed by asynchronous code we can mark the suspendingFunctionsSample function with
        a suspend modifier, so that it can perform its work without blocking and return the result as a list:
     */
    runBlocking {
        suspendingFunctionsSample().forEach { value -> println(value) }
        // This code prints the numbers after waiting for a second.
    }

    println("\n--------------\n")

    /*  TODO 2. Flows
        Using the List<Int> result type, means we can only return all the values at once.
        To represent the stream of values that are being asynchronously computed,
        we can use a Flow<Int> type just like we would use the Sequence<Int> type for synchronously computed values:

        Sequence -> synchronously
        Flow -> asynchronously
    */
    runBlocking {

        // Launch a concurrent coroutine to check if the main thread is blocked
        launch {
            for (k in 1..3) {
                println("I'm not blocked $k")
                delay(1000)
            }
        }
        // Collect the flow
        flowSample().collect { value -> println(value) }

        /*
            This code waits 1000ms before printing each number without blocking the main thread.
            This is verified by printing "I'm not blocked" every 1000ms from a separate coroutine
            that is running in the main thread (look at log)

            Notice the following differences in the code with the Flow from the earlier examples:
            - A builder function for Flow type is called flow.
            - Code inside the flow { ... } builder block can suspend.
            - The flowSample function is no longer marked with suspend modifier.
            - Values are emitted from the flow using emit function.
            - Values are collected from the flow using collect function.

            We can replace delay with Thread.sleep in the body of flowSample's flow { ... } and
            see that the main thread is blocked in this case.
         */
    }
}

fun simple(): List<Int> = listOf(1, 2, 3)

fun sequencesSample(): Sequence<Int> = sequence { // sequence builder
    println("sequencesSample start")

    for (i in 1..3) {
        Thread.sleep(100) // pretend we are computing it
        yield(i) // yield next value
    }
}

suspend fun suspendingFunctionsSample(): List<Int> {
    println("suspendingFunctionsSample start")

    delay(1000) // pretend we are doing something asynchronous here
    return listOf(1, 2, 3)
}

fun flowSample(): Flow<Int> = flow { // flow builder
    println("flowSample start")

    for (i in 1..3) {
        delay(1000) // pretend we are doing something useful here
        emit(i) // emit next value
    }
}

fun flowsAreCold() {
    /*
        Flows are cold streams similar to sequences — the code inside a flow builder does not run
        until the flow is collected. This becomes clear in the following example:
     */

    runBlocking {
        println("Calling flowsAreColdSample function...")
        val flow = flowsAreColdSample()
        println("Calling collect...")
        flow.collect { value -> println(value) }
        println("Calling collect again...")
        flow.collect { value -> println(value) }
    }

    /*
        This is a key reason the flowsAreColdSample function (which returns a flow) is not marked with suspend modifier.
        By itself, flowsAreColdSample() call returns quickly and does not wait for anything.
        The flow starts every time it is collected, that is why we see "Flow started" when we call collect again.
     */
}

fun flowsAreColdSample(): Flow<Int> = flow {
    println("Flow started")
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}

fun flowCancellationBasics() {
    /*
        Flow adheres to the general cooperative cancellation of coroutines. As usual,
         flow collection can be cancelled when the flow is suspended in a cancellable suspending
         function (like delay). The following example shows how the flow gets cancelled on a timeout
         when running in a withTimeoutOrNull block and stops executing its code:
     */

    println("flowCancellationBasics start")

    runBlocking {
        withTimeoutOrNull(250) { // Timeout after 250ms
            flowCancellationBasicsSample().collect { value -> println(value) }
        }
        println("Done")
    }
}

fun flowCancellationBasicsSample(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)
        println("Emitting $i")
        emit(i)
    }
}

