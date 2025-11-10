package com.iyox.vouch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iyox.vouch.model.Contact
import com.iyox.vouch.model.ContactsRepository
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val contactsRepository = ContactsRepository(application.contentResolver)
    private val _contacts = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = _contacts
    private val _isLoadingContacts = MutableLiveData<Boolean>()
    val isLoadingContacts: LiveData<Boolean> = _isLoadingContacts

    private val _hasContactsPermission = MutableLiveData<Boolean>(false)
    val hasContactsPermission: LiveData<Boolean> = _hasContactsPermission

    fun setPermissionGranted(granted: Boolean) {
        _hasContactsPermission.value = granted
        if (granted) {
            fetchContacts()
        }
    }

    fun fetchContacts() {
        viewModelScope.launch {
            _isLoadingContacts.postValue(true)
            try {
                val result = contactsRepository.fetchContacts()
                _contacts.postValue(result)
            } catch (e: Exception) {
                _contacts.postValue(emptyList())
            }
            _isLoadingContacts.postValue(false)
        }
    }
}

