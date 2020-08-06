package com.bsrakdg.coroutinesamples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sequential.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.system.measureTimeMillis

class SequentialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sequential)

        button.setOnClickListener {
            setNewText("Clicked!")
            fakeApiRequest()
        }
    }

    private fun fakeApiRequest() {
        CoroutineScope(IO).launch {
            val executionTime = measureTimeMillis {
                val result1 = async {
                    println("debug : launching job1 : ${Thread.currentThread().name}")
                    getResult1FromApi()
                }.await()

                val result2 = async {
                    println("debug : launching job2 : ${Thread.currentThread().name}")
                    getResult2FromApi(result1)
                }.await()

                println("debug : job2 result : $result2")
            }
            println("debug : total time elapsed : $executionTime")
        }
    }

    private suspend fun getResult1FromApi(): String {
        println("debug : getResult1FromApi")
        delay(1000) // Does not block thread. Just suspends the coroutine inside the thread
        return "Result #1"
    }

    private suspend fun getResult2FromApi(result1: String): String {
        println("debug : getResult2FromApi")
        delay(1700)
        return "Result #2"
    }

    private fun setNewText(input: String) {
        val newText = text.text.toString() + "\n$input"
        text.text = newText
    }

    private suspend fun setTextOnMainThread(input: String) {
        withContext(Main) {
            setNewText(input)
        }
    }
}