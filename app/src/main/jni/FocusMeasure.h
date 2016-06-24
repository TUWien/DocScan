/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   16. June 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

#pragma once

#include <opencv2/core/core.hpp>

#ifndef DllCoreExport
#ifdef DK_DLL_EXPORT
#define DllCoreExport __declspec(dllexport)
#else
#define DllCoreExport
#endif
#endif

// hide dll-interface warning
#pragma warning(disable: 4251)


namespace dsc {

    // read defines
    class Patch;

	class BasicFM {

	public:
		BasicFM();
		BasicFM(const cv::Mat& img);

		double computeBREN();
		double computeGLVA();
		double computeGLVN();
		double computeGLLV();
		double computeGRAT();
		double computeGRAS();
		double computeLAPE();
		double computeLAPV();
		double computeROGR();


		void setImg(const cv::Mat& img);
		double val() const;
		void setWindowSize(int s);
		int windowSize() const;

	protected:
		bool checkInput();

		cv::Mat mSrcImg;

		// parameters
		double mVal = -1.0;
		int mWindowSize = 15;

	};


	class BasicContrast {

	public:
		BasicContrast();
		BasicContrast(const cv::Mat& img);

		double computeWeber();
		double computeMichelson();
		double computeRMS();

		void setImg(const cv::Mat& img);
		void setLum(bool l);
		double val() const;
		void setWindowSize(int s);
		int windowSize() const;

	protected:

		cv::Mat mSrcImg;

		// parameters
		double mVal = -1.0;
		int mWindowSize = 15;
		bool mLuminance = false;
	};


	class DllCoreExport Patch {
	
	public:
		Patch();
		Patch(cv::Point p, int w, int h, double f);
		Patch(cv::Point p, int w, int h, double f, double fRef);

		void setPosition(cv::Point p, int w, int h);
		cv::Point upperLeft() const;
		cv::Point center() const;
		void setFmRef(double f);
		void setWeight(double w);
		void setArea(double a);
		int width() const;
		int height() const;

		double fm() const;
		double weight() const;
		double area() const;
		std::string fmS() const;
		double fmRef() const;

		// inserted by fabian (allows for getting the coordinates without using OpenCV):
        int upperLeftX() const;
        int upperLeftY() const;

	protected:

		cv::Point mUpperLeft;
		int mWidth = 0;
		int mHeight = 0;

		double mFm = -1;
		double mFmReference = -1;
		double mWeight = -1;
		double mArea = -1;
	};

	class DllCoreExport FocusEstimation {

	public:
		enum FocusMeasure { BREN = 0, GLVA, GLVN, GLLV, GRAT, GRAS, LAPE, LAPV, ROGR };

		FocusEstimation();
		FocusEstimation(const cv::Mat& img);
		FocusEstimation(const cv::Mat& img, int wSize);

		bool compute(FocusMeasure fm = BREN, cv::Mat fmImg = cv::Mat(), bool binary = false);
		bool computeRefPatches(FocusMeasure fm = BREN, bool binary = false);
		std::vector<Patch> fmPatches() const;

		void setImg(const cv::Mat& img);
		void setWindowSize(int s);
		void setSplitSize(int s);
		int windowSize() const;

		static std::vector<dsc::Patch> apply(const cv::Mat& src);

	protected:
		cv::Mat mSrcImg;

		std::vector<Patch> mFmPatches;

		// parameters
		int mWindowSize = 40;
		int mSplitSize = 0;
	};


	class ContrastEstimation {

	public:
		enum ContrastMeasure { WEBER = 0, MICHELSON, RMS };

		ContrastEstimation();
		ContrastEstimation(const cv::Mat& img);
		ContrastEstimation(const cv::Mat& img, int wSize);

		bool compute(ContrastMeasure fm = WEBER);
		std::vector<Patch> cPatches() const;

		void setImg(const cv::Mat& img);
		void setWindowSize(int s);
		void setSplitSize(int s);
		int windowSize() const;
		bool checkInput();
		void setLum(bool b);

	protected:
		cv::Mat mSrcImg;

		std::vector<Patch> mContPatches;

		// parameters
		int mWindowSize = 100;
		int mSplitSize = 0;
		bool mLuminance = false;
	};




};