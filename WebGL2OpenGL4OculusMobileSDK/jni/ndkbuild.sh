# Rebuilt (-B) specifying the makefile to use and the output folder for the final libraries and intermediate files
echo "Rebuilding..."
$ANDROID_NDK_PATH/ndk-build NDK_APPLICATION_MK=./Application.mk NDK_LIBS_OUT=../libs NDK_OUT=./objs -B
if [ $? -ne 0 ];then exit $?;fi
echo "Rebuilt!"

