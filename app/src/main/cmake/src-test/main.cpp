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

#pragma comment (linker, "/SUBSYSTEM:CONSOLE")

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

bool testPageSegmentation(const std::string& filePath);
bool testFocusMeasure(const std::string& filePath);
 
int main(int argc, char** argv) {

	if (argc < 2) {
		std::cerr << "[ERROR] first argument must be the absolute path to a test image" << std::endl;
		return 0;
	}

	std::string path = argv[1];

	// test the focus measure module
	if (!testFocusMeasure(path))
		std::cerr << "[FAIL] focus measure NOT succesfull... input file: " << path << std::endl;
	else
		std::cout << "[SUCCESS] focus measure succesfull..." << std::endl;

	// test the page segmentation module
	if (!testPageSegmentation(path))
		std::cerr << "[FAIL] page segmentation NOT succesfull... input file: " << path << std::endl;
	else
		std::cout << "[SUCCESS] page segmentation succesfull..." << std::endl;

	return 0;
}

bool testFocusMeasure(const std::string& filePath) {

	cv::Mat img = cv::imread(filePath);

	if (img.empty()) {
		std::cerr << "could not load: " << img << std::endl;
		return false;
	}

	std::vector<dsc::Patch> patches = dsc::FocusEstimation::apply(img);

	if (patches.empty()) {
		std::cerr << "could not load: " << img << std::endl;
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

bool testPageSegmentation(const std::string& filePath) {

	std::cout << "test page segmentation not implemented..." << std::endl;
	return false;
}

