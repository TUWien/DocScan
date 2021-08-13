/*******************************************************************************************************
 Illumination.h

 nomacs is a fast and small image viewer with the capability of synchronizing multiple instances

 Copyright (C) 2011-2015 Markus Diem <markus@nomacs.org>

 This file is part of nomacs.

 nomacs is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 nomacs is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 *******************************************************************************************************/

#pragma once

#include "PageSegmentationUtils.h"

#pragma warning(push, 0)    // no warnings from includes - begin

#include <opencv2/core/core.hpp>

#pragma warning(pop)        // no warnings from includes - end

#ifndef DllCoreExport
#ifdef DK_DLL_EXPORT
#define DllCoreExport __declspec(dllexport)
#elif DK_DLL_IMPORT
#define DllCoreExport
#else
#define DllCoreExport
#endif
#endif

namespace dsc {

    class DllCoreExport DkIllumination {

    public:
        DkIllumination(const cv::Mat &img = cv::Mat(), const DkPolyRect &rect = DkPolyRect());

        virtual void compute();

        double value() const;

        static double apply(const cv::Mat &src, const DkPolyRect &rect = DkPolyRect());

        cv::Mat debugImage() const;

    protected:
        cv::Mat mImg;
        cv::Mat mDbgImg;    // unused

        DkPolyRect mPageRect;

        // parameters
        int mEps = 5;
        int mNumScanLines = 30;

        double mIlluminationValue = 0.0;    // result

        double scanLineValue(const cv::Mat &scanline) const;
    };

};
