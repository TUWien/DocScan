# Windows specific cmake calls

# copy required dlls to the directories
foreach (opencvlib ${OpenCV_LIBS})
    file(GLOB dllpath ${OpenCV_DIR}/bin/Release/${opencvlib}*.dll)
    file(COPY ${dllpath} DESTINATION ${CMAKE_CURRENT_BINARY_DIR}/Release)
    file(COPY ${dllpath} DESTINATION ${CMAKE_CURRENT_BINARY_DIR}/RelWithDebInfo)

    file(GLOB dllpath ${OpenCV_DIR}/bin/Debug/${opencvlib}*d.dll)
    file(COPY ${dllpath} DESTINATION ${CMAKE_CURRENT_BINARY_DIR}/Debug)
endforeach (opencvlib)

# set properties for Visual Studio Projects
add_definitions(/Zc:wchar_t-)
set(CMAKE_CXX_FLAGS_DEBUG "/W4 ${CMAKE_CXX_FLAGS_DEBUG}")
set(CMAKE_CXX_FLAGS_RELEASE "/W4 /O2 ${CMAKE_CXX_FLAGS_RELEASE}")

source_group("Generated Files" FILES ${DOCSCAN_RC})
set_source_files_properties(${DOCSCAN_TRANSLATIONS} PROPERTIES HEADER_FILE_ONLY TRUE)

# add build incrementer command if requested
if (ENABLE_INCREMENTER)
    add_custom_command(TARGET ${BINARY_NAME} COMMAND cscript /nologo ${CMAKE_CURRENT_SOURCE_DIR}/cmake/incrementer.vbs ${CMAKE_CURRENT_SOURCE_DIR}/cmake/DocScan.rc)
endif ()

# generate configuration file
if (DLL_NAME)
    get_property(DOCSCAN_DEBUG_NAME TARGET ${DLL_NAME} PROPERTY DEBUG_OUTPUT_NAME)
    get_property(DOCSCAN_RELEASE_NAME TARGET ${DLL_NAME} PROPERTY RELEASE_OUTPUT_NAME)
    set(DOCSCAN_LIBS optimized ${CMAKE_BINARY_DIR}/libs/${DOCSCAN_RELEASE_NAME}.lib debug ${CMAKE_BINARY_DIR}/libs/${DOCSCAN_DEBUG_NAME}.lib)
    # todo: missing loader & core
endif ()

