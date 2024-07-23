# 此变量表示源文件在开发树中的位置。
# 在上述命令中，构建系统提供的宏函数 my-dir 将返回当前目录（Android.mk 文件本身所在的目录）的路径。
LOCAL_PATH := $(call my-dir)
# 声明 CLEAR_VARS 变量，其值由构建系统提供
# CLEAR_VARS 变量指向一个特殊的 GNU Makefile，后者会为我们清除许多 LOCAL_XXX 变量
include $(CLEAR_VARS)

# 变量存储您要构建的模块的名称
LOCAL_MODULE := posescore
# 列举源文件，以空格分隔多个文件
LOCAL_SRC_FILES := pose_score.cpp
# 共享库
include $(BUILD_SHARED_LIBRARY)