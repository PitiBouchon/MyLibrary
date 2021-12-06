package com.example.mylibrary

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*


class BookAdapter(
    context: Context,
    booksArray: ArrayList<Book>,
    private val db: BooksDatabase,
    private val onlineSearch: Boolean
) :
    ArrayAdapter<Book>(context, 0, booksArray) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        val book : Book = getItem(position)!!

         // Check if an existing view is being reused, otherwise inflate the view
        val convertView2 =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.book, parent, false)

        // Lookup view for data population
        val title = convertView2.findViewById(R.id.book_title) as TextView
        val description = convertView2.findViewById(R.id.book_description) as TextView
        val button = convertView2.findViewById(R.id.action_button) as ImageButton

        // Populate the data into the template view using the data object
        title.text = book.title
        description.text = book.description
        if (!db.checkIn(book)) {
            button.setImageResource(android.R.drawable.ic_input_add)
        }
        else {
            button.setImageResource(android.R.drawable.ic_delete)
        }

        val icon = book.icon
        if (icon != null) {
            val bookIcon = convertView2.findViewById(R.id.image_book) as ImageView
            bookIcon.setImageBitmap(icon)
        }

        val favBt = convertView2.findViewById<ImageButton>(R.id.fav_bt)
        if (onlineSearch) {
            favBt.visibility = View.GONE
            favBt.maxHeight = 0
        }
        else {
            if (book.favorite) {
                favBt.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                favBt.setImageResource(android.R.drawable.btn_star_big_off)
            }

            favBt.setOnClickListener {
                val booksDb = BooksDatabase(this.context)
                book.favorite = !book.favorite
                db.update(book)
                if (book.favorite) {
                    favBt.setImageResource(android.R.drawable.btn_star_big_on)
                } else {
                    favBt.setImageResource(android.R.drawable.btn_star_big_off)
                }
            }
        }

        button.setOnClickListener {
            val booksDb = BooksDatabase(this.context)
            if (!db.checkIn(book)) {
                booksDb.insertData(book)
                button.setImageResource(android.R.drawable.ic_delete)
            }
            else {
                booksDb.removeData(book)
                button.setImageResource(android.R.drawable.ic_input_add)
                if (!onlineSearch) {
                    this.remove(book)
                }
            }
        }

        convertView2.setOnClickListener {
            if (description.maxHeight == -1 || description.maxHeight == Int.MAX_VALUE)
            {
                description.maxHeight = 150
            }
            else {
                description.maxHeight = Int.MAX_VALUE
            }
        }

        // Return the completed view to render on screen
        return convertView2
    }
}