package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.measureTimeMillis

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

    println("\n****************************\n")

    // TODO Flows are sequential
    flowsAreSequential()

    println("\n****************************\n")

    // TODO Flow context
    flowContext()

    println("\n****************************\n")

    // TODO Buffering
    buffering()

    println("\n****************************\n")

    // TODO Composing multiple flows
    composingMultipleFlows()
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

fun flowsAreSequential() {
    /*
        Each individual collection of a flow is performed sequentially unless special operators
        that operate on multiple flows are used. The collection works directly in the coroutine
        that calls a terminal operator. No new coroutines are launched by default.
        Each emitted value is processed by all the intermediate operators from upstream to downstream
        and is then delivered to the terminal operator after.

        See the following example that filters the even integers and maps them to strings:
     */
    println("flowsAreSequential start")

    runBlocking {
        (1..5).asFlow()
            .filter {
                println("Filter $it")
                it % 2 == 0
            }
            .map {
                println("Map $it")
                "string $it"
            }.collect {
                println("Collect $it")
            }
    }
}

fun flowContext() {
    /*
        Collection of a flow always happens in the context of the calling coroutine.
        For example, if there is a simple flow, then the following code runs in the context specified
        by the author of this code, regardless of the implementation details of the simple flow:

         withContext(context) {
            simple().collect { value ->
                println(value) // run in the specified context
            }
        }

        This property of a flow is called context preservation.

        So, by default, code in the flow { ... } builder runs in the context that is provided by
        a collector of the corresponding flow. For example, consider the implementation of a
        flowContextSample function that prints the thread it is called on and emits three numbers.
     */

    runBlocking {
        flowContextSample().collect { value -> log("Collected $value") }
    }

    /*
        Since flowContextSample().collect is called from the main thread,
        the body of flowContextSample's flow is also called in the main thread.
        This is the perfect default for fast-running or asynchronous code that does not care about
        the execution context and does not block the caller.
     */

    println("\n--------------\n")

    /*  TODO 1. Wrong emission withContext
        However, the long-running CPU-consuming code might need to be executed in the context of
        Dispatchers.Default and UI-updating code might need to be executed in the context of
        Dispatchers.Main. Usually, withContext is used to change the context in the code using
        Kotlin coroutines, but code in the flow { ... } builder has to honor the context preservation
        property and is not allowed to emit from a different context.
     */
    runBlocking {
        // This code produces the following exception: Flow invariant is violated
        // wrongEmissionWithContext().collect { value -> println(value) }
    }

    /*  TODO 2. flowOn operator
        The exception refers to the flowOn function that shall be used to change the context of the
        flow emission. The correct way to change the context of a flow is shown in the example below,
        which also prints the names of the corresponding threads to show how it all works:
    */

    runBlocking {
        log("flowOnOperator start")

        flowOnOperator().collect { value ->
            log("Collected $value")
        }
    }

    /*
        Notice how flow { ... } works in the background thread, while collection happens in the main thread.

        Another thing to observe here is that the flowOn operator has changed the default sequential
        nature of the flow. Now collection happens in one coroutine ("coroutine#1") and
        emission happens in another coroutine ("coroutine#2") that is running in another thread
        concurrently with the collecting coroutine. The flowOn operator creates another coroutine
        for an upstream flow when it has to change the CoroutineDispatcher in its context.

        [DefaultDispatcher-worker-1 @coroutine#2] Emitting 1
        [main @coroutine#1] Collected 1
        [DefaultDispatcher-worker-1 @coroutine#2] Emitting 2
        [main @coroutine#1] Collected 2
        [DefaultDispatcher-worker-1 @coroutine#2] Emitting 3
        [main @coroutine#1] Collected 3
     */
}

fun flowContextSample(): Flow<Int> = flow {
    log("Started flowContextSample flow")
    for (i in 1..3) {
        emit(i)
    }
}

fun wrongEmissionWithContext(): Flow<Int> = flow {
    // The WRONG way to change context for CPU-consuming code in flow builder
    println("wrongEmissionWithContext start")

    withContext(Dispatchers.Default) {
        for (i in 1..3) {
            Thread.sleep(100) // pretend we are computing it in CPU-consuming way
            emit(i) // emit next value
        }
    }
}

fun flowOnOperator(): Flow<Int> = flow {
    for (i in 1..3) {
        Thread.sleep(100) // pretend we are computing it in CPU-consuming way
        log("Emitting $i")
        emit(i) // emit next value
    }
}.flowOn(Dispatchers.Default) // RIGHT way to change context for CPU-consuming code in flow builder

