#import "TensorioTflite.h"
#import "TensorioPixelBufferUtilities.h"

#import <TensorIO/NSDictionary+TIOExtensions.h>
#import <TensorIO/UIImage+TIOCVPixelBufferExtensions.h>

#import <TensorIO/TIOLayerInterface.h>
#import <TensorIO/TIOModel.h>
#import <TensorIO/TIOModelBackend.h>
#import <TensorIO/TIOModelIO.h>
#import <TensorIO/TIOModelModes.h>
#import <TensorIO/TIOPixelBuffer.h>

/// Image input keys.

static NSString * const RNTIOImageKeyData =         @"RNTIOImageKeyData";
static NSString * const RNTIOImageKeyFormat =       @"RNTIOImageKeyFormat";
static NSString * const RNTIOImageKeyWidth =        @"RNTIOImageKeyWidth";
static NSString * const RNTIOImageKeyHeight =       @"RNTIOImageKeyHeight";
static NSString * const RNTIOImageKeyOrientation =  @"RNTIOImageKeyOrientation";

/// Supported image encodings.

typedef NS_ENUM(NSInteger, RNTIOImageDataType) {
    RNTIOImageDataTypeUnknown,
    RNTIOImageDataTypeARGB,
    RNTIOImageDataTypeBGRA,
    RNTIOImageDataTypeJPEG,
    RNTIOImageDataTypePNG,
    RNTIOImageDataTypeFile,
    RNTIOImageDataTypeAsset
};

// MARK: -

@implementation RCTConvert (RNTensorIOEnumerations)

/// Bridged constants for supported image encodings. React Native images are
/// encoded as base64 strings and their format must be specified for image inputs.

RCT_ENUM_CONVERTER(RNTIOImageDataType, (@{
    @"imageTypeUnknown": @(RNTIOImageDataTypeUnknown),
    @"imageTypeARGB":    @(RNTIOImageDataTypeARGB),
    @"imageTypeBGRA":    @(RNTIOImageDataTypeBGRA),
    @"imageTypeJPEG":    @(RNTIOImageDataTypeJPEG),
    @"imageTypePNG":     @(RNTIOImageDataTypePNG),
    @"imageTypeFile":    @(RNTIOImageDataTypeFile),
    @"imageTypeAsset":   @(RNTIOImageDataTypeAsset)
}), RNTIOImageDataTypeUnknown, integerValue);

/// Bridged constants for suppoted image orientations. Most images will be
/// oriented 'Up', and that is the default value, but images coming directly
/// from a camera pixel buffer will be oriented 'Right'.

RCT_ENUM_CONVERTER(CGImagePropertyOrientation, (@{
    @"imageOrientationUp":              @(kCGImagePropertyOrientationUp),
    @"imageOrientationUpMirrored":      @(kCGImagePropertyOrientationUpMirrored),
    @"imageOrientationDown":            @(kCGImagePropertyOrientationDown),
    @"imageOrientationDownMirrored":    @(kCGImagePropertyOrientationDownMirrored),
    @"imageOrientationLeftMirrored":    @(kCGImagePropertyOrientationLeftMirrored),
    @"imageOrientationRight":           @(kCGImagePropertyOrientationRight),
    @"imageOrientationRightMirrored":   @(kCGImagePropertyOrientationRightMirrored),
    @"imageOrientationLeft":            @(kCGImagePropertyOrientationLeft)
}), kCGImagePropertyOrientationUp, integerValue);

@end

// MARK: -

@implementation TensorioTflite

RCT_EXPORT_MODULE()

/// Bridged method that loads a model given a model path. Relative paths will be loaded from the application bundle.

RCT_EXPORT_METHOD(load:(NSString*)path
                  name:(nullable NSString*)name
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject) {
    
    if (self.models == nil) {
        self.models = [[NSMutableDictionary alloc] init];
    }
    
    if (name == nil) {
        name = path;
    }
    
    if (self.models[name] != nil) {
        // TODO: reject
        return;
    }
    
    // TODO: Error Checking
    
    TIOModelBundle *bundle = [[TIOModelBundle alloc] initWithPath:[self absolutePath:path]];
    self.models[name] = bundle.newModel;
    resolve(@YES);
}

/// Bridged method that unloads a model, freeing the underlying resources.

