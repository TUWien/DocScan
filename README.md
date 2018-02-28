# Transkribus DocScan
App for document scanning.

## Authors
Fabian Hollaus,
Florian Kleber,
Markus Diem

## Build Instructions

### Dependencies
- `JDK` v7 or newer
- Install `Android Studio`
- `OpenCV` for Android [1]
- Android NDK (will be installed via Android Studio)

### Configuration
- Setup OpenCV
  - Follow the steps listed here: https://www.learn2crack.com/2016/03/setup-opencv-sdk-android-studio.html in the section 'Download OpenCV Android SDK' and 'Setup OpenCV Android SDK'. (The steps in the other sections are not needed for this project.)
- Tell the project the location of your OpenCV installation:
  - copy`.\app\src\main\jni\local\AndroidSkel.mk` to `.\app\src\main\jni\local\Android.mk`
  - open `.\app\src\main\jni\local\Android.mk` in a text editor
  - uncomment and change the line `MY_OPENCVROOT:= C:/cvl/dmrz/code/opencv_sdk/OpenCV-android-sdk` such that
  it points to your opencv installation (contains the folders: apk, sample, sdk)
- Make sure Android NDK is enabled (`Tools > Android > SDK Manager`)

### Error messages
  - 'Could not find method android() for arguments...':

    Maybe you have an obsolete android() block in your top-level gradle file. See:
     https://stackoverflow.com/questions/37250493/could-not-find-method-android-for-arguments for solution

### Exporting to apk
- Build -> Generate Signed APK
- The APK is now under 'DocScan\app\build\outputs\apk\app-release-unaligned.apk'

### Links
- [1] https://opencv.org/releases.html
- [2] https://www.learn2crack.com/2016/03/setup-opencv-sdk-android-studio.html


## Visual Studio Project (C++ Library)
- Optional
- C++ lib for Page Segmentation and Focus Measure
- use CMake to create a Visual Studio Project
  - Source Code Path: DocScan/app/src/main
  - binaries path e.g.: DocScan/build2015-x64
  - Specify OpenCV_DIR

## App Dependencies
Google Play services need to be installed to use Firebase Jobdispatcher (https://github.com/firebase/firebase-jobdispatcher-android)
