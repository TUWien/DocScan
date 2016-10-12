/*******************************************************************************************************
 DkPageSegmentation.cpp

 nomacs is a fast and small image viewer with the capability of synchronizing multiple instances

 Copyright (C) 2015 Markus Diem

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

#include "DkMath.h"	// nomacs
#include "Utils.h"

#pragma warning(push, 0)	// no warnings from includes - begin
#include <opencv2/imgproc/imgproc.hpp>
#include "Illumination.h"
#pragma warning(pop)		// no warnings from includes - end

namespace dsc {
// DkIllumination --------------------------------------------------------------------
DkIllumination::DkIllumination(const cv::Mat & img, const DkPolyRect & rect) {

	mImg = img;
	mPageRect = rect;
}

void DkIllumination::compute() {

	// TODO: crop according to the page rect
	cv::Mat img = mImg;
	
	if (img.channels() > 1)
		cv::cvtColor(img, img, CV_RGB2GRAY);


	int rStep = std::max(dsc::round((double)img.rows / mNumScanLines), 1);
	int cStep = std::max(dsc::round((double)img.cols / mNumScanLines), 1);

	double sumVal = 0;
	int nSum = 0;
	
	// for all rows
	for (int idx = 0; idx < img.rows; idx += rStep) {
		sumVal += scanLineValue(img.row(idx));
		nSum++;
	}

	// for all cols
	for (int idx = 0; idx < img.cols; idx += cStep) {
		sumVal += scanLineValue(img.col(idx).t());
		nSum++;
	}

	mIlluminationValue = sumVal / nSum;

	std::cout << "illumination evaluated on " << nSum << " scanlines..." << std::endl;
}



double DkIllumination::value() const {
	return mIlluminationValue;
}

double DkIllumination::scanLineValue(const cv::Mat & scanline) const {

	cv::Mat sc = scanline;
	cv::medianBlur(sc, sc, 5);

	int sumVal = 0;
	int nSum = 0;

	const unsigned char* sPtr = sc.ptr<unsigned char>();
	
	for (int idx = 0; idx < sc.cols-1; idx++) {

		int diff = (int)sPtr[idx] - (int)sPtr[idx+1];

		if (std::abs(diff) < mEps) {
			sumVal += diff;
			nSum++;
		}
	}

	double val = std::abs(sumVal);
	//std::cout << "scanline val: " << val << " #" << nSum << std::endl;

	return val/nSum;
}

double DkIllumination::apply(const cv::Mat & src, const DkPolyRect & pageRect) {

	DkIllumination module(src, pageRect);
	module.compute();

	return module.value();
}

cv::Mat DkIllumination::debugImage() const {
	return mDbgImg;
}

};

