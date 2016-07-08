//
// Created by fabian on 08.07.2016.
//

#include "Utils.h"

#include <iostream>

#ifndef NO_JNI
#include <android/log.h>
#endif


namespace dsc {

void Utils::print(const std::string &text, const std::string &moduleName) {

#ifdef NO_JNI
	std::cout << moduleName << " " << text << std::endl;
#else

	const char* c_text = (const char*)text.c_str();
	const char* c_moduleName = (const char*)moduleName.c_str();

	__android_log_write(ANDROID_LOG_INFO, c_moduleName, c_text);

#endif

}

}