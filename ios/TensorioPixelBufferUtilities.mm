//
//  TensorioPixelBufferUtilities.m
//  TensorioTflite
//
//  Created by Philip Dow on 12/10/20.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "TensorioPixelBufferUtilities.h"

// Format must be kCVPixelFormatType_32ARGB or kCVPixelFormatType_32BGRA
// You must call CFRelease on the pixel buffer

_Nullable CVPixelBufferRef CreatePixelBufferWithBytes(unsigned char *bytes, size_t width, size_t height, OSType format) {
    size_t bytes_per_row = width * 4; // ARGB and BGRA are four channel formats
    size_t byte_count = height * bytes_per_row;
    
    CVPixelBufferRef pixelBuffer;
    
    CVReturn status = CVPixelBufferCreate(
        kCFAllocatorDefault,
        width,
        height,
        format,
        NULL,
        &pixelBuffer);
    
    if ( status != kCVReturnSuccess ) {
        return NULL;
    }
    
    CVPixelBufferLockBaseAddress(pixelBuffer, kNilOptions);
    unsigned char *base_address = (unsigned char *)CVPixelBufferGetBaseAddress(pixelBuffer);
    
    memcpy(base_address, bytes, byte_count);
    
    CVPixelBufferUnlockBaseAddress(pixelBuffer, kNilOptions);
    
    return pixelBuffer;
}
