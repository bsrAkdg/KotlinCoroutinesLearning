package com.bsrakdg.coroutinesamples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

// https://en.wikipedia.org/wiki/Structured_concurrency
class StructuredConcurrencyActivity : AppCompatActivity() {

    private val tag: String = "AppDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_structured_concurrency)

        main()
    }

    // Can only be used by parent job
    private val handler = CoroutineExceptionHandler { _, exception ->
        println("Exception thrown in one of the children: $exception")
    }

    private fun main() {
        /*
             -----------------
            |    Thread      |
             ----------------
            |---> Job #1 (coroutine 1)  Success

            |---> Job #2 (coroutine 2)  Case 1 : Exception / Case 2 : Cancellation / Case 3 : CancellationException

            |---> Job #3 (coroutine 3)  Case 1 : Since JOB2 throws an error, JOB3 throws an error even though it is successful. (Parent job is fail)
                                        Case 2 : Since JOB2 cancels, JOB3 is successful. (Parent job is success)
                                        Case 3 : Since JOB2 throws an CancellationException,  JOB3 is successful. (Parent job is success)
         */

        val parentJob = CoroutineScope(IO).launch(handler) {

            // --------- JOB A ---------
            val jobA = launch {
                val resultA = getResult(1)
                println("resultA: $resultA")
            }
            jobA.invokeOnCompletion { throwable ->
                if (throwable != null) {
                    println("Error getting resultA: $throwable")
                }
            }

            // --------- JOB B ---------
            val jobB = launch {
                val resultB = getResult(2)
                println("resultB: $resultB")
            }
            jobB.invokeOnCompletion { throwable ->
                if (throwable != null) {
                    println("Error getting resultB: $throwable")
                }
            }

            // --------- JOB C ---------
            val jobC = launch {
                val resultC = getResult(3)
                println("resultC: $resultC")
            }
            jobC.invokeOnCompletion { throwable ->
                if (throwable != null) {
                    println("Error getting resultC: $throwable")
                }
            }
        }
        parentJob.invokeOnCompletion { throwable ->
            if (throwable != null) {
                println("Parent job failed: $throwable")
            } else {
                println("Parent job SUCCESS")
            }
        }
    }

    private suspend fun getResult(number: Int): Int {
        return withContext(Main) {
            delay(number * 500L)
            if (number == 2) {
                // throw Exception("Error getting result for number $number") // Case 1
                // cancel("Error getting result for number $number") // Case 2
                throw CancellationException("Error getting result for number $number") // Case 3
            }
            number * 2
        }
    }

    private fun println(message: String) {
        Log.d(tag, message)
    }
}