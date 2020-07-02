package com.bsrakdg.coroutinesamples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_freeze.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FreezeUIActivity : AppCompatActivity() {

    private val tag: String = "AppDebug"
    var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_freeze)

        main()

        button.setOnClickListener {
            text.text = (count++).toString()
        }
    }

    private fun main() {
        // UI Thread is completely blocked
        // Thread.sleep(3000)

        // UI Thread is not blocked, this is a job.
        CoroutineScope(Main).launch { // parent job
            println("Current thread : ${Thread.currentThread().name}")

            // 100.000 children job, freeze UI (OVERWHELMED)
//            for (i in 1..100_000) {
//                launch {
//                    doNetworkRequest()
//                }
//            }

            doNetworkRequest() // dont freeze UI (just one children job)
        }
    }

    private suspend fun doNetworkRequest() {
        println("debug : Starting network request ...")
        delay(3000)
        println("debug : Finished network request!")
    }

    private fun println(message: String) {
        Log.d(tag, message)
    }
}