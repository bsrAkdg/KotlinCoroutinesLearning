package com.bsrakdg.coroutinesamples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_global_scope.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main

class GlobalScopeActivity : AppCompatActivity() {

    private val tag: String = "AppDebug"
    lateinit var parentJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_scope)

        main()

        button.setOnClickListener {
            parentJob.cancel()
        }
    }

    suspend fun work(i: Int) {
        delay(3000)
        println("Work $i done. ${Thread.currentThread().name}")
    }

    private fun main() {
        val startTime = System.currentTimeMillis()
        println("Starting parent job...")

        // If Parent job is canceled, all dependent child jobs are canceled
        parentJob = CoroutineScope(Main).launch { // parent job

            GlobalScope.launch { // child job independents parent job
                work(1)
            }

            GlobalScope.launch {  // child job independents parent job
                work(2)
            }

            launch { // child job dependents parent job // the same with launch(Dispatchers.Default)
                work(3)
            }

            launch { // child job dependents parent job
                work(4)
            }
        }
        parentJob.invokeOnCompletion { throwable ->
            if (throwable != null) {
                println("Job was canceled after ${System.currentTimeMillis() - startTime} ms.")
            } else {
                println("Done in ${System.currentTimeMillis() - startTime} ms.")
            }
        }
    }

    private fun println(message: String) {
        Log.d(tag, message)
    }
}