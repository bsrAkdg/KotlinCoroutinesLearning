package com.bsrakdg.coroutinesamples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

class TimeoutActivity : AppCompatActivity() {

    private val resultOne = "Result#1"
    private val resultTwo = "Result#2"

    val jobTimeout = 1900L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timeout)

        button.setOnClickListener {
            setNewText("Let's click!")
            // IO, Main, Default
            CoroutineScope(IO).launch {
                // go coroutines
                fakeApiRequest()
            }
        }
    }

    private suspend fun fakeApiRequest() {
        withContext(IO) {
            val job1 = withTimeoutOrNull(jobTimeout) { // start timeout 1.9 second
                val result1 = getResultOneFromApi() // wait 1 second
                setTextOnMainThread("$result1 on Job 1")

                val result2 = getResultTwoFromApi(result1) // wait 1 second (worked timeout)
                setTextOnMainThread("$result2 on Job 1")
            }

            if (job1 == null) { // cancel job, assign null when work timeout
                val cancelMessage = "Cancelling job... Job took longer than $jobTimeout ms"
                println("debug $cancelMessage")
                setTextOnMainThread(cancelMessage)
            }

            val job2 = launch { // wait job 1 completion
                val result1 = getResultOneFromApi()
                setTextOnMainThread("$result1 on Job 2")
            }

        }

    }

    private suspend fun getResultOneFromApi(): String {
        logThread("getResult1FromApi")
        delay(1000) // delays the current coroutine
        return resultOne
    }

    private suspend fun getResultTwoFromApi(resultOne: String): String {
        println("debug result1 in the getResultTwoFromApi : $resultOne")
        logThread("getResult2FromApi")
        delay(1000) // delays the current coroutine
        return resultTwo
    }

    private fun logThread(methodName: String) {
        println("debug $methodName : ${Thread.currentThread().name}")
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