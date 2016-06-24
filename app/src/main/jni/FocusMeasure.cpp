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

#include "FocusMeasure.h"

#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>

#include <sstream>
#include <string>
#include <iostream>

namespace rdf {

    std::vector<rdf::Patch> getPatches(cv::Mat inputImg) {

        static rdf::FocusEstimation fe;
        int w = inputImg.cols < inputImg.rows ? inputImg.cols : inputImg.rows;
        int ws = (int)ceil((double)w / 5.0);
        //int ws = 500;
        fe.setWindowSize(ws);

        fe.setImg(inputImg);

        //version 1
        fe.compute(rdf::FocusEstimation::FocusMeasure::LAPV);
        std::vector<rdf::Patch> resultP = fe.fmPatches();

        //version 2
        //fe.compute(rdf::FocusEstimation::FocusMeasure::LAPV);
        //std::vector<rdf::Patch> resultP = fe.fmPatches();

        //version 3 with foreground estimation
        //fe.compute();
        //std::vector<rdf::Patch> resultP = fe.fmPatches();
        //fe.computeRefPatches();
        //std::vector<rdf::Patch> refResults = fe.fmPatches();

        //for (int i = 0; i < resultP.size(); i++) {
        //	rdf::Patch tmpPatch = resultP[i];
        //	rdf::Patch tmpPatchRef = refResults[i];
        //    double fmV = tmpPatchRef.fm() > 0 ? tmpPatch.fm() / tmpPatchRef.fm() : 0;
        //    resultP[i].setFm(fmV);
        //}


        return resultP;


    //    std::ostringstream ss;
    //    ss << results[0].fm();
    //    __android_log_write(ANDROID_LOG_INFO, "FocusMeasureDummy", ss.str().c_str());

    }

	BasicFM::BasicFM()
	{
	}

	BasicFM::BasicFM(const cv::Mat & img)
	{
		mSrcImg = img;
	}

	double BasicFM::computeBREN()
	{

		if (checkInput()) {

			cv::Mat dH = mSrcImg(cv::Range::all(), cv::Range(2, mSrcImg.cols)) - mSrcImg(cv::Range::all(), cv::Range(0, mSrcImg.cols - 2));
			cv::Mat dV = mSrcImg(cv::Range(2, mSrcImg.rows), cv::Range::all()) - mSrcImg(cv::Range(0, mSrcImg.rows - 2), cv::Range::all());
			dH = cv::abs(dH);
			dV = cv::abs(dV);

			cv::Mat FM = cv::max(dH(cv::Range(0, dH.rows - 2), cv::Range::all()), dV(cv::Range::all(), cv::Range(0, dV.cols - 2)));
			FM = FM.mul(FM);

			cv::Scalar fm = cv::mean(FM);
			//normalize
			//255*255 / 2 -> max value
			fm[0] = fm[0] / ((255.0*255.0)/2.0);
			mVal = fm[0];
		}
		else {
			mVal = -1;
		}

		return mVal;
	}

	double BasicFM::computeGLVA()
	{

		if (checkInput()) {

			cv::Scalar m, v;
			cv::meanStdDev(mSrcImg, m, v);
			mVal = v[0];

		}


		return mVal;
	}

	double BasicFM::computeGLVN()
	{

		if (checkInput()) {
			cv::Scalar m, v;
			cv::meanStdDev(mSrcImg, m, v);
			mVal = v[0] * v[0] / (m[0] * m[0] + std::numeric_limits<double>::epsilon());
		}

		return mVal;
	}

	double BasicFM::computeGLLV()
	{

		if (checkInput()) {

			cv::Mat sqdImg = mSrcImg.mul(mSrcImg);
			cv::Mat meanImg, meanSqdImg;
			cv::boxFilter(mSrcImg, meanImg, CV_32FC1, cv::Size(mWindowSize, mWindowSize));
			cv::boxFilter(sqdImg, meanSqdImg, CV_32FC1, cv::Size(mWindowSize, mWindowSize));

			cv::Mat localStdImg = meanSqdImg - meanImg.mul(meanImg);
			localStdImg = localStdImg.mul(localStdImg);

			cv::Scalar m, v;
			cv::meanStdDev(localStdImg, m, v);
			mVal = v[0] * v[0];
		}


		return mVal;
	}

