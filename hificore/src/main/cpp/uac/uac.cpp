//
// Created by nick on 08.06.25.
//

#include <jni.h>
#include <unistd.h>

extern "C"
JNIEXPORT void JNICALL
Java_org_nift4_gramophone_hificore_UacManager_getDetailsForUsbDevice(JNIEnv *env, jobject thiz, jint inFd) {
	int fd = dup(inFd);
	// TODO: implement getDetailsForUsbDevice()
}