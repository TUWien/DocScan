/*******************************************************************************************************
 FlowView is a visualization and auto-gating tool for FCS data (Flow Cytometry Data).
 This software is part of the EU Project AutoFLOW [1] and
 is developed at the Computer Vision Lab [2] at TU Wien.

 Copyright (C) 2014-2016 Markus Diem <diemmarkus@gmail.com>

 This file is part of FlowView.

 FlowView is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 FlowView is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 related links:
 [1] http://www.autoflow-project.eu/
 [2] http://www.caa.tuwien.ac.at/cvl/
 [3] http://nomacs.org/
 *******************************************************************************************************/

#include <iostream>

#include "FocusMeasure.h"
#include "PageSegmentation.h"
#include "Illumination.h"

#pragma comment (linker, "/SUBSYSTEM:CONSOLE")

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

bool testPageSegmentation(const std::string& filePath, const std::string& outputPath = "");
bool testFocusMeasure(const std::string& filePath);
 
int main(int argc, char** argv) {

	if (argc < 2) {
		std::cerr << "[ERROR] first argument must be the absolute path to a test image" << std::endl;
		return 0;
	}

	std::string path = argv[1];
	//path = "C:/VSProjects/DocScan/img/tests/test.png";

	//// test the focus measure module
	//if (!testFocusMeasure(path))
	//	std::cerr << "[FAIL] focus measure NOT succesfull... input file: " << path << std::endl;
	//else
	//	std::cout << "[SUCCESS] focus measure succesfull..." << std::endl;

	std::string oPath = argc > 2 ? argv[2] : "";
	//oPath = "C:/VSProjects/DocScan/img/tests/test-res.png";

	// test the page segmentation module
	if (!testPageSegmentation(path, oPath))
		std::cerr << "[FAIL] page segmentation NOT succesfull... input file: " << path << std::endl;
	else
		std::cout << "[SUCCESS] page segmentation succesfull..." << std::endl;

	return 0;
}

bool loadImage(const std::string& filePath, cv::Mat& img) {

	img = cv::imread(filePath);

	if (img.empty()) {
		std::cerr << "could not load: " << img << std::endl;
		return false;
	}

	return true;
}

bool testFocusMeasure(const std::string& filePath) {

	cv::Mat img;
	
	if (!loadImage(filePath, img))
		return false;

	std::vector<dsc::Patch> patches = dsc::FocusEstimation::apply(img);

	if (patches.empty()) {
		std::cerr << "FocusEstimation returned no patches for image: " << img << std::endl;
		return false;
	}

	int sumError = 0;

	for (auto p : patches) {
		if (p.fm() == -1.0f)
			sumError++;
	}

	if (sumError > 0) {
		std::cerr << sumError << " out of " << patches.size() << " failed..." << std::endl;
		return false;
	}

	return true;
}

bool testPageSegmentation(const std::string& filePath, const std::string& outputPath) {

	cv::Mat img;

	if (!loadImage(filePath, img))
		return false;


	std::vector<dsc::DkPolyRect> pageRects = dsc::DkPageSegmentation::apply(img);

	if (pageRects.empty()) {

		//double illVal = dsc::DkIllumination::apply(img);
		//std::cout << "illumination value: " << illVal << std::endl;

		std::cerr << "PageSegmentation returned no page rects for: " << filePath << std::endl;
		return false;
	}

	// save results?!
	if (!outputPath.empty()) {

		cv::FileStorage fs(outputPath, cv::FileStorage::WRITE);
		
		//TODO write output coordinates
		dsc::DkPolyRect rect = pageRects[0];
		std::vector<dsc::DkVector> corners = rect.getCorners();
		cv::Point cpt;
		cpt = corners[0].getCvPoint();
		fs << "cornerPoint0" << cpt;
		cpt = corners[1].getCvPoint();
		fs << "cornerPoint1" << cpt;
		cpt = corners[2].getCvPoint();
		fs << "cornerPoint2" << cpt;
		cpt = corners[3].getCvPoint();
		fs << "cornerPoint3" << cpt;
		fs.release();

		//cv::Mat dstImg = img.clone();
		//for (auto rect : pageRects)
		//	rect.draw(dstImg);

		//cv::imwrite(outputPath, dstImg);
	}

	//double illVal = dsc::DkIllumination::apply(img, pageRects[0]);
	//std::cout << "illumination value: " << illVal << std::endl;

	//std::cout << "test page segmentation not implemented..." << std::endl;
	return true;
}