	double BasicFM::computeGRAT()
	{

		if (checkInput()) {

			cv::Mat dH = mSrcImg(cv::Range::all(), cv::Range(1, mSrcImg.cols)) - mSrcImg(cv::Range::all(), cv::Range(0, mSrcImg.cols - 1));
			cv::Mat dV = mSrcImg(cv::Range(1, mSrcImg.rows), cv::Range::all()) - mSrcImg(cv::Range(0, mSrcImg.rows - 1), cv::Range::all());
			//dH = cv::abs(dH);
			//dV = cv::abs(dV);

			//cv::Mat FM = cv::max(dH, dV);
			cv::Mat FM = cv::max(dH(cv::Range(0, dH.rows - 1), cv::Range::all()), dV(cv::Range::all(), cv::Range(0, dV.cols - 1)));

			double thr = 0;
			cv::Mat mask = FM >= thr;
			mask.convertTo(mask, CV_32FC1, 1 / 255.0);

			FM = FM.mul(mask);

			cv::Scalar fm = cv::sum(FM) / cv::sum(mask);

			mVal = fm[0];
		}

		return mVal;
	}

	double BasicFM::computeGRAS()
	{


		if (checkInput()) {

			cv::Mat dH = mSrcImg(cv::Range::all(), cv::Range(1, mSrcImg.cols)) - mSrcImg(cv::Range::all(), cv::Range(0, mSrcImg.cols - 1));
			dH = dH.mul(dH);

			cv::Scalar fm = cv::mean(dH);
			mVal = fm[0];
		}

		return mVal;
	}

	double BasicFM::computeLAPE()
	{
		cv::Mat laImg;
		cv::Laplacian(mSrcImg, laImg, CV_32F);
		laImg = laImg.mul(laImg);

		cv::Scalar m, v;
		cv::meanStdDev(laImg, m, v);

		mVal = m[0];

		return mVal;
	}

	double BasicFM::computeLAPV()
	{
		cv::Mat laImg;
		cv::Laplacian(mSrcImg, laImg, CV_32F);

		cv::Scalar m, v;
		cv::meanStdDev(laImg, m, v);

		mVal = (v[0] * v[0]);  //mVal = Var = sigma*sigma
		//max(var) = 1040400 = 1020*1020
		//1020 = 4 *255 if filter [0 1 0; 1 -4 1; 0 1 0]
		//see cv::Laplacian
		//normalize
		mVal = mVal / 1040400.0;

		return mVal;
	}

	double BasicFM::computeROGR()
	{
		if (checkInput()) {

			cv::Mat dH = mSrcImg(cv::Range::all(), cv::Range(1, mSrcImg.cols)) - mSrcImg(cv::Range::all(), cv::Range(0, mSrcImg.cols - 1));
			cv::Mat dV = mSrcImg(cv::Range(1, mSrcImg.rows), cv::Range::all()) - mSrcImg(cv::Range(0, mSrcImg.rows - 1), cv::Range::all());
			dH = cv::abs(dH);
			dV = cv::abs(dV);

			cv::Mat FM = cv::max(dH(cv::Range(0, dH.rows - 1), cv::Range::all()), dV(cv::Range::all(), cv::Range(0, dV.cols - 1)));
			FM = FM.mul(FM);

			cv::Scalar m = cv::mean(FM);
			cv::Mat tmp;
			FM.convertTo(tmp, CV_32F);

			// TODO: check if we need Algorithms.h
			//double r = (double)rdf::Algorithms::instance().statMomentMat(tmp, cv::Mat(), 0.98f);

            double r = 1;
			//mVal = r > 0 ? m[0] / r : m[0];
			mVal = m[0] / r;
		}

		return mVal;
	}

	void BasicFM::setImg(const cv::Mat & img)
	{
		mSrcImg = img;
	}

	double BasicFM::val() const
	{
		return mVal;
	}

	void BasicFM::setWindowSize(int s)
	{
		mWindowSize = s;
	}

	int BasicFM::windowSize() const
	{
		return mWindowSize;
	}

	bool BasicFM::checkInput()
	{
		if (mSrcImg.empty())
			return false;

		if (mSrcImg.cols < 4 || mSrcImg.rows < 4)
			return false;

		if (mSrcImg.channels() != 1)
			cv::cvtColor(mSrcImg, mSrcImg, CV_RGB2GRAY);

		if (mSrcImg.depth() != CV_32F)
			mSrcImg.convertTo(mSrcImg, CV_32F);

		return true;
	}


	FocusEstimation::FocusEstimation()
	{
	}

	FocusEstimation::FocusEstimation(const cv::Mat & img)
	{

		setImg(img);

	}

	FocusEstimation::FocusEstimation(const cv::Mat & img, int wSize)
	{

		setImg(img);

		mWindowSize = wSize;
	}

