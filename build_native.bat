set CURRENT_DIR=%cd%
echo  %CURRENT_DIR% 
D:\work\android\android-ndk-r8e\ndk-build -C %CURRENT_DIR% NDK_APPLICATION_MK=./jni/Application.mk NDE_DEBUG=1 -j8