RCT_EXPORT_METHOD(unload:(NSString*)name
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject) {
    
    if (self.models[name] == nil) {
        // TODO: Reject
        return;
    }
    
    [self.models[name] unload];
    self.models[name] = nil;
    resolve(@YES);
}

/// Bridged method returns YES if model is trainable, NO otherwise
/// TF Lite models will always return NO

RCT_EXPORT_METHOD(isTrainable:(NSString*)name
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@(self.models[name].modes.trains));
}

/// Bridged methods that performs inference with a loaded model and returns the results.

RCT_EXPORT_METHOD(run:(NSString*)name
                  data:(NSDictionary*)inputs
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject) {
    
    // TODO: Proper error handling
    
    id<TIOModel> model = self.models[name];
    
    // Ensure that a model has been loaded
    
    if (model == nil) {
        NSString *msg = @"No model has been loaded. Call load() with the name of a model before calling run().";
        NSError *error = [NSError errorWithDomain:@"" code:0 userInfo:nil];
        reject(@"", msg, error);
        return;
    }
    
    // Ensure that the provided keys match the model's expected keys, or return an error
    
    NSSet<NSString*> *expectedKeys = [NSSet setWithArray:[self inputKeysForModel:model]];
    NSSet<NSString*> *providedKeys = [NSSet setWithArray:inputs.allKeys];
    
    if (![expectedKeys isEqualToSet:providedKeys]) {
        NSString *msg = [NSString stringWithFormat:@"Provided inputs %@ don't match model's expected inputs %@", providedKeys, expectedKeys];
        NSError *error = [NSError errorWithDomain:@"" code:0 userInfo:nil];
        reject(@"", msg, error);
        return;
    }
    
    // Prepare inputs, converting base64 encoded image data or reading image data from the filesystem
    
    NSDictionary *preparedInputs = [self preparedInputs:inputs model:model];
    
    if (preparedInputs == nil) {
        NSString *msg = @"There was a problem preparing the inputs. Ensure that your image inputs are property encoded.";
        NSError *error = [NSError errorWithDomain:@"" code:0 userInfo:nil];
        reject(@"", msg, error);
        return;
    }
    
    // Perform inference
    
    // TODO: Error Checking
    
    NSError *error;
    NSDictionary *results = (NSDictionary*)[model runOn:preparedInputs error:&error];
    
    // Prepare outputs, converting pixel buffer outputs to base64 encoded jpeg string data
    
    NSDictionary *preparedResults = [self preparedOutputs:results model:model];
    
    if (preparedResults == nil) {
        NSString *msg = @"There was a problem preparing the outputs. Pixel buffer outputs could not be converted to base64 JPEG string data.";
        NSError *error = [NSError errorWithDomain:@"" code:0 userInfo:nil];
        reject(@"", msg, error);
        return;
    }
    
    // Return results
    
    resolve(preparedResults);
}

/// Returns the names of the model inputs, derived from a model bundle's model.json file.

- (NSArray<NSString*>*)inputKeysForModel:(id<TIOModel>)model {
    NSMutableArray<NSString*> *keys = [[NSMutableArray alloc] init];
    for (TIOLayerInterface *input in model.io.inputs.all) {
        [keys addObject:input.name];
    }
    return keys.copy;
}

// MARK: - Load Utilities

/// Returns the absolute path to a model. If an absolute path is provided it is returned.
/// Otherwise the path will be treated as relative to the application bundle.

- (NSString*)absolutePath:(NSString*)path {
    NSString *absolutePath;

    if ([self isAbsoluteFilepath:path]) {
        absolutePath = path;
    } else {
        if ([path.pathExtension isEqualToString:TIOModelBundleExtension]) {
            path = [path stringByDeletingPathExtension];
        }
        absolutePath = [NSBundle.mainBundle pathForResource:path ofType:TIOModelBundleExtension];
    }

    return absolutePath;
}

/// Returns YES if the path describes an absolute path rather than a relative one.

- (BOOL)isAbsoluteFilepath:(NSString*)path {
    NSString *fullpath = [path stringByExpandingTildeInPath];
    return [fullpath hasPrefix:@"/"] || [fullpath hasPrefix:@"file:/"];
}

// MARK: - Input/Output Preparation

/// Prepares the model inputs sent from javascript for inference. Image inputs
/// are encoded as a base64 string and must be decoded and converted to pixel
/// buffers. Other inputs are taken as is.