fun buffering() {
    /*
        Running different parts of a flow in different coroutines can be helpful from the standpoint
        of the overall time it takes to collect the flow, especially when long-running asynchronous
        operations are involved. For example, consider a case when the emission by a bufferingSample flow is slow,
        taking 100 ms to produce an element; and collector is also slow, taking 300 ms to process an element.
        Let's see how long it takes to collect such a flow with three numbers:
     */

    println("buffering start")

    runBlocking {
        val time = measureTimeMillis {
            bufferingSample().collect { value ->
                delay(300) // pretend we are processing it for 300 ms
                println("Collected value : $value")
            }
        }
        println("Collected in $time ms")
    }

    println("\n--------------\n")

    /*
        It produces something like this, with the whole collection taking around 1200 ms (three numbers, 400 ms for each).

        We can use a buffer operator on a flow to run emitting code of the simple flow concurrently
        with collecting code, as opposed to running them sequentially:
     */

    println("Buffer operator start  :")

    runBlocking {
        val time = measureTimeMillis {
            bufferingSample()
                .buffer() // buffer emissions, don't wait
                .collect { value ->
                    delay(300) // pretend we are processing it for 300 ms
                    println("Collected value : $value")
                }
        }
        println("Collected in $time ms")
    }

    /*
        It produces the same numbers just faster, as we have effectively created a processing pipeline,
        having to only wait 100 ms for the first number and then spending only 300 ms to process each number.
        This way it takes around 1000 ms to run.

        Note that the flowOn operator uses the same buffering mechanism when it has to change a CoroutineDispatcher,
        but here we explicitly request buffering without changing the execution context.
     */

    println("\n--------------\n")

    /* TODO 1. Conflation
       When a flow represents partial results of the operation or operation status updates,
       it may not be necessary to process each value, but instead, only most recent ones.
       In this case, the conflate operator can be used to skip intermediate values when a collector
       is too slow to process them. Building on the previous example:
    */

    println("Conflation start")

    runBlocking {
        val time = measureTimeMillis {
            bufferingSample()
                .conflate() // conflate emissions, don't process each one
                .collect { value ->
                    delay(300) // pretend we are processing it for 300 ms
                    println(value)
                }
        }
        println("Collected in $time ms")
    }

    /*
        We see that while the first number was still being processed the second,
        and third were already produced, so the second one was conflated and only
        the most recent (the third one) was delivered to the collector.
     */

    println("\n--------------\n")

    /* TODO 2. Processing the latest value
       Conflation is one way to speed up processing when both the emitter and collector are slow.
       It does it by dropping emitted values. The other way is to cancel a slow collector and restart
       it every time a new value is emitted. There is a family of xxxLatest operators that perform
       the same essential logic of a xxx operator, but cancel the code in their block on a new value.
       Let's try changing conflate to collectLatest in the previous example:
    */

    println("Processing the latest value start")

    runBlocking {
        val time = measureTimeMillis {
            bufferingSample()
                .collectLatest { value -> // cancel & restart on the latest value
                    println("Collecting $value")
                    delay(300) // pretend we are processing it for 300 ms
                    println("Done $value")
                }
        }
        println("Collected in $time ms")
    }

    /*
        Since the body of collectLatest takes 300 ms, but new values are emitted every 100 ms,
        we see that the block is run on every value, but completes only for the last value.
     */
}

fun bufferingSample(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // pretend we are asynchronously waiting 100 ms
        println("Emiting value : $i")
        emit(i) // emit next value
    }
}

fun composingMultipleFlows() {

    // There are lots of ways to compose multiple flows.

    /* TODO 1. Zip
       Just like the Sequence.zip extension function in the Kotlin standard library,
       flows have a zip operator that combines the corresponding values of two flows:
     */
    println("Zip operator : ")

    runBlocking {
        val nums = (1..3).asFlow() // numbers 1..3
        val strs = flowOf("one", "two", "three", "four") // strings
        nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string
            .collect { println(it) } // collect and print
    }

    println("\n--------------\n")

    /* TODO 2. Combine
       When flow represents the most recent value of a variable or operation
       (see also the related section on conflation), it might be needed to perform a computation
       that depends on the most recent values of the corresponding flows and to recompute it whenever
       any of the upstream flows emit a value. The corresponding family of operators is called combine.

       For example, if the numbers in the previous example update every 300ms, but strings update every 400 ms,
       then zipping them using the zip operator will still produce the same result,
       albeit results that are printed every 400 ms:

       We use a onEach intermediate operator in this example to delay each element and make the code
       that emits sample flows more declarative and shorter.
     */
    println("Zip operator : ")

    runBlocking {
        val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3 every 300 ms
        val strs = flowOf("one", "two", "three", "four").onEach { delay(400) } // strings every 400 ms
        val startTime = System.currentTimeMillis() // remember the start time
        nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string with "zip"
            .collect { value -> // collect and print
                println("$value at ${System.currentTimeMillis() - startTime} ms from start")
            }
    }

    println("\n--------------\n")

    // However, when using a combine operator here instead of a zip:

    println("Combine operator : ")

    runBlocking {
        val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3 every 300 ms
        val strs = flowOf("one", "two", "three", "four").onEach { delay(400) } // strings every 400 ms
        val startTime = System.currentTimeMillis() // remember the start time
        nums.combine(strs) { a, b -> "$a -> $b" } // compose a single string with "combine"
            .collect { value -> // collect and print
                println("$value at ${System.currentTimeMillis() - startTime} ms from start")
            }
    }
}