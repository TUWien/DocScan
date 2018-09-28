# DocScan
App for document scanning.

+

<a href='https://play.google.com/store/apps/details?id=at.ac.tuwien.caa.docscan&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width="250px"/></a>

## Authors
Fabian Hollaus,
Florian Kleber,
Markus Diem

## Build Instructions for Android Studio
### Preparation:
- You need to install Android NDK if you do not have it
- Download and unzip OpenCV for Android (https://opencv.org/releases.html)
- Clone the project and open it in Android Studio
- You will see a dialog "Gradle settings for this project are not configured yet. Would you like the project to use the Gradle wrapper? ..." - click Ok
### Import the OpenCV module
- In Android Studio: 'File' -> 'New' -> 'Import Module'
- Set 'Source directory' to: `{your_local_path}/OpenCV-android-sdk/sdk/java`
- Set 'Module name' to: `openCVLibrary` (Note: Do not include any version number here. If Android studio is saying that there is such a module existing, delete the auto generated openCVLibrary folder in the app root folder.)
- Open 'openCVLibrary/build.gradle'
- Correct the version numbers of `compileSdkVersion`, `minSdkVersion`, `targetSdkVersion` so that it matches the version numbers in your app build.gradle (You can also delete the buildToolsVersion since this is not needed by gradle anymore)

### Set the OpenCV SDK path
- copy `\app\src\main\jni\local\AndroidSkel.mk` to `.\app\src\main\jni\local\Android.mk`
- open `.\app\src\main\jni\local\Android.mk`
- uncomment and change the following line so that it points to your opencv installation (contains the folders: apk, sample, sdk): `# MY_OPENCVROOT:= C:/somepath/OpenCV-android-sdk` (Do not forget to remove the `#`!)

### Build the project

## Visual Studio Project (C++ Library)
- Optional for testing the C++ module
- C++ lib for page segmentation and focus measure
- use CMake to create a Visual Studio Project
  - source code path: DocScan/app/src/main
  - binaries path e.g.: DocScan/build2015-x64
  - Specify OpenCV_DIR

## App Dependencies
Google Play services need to be installed to use Firebase Jobdispatcher (https://github.com/firebase/firebase-jobdispatcher-android)