	bool FocusEstimation::compute(FocusMeasure fm, cv::Mat fmImg, bool binary)
	{

		cv::Mat fImg = fmImg;
		if (fImg.empty())
			fImg = mSrcImg;

		if (fImg.empty())
			return false;

		if (fmImg.channels() != 1 || fImg.depth() != CV_32F)
			return false;

		BasicFM fmClass;
		double f;
		mFmPatches.clear();

		for (int row = 0; row < fImg.rows; row += (mWindowSize+mSplitSize)) {
			for (int col = 0; col < fImg.cols; col += (mWindowSize+mSplitSize)) {

				cv::Range rR(row, cv::min(row + mWindowSize, fImg.rows));
				cv::Range cR(col, cv::min(col + mWindowSize, fImg.cols));

				cv::Mat tile = fImg(rR, cR);

				fmClass.setImg(tile);

				switch (fm)
				{
				case rdf::FocusEstimation::BREN:
					f = fmClass.computeBREN();
					break;
				case rdf::FocusEstimation::GLVA:
					f = fmClass.computeGLVA();
					break;
				case rdf::FocusEstimation::GLVN:
					f = fmClass.computeGLVN();
					break;
				case rdf::FocusEstimation::GLLV:
					f = fmClass.computeGLLV();
					break;
				case rdf::FocusEstimation::GRAT:
					f = fmClass.computeGRAT();
					break;
				case rdf::FocusEstimation::GRAS:
					f = fmClass.computeGRAS();
					break;
				case rdf::FocusEstimation::LAPE:
					f = fmClass.computeLAPE();
					break;
				case rdf::FocusEstimation::LAPV:
					f = fmClass.computeLAPV();
					break;
				case rdf::FocusEstimation::ROGR:
					f = fmClass.computeROGR();
					break;
				default:
					f = -1;
					break;
				}

				Patch r(cv::Point(col, row), mWindowSize, mWindowSize, f);

				if (binary) {
					cv::Scalar relArea = cv::sum(tile);
					relArea[0] = relArea[0] / (double)(mWindowSize * mWindowSize);
					r.setArea(relArea[0]);
					//area completely written with text ~ 0.1
					//normalize to 1
					relArea[0] *= 10.0;

					//weight with sigmoid function
					//-6: shift sigmoid to the right
					//*10: scale normalized Area
					double a = 10.0;
					double b = -6.0;
					double weight = 1.0 / (1 + std::exp(-(relArea[0] * a + b)));
					r.setWeight(weight);
				}


				mFmPatches.push_back(r);
			}
		}

		return true;
	}

	bool FocusEstimation::computeRefPatches(FocusMeasure fm, bool binary)
	{
		if (mSrcImg.empty())
			return false;

		cv::Mat binImg;
		mSrcImg.convertTo(binImg, CV_8U);
		cv::threshold(binImg, binImg, 0, 255, CV_THRESH_BINARY_INV | CV_THRESH_OTSU);
		binImg.convertTo(binImg, CV_32F);

		return compute(fm, binImg, binary);
	}

	std::vector<Patch> FocusEstimation::fmPatches() const
	{
		return mFmPatches;
	}

	void FocusEstimation::setImg(const cv::Mat & img)
	{
		mSrcImg = img;

		if (mSrcImg.channels() != 1) {
			cv::cvtColor(mSrcImg, mSrcImg, CV_RGB2GRAY);
		}

		if (mSrcImg.depth() == CV_8U)
			mSrcImg.convertTo(mSrcImg, CV_32F);

		if (mSrcImg.depth() == CV_32F)
			mSrcImg.convertTo(mSrcImg, CV_32F);
	}

	void FocusEstimation::setWindowSize(int s)
	{
		mWindowSize = s;
	}

	void FocusEstimation::setSplitSize(int s)
	{
		mSplitSize = s;
	}

	int FocusEstimation::windowSize() const
	{
		return mWindowSize;
	}

	Patch::Patch()
	{
	}

	Patch::Patch(cv::Point p, int w, int h, double f)
	{
		mUpperLeft = p;
		mWidth = w;
		mHeight = h;
		mFm = f;
	}

	Patch::Patch(cv::Point p, int w, int h, double f, double fRef)
	{
		mUpperLeft = p;
		mWidth = w;
		mHeight = h;
		mFm = f;
		mFmReference = fRef;
	}

	void Patch::setPosition(cv::Point p, int w, int h)
	{
		mUpperLeft = p;
		mWidth = w;
		mHeight = h;
	}

	cv::Point Patch::upperLeft() const
	{
		return mUpperLeft;
	}

	cv::Point Patch::center() const
	{
		cv::Point center(mUpperLeft.x+mWidth/2, mUpperLeft.y+mHeight/2);

		return center;
	}

	void Patch::setFmRef(double f)
	{
		mFmReference = f;
	}

	void Patch::setFm(double f)
	{
	    mFm = f;
	}

	void Patch::setWeight(double w)
	{
		mWeight = w;
	}

	void Patch::setArea(double a)
	{
		mArea = a;
	}

	int Patch::width() const
	{
		return mWidth;
	}

	int Patch::height() const
	{
		return mHeight;
	}

	double Patch::fm() const
	{
		return mFm;
	}

	double Patch::weight() const
	{
		return mWeight;
	}

	double Patch::area() const
	{
		return mArea;
	}

