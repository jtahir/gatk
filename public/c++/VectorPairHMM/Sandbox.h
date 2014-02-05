/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class Sandbox */

#ifndef _Included_Sandbox
#define _Included_Sandbox
#ifdef __cplusplus
extern "C" {
#endif
#undef Sandbox_enableAll
#define Sandbox_enableAll -1LL
/*
 * Class:     Sandbox
 * Method:    jniGetMachineType
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_Sandbox_jniGetMachineType
  (JNIEnv *, jobject);

/*
 * Class:     Sandbox
 * Method:    jniInitializeClassFieldsAndMachineMask
 * Signature: (Ljava/lang/Class;Ljava/lang/Class;J)V
 */
JNIEXPORT void JNICALL Java_Sandbox_jniInitializeClassFieldsAndMachineMask
  (JNIEnv *, jobject, jclass, jclass, jlong);

/*
 * Class:     Sandbox
 * Method:    jniInitializeHaplotypes
 * Signature: (I[LSandbox/JNIHaplotypeDataHolderClass;)V
 */
JNIEXPORT void JNICALL Java_Sandbox_jniInitializeHaplotypes
  (JNIEnv *, jobject, jint, jobjectArray);

/*
 * Class:     Sandbox
 * Method:    jniFinalizeRegion
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_Sandbox_jniFinalizeRegion
  (JNIEnv *, jobject);

/*
 * Class:     Sandbox
 * Method:    jniComputeLikelihoods
 * Signature: (II[LSandbox/JNIReadDataHolderClass;[LSandbox/JNIHaplotypeDataHolderClass;[DI)V
 */
JNIEXPORT void JNICALL Java_Sandbox_jniComputeLikelihoods
  (JNIEnv *, jobject, jint, jint, jobjectArray, jobjectArray, jdoubleArray, jint);

/*
 * Class:     Sandbox
 * Method:    jniClose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_Sandbox_jniClose
  (JNIEnv *, jobject);

/*
 * Class:     Sandbox
 * Method:    doEverythingNative
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_Sandbox_doEverythingNative
  (JNIEnv *, jobject, jstring);

#ifdef __cplusplus
}
#endif
#endif
