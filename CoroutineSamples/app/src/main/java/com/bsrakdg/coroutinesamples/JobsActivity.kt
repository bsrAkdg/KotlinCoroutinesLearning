package com.bsrakdg.coroutinesamples

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_jobs.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class JobsActivity : AppCompatActivity() {

    private val progressMax = 100
    private val progressStart = 0
    private val jobTime = 4000 //ms
    private lateinit var job: CompletableJob

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jobs)

        job_button.setOnClickListener {
            if (!::job.isInitialized) { // check lateinit variable initialize with using ::
                initJob()
            }
            job_progress_bar.startJobOrCancel(job)
        }
    }

    private fun initJob() {
        job_button.text = "Start Job #1"
        updateJobCompleteTextView("")
        job = Job() // lateinit

        job.invokeOnCompletion {
            it?.message.let {
                var message = it
                if (message.isNullOrBlank()) {
                    message = "Unknown cancellation error"
                }
                println("debug $job was cancelled. Reason : $message")
                showToast(message)
            }
        }
        job_progress_bar.max = progressMax
        job_progress_bar.progress = progressStart
    }

    fun ProgressBar.startJobOrCancel(job: Job) {
        if (this.progress > 0) {
            println("debug $job is already active. Cancelling...")
            resetJob()
        } else {
            job_button.text = "Cancel job #1"
            CoroutineScope(IO + job).launch {
                println("debug coroutine $this is activiated with job $job")

                for (i in progressStart..progressMax) {
                    delay((jobTime / progressMax).toLong())
                    this@startJobOrCancel.progress = i
                }

                updateJobCompleteTextView("Job is complete")
            }
        }
    }

    private fun updateJobCompleteTextView(text: String) {
        GlobalScope.launch(Main) {
            job_complete_text.text = text
        }
    }

    private fun resetJob() {
        if (job.isActive || job.isCompleted) {
            job.cancel(CancellationException("Resetting job"))
        }
        initJob()
    }

    private fun showToast(text: String) {
        GlobalScope.launch(Main) {
            Toast.makeText(this@JobsActivity, text, Toast.LENGTH_LONG).show()
        }
    }

}