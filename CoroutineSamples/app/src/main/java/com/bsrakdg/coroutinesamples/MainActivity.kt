package com.bsrakdg.coroutinesamples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val resultOne = "Result#1"
    private val resultTwo = "Result#2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            // IO, Main, Default
            CoroutineScope(IO).launch {
                // go coroutines
                fakeApiRequest()
            }
        }
    }

    private suspend fun fakeApiRequest() {
        val result1 = getResultOneFromApi()
        println("debug $result1")
        // text.text = result1 // you don't do this, it will be crash because you are on IO thread!
        setTextOnMainThread(result1) // you should use withContext(Main) for access UI components

        val result2 = getResultTwoFromApi(result1) // waits result1 completion
        println("debug $result2")
        setTextOnMainThread(result2)

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
        withContext(Main) {
            setNewText(input)
        }
    }

    private fun setNewText(input: String) {
        val newText = text.text.toString() + "\n$input"
        text.text = newText
    }
}
