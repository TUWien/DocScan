/*******************************************************************************************************
 DkPageSegmentation.h

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

#pragma warning(push, 0)	// no warnings from includes - begin
#include <opencv2/core/core.hpp>
#pragma warning(pop)		// no warnings from includes - end

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

class DllCoreExport DkPageSegmentation {

public:
	DkPageSegmentation(const cv::Mat& colImg = cv::Mat());

	virtual void compute();
	virtual void filterDuplicates(float overlap = 0.6f, float areaRatio = 0.5f);
	virtual void filterDuplicates(std::vector<DkPolyRect>& rects, float overlap = 0.6f, float areaRatio = 0.1f) const;

	virtual std::vector<DkPolyRect> getRects() const { return mRects; };
	virtual cv::Mat getDebugImg() const;
	//virtual QImage getCropped(const QImage& mImg) const;
	virtual void draw(cv::Mat& img, const cv::Scalar& col = cv::Scalar(255, 222, 0)) const;
	//virtual void draw(QImage& mImg, const QColor& col = QColor(255, 222, 0)) const;
	virtual void draw(cv::Mat& img, const std::vector<DkPolyRect>& rects, const cv::Scalar& col = cv::Scalar(255, 222, 0)) const;
	DkPolyRect getMaxRect() const;
	DkPolyRect getDocumentRect() const;

    static std::vector<DkPolyRect> apply(const cv::Mat& src);

protected:
	cv::Mat mImg;
	cv::Mat dbgImg;

	int thresh = 80;
	int numThresh = 10;
	double mMinArea = 12000;
	double mMaxArea = 0;
	float maxSide = 0;   // do not find the image's border
	float maxSideFactor = 0.97f;
	float scale = 1.0f;
	bool looseDetection = true;

	std::vector<DkPolyRect> mRects;

	virtual cv::Mat findRectangles(const cv::Mat& img, std::vector<DkPolyRect>& squares) const;
	//QImage cropToRect(const QImage& mImg, const nmc::DkRotatingRect& rect, const QColor& bgCol = QColor(0,0,0)) const;
	//void drawRects(QPainter* p, const std::vector<DkPolyRect>& mRects, const QColor& col = QColor(100, 100, 100)) const;
};

};
