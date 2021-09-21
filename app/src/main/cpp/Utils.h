//
// Created by fabian on 08.07.2016.
//

#pragma once

#include <string>
#include <sstream>

#ifndef DllCoreExport
#ifdef DK_DLL_EXPORT
#define DllCoreExport __declspec(dllexport)
#else
#define DllCoreExport
#endif
#endif


namespace dsc {

    class Utils {

    public:
        static void
        print(const std::string &text, const std::string &moduleName = "[DocScanNative]");

        template<typename num>
        static std::string num2str(num n);
    };

    template<typename num>
    inline std::string Utils::num2str(num n) {

        std::stringstream ss;
        ss << n;
        std::string str = ss.str();

        return str;
    }

}

