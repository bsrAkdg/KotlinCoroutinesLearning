package com.bsrakdg.coroutinesamples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_run_blocking.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

class RunBlockingActivity : AppCompatActivity() {

    private val tag: String = "AppDebug"
    var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_blocking)

        main()

        button.setOnClickListener {
            text.text = (count++).toString()
        }
    }

    private fun main() {
        // job #1
        CoroutineScope(Main).launch {
            val result1 = getResult()
            println("result1: $result1")

            val result2 = getResult()
            println("result2: $result2")

            val result3 = getResult()
            println("result3: $result3")

            val result4 = getResult()
            println("result1: $result4")

            val result5 = getResult()
            println("result5: $result5")

        }

        // job #2
        CoroutineScope(Main).launch {
            delay(1000)

            runBlocking {
                println("Blocking thread : ${Thread.currentThread().name} ")
                delay(4000)
                println("Blocking thread completed")
            }
        }

        // job #1 and #2 work parallel on the Main thread. But if runBlocking works all other jobs
        // wait completion the runBlocking scopes. When runBlocking completed, other jobs continue
        // work.
    }

    private suspend fun getResult(): Int {
        delay(1000)
        return Random.nextInt(0, 100)
    }

    private fun println(message: String) {
        Log.d(tag, message)
    }
}