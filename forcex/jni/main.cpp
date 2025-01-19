#include <string.h>
#include <jni.h>
#include <math.h>
#include <fstream>
#include <string>
#include <sstream>
#include <stddef.h>
#include <stdio.h>
#include <vector>
#include <algorithm>
#include <stdlib.h>
#include "dxt/squish.h"
#define STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "stb_image.h"
#include "stb_image_write.h"
#include "imageformat.h"

extern "C"
{
    jbyteArray transferCtoJVM(JNIEnv* env, uint8_t* image_data, int width, int height, jintArray jout) {
        // send c data to java heap
        uint32_t size_total = (width * height * 4);
        jbyteArray imageData = env->NewByteArray(size_total);
        uint8_t* java_data = (uint8_t*)env->GetPrimitiveArrayCritical(imageData, 0);
        memcpy(java_data, image_data, size_total);
        env->ReleasePrimitiveArrayCritical(imageData, java_data, 0);
        free(image_data);
        // send image properties
        jint* out = (jint*)env->GetPrimitiveArrayCritical(jout, 0);
        out[0] = width;
        out[1] = height;
        env->ReleasePrimitiveArrayCritical(jout, out, 0);
        return imageData;
    }

	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_dxtcompress(JNIEnv* env, jobject thiz,jbyteArray input,jint width,jint height,jint inputFlags) {
		jbyteArray output = env->NewByteArray((jint)squish::GetStorageRequirements(width,height,inputFlags));
		uint8_t* rgba = (uint8_t*)env->GetPrimitiveArrayCritical(input,0);
		uint8_t* blocks = (uint8_t*)env->GetPrimitiveArrayCritical(output,0);
		squish::CompressImage(rgba, width, height, blocks, inputFlags);
		env->ReleasePrimitiveArrayCritical(input,rgba,0);
		env->ReleasePrimitiveArrayCritical(output,blocks,0);
		return output;
	}
	
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_etc1compress(JNIEnv* env, jobject thiz,jbyteArray input,jint width,jint height,jint quality) {
		jbyteArray output = env->NewByteArray((jint)squish::GetStorageRequirements(width,height,squish::kDxt1));
		uint8_t* rgba = (uint8_t*)env->GetPrimitiveArrayCritical(input, 0);
		uint8_t* blocks = (uint8_t*)env->GetPrimitiveArrayCritical(output, 0);
		etc1compress(rgba,width,height,quality,blocks);
		env->ReleasePrimitiveArrayCritical(input, rgba, 0);
		env->ReleasePrimitiveArrayCritical(output, blocks, 0);
		return output;
	}
	
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_dxtdecompress(JNIEnv* env, jobject thiz, jbyteArray input, jint width, jint height, jint inputFlags, jboolean lowData) {
		jbyteArray output = env->NewByteArray(width * height * (lowData ? 2 : 4));
		uint8_t* blocks = (uint8_t*)env->GetPrimitiveArrayCritical(input,0);
		if(lowData == JNI_FALSE) {
			uint8_t* rgba = (uint8_t*)env->GetPrimitiveArrayCritical(output,0);
			squish::DecompressImage(rgba, width, height, blocks, inputFlags);
			env->ReleasePrimitiveArrayCritical(output, rgba,0);
		} else {
			uint8_t* rgba = (uint8_t*)malloc(width * height * 4);
			squish::DecompressImage(rgba, width, height, blocks, inputFlags);
			uint8_t* img_out = (uint8_t*)env->GetPrimitiveArrayCritical(output,0);
			convertFormat(rgba, width, height, (inputFlags & squish::kDxt1) != 0, img_out);
			free(rgba);
			env->ReleasePrimitiveArrayCritical(output, img_out, 0);
		}
		env->ReleasePrimitiveArrayCritical(input, blocks, 0);
		return output;
	}
	
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_convertImageFormat(JNIEnv* env, jobject thiz,jbyteArray input,jint width,jint height,jboolean is565) {
		jbyteArray output = env->NewByteArray(width * height * 2);
		uint8_t* from = (uint8_t*)env->GetPrimitiveArrayCritical(input, 0);
		uint8_t* to =   (uint8_t*)env->GetPrimitiveArrayCritical(output, 0);
		convertFormat(from, width, height, is565 == JNI_TRUE, to);
		env->ReleasePrimitiveArrayCritical(output, to, 0);
		env->ReleasePrimitiveArrayCritical(input, from, 0);
		return output;
	}
	
	JNIEXPORT void JNICALL Java_com_forcex_core_CoreJni_imageEncode(JNIEnv* env, jobject thiz, jbyteArray input, jstring jpath, jint width, jint height, jint type) {
		const char *path = env->GetStringUTFChars(jpath, 0);
		uint8_t * input_buffer = (uint8_t *)env->GetPrimitiveArrayCritical(input,0);
		if(type == 0) {
		    stbi_write_png(path, width, height, 4, input_buffer, width * 4);
		} else {
		    stbi_write_jpg(path, width, height, 4, input_buffer, 100);
		}
		env->ReleasePrimitiveArrayCritical(input, input_buffer, 0);
		env->ReleaseStringUTFChars(jpath, path);
	}
	
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_imageDecodeFromPath(JNIEnv* env, jobject thiz,jintArray jout,jstring jpath) {
		const char *path = env->GetStringUTFChars(jpath, 0);
  		int32_t width, height, channels;
 		uint8_t* image_data = stbi_load(path, &width, &height, &channels, 4);
		if(image_data) {
		    jbyteArray imageData = transferCtoJVM(env, image_data, width, height, jout);
			env->ReleaseStringUTFChars(jpath, path);
			return imageData;
		}
		env->ReleaseStringUTFChars(jpath, path);
		return NULL;
	}

	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_imageDecodeFromMemory(JNIEnv* env, jobject thiz, jintArray jout, jbyteArray pngData) {
        uint8_t* png_java_c = (uint8_t*)env->GetPrimitiveArrayCritical(pngData, 0);
        int32_t width, height, channels;
        uint8_t* image_data = stbi_load_from_memory(png_java_c, env->GetArrayLength(pngData), &width, &height, &channels, 4);
        if(image_data) {
            jbyteArray imageData = transferCtoJVM(env, image_data, width, height, jout);
            env->ReleasePrimitiveArrayCritical(pngData, png_java_c, 0);
            return imageData;
        }
        env->ReleasePrimitiveArrayCritical(pngData, png_java_c, 0);
        return NULL;
    }
}
