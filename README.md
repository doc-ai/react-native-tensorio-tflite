# react-native-tensorio-tflite

Tensor/IO for React Native with the TF Lite backend

## Installation

```sh
npm install react-native-tensorio-tflite
```

While in develompent you can add the following to your package.json dependencies section and then npm update or yarn or whatever magic you do:

```json
"react-native-tensorio-tflite": "https://github.com/doc-ai/react-native-tensorio-tflite.git#dev"
```

### iOS

Run pod install in your application's root directory to install the Tensor/IO and TF Lite dependencies.

### Android

Add support for desugaring to your application's main *bundle.gradle* file:

```gradle
android {
  ...
  
  compileOptions {
    coreLibraryDesugaringEnabled true
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}

dependencies {
  coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.5'
  ...
}
```

And add the following aapt and and packaging options:

```
android {
  ...
  
  aaptOptions {
    noCompress "tflite"
  }

  packagingOptions {
    pickFirst "lib/armeabi-v7a/libc++_shared.so"
    pickFirst "lib/arm64-v8a/libc++_shared.so"
    pickFirst "lib/x86/libc++_shared.so"
    pickFirst "lib/x86_64/libc++_shared.so"
	
    pickFirst 'META-INF/ASL-2.0.txt'
    pickFirst 'draftv4/schema'
    pickFirst 'draftv3/schema'
    pickFirst 'META-INF/LICENSE'
    pickFirst 'META-INF/LGPL-3.0.txt'
  }
}
```

Sync the project's gradle files to install the Tensor/IO and TF Lite dependencies.

You may need to increase the amount of heap memory available to the JVM. If you get an error when you build your application with something about the "Java heap space" add the following to your *gradle.properties*:

```gradle
org.gradle.jvmargs=-Xms512M -Xmx4g -XX:MaxPermSize=1024m -XX:MaxMetaspaceSize=1g -Dkotlin.daemon.jvm.options="-Xmx1g"
```

## Usage

```js
import TensorioTflite from "react-native-tensorio-tflite";

// If you're using images acquire the image object constants you need
const { imageKeyFormat, imageKeyData, imageTypeAsset } = TensorioTflite.getConstants();

// In our example we have an image bundled as an image asset on ios and an asset on android
const imageAsset = Platform.OS === 'ios' ? 'elephant' : 'elephant.jpg';

// To use the model load it and give it a name. This model is bundled in the application.
TensorioTflite.load('image-classification.tiobundle', 'classifier');

// Call run using the name you gave the model and its expected inputs.
// All model functions return promises, so you can use `then` on the result and `catch` for errors

TensorioTflite
  .run('classifier', {
    'image': {
      [imageKeyFormat]: imageTypeAsset,
      [imageKeyData]: imageAsset
    }
  })
  .then(output => {
    // Do something with model output
  })
  .catch(error => {
    // Handle error
  });

// When you're done with the model always unload it to free up the underlying resources
TensorioTflite.unload('classifier');
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

Apache 2

## Additional Documentation

### About Tensor/IO

Tensor/IO uses model bundles to wrap an underlying model and a description of its inputs and outputs along with any assets the model requires, such as text labels for image classification outputs. They are simply folders with the *.tiobundle* extension ([learn more](https://github.com/doc-ai/tensorio)). You will need to add these bundles to your React Native application in order to perform inference with the underlying models.

Every Tensor/IO bundle includes a description of the underlying model. Model inputs and outputs are named and indicate what kind of data they expect or produce. You must know these names in order to pass data to the model and extract results from it. From the perspective of a React Native application, you will pass an object to the model whose name-value pairs match the model's input names, and you will receive an object back from the model whose name-value pairs match the model's output names.

All this information appears in a bundle's *model.json* file. Let's have a look at the json description of a simple test model that takes a single input and produces a single output. Notice specifically the *inputs* and *outputs* fields:

```json
{
  "name": "1 in 1 out numeric test",
  "details": "Simple model with a single valued input and single valued output",
  "id": "1_in_1_out_number_test",
  "version": "1",
  "author": "doc.ai",
  "license": "Apache 2.0",
  "model": {
    "file": "model.tflite",
    "backend": "tflite",
    "quantized": false
  },
  "inputs": [
    {
      "name": "x",
      "type": "array",
      "shape": [1],
    }
  ],
  "outputs": [
    {
      "name": "y",
      "type": "array",
      "shape": [1],
    }
  ]
}
```

The *inputs* and *outputs* fields tell us that this model takes a single input named *"x"* whose value is a single number and produces a single output named *"y"* whose value is also a single number. We know that the values are a single number from the shape. Let's see how to use a model like this in your own application.

### Basic Usage

Add a Tensor/IO bundle to your application in Xcode. Simply drag the bundle into the project under the project's primary folder (it will be the folder with the same name as your project). Make sure to check *Copy items if needed*, select *Create folder references*, and that your build target is selected.

Then in javascript, import `TensorioTflite  from 'react-native-tensorio-tflite`. Load the model by providing its name or a fully qualified path, run inference with it, and unload the model when you are done to free the underlying resources.

