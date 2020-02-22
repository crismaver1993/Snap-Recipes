package com.shubhampandey.snaprecipes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Created abstract class to get prediction in background
// Used Kotlin coroutines to do task in background

abstract class CoroutineAsyncTask<ByteArray, Progress, Result> {
    open fun onPreExecute() { }

    abstract fun doInBackground(param: ByteArray): Result

    open fun onPostExecute(result: Result) { }

    fun execute(params: ByteArray) {
        GlobalScope.launch(Dispatchers.Default) {

            val result = doInBackground(params)

            withContext(Dispatchers.Main) {
                onPostExecute(result)
            }
        }
    }
}