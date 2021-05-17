package com.teoking.canvasdemos.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "CanvasDemos shows usages of Canvas API."
    }
    val text: LiveData<String> = _text
}