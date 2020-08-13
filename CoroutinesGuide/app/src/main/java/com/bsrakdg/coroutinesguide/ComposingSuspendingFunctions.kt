package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis

/** https://kotlinlang.org/docs/reference/coroutines/composing-suspending-functions.html
 *  This section covers various approaches to composition of suspending functions.
 */

suspend fun main() {

    // TODO Sequential by default
    sequentialByDefault()

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