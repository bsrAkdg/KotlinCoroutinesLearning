package com.bsrakdg.coroutinesamples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlin.system.measureTimeMillis

class AsyncAndAwaitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_async_await)
        button.setOnClickListener {
            setNewText("Clicked!")

            // fakeApiRequestWayOne()
            fakeApiRequestWayTwo()
        }
    }

    /**
     * async() is a blocking call (similar to the job pattern with job.join())
     *  NOTES:
     *  1) IF you don't call await(), it does not wait for the result
     *  2) Calling await() on both these Deferred values will EXECUTE THEM IN PARALLEL. But the RESULTS won't
     *     be published until the last result is available (in this case that's result2)
     *
     */
    private fun fakeApiRequestWayOne() {
        val startTime = System.currentTimeMillis()
        val parentJob = CoroutineScope(IO).launch {
            val job1 = launch {
                val time1 = measureTimeMillis {
                    println("debug : launching job1 in thread: ${Thread.currentThread().name}")
                    val result1 = getResultOneFromApi()
                    setTextOnMainThread(result1)
                }
                println("debug : completed job1 in $time1 ms.")
            }

            val job2 = launch { // do not wait job1
                val time2 = measureTimeMillis {
                    println("debug : launching job2 in thread: ${Thread.currentThread().name}")
                    val result2 = getResultTwoFromApi()
                    setTextOnMainThread(result2)
                }
                println("debug : completed job2 in $time2 ms.")
            }
        }
        parentJob.invokeOnCompletion {
            println("debug : total elapsed time : ${System.currentTimeMillis() - startTime}")
        }
    }

    private fun fakeApiRequestWayTwo() {
        CoroutineScope(IO).launch {
            val executionTime = measureTimeMillis {
                val result1: Deferred<String> = async {
                    println("debug : launching job1: ${Thread.currentThread().name}")
                    getResultOneFromApi()
                }

                val result2: Deferred<String> = async {
                    println("debug : launching job2 : ${Thread.currentThread().name}")
                    getResultTwoFromApi()
                }

                setTextOnMainThread("Got ${result1.await()}")
                setTextOnMainThread("Got ${result2.await()}")
            }
            println("debug : total time elapsed : $executionTime")
        }
    }

    private suspend fun getResultOneFromApi(): String {
        delay(1000)
        return "Result #1"
    }

    private suspend fun getResultTwoFromApi(): String {
        delay(1000)
        return "Result #2"
    }

    private suspend fun setTextOnMainThread(input: String) {
        withContext(Dispatchers.Main) {
            setNewText(input)
        }
    }

    private fun setNewText(input: String) {
        val newText = text.text.toString() + "\n$input"
        text.text = newText
    }
}