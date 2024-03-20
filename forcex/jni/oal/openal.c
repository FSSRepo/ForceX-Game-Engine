#include <jni.h>
#include <sys/types.h>
#include "AL/al.h"
#include "AL/alc.h"
#include "AL/alext.h"
#include "AL/efx.h"

/*
						Open AL Context
*/

JNIEXPORT jboolean JNICALL Java_com_forcex_audio_ALC_create(JNIEnv* env, jobject thiz){
		ALCdevice *device;
   		ALCcontext *ctx;
		device = alcOpenDevice(NULL);
    	if(!device){
        	return JNI_FALSE;
   		}
		ctx = alcCreateContext(device, NULL);
   		if(ctx == NULL || alcMakeContextCurrent(ctx) == ALC_FALSE)
    	{
        	if(ctx != NULL){
          	  alcDestroyContext(ctx);
			}
        	alcCloseDevice(device);
        	return JNI_FALSE;
   		 }
		 return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_forcex_audio_ALC_destroy(JNIEnv* env, jobject thiz){
		ALCdevice *device;
   		ALCcontext *ctx;
		ctx = alcGetCurrentContext();
		if(ctx == NULL){
			return;
		}
		device = alcGetContextsDevice(ctx);
		alcMakeContextCurrent(NULL);
		alcDestroyContext(ctx);
		alcCloseDevice(device);
}
/*
							Open AL API
*/

/*
		Open AL Sources
*/

JNIEXPORT jint JNICALL Java_com_forcex_audio_AL11_alGenSource(JNIEnv* env, jobject thiz){
	ALuint src;
	alGenSources(1,&src);
	return src;
}

JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alDeleteSource(JNIEnv* env, jobject thiz,jint src){
	alDeleteSources(1,&src);
}

JNIEXPORT jboolean JNICALL Java_com_forcex_audio_AL11_alIsSource(JNIEnv* env, jobject thiz,jint src){
	jboolean result = alIsSource(src);
	return result;
}

JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alSourcef(JNIEnv* env, jobject thiz,jint source,jint pname,jfloat v){
	alSourcef(source,pname,v);
}

JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alSource3f(JNIEnv* env, jobject thiz,jint source,jint pname,jfloat v1,jfloat v2,jfloat v3){
	alSource3f(source,pname,v1,v2,v3);
}

JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alSource3i(JNIEnv* env, jobject thiz,jint source,jint pname,jint v1,jint v2,jint v3){
	alSource3i(source,pname,v1,v2,v3);
}

JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alSourcei(JNIEnv* env, jobject thiz,jint source,jint pname,jint v){
	alSourcei(source,pname,v);
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alSourcePlay(JNIEnv* env, jobject thiz,jint source){
	alSourcePlay(source);
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alSourcePause(JNIEnv* env, jobject thiz,jint source){
	alSourcePause(source);
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alSourceStop(JNIEnv* env, jobject thiz,jint source){
	alSourceStop(source);
}
JNIEXPORT jfloat JNICALL Java_com_forcex_audio_AL11_alGetSourcef(JNIEnv* env, jobject thiz,jint source,jint pname){
	ALfloat v;
	alGetSourcef(source,pname,&v);
	return v;
}
JNIEXPORT jfloatArray JNICALL Java_com_forcex_audio_AL11_alGetSource3f(JNIEnv* env, jobject thiz,jint source,jint pname){
	jfloatArray val = (*env)->NewFloatArray(env,3);
	float* cval = (float*)(*env)->GetPrimitiveArrayCritical(env,val,0);
	alGetSource3f(source,pname,&cval[0],&cval[1],&cval[2]);
	(*env)->ReleasePrimitiveArrayCritical(env,val,cval,0);
	return val;
}
JNIEXPORT jint JNICALL Java_com_forcex_audio_AL11_alGetSourcei(JNIEnv* env, jobject thiz,jint source,jint pname){
	ALint v;
	alGetSourcei(source,pname,&v);
	return v;
}

/*
		Open AL buffers
*/
JNIEXPORT jint JNICALL Java_com_forcex_audio_AL11_alGenBuffer(JNIEnv* env, jobject thiz){
	ALuint buf;
	alGenBuffers(1,&buf);
	return buf;
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alDeleteBuffer(JNIEnv* env, jobject thiz,jint buffer){
	alDeleteBuffers(1,&buffer);
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alBufferData
(JNIEnv* env, jobject obj,jint buffer,jint format,jbyteArray bufferData,jint frequency){
	ALubyte* pBufferData = (ALubyte*) (*env)->GetPrimitiveArrayCritical(env, bufferData,0);
	alBufferData((ALuint)buffer, (ALenum)format, pBufferData,(ALsizei)(*env)->GetArrayLength(env,bufferData), (ALsizei)frequency);
	(*env)->ReleasePrimitiveArrayCritical(env,bufferData,pBufferData,0);
}
/*
		Open AL Listener
*/
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alListenerf(JNIEnv* env, jobject thiz,jint pname,jfloat v){
	alListenerf(pname,v);
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alListener3f(JNIEnv* env, jobject thiz,jint pname,jfloat v1,jfloat v2,jfloat v3){
	alListener3f(pname,v1,v2,v3);
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alListenerfv(JNIEnv* env, jobject thiz,jint pname,jfloatArray values){
	ALfloat* cval = (ALfloat*)(*env)->GetPrimitiveArrayCritical(env,values,0);
	alListenerfv(pname,cval);
	(*env)->ReleasePrimitiveArrayCritical(env,values,cval,0);
}
JNIEXPORT jfloat JNICALL Java_com_forcex_audio_AL11_alGetListenerf(JNIEnv* env, jobject thiz,jint pname){
	ALfloat v;
	alGetListenerf(pname,&v);
	return v;
}
JNIEXPORT jfloatArray JNICALL Java_com_forcex_audio_AL11_alGetListener3f(JNIEnv* env, jobject thiz,jint pname){
	jfloatArray val = (*env)->NewFloatArray(env,3);
	float* cval = (float*)(*env)->GetPrimitiveArrayCritical(env,val,0);
	alGetListener3f(pname,&cval[0],&cval[1],&cval[2]);
	(*env)->ReleasePrimitiveArrayCritical(env,val,cval,0);
	return val;
}
JNIEXPORT jfloatArray JNICALL Java_com_forcex_audio_AL11_alGetListenerfv(JNIEnv* env, jobject thiz,jint pname,jint len){
	jfloatArray val = (*env)->NewFloatArray(env,len);
	ALfloat* cval = (ALfloat*)(*env)->GetPrimitiveArrayCritical(env,val,0);
	alGetListenerfv(pname,cval);
	(*env)->ReleasePrimitiveArrayCritical(env,val,cval,0);
	return val;
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alListener3i(JNIEnv* env, jobject thiz,jint pname,jint v1,jint v2,jint v3){
	alListener3i(pname,v1,v2,v3);
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alListeneri(JNIEnv* env, jobject thiz,jint pname,jint v){
	alListeneri(pname,v);
}
/*
			Open AL Core
*/
JNIEXPORT jstring JNICALL Java_com_forcex_audio_AL11_alGetString(JNIEnv* env, jobject thiz,jint pname){
		return (*env)->NewStringUTF(env,alGetString(pname));
}
JNIEXPORT jint JNICALL Java_com_forcex_audio_AL11_alGetError(JNIEnv* env, jobject thiz){
	return alGetError();
}
JNIEXPORT jboolean JNICALL Java_com_forcex_audio_AL11_alIsExtensionPresent(JNIEnv* env, jobject thiz,jstring extension){
	const char* strExtension = (*env)->GetStringUTFChars(env, extension, NULL);
    jboolean result = alIsExtensionPresent(strExtension);
    (*env)->ReleaseStringUTFChars(env, extension, strExtension);
    return result; 
}
JNIEXPORT void JNICALL Java_com_forcex_audio_AL11_alDistanceModel(JNIEnv* env, jobject thiz,jint model){
	alDistanceModel(model);
}
