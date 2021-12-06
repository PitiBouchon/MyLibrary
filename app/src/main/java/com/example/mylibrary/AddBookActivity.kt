package com.example.mylibrary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.net.ssl.HttpsURLConnection
import java.io.*
import java.net.*


class AddBookActivity : AppCompatActivity() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        val view = MenuItemCompat.getActionView(searchMenuItem)

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                val inputMethodManager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.toggleSoftInput(
                    InputMethodManager.HIDE_IMPLICIT_ONLY,
                    InputMethodManager.HIDE_IMPLICIT_ONLY
                )
                item?.actionView?.findViewById<EditText>(R.id.searchEditText)?.clearFocus()
                return true
            }

        })

        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        searchEditText.setOnEditorActionListener { v, actionId, event ->
            Snackbar.make(view, "Searching books...", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
            findViewById<ListView>(R.id.add_book_list).adapter = null
            val strUrl = "https://www.googleapis.com/books/v1/volumes?q=${searchEditText.text}&key=AIzaSyCCxOOWnzxZ7Y_K22Und4OSJfWxB4IXWqA&maxResults=10"
            fetchBooks(strUrl)
            false
        }

        val extraStr = intent.getStringExtra("searchTxt")
        if (extraStr != null) {
            searchEditText.setText(extraStr)
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
            val strUrl = "https://www.googleapis.com/books/v1/volumes?q=${searchEditText.text}&key=AIzaSyCCxOOWnzxZ7Y_K22Und4OSJfWxB4IXWqA&maxResults=10"
            fetchBooks(strUrl)
        }

        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
            R.id.action_search -> {
                item.actionView.findViewById<EditText>(R.id.searchEditText).requestFocus()
                val inputMethodManager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.toggleSoftInput(
                    InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_book)

        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Online Search"
    }

    private fun createAdapter() : BookAdapter {
        return BookAdapter(this, ArrayList(), BooksDatabase(this), true)
    }

    private fun fetchBooks(str_url: String, add: Boolean = false, start_index: Int = 0) {
        val strUrlIndexed = "$str_url&startIndex=$start_index"
        Log.d("INFO", "Request url : $strUrlIndexed")
        var notAll = false
        lifecycleScope.launch(Dispatchers.IO) {
            val result = getRequest(strUrlIndexed)
            if (result != null) {
                try {
                    // Parse JSON
                    Log.d("INFO", "Parsing JSON")
                    val json = JSONObject(result)
                    if (json.has("totalItems") && json.getInt("totalItems") == 0) {
                        withContext(Dispatchers.Main) {
                            findViewById<ProgressBar>(R.id.progressBar).visibility = View.INVISIBLE
                        }
                        return@launch
                    }
                    val booksFound = json.getJSONArray("items")
                    val bookList: MutableList<Book> = emptyList<Book>().toMutableList()

                    for (i in 0 until booksFound.length()) {
                        val title: String =
                            booksFound.getJSONObject(i).getJSONObject("volumeInfo")
                                .getString("title")
                        var description = "Unknown description"
                        if (booksFound.getJSONObject(i).getJSONObject("volumeInfo")
                                .has("description")
                        ) {
                            description =
                                booksFound.getJSONObject(i).getJSONObject("volumeInfo")
                                    .getString("description")
                        }
                        var imageLink: String? = null
                        if (booksFound.getJSONObject(i).getJSONObject("volumeInfo")
                                .has("imageLinks")
                        ) {
                            imageLink = booksFound.getJSONObject(i).getJSONObject("volumeInfo")
                                .getJSONObject("imageLinks").getString("thumbnail")
                        }
                        var icon: Bitmap? = null
                        if (imageLink != null) {
                            Log.d("INFO", "Downloading : $imageLink")
                            icon = BitmapFactory.decodeStream(
                                URL(imageLink).openConnection().getInputStream()
                            )
                        }
                        val book = Book(icon, title, description, false)
                        bookList.add(book)
                    }

                    withContext(Dispatchers.Main) {
                        val addBookList = findViewById<ListView>(R.id.add_book_list)
                        var bookAdapter = createAdapter()
                        if (add) {
                            if (addBookList.adapter != null) {
                                bookAdapter = addBookList.adapter as BookAdapter
                            }
                            else {
                                Log.d("INFO", "Error Adapter should not be null")
                            }
                        }
                        Log.d("INFO", "Showing results")
                        bookList.forEach {
                            bookAdapter.add(it)
                        }
                        if (!add) {
                            addBookList.adapter = bookAdapter
                        }
                        bookAdapter.notifyDataSetChanged()
//                        findViewById<ProgressBar>(R.id.progressBar).visibility = View.INVISIBLE
                    }

                    if (json.has("totalItems") && json.getInt("totalItems") - start_index > 10)
                    {
                        notAll = true
                        fetchBooks(str_url, true, start_index + 10)
                    }
                }
                catch (err: Error) {
                    Log.d("INFO", "Error when parsing JSON : " + err.localizedMessage)
                }
            }
            else {
                Log.d("INFO", "Error : Get request returned no response")
            }
            if (!notAll) {
                withContext(Dispatchers.Main) {
                    Log.d("INFO", "Stop progress bar")
                    findViewById<ProgressBar>(R.id.progressBar).visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun getRequest(str_url: String): String? {
        val inputStream: InputStream
        var result: String? = null

        try {
            // Create URL
            val url = URL(str_url)

            // Create HttpURLConnection
            val conn: HttpsURLConnection = url.openConnection() as HttpsURLConnection

            // Launch GET request
            conn.connect()

            // Receive response as inputStream
            inputStream = conn.inputStream

            result = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: "error: inputStream is null"
        }

        catch(err:Error) {
            Log.d("INFO", "Error when executing get request: " + err.localizedMessage)
        }

        return result
    }
}