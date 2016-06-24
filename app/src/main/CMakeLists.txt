
# check cmake requirements
cmake_minimum_required(VERSION 3.4)

######################## begin of project
project(DocScan)

# load paths from the user file if exists
if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/CMakeUser.cmake)
	include(${CMAKE_CURRENT_SOURCE_DIR}/CMakeUser.cmake)
endif()

set(DOC_SCAN_VERSION 0.1)
add_definitions(-DDOC_SCAN_VERSION="${DOC_SCAN_VERSION}")

option(ENABLE_INCREMENTER "If checked, the version number is incremented on every build (Windows Only)" OFF)

set(CMAKE_INCLUDE_CURRENT_DIR ON)

unset(OpenCV_CONFIG_PATH CACHE)

# OpenCV
SET(OpenCV_REQUIRED_MODULES core imgproc highgui)
SET(OpenCV_LIBS "")

IF (OpenCV_LIBS STREQUAL "")
	find_package(OpenCV)
ENDIF()

IF (NOT OpenCV_FOUND)
	message(FATAL_ERROR "OpenCV not found. Please specify a valid path")
ELSE()
	add_definitions(-DWITH_OPENCV)
	unset(OPENCV_LIBS)
	set(OpenCV_LIBS opencv_core opencv_imgproc opencv_ml opencv_highgui opencv_imgcodecs)
	message(STATUS "${OpenCV_LIBS}")
	message(STATUS "${OpenCV_REQUIRED_MODULES}")
ENDIF()
# OpenCV end

# gather information for building
include_directories (BEFORE SYSTEM
	${OpenCV_INCLUDE_DIRS}
)
include_directories (
	${CMAKE_CURRENT_BINARY_DIR}
	${CMAKE_SOURCE_DIR}/jni
)

file(GLOB DOCSCAN_SOURCES "jni/*.cpp")
file(GLOB DOCSCAN_HEADERS "jni/*.h")

file(GLOB DOCSCAN_EXE_SOURCES "cmake/src-test/*.cpp")
file(GLOB DOCSCAN_EXE_HEADERS "cmake/src-test/*.h")

set(DOCSCAN_RC cmake/DocScan.rc) #add resource file when compiling with MSVC
set(DOCSCAN_LIB_PATH ${CMAKE_CURRENT_BINARY_DIR}/libs)
set(LIBRARY_DIR ${DOCSCAN_LIB_PATH} ${CMAKE_CURRENT_BINARY_DIR}) #add libs directory to library dirs

# create the targets
set(BINARY_NAME ${CMAKE_PROJECT_NAME})
set(DLL_CORE_NAME ${CMAKE_PROJECT_NAME}Lib)

if(MSVC)
	set(VERSION_LIB Version.lib)
	set(LIB_CORE_NAME optimized ${DLL_CORE_NAME}.lib debug ${DLL_CORE_NAME}d.lib)

elseif(UNIX)
	include(CheckCXXCompilerFlag)
	CHECK_CXX_COMPILER_FLAG("-std=c++11" COMPILER_SUPPORTS_CXX11)
	CHECK_CXX_COMPILER_FLAG("-std=c++0x" COMPILER_SUPPORTS_CXX0X)
	if(COMPILER_SUPPORTS_CXX11)
		set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
	elseif(COMPILER_SUPPORTS_CXX0X)
		set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++0x")
	else()
		message(STATUS "The compiler ${CMAKE_CXX_COMPILER} has no C++11 support. Please use a different C++ compiler.")
	endif()
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-unknown-pragmas")

	# warnings
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pedantic -Wall -Woverloaded-virtual -Winit-self -Wundef -Wcast-align")
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wextra -Wcast-qual -Wchar-subscripts -Wformat -Wformat-y2k -Wformat-security -Wformat-nonliteral -Wnon-virtual-dtor")
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wmissing-include-dirs -Wuninitialized -Wunused -Wunused-parameter -Wstrict-null-sentinel")
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wmissing-declarations -Wlogical-op -Wstrict-aliasing=1")

    set(LIB_CORE_NAME ${DLL_CORE_NAME})
else()
	message(FATAL_ERROR "Your OS is not supported")
endif()

# binary
link_directories(${LIBRARY_DIR} ${OpenCV_LIBRARY_DIRS})
add_executable(${BINARY_NAME} WIN32 ${DOCSCAN_EXE_SOURCES} ${DOCSCAN_EXE_HEADERS} ${DOCSCAN_RC})
target_link_libraries(${BINARY_NAME} ${VERSION_LIB} ${LIB_CORE_NAME} ${OpenCV_LIBS})

set_target_properties(${BINARY_NAME} PROPERTIES COMPILE_FLAGS "-DDK_DLL_IMPORT")
set_target_properties(${BINARY_NAME} PROPERTIES IMPORTED_IMPLIB "")

# add core
add_library(${DLL_CORE_NAME} SHARED ${DOCSCAN_SOURCES} ${DOCSCAN_HEADERS} ${DOCSCAN_RC})
target_link_libraries(${DLL_CORE_NAME} ${VERSION_LIB} ${OpenCV_LIBS})
add_dependencies(${BINARY_NAME} ${DLL_CORE_NAME})

# core flags etc
set_target_properties(${DLL_CORE_NAME} PROPERTIES ARCHIVE_OUTPUT_DIRECTORY_DEBUG ${DOCSCAN_LIB_PATH})
set_target_properties(${DLL_CORE_NAME} PROPERTIES ARCHIVE_OUTPUT_DIRECTORY_RELEASE ${DOCSCAN_LIB_PATH})

set_target_properties(${DLL_CORE_NAME} PROPERTIES COMPILE_FLAGS "-DNO_JNI -DDK_DLL_EXPORT")
set_target_properties(${DLL_CORE_NAME} PROPERTIES DEBUG_OUTPUT_NAME ${DLL_CORE_NAME}d)
set_target_properties(${DLL_CORE_NAME} PROPERTIES RELEASE_OUTPUT_NAME ${DLL_CORE_NAME})

set_target_properties(${OpenCV_LIBS} PROPERTIES MAP_IMPORTED_CONFIG_RELWITHDEBINFO RELEASE)

if(MSVC)
	include(${CMAKE_CURRENT_SOURCE_DIR}/cmake/Win.cmake)
elseif(UNIX)
	install(TARGETS ${BINARY_NAME} ${DLL_NAME} ${DLL_LOADER_NAME} ${DLL_CORE_NAME} DESTINATION bin LIBRARY DESTINATION lib${LIB_SUFFIX})
	# translations
	install(FILES ${DOCSCAN_QM} DESTINATION share/DOCSCAN/translations)
endif()

set(DOCSCAN_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR})
set(DOCSCAN_BUILD_DIRECTORY ${CMAKE_BINARY_DIR})

if(MSVC)
   set(DOCSCAN_LIBS ${DOCSCAN_LIB_PATH}/${DLL_CORE_NAME}.lib)

elseif(UNIX)
   set(DOCSCAN_LIBS ${CMAKE_BINARY_DIR}/${CMAKE_SHARED_LIBRARY_PREFIX}${DLL_CORE_NAME}.so)
endif()	 	

set(DOCSCAN_INCLUDE_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/jni ${CMAKE_CURRENT_SOURCE_DIR}/cmake/src-test)

configure_file(${DOCSCAN_SOURCE_DIR}/cmake/DocScan.cmake.in ${CMAKE_BINARY_DIR}/DocScanConfig.cmake)
