# DocScan
App for document scanning.


## Dependencies
- `JDK` v7 or newer
- Install `Android Studio`
- `OpenCV` for Android [1]
- Android NDK (will be installed via Android Studio)

## Configuration
- copy`.\app\src\main\jni\AndroidSkel.mk` to `.\app\src\main\jni\Android.mk`
- open `.\app\src\main\jni\Android.mk` in a text editor
- change `OPENCVROOT:= C:\cvl\dmrz\code\opencv_sdk\OpenCV-android-sdk` such that
it points to your opencv installation
- Open Project from Android Studio
- You will see a dialog "Gradle settings for this project are not configured yet. Would you like the project to use the Gradle wrapper? ..." - click Ok
- Import OpenCV module [2]
- Add Android NDK
  - Open `Tools > Android > SDK Manager`
  - Tab SDK Tools
  - Check NDK (takes a few minutes to download/install)
  - Restart Android Studio
  - In `.\app\build.gradle` change this line:
    `commandLine "C:\\...\\Local\\Android\\Sdk\\ndk-bundle\\ndk-build.cmd",`
    You will find the path in (right click `app > Open Module Settings > SDK Location`)

## Debugging Hint
 `Genymotion` is a nice tool for debugging the app (on your PC)



## Links
- [1] https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.1.0/OpenCV-3.1.0-android-sdk.zip/download
- [2] https://www.learn2crack.com/2016/03/setup-opencv-sdk-android-studio.html