Again, imagine we had a model that takes a single input named *"x"* with a single value and produces a singe output named *"y"* with a single value:

```json
"inputs": [
  {
    "name": "x",
    "type": "array",
    "shape": [1],
  }
],
"outputs": [
  {
    "name": "y",
    "type": "array",
    "shape": [1],
  }
]
```

We would use this model as follows. Notice that we pass an object to the run function whose name-value pairs match those of the model's inputs and we extract name-value pairs from the results that match those of the model's outputs:

```javascript
import TensorioTflite from "react-native-tensorio-tflite";

TensorioTflite.load('model.tiobundle', 'model');

TensorioTflite
  .run('model', {
    'x': [42]
  })
  .then(output => {
    const y = output['y']
    console.log(y);
  })
  .catch(error => {
    // Handle error
  });

TensorioTflite.unload('model');
```

### Image Models

React Native represents image data as a base64 encoded string. When you pass that data to a model that has image inputs you must include some additional metadata that describes the encoded image. For example, is it JPEG or PNG data, raw pixel buffer data, or a path to an image on the filesystem?

#### About Image Data

Models that take image inputs must receive those inputs in a pixel buffer format. A pixel buffer is an unrolled vector of bytes corresponding to the red-green-blue (RGB) values that define the pixel representation of an image. Image models are trained on these kinds of representations and expect them for inputs.

React Native represents image data in javascript as a base64 encoded string. In order to perform inference with an image model you must provide this base64 encoded string to the run function as well as a description of the those bytes that may included metadata such as the width, height, and format of the image they represent. To run an image model you'll pack this information into a javascript object and use that object in one of the name-value pairs you provide to the run function.

<a name="image-classification-example"></a>
#### An Image Classification Example

Let's look at a basic image classification model and see how to use it in React Native. The JSON description for the ImageNet MobileNet classification model is as follows. Again, pay special attention to the *inputs* and *outputs* fields:

```json
{
  "name": "MobileNet V2 1.0 224",
  "details": "MobileNet V2 with a width multiplier of 1.0 and an input resolution of 224x224. \n\nMobileNets are based on a streamlined architecture that have depth-wise separable convolutions to build light weight deep neural networks. Trained on ImageNet with categories such as trees, animals, food, vehicles, person etc. MobileNets: Efficient Convolutional Neural Networks for Mobile Vision Applications.",
  "id": "mobilenet-v2-100-224-unquantized",
  "version": "1",
  "author": "Andrew G. Howard, Menglong Zhu, Bo Chen, Dmitry Kalenichenko, Weijun Wang, Tobias Weyand, Marco Andreetto, Hartwig Adam",
  "license": "Apache License. Version 2.0 http://www.apache.org/licenses/LICENSE-2.0",
  "model": {
    "file": "model.tflite",
    "backend": "tflite",
    "quantized": false,
  },
  "inputs": [
    {
      "name": "image",
      "type": "image",
      "shape": [224,224,3],
      "format": "RGB",
      "normalize": {
        "standard": "[-1,1]"
      }
    },
  ],
  "outputs": [
    {
      "name": "classification",
      "type": "array",
      "shape": [1,1000],
      "labels": "labels.txt"
    },
  ]
}
```

The *inputs* and *outputs* fields tell us that this model expects a single image input whose name is *"image"* and produces a single output whose name is *"classification"*. You don't need to worry about the image input details. Tensor/IO will take care of preparing an image input for the model using this information. But the output field tells you that the classification output will be a labeled list of 1000 values (1 x 1000 from the shape).

Let's see how to use this model in React Native. Assuming we have some base64 encoded JPEG data:


```js
import TensorioTflite from "react-native-tensorio-tflite";
const { imageKeyFormat, imageKeyData, imageKeyOrientation, imageOrientationUp, imageTypeJPEG } = TensorioTflite.getConstants();

const data = 'data:image/jpeg;base64,' + some.data;
const orientation = imageOrientationUp;
const format = imageTypeJPEG;

TensorioTflite.load('mobilenet.tiobundle', 'model');

TensorioTflite
  .run('model', {
    'image': {
      [imageKeyData]: data,
      [imageKeyFormat]: format,
      [imageKeyOrientation]: orientation
    }
  })
  .then(output => {
    const classifications = output['classification'];
    console.log(classifications);
  })
  .catch(error => {
    // Handle error
  });

TensorioTflite.unload('model');
```

