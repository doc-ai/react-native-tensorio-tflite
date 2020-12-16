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
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer

const val RNTIOImageKeyData = "RNTIOImageKeyData"
const val RNTIOImageKeyFormat = "RNTIOImageKeyFormat"
const val RNTIOImageKeyWidth = "RNTIOImageKeyWidth"
const val RNTIOImageKeyHeight = "RNTIOImageKeyHeight"
const val RNTIOImageKeyOrientation = "RNTIOImageKeyOrientation"

class TensorioTfliteModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  enum class ImageFormat(val value: Int) {
    Unknown (0),
    ARGB    (1),
    BGRA    (2),
    JPEG    (3),
    PNG     (4),
    File    (5),
    Asset   (6);

    companion object {
      fun valueOf(value: Int) = ImageFormat.values().first { it.value == value }
    }
  }

  enum class ImageOrientation(val value: Int) {
    Up            (1),
    UpMirrored    (2),
    Down          (3),
    DownMirrored  (4),
    LeftMirrored  (5),
    Right         (6),
    RightMirrored (7),
    Left          (8);

    companion object {
      fun valueOf(value: Int) = ImageFormat.values().first { it.value == value }
    }
  }

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

  override fun getConstants(): Map<String, Any> {
    val constants: HashMap<String, Any> = HashMap()

    constants["imageKeyData"]         = RNTIOImageKeyData
    constants["imageKeyFormat"]       = RNTIOImageKeyFormat
    constants["imageKeyWidth"]        = RNTIOImageKeyWidth
    constants["imageKeyHeight"]       = RNTIOImageKeyHeight
    constants["imageKeyOrientation"]  = RNTIOImageKeyOrientation

    constants["imageTypeUnknown"]     = ImageFormat.Unknown.value
    constants["imageTypeARGB"]        = ImageFormat.ARGB.value
    constants["imageTypeBGRA"]        = ImageFormat.BGRA.value
    constants["imageTypeJPEG"]        = ImageFormat.JPEG.value
    constants["imageTypePNG"]         = ImageFormat.PNG.value
    constants["imageTypeFile"]        = ImageFormat.File.value
    constants["imageTypeAsset"]       = ImageFormat.Asset.value

    constants["imageOrientationUp"]             = ImageOrientation.Up.value
    constants["imageOrientationUpMirrored"]     = ImageOrientation.UpMirrored.value
    constants["imageOrientationDown"]           = ImageOrientation.Down.value
    constants["imageOrientationDownMirrored"]   = ImageOrientation.DownMirrored.value
    constants["imageOrientationLeftMirrored"]   = ImageOrientation.LeftMirrored.value
    constants["imageOrientationRight"]          = ImageOrientation.Right.value
    constants["imageOrientationRightMirrored"]  = ImageOrientation.RightMirrored.value
    constants["imageOrientationLeft"]           = ImageOrientation.Left.value

    return constants
  }

  /**
   * Bridged method that loads a model given a model path. Relative paths will be loaded as assets
   * from the application bundle.
   */

  @ReactMethod
  fun load(path: String, name: String?, promise: Promise) {
    val hashname = name ?: path

    try {

      // Reject if model with name is already loaded

      if (models[hashname] != null) {
        promise.reject("ai.doc.tensorio.tflite.rn:load:name-in-use", "Name already use. Use a different name, and do not reload a model without unloading it first")
        return
      }

      // Load the model

      val bundle = if (isAbsoluteFilepath(path)) {
        ModelBundle.bundleWithFile(File(path))
      } else {
        ModelBundle.bundleWithAsset(reactApplicationContext, path)
      }

      val model = bundle.newModel() as TFLiteModel

      // Cache it

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
      promise.reject("ai.doc.tensorio.tflite.rn:unload:model-not-found", "No model with this name was found")
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
      promise.reject("ai.doc.tensorio.tflite.rn:run:model-not-found", "No model with this name was found")
      return
    }

    val model = models[name] as Model
    val hashmap = MapUtil.toMap(data)

    // Ensure that the provided keys match the model's expected keys, or return an error

    if (!model.io.inputs.keys().equals(hashmap.keys)) {
      promise.reject("ai.doc.tensorio.tflite.rn:run:input-mismatch", "Provided inputs do not match expected inputs")
      return
    }

    // Prepare inputs, converting base64 encoded image data or reading image data from the filesystem

    val preparedInputs = preparedInputs(hashmap, model)

    if (preparedInputs == null) {
      promise.reject("ai.doc.tensorio.tflite.rn:run:prepared-inputs", "Unable to prepare inputs from react native for inference")
      return
    }

    // Perform inference

    val outputs: Map<String, Any>

    try {
      outputs = model.runOn(preparedInputs)
    } catch (e: Exception) {
      promise.reject("ai.doc.tensorio.tflite.rn:run:inference-exception", e)
      return
    }

    // Prepare outputs, converting pixel buffer outputs to base64 encoded jpeg string data

    val preparedOutputs = preparedOutputs(outputs, model)

    if (preparedOutputs == null) {
      promise.reject("ai.doc.tensorio.tflite.rn:run:prepared-outputs", "Unable to prepare outputs for return to react native")
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

  /**
   * Returns true if this is an absolute filepath, false otherwise. Filepaths that are not absolute
   * will be treated as paths to Assets rather than files on the filesystem.
   */

  fun isAbsoluteFilepath(path: String): Boolean {
    return path.startsWith("file:/") || path.startsWith("/")
  }

  // Input/Output Preparation

  /**
   * Prepares the model inputs sent from javascript for inference. Image inputs are encoded
   * as a base64 string and must be decoded and converted to pixel buffers. Other inputs are
   * taken as is.
   */

  private fun preparedInputs(inputs: Map<String, Any>, model: Model): Map<String, Any>? {
    val preparedInputs = HashMap<String, Any>()
    var error = false

    for (layer in model.io.inputs.all()) {
      layer.doCase(
        {
          // Vector layer
          preparedInputs[layer.name] = inputs[layer.name] as Any
        },
        {
          // Image layer
          val bitmap = bitmapForInput(inputs[layer.name] as Map<String, Any>)
          if (bitmap != null) {
            preparedInputs[layer.name] = bitmap
          } else {
            error = true
          }
        },
        {
          // Bytes layer
          preparedInputs[layer.name] = inputs[layer.name] as Any
        })
    }

    if (error) {
      return null
    }

    return preparedInputs
  }

  /**
   * Prepares the model outputs for consumption by javascript. Pixel buffer outputs are converted
   * to base64 strings. Other outputs are taken as is.
   */

  private fun preparedOutputs(outputs: Map<String, Any>, model: Model): Map<String, Any>? {
    val preparedOutputs = HashMap<String, Any>()
    var error = false

    for (layer in model.io.outputs.all()) {
      layer.doCase(
        {
          // Vector layer
          preparedOutputs[layer.name] = outputs[layer.name] as Any
        },
        {
          // Image layer
          val encoded = base64JPEGDataForBitmap(outputs[layer.name] as Bitmap)
          if (encoded != null) {
            preparedOutputs[layer.name] = encoded
          } else {
            error = true
          }
        },
        {
          // Bytes layer
          preparedOutputs[layer.name] = outputs[layer.name] as Any
        })
    }

    if (error) {
      return null
    }

    return preparedOutputs
  }

  // Image Utilities

  /**
   * Converts a pixel buffer output to a base64 encoded string that can be consumed by React Native.
   * See https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string
   */

  private fun base64JPEGDataForBitmap(bitmap: Bitmap): String? {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

    val bytes = stream.toByteArray()
    return Base64.encodeToString(bytes, Base64.DEFAULT)
  }

  /**
   * Converts base64 encoded JPEG or PNG to Bitmap
   */

  private fun bitmapForBase64Data(string: String): Bitmap? {
    val bytes = Base64.decode(string, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  }

  /**
   * Converts base64 encoded pixels to Bitmap
   */

  private fun bitmapForBase64Pixels(string: String, width: Int, height: Int, format: Bitmap.Config): Bitmap {
    val bytes = Base64.decode(string, Base64.DEFAULT)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val intbuffer = ByteBuffer.wrap(bytes).asIntBuffer()
    val ints = IntArray(intbuffer.remaining())

    intbuffer.get(ints)
    bitmap.setPixels(ints, 0, 0, 0, 0, width, height)

    return bitmap
  }

  /**
   * Prepares a pixel buffer input given an image encoding dictionary sent from javascript,
   * converting a base64 encoded string or reading data from the file system.
   */

  // TODO: Test all conversions

  private fun bitmapForInput(input: Map<String, Any>): Bitmap? {
    val format = ImageFormat.valueOf((input["RNTIOImageKeyFormat"] as Double).toInt())

    return when (format) {
      ImageFormat.ARGB -> {
        val string = input[RNTIOImageKeyData] as String
        val width = (input[RNTIOImageKeyWidth] as Double).toInt()
        val height = (input[RNTIOImageKeyHeight] as Double).toInt()
        bitmapForBase64Pixels(string, width, height, Bitmap.Config.ARGB_8888)
      }
      ImageFormat.BGRA -> {
        val string = input[RNTIOImageKeyData] as String
        val width = (input[RNTIOImageKeyWidth] as Double).toInt()
        val height = (input[RNTIOImageKeyHeight] as Double).toInt()
        bitmapForBase64Pixels(string, width, height, Bitmap.Config.ARGB_8888)
      }
      ImageFormat.JPEG -> {
        val base64 = input[RNTIOImageKeyData] as String
        bitmapForBase64Data(base64)
      }
      ImageFormat.PNG -> {
        val base64 = input[RNTIOImageKeyData] as String
        bitmapForBase64Data(base64)
      }
      ImageFormat.File -> {
        val filepath = input[RNTIOImageKeyData] as String
        BitmapFactory.decodeFile(filepath)
      }
      ImageFormat.Asset -> {
        val name = input[RNTIOImageKeyData] as String
        val stream = reactApplicationContext.assets.open(name)
        BitmapFactory.decodeStream(stream);
      }
      ImageFormat.Unknown -> {
        null
      }
    }
  }
}
