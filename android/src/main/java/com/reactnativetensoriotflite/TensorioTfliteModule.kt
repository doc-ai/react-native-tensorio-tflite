package com.reactnativetensoriotflite

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

import ai.doc.tensorio.core.model.Backend

class TensorioTfliteModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "TensorioTflite"
    }

    // Example method
    // See https://facebook.github.io/react-native/docs/native-modules-android
    @ReactMethod
    fun multiply(a: Int, b: Int, promise: Promise) {

      val backend = Backend.availableBackend()

      promise.resolve(a * b)

    }


}
