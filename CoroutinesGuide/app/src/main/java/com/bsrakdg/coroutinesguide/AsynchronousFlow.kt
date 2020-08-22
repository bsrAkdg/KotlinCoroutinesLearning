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

    println("\n****************************\n")

    // TODO Flow builders
    flowBuilders()

    println("\n****************************\n")

    // TODO Intermediate flow operators
    intermediateFlowOperators()

    println("\n****************************\n")

    // TODO Terminal flow operators
    terminalFlowOperators()
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

fun flowBuilders() {
    /*
        The flow { ... } builder from the previous examples is the most basic one. There are other
        builders for easier declaration of flows:

        - flowOf builder that defines a flow emitting a fixed set of values.
        - Various collections and sequences can be converted to flows using .asFlow() extension functions.

        So, the example that prints the numbers from 1 to 3 from a flow can be written as:
     */
    runBlocking {
        // Convert an integer range to a flow
        println("flowBuilders start")

        (1..3).asFlow().collect { value -> println(value) }
    }
}

fun intermediateFlowOperators() {
    /*
        Flows can be transformed with operators, just as you would with collections and sequences.
        Intermediate operators are applied to an upstream flow and return a downstream flow.
        These operators are cold, just like flows are. A call to such an operator is not a suspending function itself.
        It works quickly, returning the definition of a new transformed flow.

        The basic operators have familiar names like map and filter.
        The important difference to sequences is that blocks of code inside these operators can call suspending functions.

        For example, a flow of incoming requests can be mapped to the results with the map operator,
        even when performing a request is a long-running operation that is implemented by a suspending function:
     */
    println("intermediateFlowOperators start")

    runBlocking {
        (1..3).asFlow() // a flow of requests
            .map { request -> performRequest(request) }
            .collect { response -> println(response) }
    }

    println("\n--------------\n")

    // It produces the following three lines, each line appearing after each second.

    /*  TODO Transform operator
        Among the flow transformation operators, the most general one is called transform.
        It can be used to imitate simple transformations like map and filter, as well as implement
        more complex transformations. Using the transform operator,
        we can emit arbitrary values an arbitrary number of times.

        For example, using transform we can emit a string before performing
        a long-running asynchronous request and follow it with a response:
    */
    println("Transform operator start")

    runBlocking {
        (1..3).asFlow() // a flow of requests
            .transform { request ->
                emit("Making request $request")
                emit(performRequest(request))
            }
            .collect { response -> println(response) }
    }

    println("\n--------------\n")

    /* TODO Size-limiting operators
       Size-limiting intermediate operators like take cancel the execution of the flow when
       the corresponding limit is reached. Cancellation in coroutines is always performed by
       throwing an exception, so that all the resource-management functions (like try { ... } finally { ... } blocks)
       operate normally in case of cancellation:
     */

    println("Size-limiting operators start")

    runBlocking {
        numbers()
            .take(2) // take only the first two
            .collect { value -> println(value) }

        // The output of this code clearly shows that the execution of the flow { ... } body in
        // the numbers() function stopped after emitting the second number.
    }
}

suspend fun performRequest(request: Int): String {
    delay(1000) // imitate long-running asynchronous work
    return "response $request"
}

fun numbers(): Flow<Int> = flow {
    try {
        emit(1)
        emit(2)
        println("This line will not execute")
        emit(3)
    } finally {
        println("Finally in numbers")
    }
}

fun terminalFlowOperators() {
    /*
        Terminal operators on flows are suspending functions that start a collection of the flow.
        The collect operator is the most basic one, but there are other terminal operators, which can make it easier:
        - Conversion to various collections like toList and toSet.
        - Operators to get the first value and to ensure that a flow emits a single value.
        - Reducing a flow to a value with reduce and fold.
        For example:
     */

    println("terminalFlowOperators start")

    runBlocking {
        val sum = (1..5).asFlow()
            .map { it * it } // squares of numbers from 1 to 5
            .reduce { a, b -> a + b } // sum them (terminal operator)
        println(sum) // Prints a single number:
    }
}