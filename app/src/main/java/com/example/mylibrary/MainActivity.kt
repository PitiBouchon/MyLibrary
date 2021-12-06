package com.example.mylibrary

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.security.Permission
import java.security.Permissions

class MainActivity : AppCompatActivity() {
    private var favorite : Boolean  = false
    private var strFilter : String = ""

    private fun setStrFilter() {
        val bookList = findViewById<ListView>(R.id.book_list)
        bookList.forEachIndexed { index, view ->
            val b : Book = bookList.adapter.getItem(index) as Book
            if (strFilter.split(" ").all { word -> word.lowercase() in view.findViewById<TextView>(R.id.book_title).text.toString().lowercase() }) {
                if (favorite) {
                    if (b.favorite) {
                        view.findViewById<ConstraintLayout>(R.id.constraintLayout).maxHeight = Int.MAX_VALUE
                    }
                    else {
                        view.findViewById<ConstraintLayout>(R.id.constraintLayout).maxHeight = 0
                    }
                }
                else {
                    view.findViewById<ConstraintLayout>(R.id.constraintLayout).maxHeight = Int.MAX_VALUE
                }
            }
            else {
                view.findViewById<ConstraintLayout>(R.id.constraintLayout).maxHeight = 0
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_favorite -> {
                favorite = !favorite
                setStrFilter()

                if (favorite) {
                    item.setIcon(android.R.drawable.btn_star_big_on)
                }
                else {
                    item.setIcon(android.R.drawable.btn_star_big_off)
                }
                return true
            }
            R.id.action_search -> {
                Log.d("INFO", "Open Keyboard")
                item.actionView.findViewById<EditText>(R.id.searchEditText).requestFocus()
                val inputMethodManager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // inputMethodManager.showSoftInput(item.actionView.findViewById<EditText>(R.id.searchEditText), InputMethodManager.SHOW_IMPLICIT)
                inputMethodManager.toggleSoftInput(
                    InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        val view = MenuItemCompat.getActionView(searchMenuItem)
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                val bookList = findViewById<ListView>(R.id.book_list)
                Log.d("INFO", "Un-filter book list")
                strFilter = ""
                setStrFilter()
                val inputMethodManager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // inputMethodManager.hideSoftInputFromWindow(item?.actionView?.findViewById<EditText>(R.id.searchEditText)?.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
                inputMethodManager.toggleSoftInput(
                    InputMethodManager.HIDE_IMPLICIT_ONLY,
                    InputMethodManager.HIDE_IMPLICIT_ONLY
                )
                item?.actionView?.findViewById<EditText>(R.id.searchEditText)?.clearFocus()
                return true
            }

        })
        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // val bookList = findViewById<ListView>(R.id.book_list)
                Log.d("INFO", "Filter book list : " + s.toString())
                strFilter = s.toString()
                setStrFilter()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById<View>(R.id.toolbar_main) as Toolbar)
        supportActionBar?.title = "Library"

        Log.d("INFO", "Starting Application")

        val bt = findViewById<FloatingActionButton>(R.id.add_book)

        bt.setOnClickListener { view ->
//            Snackbar.make(view, "Add books you want", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
            val switchActivity = Intent(this, AddBookActivity::class.java)
            startActivity(switchActivity)
        }

        val btCam = findViewById<FloatingActionButton>(R.id.add_book_cam)

        btCam.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this.applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                Log.d("INFO", "Request camera permission")
                shouldShowRequestPermissionRationale("The Camera is used to scan bar code on books")
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            }
            else {
                val switchActivity = Intent(this, AddBookCamActivity::class.java)
                startActivity(switchActivity)
            }
        }

        setBookList()
    }

    override fun onResume() {
        super.onResume()
        setBookList()
    }

    @SuppressLint("Range")
    private fun setBookList() {
        val booksDb = BooksDatabase(this)

        val bookList = findViewById<ListView>(R.id.book_list)
        val bookAdapter = BookAdapter(this, ArrayList(), booksDb, false)

        val cursor = booksDb.cursor
        if (cursor.count > 0) {
            cursor.moveToFirst( )
            do {
                val byteArray = cursor.getBlob(cursor.getColumnIndex(BooksDatabase.ICON))
                val icon : Bitmap? = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                val book = Book(icon,
                    cursor.getString(cursor.getColumnIndex(BooksDatabase.TITLE)),
                    cursor.getString(cursor.getColumnIndex(BooksDatabase.DESCRIPTION)),
                    cursor.getInt(cursor.getColumnIndex(BooksDatabase.FAVORITE)) > 0
                )
                bookAdapter.add(book)
            } while (cursor.moveToNext())
        }
        bookList.adapter = bookAdapter
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i : Int in permissions.indices)
        {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Log.d("INFO", "Show permissions settings")
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            else if(permissions[i] == Manifest.permission.CAMERA) {
                val switchActivity = Intent(this, AddBookCamActivity::class.java)
                startActivity(switchActivity)
            }
        }
    }
}