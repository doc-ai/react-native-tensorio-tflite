package com.reactnativetensoriotflite

import ai.doc.tensorio.core.model.Model
import ai.doc.tensorio.core.modelbundle.ModelBundle
import ai.doc.tensorio.core.modelbundle.ModelBundle.ModelBundleException
import ai.doc.tensorio.core.utilities.ClassificationHelper
import ai.doc.tensorio.tflite.model.TFLiteModel
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.facebook.react.bridge.*
import java.io.ByteArrayOutputStream
import java.lang.Exception

class TensorioTfliteModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  /**
   * maps path or model names to loaded models, allowing the module to load,
   * and use more than one model at a time.
   */

  private val models: HashMap<String, TFLiteModel> = HashMap()

  /**
   * React Native override for the module name
   */

  override fun getName(): String {
    return "TensorioTflite"
  }

  /**
   * React Native override for exported constants
   */

  // TODO: implement

  override fun getConstants(): Map<String, Any> {
    var constants: Map<String, Any> = HashMap()

    return constants
  }

  /**
   * Bridged method that loads a model given a model path. Relative paths will be loaded as assets
   * from the application bundle.
   */

  @ReactMethod
  fun load(path: String, name: String?, promise: Promise) {
    try {

      val hashname = if (name == null) {
        path
      } else {
        name
      }

      if (models[hashname] != null) {
        // TODO: reject
        return
      }

      // TODO: Fork on Asset vs Filepath loading

      val bundle = ModelBundle.bundleWithAsset(reactApplicationContext, path)
      val model = bundle.newModel() as TFLiteModel

      models[hashname] = model
      promise.resolve(true)

    } catch (e: ModelBundleException) {
      promise.reject(e)
    }
  }

  /**
   * Bridged method that unloads a model, freeing the underlying resources.
   */

  @ReactMethod
  fun unload(name: String, promise: Promise) {
    if (models[name] == null) {
      // TODO: reject
      return
    }

    models[name]?.unload()
    models.remove(name)

    promise.resolve(true)
  }

  /**
   * Bridged method returns YES if model is trainable, NO otherwise.
   * TF Lite models will always return NO
   */

  @ReactMethod
  fun isTrainable(name: String, promise: Promise) {
    val trains = models[name]?.bundle?.modes?.trains() ?: false
    promise.resolve(trains)
  }

  /**
   * Bridged methods that performs inference with a loaded model and returns the results.
   */

  @ReactMethod
  fun run(name: String, data: ReadableMap, promise: Promise) {
    if (models[name] == null) {
      // TODO: reject
      return
    }

    val model = models[name] as Model
    val hashmap = MapUtil.toMap(data)

    // Ensure that the provided keys match the model's expected keys, or return an error

    if (!model.io.inputs.keys().equals(hashmap.keys)) {
      // TODO: reject
      return
    }

    // Prepare inputs, converting base64 encoded image data or reading image data from the filesystem

    val preparedInputs = preparedInputs(hashmap, model)

    if (preparedInputs == null) {
      // TODO: reject
      return
    }

    // Perform inference

    val outputs: Map<String, Any>

    try {
      outputs = model.runOn(preparedInputs)
    } catch (e: Exception) {
      // TODO: reject
      return
    }

    // Prepare outputs, converting pixel buffer outputs to base64 encoded jpeg string data

    val preparedOutputs = preparedOutputs(outputs, model)

    if (preparedOutputs == null) {
      // TODO: reject
      return
    }

    // Return results in React Native format

    promise.resolve(MapUtil.toWritableMap(preparedOutputs))
  }

  /**
   * Bridged utility method for image classification models that returns the
   * top N probability label-scores.
   */

  // TODO: Remove and implement in javasript

  @ReactMethod
  fun topN(count: Int, threshold: Float, classifications: ReadableMap, promise: Promise) {
    val hashmap = MapUtil.toMap(classifications) as Map<String, Double>
    val floatmap = hashmap.map { it.key to it.value.toFloat() }.toMap()
    val topN = ClassificationHelper.topN(floatmap, count, threshold)
    val topNMap = HashMap<String, Float>()

    for (entry in topN) {
      topNMap[entry.key] = entry.value
    }

    // Return results in React Native format

    promise.resolve(MapUtil.toWritableMap(topNMap as Map<String, Any>))
  }

  // Load Utilities

  // ...

  // Input/Output Preparation

  /**
   * Prepares the model inputs sent from javascript for inference. Image inputs are encoded
   * as a base64 string and must be decoded and converted to pixel buffers. Other inputs are
   * taken as is.
   */

  private fun preparedInputs(inputs: Map<String, Any>, model: Model): Map<String, Any> {
    val preparedInputs = HashMap<String, Any>()
    var error = false

    for (layer in model.io.inputs.all()) {
      layer.doCase(
        {
          preparedInputs[layer.name] = inputs[layer.name] as Any
        },
        {
          val bitmap = bitmapForInput(inputs[layer.name] as Map<String, Any>)
          if (bitmap != null) {
            preparedInputs[layer.name] = bitmap
          } else {
            error = true
          }
        },
        {
          preparedInputs[layer.name] = inputs[layer.name] as Any
        })
    }

    // TODO: error handling

    if (error) {

    }

    return preparedInputs
  }

  /**
   * Prepares the model outputs for consumption by javascript. Pixel buffer outputs are converted
   * to base64 strings. Other outputs are taken as is.
   */

  private fun preparedOutputs(outputs: Map<String, Any>, model: Model): Map<String, Any> {
    val preparedOutputs = HashMap<String, Any>()
    var error = false

    for (layer in model.io.outputs.all()) {
      layer.doCase(
        {
          preparedOutputs[layer.name] = outputs[layer.name] as Any
        },
        {
          val encoded = base64JPEGDataForBitmap(outputs[layer.name] as Bitmap)
          if (encoded != null) {
            preparedOutputs[layer.name] = encoded
          } else {
            error = true
          }
        },
        {
          preparedOutputs[layer.name] = outputs[layer.name] as Any
        })
    }

    // TODO: error handling

    if (error) {

    }

    return preparedOutputs
  }

  // Image Utilities

  /**
   * Converts a pixel buffer output to a base64 encoded string that can be consumed by React Native.
   * See https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string
   */

  private fun base64JPEGDataForBitmap(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

    val bytes = stream.toByteArray()
    return Base64.encodeToString(bytes, Base64.DEFAULT)
  }

  /**
   * Prepares a pixel buffer input given an image encoding dictionary sent from javascript,
   * converting a base64 encoded string or reading data from the file system.
   */

  private fun bitmapForInput(input: Map<String, Any>): Bitmap? {
    val format = input["RNTIOImageKeyFormat"] as Double // double! gross

    val bitmap =  when (format) {
      0.toDouble() -> null // RNTIOImageDataTypeUnknown
      1.toDouble() -> null // RNTIOImageDataTypeARGB
      2.toDouble() -> null // RNTIOImageDataTypeBGRA
      3.toDouble() -> null // RNTIOImageDataTypeJPEG
      4.toDouble() -> null // RNTIOImageDataTypePNG
      5.toDouble() -> null // RNTIOImageDataTypeFile
      6.toDouble() -> { // RNTIOImageDataTypeAsset
        val name = input["RNTIOImageKeyData"] as String
        val stream = reactApplicationContext.assets.open(name)
        val bitmap = BitmapFactory.decodeStream(stream);
        bitmap
      }
      else -> null
    }

    return bitmap
  }
}
