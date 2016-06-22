# DocScan
App for document scanning.


## Dependencies
- `JDK` v7 or newer
- Install `Android Studio`
- `OpenCV` for Android [1]
- Android NDK

## Configuration
- copy`.\app\src\main\jni\AndroidSkel.mk` to `.\app\src\main\jni\Android.mk`
- open `.\app\src\main\jni\Android.mk` in a text editor
- change `OPENCVROOT:= C:\cvl\dmrz\code\opencv_sdk\OpenCV-android-sdk` such that
it points to your opencv installation
- Open Project from Android Studio
- Import OpenCV module [2]
- Add Android NDK
  - Open `Tools > Android > SDK Manager`
  - Tab SDK Tools
  - Check NDK (takes a few minutes to download/install)
  - In `.\app\build.gradle` change this line:
    `commandLine "C:\\...\\Local\\Android\\Sdk\\ndk-bundle\\ndk-build.cmd",`
    You will find the path in (right click `app > Open Module Settings > SDK Location`)

## Debugging Hint
 `Genymotion` is a nice tool for debugging the app (on your PC)
 - If you wanna debug on PC with Genymotion you must change the architecture:
 - In `.\app\src\main\jni\Application.mk` set: `APP_ABI := x86`
 - Note: By using the x86 the Genymotion emulation is much faster (compared to the standard ADB emulator which emulates arm processors)



## Links
- [1] https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.1.0/OpenCV-3.1.0-android-sdk.zip/download
- [2] https://www.learn2crack.com/2016/03/setup-opencv-sdk-android-studio.html