This time we provide an object for the *"image"* name-value pair and this object contains three pieces of information: the base64 encoded string, the format of the underlying data, in this case, JPEG data, and the image's orientation. The names used in this object are exported by the TensorioTflite module along with the supported image orientations and image data types. These are all described in more detail below.

TensorioTflite supports image data in a number of formats. Imagine instead that we have the path to an image on the filesystem. We would run the model as follows, and this time we'll omit the image orientation, which is assumed to be 'Up' by default:

```js
import TensorioTflite from "react-native-tensorio-tflite";
const { imageKeyFormat, imageKeyData, imageTypeFile } = TensorioTflite.getConstants();

const data = '/path/to/image.png';
const format = imageTypeFile;

TensorioTflite.load('mobilenet.tiobundle', 'model');

TensorioTflite
  .run('model', {
    'image': {
      [imageKeyData]: data,
      [imageKeyFormat]: format
    }
  })
  .then(output => {
    const classifications = output['classification'];
    console.log(classifications);
  })
  .catch(error => {
    // Handle error
  });

TensorioTflite.unload('model');
```

Another use case might be real time pixel buffer data coming from a device camera. In this case, and on iOS, the bytes will represent raw pixel data in the BGRA format. This representation tells us nothing else about the image, so we'll also need to specify its width, height, and orientation. On iOS, pixel buffer data coming from the camera is often 640x480 and will be right oriented. We'd run the model as follows:

```js
import TensorioTflite from "react-native-tensorio-tflite";
const { imageKeyFormat, imageKeyData, imageKeyOrientation, imageKeyWidth, imageKeyHeight, imageTypeBGRA, imageOrientationRight } = TensorioTflite.getConstants();

const data; // pixel buffer data as a base64 encoded string
const format = imageTypeBGRA; // ios camera format
const orientation = imageOrientationRight; // ios camera orientation
const width = 640;
const height = 480;

TensorioTflite.load('mobilenet.tiobundle', 'model');

TensorioTflite
  .run('model', {
    'image': {
      [imageKeyData]: data,
      [imageKeyFormat]: format,
      [imageKeyOrientation]: orientation,
      [imageKeyWidth]: width,
      [imageKeyHeight]: height
    }
  })
  .then(output => {
    const classifications = output['classification'];
    console.log(classifications);
  })
  .catch(error => {
    // Handle error
  });
  
TensorioTflite.unload('model');
```

All image models that take image inputs will be run in this manner.

<a name="image-outputs"></a>
#### Image Outputs

Some models will produce image outputs. In this case the value for that output will be provided to javascript as base64 encoded jpeg data. You'll likely need to prefix it as follows before being able to display it:

```js
TensorioTflite
  .run('model', {
    'image': {
      // ...
    }
  }) 
  .then(output => {
    const image = output['image'];
    const data = 'data:image/jpeg;base64,' + image;
  })
  .catch(error => {
    // Handle error
  });
```

## The TensorioTflite Module
  
Listed below are the functions and constants exported by this module.

### Functions

All functions execute synchronously and return promises.

#### load(path, name): Promise\<boolean\>

Loads the model at the given path and assigns it the name. If the path is a relative path the model will be loaded from the application bundle on iOS or as an asset on Android. Use the name with the other module functions that interact with models. Returns a boolean promise that resolves to true if the model was loaded and false otherwise.

Usage:

```js
TensorioTflite.load('model.tiobundle', 'model');
```

#### unload(name): Promise\<boolean\>

Unloads a model and frees the underlying resources. Resolves to true if successful and false otherwise. Always unload models when you are done with them to free the underlying resources associated with them.

Usage:

```js
TensorioTflite.unload('model')
```


#### run(name, input): Promise\<object\>

Perform inference with the model on the input. 

The input must be a javascript object whose name-value pairs match the names expected by the underlying model's inputs and which are described in the model bundle's *model.json* file. The resulting promise resolves to an object that contains the model's output with the key-value pairs as they are described in the bundle's *model.json* file.

Usage:

```js
TensorioTflite
  .run('model', {
    'input': [1]
  })
  .then(output => {
    console.log(output);
  })
  .catch(error => {
    // Handle error
  });
```

#### isTrainable(name): Promise\<boolean\>

