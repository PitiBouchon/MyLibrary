package com.example.mylibrary

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.util.Log
import java.io.ByteArrayOutputStream

class BooksDatabase(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_FILE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val DATABASE_TABLE_CREATE = "CREATE TABLE " + DATABASE_TABLE_NAME + " (" +
                PKEY + " INTEGER PRIMARY KEY," +
                ICON + " BLOB," +
                TITLE + " TEXT," +
                DESCRIPTION + " TEXT," +
                FAVORITE + " BOOLEAN" +
                ");"
        db.execSQL(DATABASE_TABLE_CREATE)
    }

    fun insertData(book: Book) {
        Log.d("INFO", "Insert data in database")
        val db = writableDatabase
        db.beginTransaction()
        val values = ContentValues()
        val bos = ByteArrayOutputStream()
        book.icon?.compress(Bitmap.CompressFormat.PNG, 100, bos)
        values.put(ICON, bos.toByteArray())
        values.put(TITLE, book.title)
        values.put(DESCRIPTION, book.description)
        values.put(FAVORITE, book.favorite)
        db.insertOrThrow(DATABASE_TABLE_NAME, null, values)
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    fun removeData(book: Book) {
        val db = writableDatabase
        db.beginTransaction()
        db.execSQL("DELETE FROM $DATABASE_TABLE_NAME WHERE $TITLE=" + '"' + book.title + '"')
        // db.close()
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    @SuppressLint("Recycle")
    fun checkIn(book: Book) : Boolean {
        val db = writableDatabase
        val out : Array<String> = emptyArray()
        // "SELECT DISTINCT $TITLE FROM $DATABASE_TABLE_NAME WHERE $TITLE=" + '"' + book.title + '"
        val cursor = db.rawQuery("SELECT DISTINCT $TITLE FROM $DATABASE_TABLE_NAME WHERE $TITLE = ?", arrayOf(book.title.toString()))
        // db.close()
        return cursor.count > 0
    }

    fun update(book : Book) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(FAVORITE, book.favorite)
        db.update(DATABASE_TABLE_NAME, cv, "$TITLE = ?", arrayOf(book.title.toString()))
    }

    val cursor: Cursor
        get() {
            Log.d("INFO", "Getting Cursor of DataBase")
            val select = "SELECT * from $DATABASE_TABLE_NAME"
            val db = readableDatabase
            return db.rawQuery(select, null)
        }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_FILE_NAME = "book_database"
        private const val DATABASE_TABLE_NAME = "book_database"
        const val PKEY = "pkey"
        const val ICON = "col1"
        const val TITLE = "col2"
        const val DESCRIPTION = "col3"
        const val FAVORITE = "col4"
    }
}