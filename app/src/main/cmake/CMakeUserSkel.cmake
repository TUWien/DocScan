# If you want to use prefix paths with cmake, copy and rename this file to CMakeUser.cmake
# Do not add this file to GIT!

# set your preferred OpenCV Library path
IF (CMAKE_CL_64)
    SET(CMAKE_PREFIX_PATH ${CMAKE_PREFIX_PATH} "C:/VSProjects/OpenCV3/build2015-x64")
ELSE ()
    SET(CMAKE_PREFIX_PATH ${CMAKE_PREFIX_PATH} "C:/VSProjects/OpenCV3/build2015-x86")
ENDIF ()