	std::string Patch::fmS() const
	{
		return std::string();
	}

	double Patch::fmRef() const
	{
		return mFmReference;
	}

	// inserted by fabian (allows for getting the coordinates without using OpenCV):
	int Patch::upperLeftX() const {

	    return (int) mUpperLeft.x;

	}

    int Patch::upperLeftY() const {

        return (int) mUpperLeft.y;

    }

	BasicContrast::BasicContrast()
	{
	}

	BasicContrast::BasicContrast(const cv::Mat & img)
	{
		mSrcImg = img;
	}

	double BasicContrast::computeWeber()
	{

			double min, max;
			cv::minMaxLoc(mSrcImg, &min, &max);

			mVal = (max-min) / (max+min+std::numeric_limits<double>::epsilon());


		return mVal;
	}

	double BasicContrast::computeMichelson()
	{


			double min, max;
			cv::minMaxLoc(mSrcImg, &min, &max);

			mVal = max / (min + +std::numeric_limits<double>::epsilon()) - 1;

		return mVal;
	}

	double BasicContrast::computeRMS()
	{


			cv::Scalar m;
			m = cv::mean(mSrcImg);

			cv::Mat tmp = mSrcImg - m;
			tmp = tmp.mul(tmp);
			cv::Scalar s = cv::sum(tmp);
			mVal = s[0] / (double)(mSrcImg.rows*mSrcImg.cols);

		return mVal;
	}

	void BasicContrast::setImg(const cv::Mat & img)
	{
		mSrcImg = img;
	}

	void BasicContrast::setLum(bool l)
	{
		mLuminance = l;
	}

	double BasicContrast::val() const
	{
		return mVal;
	}

	void BasicContrast::setWindowSize(int s)
	{
		mWindowSize = s;
	}

	int BasicContrast::windowSize() const
	{
		return mWindowSize;
	}


	ContrastEstimation::ContrastEstimation()
	{
	}

	ContrastEstimation::ContrastEstimation(const cv::Mat & img)
	{
		mSrcImg = img;
	}

	ContrastEstimation::ContrastEstimation(const cv::Mat & img, int wSize)
	{
		mSrcImg = img;
		mWindowSize = wSize;
	}

	bool ContrastEstimation::compute(ContrastMeasure fm)
	{

		BasicContrast contClass;
		double c;
		mContPatches.clear();

		if (checkInput()) {

			for (int row = 0; row < mSrcImg.rows; row += (mWindowSize + mSplitSize)) {
				for (int col = 0; col < mSrcImg.cols; col += (mWindowSize + mSplitSize)) {

					cv::Range rR(row, cv::min(row + mWindowSize, mSrcImg.rows));
					cv::Range cR(col, cv::min(col + mWindowSize, mSrcImg.cols));

					cv::Mat tile = mSrcImg(rR, cR);

					contClass.setImg(tile);

					switch (fm)
					{
					case rdf::ContrastEstimation::WEBER:
						c = contClass.computeWeber();
						break;
					case rdf::ContrastEstimation::MICHELSON:
						c = contClass.computeMichelson();
						break;
					case rdf::ContrastEstimation::RMS:
						c = contClass.computeRMS();
						break;
					default:
						c = -1;
						break;
					}

					Patch r(cv::Point(col, row), mWindowSize, mWindowSize, c);

					mContPatches.push_back(r);
				}
			}
		}

		return true;
	}

	std::vector<Patch> ContrastEstimation::cPatches() const
	{
		return mContPatches;
	}

	void ContrastEstimation::setImg(const cv::Mat & img)
	{
		mSrcImg = img;
	}

	void ContrastEstimation::setWindowSize(int s)
	{
		mWindowSize = s;
	}

	void ContrastEstimation::setSplitSize(int s)
	{
		mSplitSize = s;
	}

	int ContrastEstimation::windowSize() const
	{
		return mWindowSize;
	}

	bool ContrastEstimation::checkInput()
	{
		if (mSrcImg.empty())
			return false;

		if (mSrcImg.cols < 4 || mSrcImg.rows < 4)
			return false;

		if (mSrcImg.channels() != 1 && mLuminance == false) {
			cv::cvtColor(mSrcImg, mSrcImg, CV_RGB2GRAY);
		}
		else if (mSrcImg.channels() != 1 && mLuminance == true) {
			cv::cvtColor(mSrcImg, mSrcImg, CV_RGB2Luv);
			std::vector<cv::Mat> channels;
			cv::split(mSrcImg, channels);
			mSrcImg = channels[0];
		}

		if (mSrcImg.depth() != CV_32F)
			mSrcImg.convertTo(mSrcImg, CV_32F, 1.0/255.0);

		return true;
	}

	void ContrastEstimation::setLum(bool b)
	{
		mLuminance = b;
	}

}

