# DocScan
App for document scanning.

<a href='https://play.google.com/store/apps/details?id=at.ac.tuwien.caa.docscan&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' width="250px"/></a>

## Authors
Fabian Hollaus,
Florian Kleber,
Markus Diem

## Build Instructions
DocScan makes use of native (C++) OpenCV and OpenCV Java API. The Java part is automatically downloaded after syncing.
### Download native library
For the native part you have to clone this repo:

```shell
git clone https://github.com/hollaus/opencv_libs.git opencv_native
```
### Add native library to the project
Next you have to copy two folders from the native library to the project.
- copy `opencv_native/sdk/native/libs` to `app/src/main/jniLibs`
- copy `opencv_native/sdk/native/jni/include` to `app/src/main/include`

Alternatively, you can create symlinks - as written below. It is assumed that your project root folder (DocScan) and the opencv_native folder are on the same hierarchy level.

Windows:

```shell
cd app/src/main
mklink /d jniLibs ..\..\..\..\opencv_native\sdk\native\libs
mklink /d include ..\..\..\..\opencv_native\sdk\native\jni\include
```

Linux:
```shell
cd app/src/main
ln -s ../../../opencv_native/sdk/native/libs jniLibs
ln -s ../../../opencv_native/sdk/native/jni/include include
```


### Sync and build the project

## API keys
The app makes use of two APIs that require keys which are not published in the repository and should never be provided to the public. Instead not working dummy keys are provided in the following files:
- `gradle.properties`: contains the key for Dropbox integration
- `google-services.json`: contains the key for Firebase integration (needed for OCR)

You can get the API key if you send a mail to docscan@cvl.tuwien.ac.at. Before you replace the dummy keys, assure that you do not commit the keys with the following commands:
- `git update-index --assume-unchanged google-services.json`
- `git update-index --assume-unchanged gradle.properties`

## Visual Studio Project (C++ Library)
- Optional for testing the C++ module
- C++ lib for page segmentation and focus measure
- use CMake to create a Visual Studio Project
  - source code path: DocScan/app/src/main
  - binaries path e.g.: DocScan/build2015-x64
  - Specify OpenCV_DIR
