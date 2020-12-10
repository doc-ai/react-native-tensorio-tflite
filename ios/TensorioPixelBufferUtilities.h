//
//  TensorioPixelBufferUtilities.h
//  TensorioTflite
//
//  Created by Philip Dow on 12/10/20.
//  Copyright © 2020 Facebook. All rights reserved.
//

#ifndef TensorioPixelBufferUtilities_h
#define TensorioPixelBufferUtilities_h

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <VideoToolbox/VideoToolbox.h>

NS_ASSUME_NONNULL_BEGIN

_Nullable CVPixelBufferRef CreatePixelBufferWithBytes(unsigned char *bytes, size_t width, size_t height, OSType format);

NS_ASSUME_NONNULL_END

#endif /* TensorioPixelBufferUtilities_h */
