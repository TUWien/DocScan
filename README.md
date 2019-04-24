# DocScan
App for document scanning.

<a href='https://play.google.com/store/apps/details?id=at.ac.tuwien.caa.docscan&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width="250px"/></a>

## Authors
Fabian Hollaus,
Florian Kleber,
Markus Diem

## Build Instructions for Android Studio
### Preparation:
- You need to install Android NDK (with CMake) if you do not have it
- Download and unzip OpenCV for Android (https://opencv.org/releases.html)
- The latest tested version is OpenCV 3.4.5
- Clone the project and open it in Android Studio

### Import the OpenCV module
- In Android Studio: 'File' -> 'New' -> 'Import Module'
- Set 'Source directory' to: `{your_local_path}/OpenCV-android-sdk/sdk/java`
- Set 'Module name' to: `openCVLibrary` (Note: Do not include any version number here. If Android studio is saying that there is such a module existing, delete the auto generated openCVLibrary folder in the app root folder.)
- Open 'openCVLibrary/build.gradle'
- Correct the version numbers of `compileSdkVersion`, `minSdkVersion`, `targetSdkVersion` so that it matches the version numbers in your app build.gradle (You can also delete the buildToolsVersion since this is not needed by gradle anymore)

### Setup CMake file
- Copy the file `app/CMakeListsSkel.txt` and paste it to `app/`, name the pasted file `CMakeLists.txt` (do not add it to the git repository!)
- Open `app/src/CMakeLists.txt`
- Change the line `include_directories(enter_your_opencv_path/sdk/native/jni/include)` so that it contains your opencv path

### Create a symlink
- Create a symlink named `jniLibs` in `app/src/main` that points to `{your_local_path}/OpenCV-android-sdk/native/libs`
- Fill in your required architecture in `app/build.gradle` under `abiFilters`

### Sync and build the project

## API keys
The app makes use of two APIs that require keys which are not published in the repository and should never be provided to the public. Instead not working dummy keys are provided in the following files:
- gradle.properties: contains the key for Dropbox integration
- google-services.json: contains the key for Firebase integration (needed for OCR)

You can get the API key if you send a mail to docscan@cvl.tuwien.ac.at. Before you replace the dummy keys, assure that you do not commit the keys with the following commands:
- git update-index --assume-unchanged google-services.json
- git update-index --assume-unchanged gradle.properties

## Visual Studio Project (C++ Library)
- Optional for testing the C++ module
- C++ lib for page segmentation and focus measure
- use CMake to create a Visual Studio Project
  - source code path: DocScan/app/src/main
  - binaries path e.g.: DocScan/build2015-x64
  - Specify OpenCV_DIR

## App Dependencies
Google Play services need to be installed to use Firebase Jobdispatcher (https://github.com/firebase/firebase-jobdispatcher-android)
