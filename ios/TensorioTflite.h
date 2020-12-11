#import <React/RCTBridgeModule.h>
#import <React/RCTConvert.h>

@protocol TIOModel;

@interface TensorioTflite : NSObject <RCTBridgeModule>

/// maps path or model names to loaded models, allowing the module to load
/// and use more than one model at a time.

@property NSMutableDictionary<NSString*, id<TIOModel>> *models;

@end
