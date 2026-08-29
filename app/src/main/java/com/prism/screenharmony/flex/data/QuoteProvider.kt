package com.prism.screenharmony.flex.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

data class QuoteItem(
    val quote: String,
    val author: String,
    val category: String = "mindfulness"
)

object QuoteProvider {
    private const val TAG = "QuoteProvider"

    // Default high-quality offline fallbacks
    private val fallbackQuotes = listOf(
        QuoteItem("Almost everything will work again if you unplug it for a few minutes, including you.", "Anne Lamott"),
        QuoteItem("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln"),
        QuoteItem("Focus is a muscle. The more you practice, the stronger it gets.", "Anonymous"),
        QuoteItem("You will never regret the time you spent focusing on your goals.", "Anonymous"),
        QuoteItem("Your future self is watching you right now through your choices.", "Anonymous"),
        QuoteItem("Small disciplines repeated with consistency every day lead to great achievements.", "John C. Maxwell"),
        QuoteItem("To find true fulfilment, you must live your life’s purpose.", "Mensah Oteh"),
        QuoteItem("Happiness is achieved when you stop waiting for your life to begin and start making the most of the moment.", "Germany Kent"),
        QuoteItem("The difference between impossible and possible is a willing heart.", "Lolly Daskal")
    )

    private val cachedQuotes = mutableListOf<QuoteItem>()
    private var isLoaded = false

    fun initialize(context: Context) {
        if (isLoaded) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val assetManager = context.applicationContext.assets
                val inputStream = assetManager.open("quotes/quotes.json")
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val jsonText = reader.use { it.readText() }

                val jsonArray = JSONArray(jsonText)
                val loadedList = mutableListOf<QuoteItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val quote = obj.optString("quote", "").trim()
                    val author = obj.optString("author", "Unknown").trim()
                    val category = obj.optString("category", "mindfulness").trim()

                    if (quote.isNotEmpty()) {
                        loadedList.add(QuoteItem(quote = quote, author = author, category = category))
                    }
                }

                synchronized(cachedQuotes) {
                    cachedQuotes.clear()
                    if (loadedList.isNotEmpty()) {
                        cachedQuotes.addAll(loadedList)
                    } else {
                        cachedQuotes.addAll(fallbackQuotes)
                    }
                    isLoaded = true
                }
                Log.i(TAG, "Successfully loaded ${cachedQuotes.size} quotes into memory.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load quotes from assets, using fallback quotes", e)
                synchronized(cachedQuotes) {
                    if (cachedQuotes.isEmpty()) {
                        cachedQuotes.addAll(fallbackQuotes)
                    }
                    isLoaded = true
                }
            }
        }
    }

    fun getRandomQuote(): QuoteItem {
        synchronized(cachedQuotes) {
            return if (cachedQuotes.isNotEmpty()) {
                cachedQuotes.random()
            } else {
                fallbackQuotes.random()
            }
        }
    }

    fun getRandomQuoteText(): String {
        return getRandomQuote().quote
    }
}