- (nullable NSDictionary*)preparedInputs:(NSDictionary*)inputs model:(id<TIOModel>)model {
    
    NSMutableDictionary<NSString*, id<TIOData>> *preparedInputs = [[NSMutableDictionary alloc] init];
    __block BOOL error = NO;
    
    for (TIOLayerInterface *layer in model.io.inputs.all) {
        [layer matchCasePixelBuffer:^(TIOPixelBufferLayerDescription * _Nonnull pixelBufferDescription) {
            TIOPixelBuffer *pixelBuffer = [self pixelBufferForInput:inputs[layer.name]];
            if (pixelBuffer == nil) {
                error = YES;
            } else {
                preparedInputs[layer.name] = pixelBuffer;
            }
        } caseVector:^(TIOVectorLayerDescription * _Nonnull vectorDescription) {
            preparedInputs[layer.name] = inputs[layer.name];
        } caseString:^(TIOStringLayerDescription * _Nonnull stringDescription) {
            preparedInputs[layer.name] = inputs[layer.name];
        }];
    }
    
    if (error) {
        return nil;
    }
    
    return preparedInputs.copy;
}

/// Prepares the model outputs for consumption by javascript. Pixel buffer outputs
/// are converted to base64 strings. Other outputs are taken as is.

- (nullable NSDictionary*)preparedOutputs:(NSDictionary*)outputs model:(id<TIOModel>)model {
    NSMutableDictionary *preparedOutputs = [[NSMutableDictionary alloc] init];
    __block BOOL error = NO;
    
    for (TIOLayerInterface *layer in model.io.outputs.all) {
        [layer matchCasePixelBuffer:^(TIOPixelBufferLayerDescription * _Nonnull pixelBufferDescription) {
            NSString *base64 = [self base64JPEGDataForPixelBuffer:outputs[layer.name]];
            if (base64 == nil) {
                error = YES;
            } else {
                preparedOutputs[layer.name] = base64;
            }
        } caseVector:^(TIOVectorLayerDescription * _Nonnull vectorDescription) {
            preparedOutputs[layer.name] = outputs[layer.name];
        } caseString:^(TIOStringLayerDescription * _Nonnull stringDescription) {
            preparedOutputs[layer.name] = outputs[layer.name];
        }];
    }
    
    if (error) {
        return nil;
    }
    
    return preparedOutputs.copy;
}

// MARK: - Image Utilities

/// Converts a pixel buffer output to a base64 encoded string that can be consumed by React Native.

- (nullable NSString*)base64JPEGDataForPixelBuffer:(TIOPixelBuffer*)pixelBuffer {
    UIImage *image = [[UIImage alloc] initWithPixelBuffer:pixelBuffer.pixelBuffer];
    NSData *data = UIImageJPEGRepresentation(image, 1.0);
    NSString *base64 = [data base64EncodedStringWithOptions:0];
    return base64;
}

/// Prepares a pixel buffer input given an image encoding dictionary sent from javascript,
/// converting a base64 encoded string or reading data from the file system.

