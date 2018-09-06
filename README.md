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
- copy`.\app\src\main\jni\local\AndroidSkel.mk` to `.\app\src\main\jni\local\Android.mk`
- open `.\app\src\main\jni\local\Android.mk` in a text editor
- uncomment and change the line `MY_OPENCVROOT:= C:/somepath/OpenCV-android-sdk` such that
it points to your opencv installation (contains the folders: apk, sample, sdk)
- Open Project from Android Studio
- You will see a dialog "Gradle settings for this project are not configured yet. Would you like the project to use the Gradle wrapper? ..." - click Ok
- Import OpenCV module
  - File -> New -> Import module
  - Set the OpenCV SDK path: .\yourlocalpath\OpenCV-android-sdk\sdk\java
  - Finish.
  - Open project view and from there open Project -> openCVLibrary->build.gradle
  - Set compileSdkVersion, targetSdkVersion and buildToolsVersion to the values of your main build.gradle file.
  See step 6 in [2]
  - Set the Android NDK path
- Add Android NDK
  - Open 'Tools > Android > SDK Manager'
  - Tab SDK Tools
  - Check NDK (takes a few minutes to download/install) (copy the NDK path, you will need it later)
  - Restart Android Studio
  - right click 'app > Open Module Settings > SDK Location'
  - Set the NDK path

### Dropbox integration
The dropbox API key is not provided on github. If you want to build the project and use dropbox, please send a mail to docscan@cvl.tuwien.ac.at and we will send you the API key.

If you received the API key, please assure that you do not commit it to the repository with the following command:

`git update-index --assume-unchanged gradle.properties`

Afterwards, just replace the dummy key in gradle.properties.

### Error messages
  - 'Could not find method android() for arguments...':

    Maybe you have an obsolete android() block in your top-level gradle file. See:
     https://stackoverflow.com/questions/37250493/could-not-find-method-android-for-arguments for solution


### Links
- [1] https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.1.0/OpenCV-3.1.0-android-sdk.zip/download
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
