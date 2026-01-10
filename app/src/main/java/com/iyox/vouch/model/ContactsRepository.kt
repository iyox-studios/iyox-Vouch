package com.iyox.vouch.model

import android.content.ContentResolver
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val contentResolver: ContentResolver) {

    suspend fun searchContacts(query: String): List<Person> = withContext(Dispatchers.IO) {
        val contactData = mutableListOf<Person>()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )

        val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"

        val selectionArgs = arrayOf("%$query%")

        val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"

        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

            var count = 0
            while (it.moveToNext() && count < 20) {
                val name = it.getString(nameIndex) ?: "Unknown"

                if (name.isNotBlank()) {
                    val parts = name.trim().split("\\s+".toRegex())
                    val firstName = parts.getOrNull(0) ?: ""
                    val secondName = parts.drop(1).joinToString(" ")
                    contactData.add(Person(-1, firstName, secondName))
                    count++
                }
            }
        }

        contactData
    }
}

