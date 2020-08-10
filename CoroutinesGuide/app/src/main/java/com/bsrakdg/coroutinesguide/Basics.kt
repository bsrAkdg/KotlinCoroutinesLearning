package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.*
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

    println("\n****************************\n")

    // TODO Structured concurrency
    structuredConcurrency()

    println("\n****************************\n")

    // TODO Scope builder
    scopeBuilder()

    println("\n****************************\n")

    // TODO Extract function refactoring
    extractFunctionRefactoring()

    println("\n****************************\n")

    // TODO Coroutines ARE light-weight
    coroutinesArLightWeight()
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

fun structuredConcurrency() {
    /*
        There is still something to be desired for practical usage of coroutines.
        When we use GlobalScope.launch, we create a top-level coroutine.
        Even though it is light-weight, it still consumes some memory resources while it runs.
        If we forget to keep a reference to the newly launched coroutine, it still runs.
        What if the code in the coroutine hangs (for example, we erroneously delay for too long),
        what if we launched too many coroutines and ran out of memory? Having to manually keep
        references to all the launched coroutines and join them is error-prone.

        There is a better solution. We can use structured concurrency in our code.
        Instead of launching coroutines in the GlobalScope, just like we usually do with threads
        (threads are always global), we can launch coroutines in the specific scope of
        the operation we are performing.

        In our example, we have a main function that is turned into a coroutine using the
        runBlocking coroutine builder. Every coroutine builder, including runBlocking, adds
        an instance of CoroutineScope to the scope of its code block. We can launch coroutines in
        this scope without having to join them explicitly, because an outer coroutine
        (runBlocking in our example) does not complete until all the coroutines launched in its
        scope complete. Thus, we can make our example simpler:
     */
    println("Start ...")
    runBlockingSample()
    println("End...")

}

fun runBlockingSample() = runBlocking { // this: CoroutineScope
    launch { // launch a new coroutine in the scope of runBlocking
        delay(1000L)
        println("World!")
    }
    launch { // launch a new coroutine in the scope of runBlocking
        delay(1000L)
        println("How")
    }
    launch { // launch a new coroutine in the scope of runBlocking
        delay(1000L)
        println("Are You?")
    }
    println("Hello,")
}


fun scopeBuilder() {
    /*
        In addition to the coroutine scope provided by different builders, it is possible to declare
        your own scope using the coroutineScope builder. It creates a coroutine scope and does not
        complete until all launched children complete.

        runBlocking and coroutineScope may look similar because they both wait for their body and
        all its children to complete. The main difference is that the runBlocking method blocks
        the current thread for waiting, while coroutineScope just suspends, releasing the underlying
        thread for other usages. Because of that difference, runBlocking is a regular function and
        coroutineScope is a suspending function.
     */

    println("Start...")
    scopeBuilderSample()
    println("End...")
}


fun scopeBuilderSample()  = runBlocking { // this: CoroutineScope
    launch {
        delay(200L)
        println("Task from runBlocking")
    }

    coroutineScope { // Creates a coroutine scope
        launch {
            delay(500L)
            println("Task from nested launch")
        }

        delay(100L)
        println("Task from coroutine scope") // This line will be printed before the nested launch
    }

    println("Coroutine scope is over") // This line is not printed until the nested launch completes

    /*
        Note that right after the "Task from coroutine scope" message (while waiting for nested launch)
         "Task from runBlocking" is executed and printed — even though the coroutineScope is not completed yet.
     */
}

fun extractFunctionRefactoring() {
    /*
        Let's extract the block of code inside launch { ... } into a separate function.
        When you perform "Extract function" refactoring on this code, you get a new function with
        the suspend modifier. This is your first suspending function. Suspending functions can be
        used inside coroutines just like regular functions, but their additional feature is that
        they can, in turn, use other suspending functions (like delay in this example) to suspend
        execution of a coroutine.

     */

    extractFunctionRefactoringSample()

    /*
        But what if the extracted function contains a coroutine builder which is invoked on
        the current scope? In this case, the suspend modifier on the extracted function is not enough.
        Making doWorld an extension method on CoroutineScope is one of the solutions, but it may
        not always be applicable as it does not make the API clearer. The idiomatic solution is to
        have either an explicit CoroutineScope as a field in a class containing the target function
        or an implicit one when the outer class implements CoroutineScope. As a last resort,
        CoroutineScope(coroutineContext) can be used, but such an approach is structurally unsafe
        because you no longer have control on the scope of execution of this method.
        Only private APIs can use this builder.

        look at doSomethingInapplicable
     */
}

fun extractFunctionRefactoringSample() = runBlocking {
    println("extractFunctionRefactoringSample start")
    launch {
        doSomething()
    }

    launch {
        doSomethingInapplicable()
    }
    println("extractFunctionRefactoringSample end")
}

suspend fun doSomething() {
    delay(1000L)
    println("doSomething completed")
}

suspend fun doSomethingInapplicable() = runBlocking {
    delay(1000L)
    println("doSomethingNewCoroutine completed")
}

fun coroutinesArLightWeight() {
    /*
        It launches 100K coroutines and, after 5 seconds, each coroutine prints a dot.
        Now, try that with threads. What would happen? (Most likely your code will produce some sort of out-of-memory error)
     */
    coroutinesArLightWeightSample()
}

fun coroutinesArLightWeightSample() = runBlocking {
    println("coroutinesArLightWeightSample start")

    repeat(10) { // launch a lot of coroutines
        launch {
            delay(5000L)
            print(".")
        }
    }

    println("coroutinesArLightWeightSample end")
}