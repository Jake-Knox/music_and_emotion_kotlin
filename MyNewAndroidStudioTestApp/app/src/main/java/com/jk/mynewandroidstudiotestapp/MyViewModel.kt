package com.jk.mynewandroidstudiotestapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MyViewModel: ViewModel() {

    val myLiveModel = MutableLiveData<MyModel>()

    init{
        myLiveModel.value = MyModel()
    }
}