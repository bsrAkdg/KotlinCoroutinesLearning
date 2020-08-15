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