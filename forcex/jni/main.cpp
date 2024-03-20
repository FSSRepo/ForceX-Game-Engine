#include <string.h>
#include <jni.h>
#include <math.h>
#include <fstream>
#include <string>
#include <sstream>
#include <vector>
#include <algorithm>
#include <stdlib.h>
#include "dxt/squish.h"
#include "png/lodepng.h"
#include "imageformat.h"

extern "C"
{
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_dxtcompress(JNIEnv* env, jobject thiz,jbyteArray input,jint width,jint height,jint inputFlags) {
		jbyteArray output = env->NewByteArray((jint)squish::GetStorageRequirements(width,height,inputFlags));
		squish::u8* rgba = (squish::u8*)env->GetPrimitiveArrayCritical(input,0);
		squish::u8* blocks = (squish::u8*)env->GetPrimitiveArrayCritical(output,0);
		squish::CompressImage(rgba,width,height,blocks,inputFlags);
		env->ReleasePrimitiveArrayCritical(input,rgba,0);
		env->ReleasePrimitiveArrayCritical(output,blocks,0);
		return output;
	}
	
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_etc1compress(JNIEnv* env, jobject thiz,jbyteArray input,jint width,jint height,jint quality) {
		jbyteArray output = env->NewByteArray((jint)squish::GetStorageRequirements(width,height,squish::kDxt1));
		squish::u8* rgba = (squish::u8*)env->GetPrimitiveArrayCritical(input,0);
		squish::u8* blocks = (squish::u8*)env->GetPrimitiveArrayCritical(output,0);
		etc1compress(rgba,width,height,quality,blocks);
		env->ReleasePrimitiveArrayCritical(input,rgba,0);
		env->ReleasePrimitiveArrayCritical(output,blocks,0);
		return output;
	}
	
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_dxtdecompress(JNIEnv* env, jobject thiz,jbyteArray input,jint width,jint height,jint inputFlags,jboolean lowData) {
		jbyteArray output = env->NewByteArray(width * height * (lowData ? 2 : 4));
		squish::u8* blocks = (squish::u8*)env->GetPrimitiveArrayCritical(input,0);
		if(lowData == JNI_FALSE){
			squish::u8* rgba = (squish::u8*)env->GetPrimitiveArrayCritical(output,0);
			squish::DecompressImage(rgba,width,height,blocks,inputFlags);
			env->ReleasePrimitiveArrayCritical(output,rgba,0);
		}else{
			uint8* rgba = (uint8*)malloc(width*height*4);
			squish::DecompressImage(rgba,width,height,blocks,inputFlags);
			squish::u8* img_out = (squish::u8*)env->GetPrimitiveArrayCritical(output,0);
			convertFormat(rgba,width,height,(inputFlags & squish::kDxt1) != 0,img_out);
			free(rgba);
			env->ReleasePrimitiveArrayCritical(output,img_out,0);
		}
		env->ReleasePrimitiveArrayCritical(input,blocks,0);
		return output;
	}
	
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_convertImageFormat(JNIEnv* env, jobject thiz,jbyteArray input,jint width,jint height,jboolean is565) {
		jbyteArray output = env->NewByteArray(width * height * 2);
		uint8* from = (uint8*)env->GetPrimitiveArrayCritical(input,0);
		uint8* to = (uint8*)env->GetPrimitiveArrayCritical(output,0);
		convertFormat(from,width,height,is565 == JNI_TRUE,to);
		env->ReleasePrimitiveArrayCritical(output,to,0);
		env->ReleasePrimitiveArrayCritical(input,from,0);
		return output;
	}
	
	JNIEXPORT void JNICALL Java_com_forcex_core_CoreJni_pngencode(JNIEnv* env, jobject thiz,jbyteArray input,jstring jpath,jint width,jint height) {
		const char *path = env->GetStringUTFChars(jpath, 0);
		squish::u8* in = (squish::u8*)env->GetPrimitiveArrayCritical(input,0);
		std::vector<squish::u8> image;
 		image.resize(width * height * 4);
		unsigned int size = width * height * 4;
		for(unsigned int i = 0;i < size;i++){
			image[i] = in[i];
		}
		env->ReleasePrimitiveArrayCritical(input,in,0);
		lodepng::encode(std::string(path), image,(unsigned)width,(unsigned)height);
		env->ReleaseStringUTFChars(jpath,path);
	}
	
	JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_pngdecode(JNIEnv* env, jobject thiz,jintArray jout,jstring jpath) {
		const char *path = env->GetStringUTFChars(jpath, 0);
		std::vector<squish::u8> image;
  		unsigned width, height;
 		unsigned error = lodepng::decode(image, width, height, path);
		if(error == 0){
			unsigned int size_total = (width*height*4);
			jbyteArray imageData = env->NewByteArray(size_total);
			squish::u8* idat = (squish::u8*)env->GetPrimitiveArrayCritical(imageData,0);
			for(unsigned int i = 0;i < size_total;i++){
				idat[i] = image[i];
			}
			env->ReleasePrimitiveArrayCritical(imageData,idat,0);
			jint* out = (jint*)env->GetPrimitiveArrayCritical(jout,0);
			out[0] = width;
			out[1] = height;
			env->ReleasePrimitiveArrayCritical(jout,out,0);
			env->ReleaseStringUTFChars(jpath,path);
			return imageData;
		}else{
			jint* out = (jint*)env->GetPrimitiveArrayCritical(jout,0);
			out[0] = error;
			env->ReleasePrimitiveArrayCritical(jout,out,0);
		}
		env->ReleaseStringUTFChars(jpath,path);
		return NULL;
	}
}
