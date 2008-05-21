/* setenv.c - a basic wrapper for the setenv() functionallity.
 */

#include <jni.h>
#include <stdio.h>

#include "setenv.h"

JNIEXPORT void JNICALL Java_env_SetEnv_setenv_1native
  (JNIEnv *env, jclass class, jstring key, jstring val)
{
	const char *key_str = (*env)->GetStringUTFChars(env, key, NULL);
	const char *val_str = (*env)->GetStringUTFChars(env, val, NULL);

	setenv(key_str, val_str);

	(*env)->ReleaseStringUTFChars(env, key, key_str);
	(*env)->ReleaseStringUTFChars(env, val, val_str);
}
