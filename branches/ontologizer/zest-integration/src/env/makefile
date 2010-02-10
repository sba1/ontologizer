#
# makefile for 
#

all: SetEnv.class setenv.h libsetenv.so SetEnvDemo.class

setenv.h: SetEnv.class 
	cd ..; javah -jni -o env/setenv.h env.SetEnv

libsetenv.so:
	gcc -I /usr/lib/j2sdk1.5-sun/include/ -I /usr/lib/j2sdk1.5-sun/include/linux/ -shared -o libsetenv.so setenv.c

SetEnv.class:
	cd ..; javac env/SetEnv.java

SetEnvDemo.class:
	cd ..; javac env/SetEnvDemo.java

clean:
	rm setenv.h libsetenv.so SetEnv.class SetEnvDemo.class
