#import "TensorioTflite.h"

#import <TensorIO/TIOModel.h>
#import <TensorIO/TIOModelBackend.h>

@implementation TensorioTflite

RCT_EXPORT_MODULE()

// Example method
// See // https://facebook.github.io/react-native/docs/native-modules-ios
RCT_REMAP_METHOD(multiply,
                 multiplyWithA:(nonnull NSNumber*)a withB:(nonnull NSNumber*)b
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)
{
    NSString *backend = [TIOModelBackend availableBackend];
    
    NSNumber *result = @([a floatValue] * [b floatValue]);
    resolve(result);
}

@end