Resolve to true or false indicating whether the model is trainable or not. TF Lite models always resolve to false.

Usage:

```js
TensorioTflite.isTrainable('model')
  .then(trains => {
    console.log(trains);
  })
  .catch(error => {
    // Handle error
  });
```

#### topN(count, threshold, classifications, callback): Promise\<object\>

A utility function for image classification models that filters for the results with the highest probabilities above a given threshold. 

Image classification models are often capable of recognizing hundreds or thousands of items and return what is called a softmax probability distribution that describes the likelihood that a recognizable item appears in the image. Often we do not want to know the entire probabilty distribution but only want to know which items have the highest probability of being in the image. Use this function to filter for those items.

Count is the number of items you would like to be returned.

Threshold is the minimum probability value an item should have in order to be returned. If there are fewer than count items above this probability, only that many items will be returned.

Classifications is the output of a classification model.

The promise resolves to an object with the same format as the classifications object passed to the function but only with those key-value pairs which meet the criteria.

Usage:

*Given the results from a model whose output has the name 'classification', filter for the top five probabilities above a threshold of 0.1:*

```js
TensorioTflite
  .run('model', {
    'image': {
      // ...
    }
  })
  .then(output => {
    return TensorioTflite.topN(5, 0.1, output['classification'])
  })
  .then(topN => {
    console.log(topN);
  })
  .catch(error => {
    // Handle error
  });
```

### Constants

#### Image Input Keys

```js
imageKeyData
imageKeyFormat
imageKeyWidth
imageKeyHeight
imageKeyOrientation
```

##### imageKeyData

The data for the image. Must be a base64 encoded string or the fully qualified path to an image on the filesystem.

##### imageKeyFormat

The image format. See supported types below. Pixel buffer data coming directly from an iOS camera will usually have the format `imageOrientationRight`.

##### imageKeyWidth

The width of the underlying image. Only required if the format is `imageTypeARGB` or `imageTypeBGRA`. Pixel buffer data coming directly from an iOS device camera will often have a width of 640.

##### imageKeyHeight

The height of the underlying image. Only required if the format is `imageTypeARGB` or `imageTypeBGRA`. Pixel buffer data coming directly from an iOS device camera will often have a height of 480.

##### imageKeyOrientation

The orientation of the image. See supported formats below. Most images will be `imageOrientationUp`, and this is the default value that is used if this field is not specified. However, pixel buffer data coming directly from an iOS device camera will be `imageOrientationRight`.

#### Image Data Types

```js
imageTypeUnknown
imageTypeARGB
imageTypeBGRA
imageTypeJPEG
imageTypePNG
imageTypeFile
imageTypeAsset
```

##### imageTypeUnknown

A placeholder for an unknown image type. TensorioTflite will return an error if you specify this format.

##### imageTypeARGB

Pixel buffer data whose pixels are unrolled into an alpha-red-green-blue byte representation.

##### imageTypeBGRA

Pixel buffer data whose pixels are unrolled into a blue-green-red-alpha byte representation. Pixel data coming directly from an iOS device camera will usually be in this format.

##### imageTypeJPEG

JPEG image data. The base64 encoded string must be prefixed with `data:image/jpeg;base64,`.

##### imageTypePNG

PNG image data. The base64 encoded string must be prefixed with `data:image/png;base64,`.

##### imageTypeFile

Indicates that the image data will contain the fully qualified path to an image on the filesystem.

##### imageTypeAsset

Indicates that the image data will contain the name of an image asset on iOS and the relative path to an image asset on Android.

#### Image Orientations

```js
imageOrientationUp
imageOrientationUpMirrored
imageOrientationDown
imageOrientationDownMirrored
imageOrientationLeftMirrored
imageOrientationRight
imageOrientationRightMirrored
imageOrientationLeft
```

##### imageOrientationUp

0th row at top, 0th column on left. Default orientation.

##### imageOrientationUpMirrored

0th row at top, 0th column on right. Horizontal flip.

##### imageOrientationDown

0th row at bottom, 0th column on right. 180 degree rotation.

##### imageOrientationDownMirrored 

0th row at bottom, 0th column on left. Vertical flip.

##### imageOrientationLeftMirrored

0th row on left, 0th column at top.

##### imageOrientationRight

0th row on right, 0th column at top. 90 degree clockwise rotation. Pixel buffer data coming from an iOS device camera will usually have this orientation.

##### imageOrientationRightMirrored

0th row on right, 0th column on bottom.

##### imageOrientationLeft

0th row on left, 0th column at bottom. 90 degree counter-clockwise rotation.