- (nullable TIOPixelBuffer*)pixelBufferForInput:(NSDictionary*)input {
    
    RNTIOImageDataType format = (RNTIOImageDataType)[input[RNTIOImageKeyFormat] integerValue];
    CVPixelBufferRef pixelBuffer;
    
    switch (format) {
    case RNTIOImageDataTypeUnknown: {
        pixelBuffer = NULL;
        }
        break;
    
    case RNTIOImageDataTypeARGB: {
        OSType imageFormat = kCVPixelFormatType_32ARGB;
        NSUInteger width = [input[RNTIOImageKeyWidth] unsignedIntegerValue];
        NSUInteger height = [input[RNTIOImageKeyHeight] unsignedIntegerValue];
        
        NSString *base64 = input[RNTIOImageKeyData];
        NSData *data = [RCTConvert NSData:base64];
        unsigned char *bytes = (unsigned char *)data.bytes;
        
        pixelBuffer = CreatePixelBufferWithBytes(bytes, width, height, imageFormat);
        CFAutorelease(pixelBuffer);
        }
        break;
        
    case RNTIOImageDataTypeBGRA: {
        OSType imageFormat = kCVPixelFormatType_32BGRA;
        NSUInteger width = [input[RNTIOImageKeyWidth] unsignedIntegerValue];
        NSUInteger height = [input[RNTIOImageKeyHeight] unsignedIntegerValue];
        
        NSString *base64 = input[RNTIOImageKeyData];
        NSData *data = [RCTConvert NSData:base64];
        unsigned char *bytes = (unsigned char *)data.bytes;
        
        pixelBuffer = CreatePixelBufferWithBytes(bytes, width, height, imageFormat);
        CFAutorelease(pixelBuffer);
        }
        break;
        
    case RNTIOImageDataTypeJPEG: {
        NSString *base64 = input[RNTIOImageKeyData];
        UIImage *image = [RCTConvert UIImage:base64];
        pixelBuffer = image.pixelBuffer;
        }
        break;
    
    case RNTIOImageDataTypePNG: {
        NSString *base64 = input[RNTIOImageKeyData];
        UIImage *image = [RCTConvert UIImage:base64];
        pixelBuffer = image.pixelBuffer;
        }
        break;
    
    case RNTIOImageDataTypeFile: {
        NSString *path = input[RNTIOImageKeyData];
        NSURL *URL = [NSURL fileURLWithPath:path];
        UIImage *image = [[UIImage alloc] initWithContentsOfFile:URL.path];
        pixelBuffer = image.pixelBuffer;
        }
        break;
            
    case RNTIOImageDataTypeAsset: {
        NSString *name = input[RNTIOImageKeyData];
        UIImage *image = [UIImage imageNamed:name];
        pixelBuffer = image.pixelBuffer;
        }
        break;
    }
    
    // Bail if the pixel buffer could not be created
    
    if (pixelBuffer == NULL)  {
        return nil;
    }
    
    // Derive the image orientation
    
    CGImagePropertyOrientation orientation;
    
    if ([input objectForKey:RNTIOImageKeyOrientation] == nil) {
        orientation = kCGImagePropertyOrientationUp;
    } else {
        orientation = (CGImagePropertyOrientation)[input[RNTIOImageKeyOrientation] integerValue];
    }
    
    // Return the results
    
    return [[TIOPixelBuffer alloc] initWithPixelBuffer:pixelBuffer orientation:orientation];
}

// MARK: - Image Classification Utilities

/// Bridged utility method for image classification models that returns the top N probability label-scores.

// TODO: Remove and implement in javasript

RCT_EXPORT_METHOD(topN:(NSUInteger)count
                  threshold:(float)threshold
                  classifications:(NSDictionary*)classifications
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject) {
    NSDictionary *topN = [classifications topN:count threshold:threshold];
    resolve(topN);
}

// MARK: - React Native Overrides

- (NSDictionary *)constantsToExport {
    return @{
        @"imageKeyData":        RNTIOImageKeyData,
        @"imageKeyFormat":      RNTIOImageKeyFormat,
        @"imageKeyWidth":       RNTIOImageKeyWidth,
        @"imageKeyHeight":      RNTIOImageKeyHeight,
        @"imageKeyOrientation": RNTIOImageKeyOrientation,
        
        @"imageTypeUnknown":    @(RNTIOImageDataTypeUnknown),
        @"imageTypeARGB":       @(RNTIOImageDataTypeARGB),
        @"imageTypeBGRA":       @(RNTIOImageDataTypeBGRA),
        @"imageTypeJPEG":       @(RNTIOImageDataTypeJPEG),
        @"imageTypePNG":        @(RNTIOImageDataTypePNG),
        @"imageTypeFile":       @(RNTIOImageDataTypeFile),
        @"imageTypeAsset":      @(RNTIOImageDataTypeAsset),
        
        @"imageOrientationUp":              @(kCGImagePropertyOrientationUp),
        @"imageOrientationUpMirrored":      @(kCGImagePropertyOrientationUpMirrored),
        @"imageOrientationDown":            @(kCGImagePropertyOrientationDown),
        @"imageOrientationDownMirrored":    @(kCGImagePropertyOrientationDownMirrored),
        @"imageOrientationLeftMirrored":    @(kCGImagePropertyOrientationLeftMirrored),
        @"imageOrientationRight":           @(kCGImagePropertyOrientationRight),
        @"imageOrientationRightMirrored":   @(kCGImagePropertyOrientationRightMirrored),
        @"imageOrientationLeft":            @(kCGImagePropertyOrientationLeft)
    };
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
