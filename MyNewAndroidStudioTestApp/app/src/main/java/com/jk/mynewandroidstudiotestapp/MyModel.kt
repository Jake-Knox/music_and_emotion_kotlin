package com.jk.mynewandroidstudiotestapp

class MyModel {

    var modelString = "Model String"
    var emotionDetected = "none"


    init{
        modelString = "Init model string"
    }

    public fun setEmotion(newEmotion: String)
    {
        emotionDetected = newEmotion
    }
    public fun getEmotion():String
    {
        return emotionDetected
    }

}