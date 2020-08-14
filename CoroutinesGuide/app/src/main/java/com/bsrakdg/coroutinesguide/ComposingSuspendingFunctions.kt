package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

/** https://kotlinlang.org/docs/reference/coroutines/composing-suspending-functions.html
 *  This section covers various approaches to composition of suspending functions.
 */

suspend fun main() {

    // TODO Sequential by default
    sequentialByDefault()

    println("\n****************************\n")

    // TODO Concurrent using async
    concurrentUsingAsync()

    println("\n****************************\n")

    // TODO Lazily started async
    lazilyStartedAsync()

    println("\n****************************\n")

    // TODO Async-style functions
    asyncStyleFunctions()

    println("\n****************************\n")
}

suspend fun sequentialByDefault() {
    /*
        Assume that we have two suspending functions defined elsewhere that do something useful
        like some kind of remote service call or computation. We just pretend they are useful,
        but actually each one just delays for a second for the purpose of this example:

        What do we do if we need them to be invoked sequentially — first doSomethingUsefulOne and
        then doSomethingUsefulTwo, and compute the sum of their results? In practice we do this if
        we use the result of the first function to make a decision on whether we need to invoke
        the second one or to decide on how to invoke it.

        We use a normal sequential invocation, because the code in the coroutine, just like in
        the regular code, is sequential by default. The following example demonstrates it by measuring
        the total time it takes to execute both suspending functions:
     */

    println("sequentialByDefault start")

    val time = measureTimeMillis {
        val one = doSomethingUsefulOne()
        val two = doSomethingUsefulTwo()
        println("The answer is ${one + two}")
    }
    println("Completed in $time ms")

    println("sequentialByDefault end")
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}

fun concurrentUsingAsync() {
    /*
        What if there are no dependencies between invocations of doSomethingUsefulOne
        and doSomethingUsefulTwo and we want to get the answer faster, by doing both concurrently?
        This is where async comes to help.

        Conceptually, async is just like launch. It starts a separate coroutine which is a light-weight
        thread that works concurrently with all the other coroutines. The difference is that launch
        returns a Job and does not carry any resulting value, while async returns
        a Deferred — a light-weight non-blocking future that represents a promise to provide a result later.
        You can use .await() on a deferred value to get its eventual result,
        but Deferred is also a Job, so you can cancel it if needed.
     */

    concurrentUsingAsyncSample()

}

fun concurrentUsingAsyncSample() = runBlocking {
    println("concurrentUsingAsyncSample start")

    val time = measureTimeMillis {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")

    println("concurrentUsingAsyncSample end")

    /*
        This is twice as fast, because the two coroutines execute concurrently.
        Note that concurrency with coroutines is always explicit.
     */
}

fun lazilyStartedAsync() {
    /*
        Optionally, async can be made lazy by setting its start parameter to CoroutineStart.LAZY.
        In this mode it only starts the coroutine when its result is required by await,
        or if its Job's start function is invoked. Run the following example:
     */

    lazilyStartedAsyncSample()
}

fun lazilyStartedAsyncSample()  = runBlocking {
    println("lazilyStartedAsyncSample start")

    val time = measureTimeMillis {
        val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
        val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
        // some computation
        one.start() // start the first one
        two.start() // start the second one
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")

    println("lazilyStartedAsyncSample end")

    /*
        So, here the two coroutines are defined but not executed as in the previous example,
        but the control is given to the programmer on when exactly to start the execution by calling start.
        We first start one, then start two, and then await for the individual coroutines to finish.

        Note that if we just call await in println without first calling start on individual coroutines,
        this will lead to sequential behavior, since await starts the coroutine execution and waits for its finish,
        which is not the intended use-case for laziness.

        The use-case for async(start = CoroutineStart.LAZY) is a replacement for the standard
        lazy function in cases when computation of the value involves suspending functions.
     */
}

fun asyncStyleFunctions() {
    /*
        We can define async-style functions that invoke doSomethingUsefulOne and doSomethingUsefulTwo
        asynchronously using the async coroutine builder with an explicit GlobalScope reference.
        We name such functions with the "…Async" suffix to highlight the fact that they only start
        asynchronous computation and one needs to use the resulting deferred value to get the result.

        Note that these xxxAsync functions are not suspending functions.
        They can be used from anywhere.
        However, their use always implies asynchronous (here meaning concurrent) execution of
        their action with the invoking code.

        The following example shows their use outside of coroutine:
     */
    asyncStyleFunctionsSample()
}

// The result type of somethingUsefulOneAsync is Deferred<Int>
fun somethingUsefulOneAsync() = GlobalScope.async {
    doSomethingUsefulOne()
}

// The result type of somethingUsefulTwoAsync is Deferred<Int>
fun somethingUsefulTwoAsync() = GlobalScope.async {
    doSomethingUsefulTwo()
}

// note that we don't have `runBlocking` to the right of `asyncStyleFunctionsSample` in this example
fun asyncStyleFunctionsSample() {
    println("asyncStyleFunctionsSample start")

    val time = measureTimeMillis {
        // we can initiate async actions outside of a coroutine
        val one = somethingUsefulOneAsync()
        val two = somethingUsefulTwoAsync()
        // but waiting for a result must involve either suspending or blocking.
        // here we use `runBlocking { ... }` to block the main thread while waiting for the result
        runBlocking {
            println("The answer is ${one.await() + two.await()}")
        }
    }
    println("Completed in $time ms")

    println("asyncStyleFunctionsSample end")

    /*
        This programming style with async functions is provided here only for illustration,
        because it is a popular style in other programming languages.
        Using this style with Kotlin coroutines is strongly discouraged for the reasons explained below.

        Consider what happens if between the
        val one = somethingUsefulOneAsync() line and one.await() expression there is some logic error
        in the code and the program throws an exception and the operation that was being performed
        by the program aborts. Normally, a global error-handler could catch this exception,
        log and report the error for developers, but the program could otherwise continue doing other operations.
        But here we have somethingUsefulOneAsync still running in the background,
        even though the operation that initiated it was aborted.

        This problem does not happen with structured concurrency, as shown in the section below.
     */
}