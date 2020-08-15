package com.bsrakdg.coroutinesguide

import kotlinx.coroutines.*

/** https://kotlinlang.org/docs/reference/coroutines/coroutine-context-and-dispatchers.html
 *  Coroutines always execute in some context represented by a value of the CoroutineContext type,
 *  defined in the Kotlin standard library.
 *
 *  The coroutine context is a set of various elements. The main elements are the Job of the coroutine,
 *  which we've seen before, and its dispatcher, which is covered in this section.
 */

fun main() {

    // TODO Dispatchers and threads
    dispatchersAndThreads()

    println("\n****************************\n")

    // TODO Unconfined vs confined dispatcher
    unconfinedConfinedDispatcher()

    println("\n****************************\n")

    // TODO Debugging coroutines and threads
    debuggingCoroutinesAndThreads()

    // TODO Debugging using logging
    debuggingUsingLogging()

    println("\n****************************\n")

}

fun dispatchersAndThreads() {
    /*
        The coroutine context includes a coroutine dispatcher (see CoroutineDispatcher) that determines
        what thread or threads the corresponding coroutine uses for its execution.
        The coroutine dispatcher can confine coroutine execution to a specific thread,
        dispatch it to a thread pool, or let it run unconfined.

        All coroutine builders like launch and async accept an optional CoroutineContext parameter that
        can be used to explicitly specify the dispatcher for the new coroutine and other context elements.

        Try the following example:
     */

    dispatchersAndThreadsSample()
}

fun dispatchersAndThreadsSample() = runBlocking<Unit> {

        /*
            When launch { ... } is used without parameters, it inherits the context (and thus dispatcher)
            from the CoroutineScope it is being launched from. In this case, it inherits the context of
            the main runBlocking coroutine which runs in the main thread.
         */
        launch { // context of the parent, main runBlocking coroutine
            println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
        }

        /*
             Dispatchers.Unconfined is a special dispatcher that also appears to run in the main thread,
             but it is, in fact, a different mechanism that is explained later.
         */
        launch(Dispatchers.Unconfined) { // not confined -- will work with main thread
            println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
        }

        /*
            The default dispatcher that is used when coroutines are launched in GlobalScope is
            represented by Dispatchers.Default and uses a shared background pool of threads,
            so launch(Dispatchers.Default) { ... } uses the same dispatcher as GlobalScope.launch { ... }.
         */
        launch(Dispatchers.Default) { // will get dispatched to DefaultDispatcher
            println("Default               : I'm working in thread ${Thread.currentThread().name}")
        }

        /*
            newSingleThreadContext creates a thread for the coroutine to run. A dedicated thread is a very expensive resource.
            In a real application it must be either released, when no longer needed, using the close function,
            or stored in a top-level variable and reused throughout the application.
         */
        launch(newSingleThreadContext("MyOwnThread")) { // will get its own new thread
            println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
        }

}

fun unconfinedConfinedDispatcher() {
    /*
        The Dispatchers.Unconfined coroutine dispatcher starts a coroutine in the caller thread,
        but only until the first suspension point. After suspension it resumes the coroutine in
        the thread that is fully determined by the suspending function that was invoked.
        The unconfined dispatcher is appropriate for coroutines which neither consume CPU time nor
        update any shared data (like UI) confined to a specific thread.

        On the other side, the dispatcher is inherited from the outer CoroutineScope by default.
        The default dispatcher for the runBlocking coroutine, in particular, is confined to the invoker thread,
        so inheriting it has the effect of confining execution to this thread with predictable FIFO scheduling.
     */

    unconfinedConfinedDispatcherSample()
}

fun unconfinedConfinedDispatcherSample() = runBlocking<Unit> {

    launch(Dispatchers.Unconfined) { // not confined -- will work with main thread
        println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
        delay(500)
        println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
    }
    launch { // context of the parent, main runBlocking coroutine
        println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
        delay(1000)
        println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
    }

    /*
        So, the coroutine with the context inherited from runBlocking {...} continues to execute
        in the main thread, while the unconfined one resumes in the default executor thread that
        the delay function is using.

        The unconfined dispatcher is an advanced mechanism that can be helpful in certain corner
        cases where dispatching of a coroutine for its execution later is not needed or produces
        undesirable side-effects, because some operation in a coroutine must be performed right away.
        The unconfined dispatcher should not be used in general code.
     */
}

fun debuggingCoroutinesAndThreads() {
    /*
        Coroutines can suspend on one thread and resume on another thread. Even with a single-threaded
        dispatcher it might be hard to figure out what the coroutine was doing, where,
        and when if you don't have special tooling.

        Debugging with IDEA : Debugging works for versions 1.3.8 or later of kotlinx-coroutines-core.
        The Debug Tool Window contains a Coroutines tab. In this tab, you can find information about
        both currently running and suspended coroutines. The coroutines are grouped by the dispatcher they are running on.

        You can:
        - Easily check the state of each coroutine.
        - See the values of local and captured variables for both running and suspended coroutines.
        - See a full coroutine creation stack, as well as a call stack inside the coroutine.
        The stack includes all frames with variable values, even those that would be lost during standard debugging.

        If you need a full report containing the state of each coroutine and its stack,
        right-click inside the Coroutines tab, and then click Get Coroutines Dump.

        https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-rc-debugging-coroutines/
     */
}

fun debuggingUsingLogging() {
    /*
        Another approach to debugging applications with threads without Coroutine Debugger is to
        print the thread name in the log file on each log statement. This feature is universally
        supported by logging frameworks. When using coroutines, the thread name alone does not give
        much of a context, so kotlinx.coroutines includes debugging facilities to make it easier.

        Run the following code with -Dkotlinx.coroutines.debug JVM option:
     */
    debuggingUsingLoggingSample()
}

fun debuggingUsingLoggingSample() = runBlocking<Unit> {
    val a = async {
        log("I'm computing a piece of the answer")
        6
    }
    val b = async {
        log("I'm computing another piece of the answer")
        7
    }
    log("The answer is ${a.await() * b.await()}")

    /*
        There are three coroutines. The main coroutine (#1) inside runBlocking and two coroutines
        computing the deferred values a (#2) and b (#3). They are all executing in the context of
        runBlocking and are confined to the main thread.
     */
}

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")
