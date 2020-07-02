package com.bsrakdg.coroutinesamples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main

class SupervisorJobActivity : AppCompatActivity() {
    private val tag: String = "AppDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_job)

        main()

    }

    private val handler = CoroutineExceptionHandler { _, exception ->
        println("Exception thrown somewhere within parent or child: $exception.")
    }

    private val childExceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("Exception thrown in one of the children: $exception.")
    }

    fun main() {
     /* Compare StructuredConcurrencyActivity Case 2!!
         -----------------
        |    Thread      |
         ----------------
        |---> Job #1 (coroutine 1)  Success

        |---> Job #2 (coroutine 2)  Exception (handled by child exception handler)

        |---> Job #3 (coroutine 3)  Success (parent is success)
     */

        val parentJob = CoroutineScope(Main).launch(handler) {

            supervisorScope { // Make sure to handle errors in children

                // --------- JOB A ---------
                val jobA = launch {
                    val resultA = getResult(1)
                    println("resultA: $resultA")
                }

                // --------- JOB B ---------
                val jobB = launch(childExceptionHandler) {
                    val resultB = getResult(2)
                    println("resultB: $resultB")
                }

                // --------- JOB C ---------
                val jobC = launch {
                    val resultC = getResult(3)
                    println("resultC: $resultC")
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
                throw Exception("Error getting result for number: $number")
            }
            number * 2
        }
    }


    private fun println(message: String) {
        Log.d(tag, message)
    }


}