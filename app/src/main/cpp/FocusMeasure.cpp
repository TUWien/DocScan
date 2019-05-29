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
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

#include "FocusMeasure.h"
#include "Utils.h"

#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>

#include <sstream>
#include <string>
#include <iostream>

namespace dsc {

	/// <summary>
	/// Initializes a new instance of the Basic Focus Measure class.
	/// </summary>
	BasicFM::BasicFM()
	{
	}

	/// <summary>
	/// Initializes a new instance of the <see cref="BasicFM"/> class and sets the source image.
	/// </summary>
	/// <param name="img">The img.</param>
	BasicFM::BasicFM(const cv::Mat & img)
	{
		mSrcImg = img;
	}

	/// <summary>
	/// Computes Brenner's focus measure (gradient based)
	/// </summary>
	/// <returns>The focus measure value</returns>
	double BasicFM::computeBREN()
	{

		if (checkInput()) {


			//this should be the faster version...

            cv::Mat FM(mSrcImg.rows - 2, mSrcImg.cols - 2, mSrcImg.type());
            for (int rowIdx = 0; rowIdx < mSrcImg.rows - 2; rowIdx++) {

                const float *ptrSrc = mSrcImg.ptr<float>(rowIdx);
                const float *ptrSrc2 = mSrcImg.ptr<float>(rowIdx+2);

                float *ptrFm = FM.ptr<float>(rowIdx);

                for (int colIdx = 0; colIdx < mSrcImg.cols-2; colIdx++) {

                    double diffH = ptrSrc[colIdx + 2] - ptrSrc[colIdx];
                    double diffV = ptrSrc2[colIdx] - ptrSrc[colIdx];

                    ptrFm[colIdx] = (float)cv::max(cv::abs(diffH), cv::abs(diffV));
                    ptrFm[colIdx] *= ptrFm[colIdx];
                }
            }

            /*
            //old version
			cv::Mat dH = mSrcImg(cv::Range::all(), cv::Range(2, mSrcImg.cols)) - mSrcImg(cv::Range::all(), cv::Range(0, mSrcImg.cols - 2));
			cv::Mat dV = mSrcImg(cv::Range(2, mSrcImg.rows), cv::Range::all()) - mSrcImg(cv::Range(0, mSrcImg.rows - 2), cv::Range::all());
			dH = cv::abs(dH);
			dV = cv::abs(dV);

			cv::Mat FM = cv::max(dH(cv::Range(0, dH.rows - 2), cv::Range::all()), dV(cv::Range::all(), cv::Range(0, dV.cols - 2)));
			FM = FM.mul(FM);
            //end old version
            */

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

	/// <summary>
	/// Computes the gray-level variance (Krotkov86).
	/// Has used for autofocus and SFF.
	/// </summary>
	/// <returns>The focus measure value</returns>
	double BasicFM::computeGLVA()
	{

		if (checkInput()) {

			cv::Scalar m, v;
			cv::meanStdDev(mSrcImg, m, v);
			mVal = v[0] / 127.5;

		}


		return mVal;
	}

	/// <summary>
	/// Computes the normalized gray level local variance (Santos97)
	/// </summary>
	/// <returns>The focus measure value</returns>
	double BasicFM::computeGLVN()
	{

		if (checkInput()) {
			cv::Scalar m, v;
			cv::meanStdDev(mSrcImg, m, v);
			mVal = v[0] * v[0] / (m[0] * m[0] + std::numeric_limits<double>::epsilon());
		}

		return mVal;
	}

	/// <summary>
	/// Computes the gray level local variance (Pech00)
	/// </summary>
	/// <returns>The focus measure value</returns>
	double BasicFM::computeGLLV()
	{

		if (checkInput()) {

			cv::Mat sqdImg = mSrcImg.mul(mSrcImg);
			cv::Mat meanImg, meanSqdImg;
			cv::boxFilter(mSrcImg, meanImg, CV_32FC1, cv::Size(mWindowSize, mWindowSize));
			cv::boxFilter(sqdImg, meanSqdImg, CV_32FC1, cv::Size(mWindowSize, mWindowSize));

			cv::Mat localStdImg = meanSqdImg - meanImg.mul(meanImg);
			cv::sqrt(localStdImg, localStdImg);
			//localStdImg = localStdImg.mul(localStdImg);

			cv::Scalar m, v;
			cv::meanStdDev(localStdImg, m, v);
			//normalize
			//max std = 127.5 -> max v = 63.75
			//63.75*63.75 = 4065
			mVal = v[0] * v[0] / 4065;
		}


		return mVal;
	}

	/// <summary>
	/// Computes the thresholded gradient (Snatos97).
	/// </summary>
	/// <returns>The focus measure value</returns>
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
			mask.convertTo(mask, CV_32FC1, 255.0);

			FM = FM.mul(mask);

			cv::Scalar fm = cv::sum(FM) / cv::sum(mask);
            //normalize
			mVal = fm[0] / 255.0;
		}

		return mVal;
	}

	/// <summary>
	/// Computes the squared gradient in horizontal direction (Eskicioglu95)
	/// </summary>
	/// <returns>The focus measure value</returns>
	double BasicFM::computeGRAS()
	{


		if (checkInput()) {

			cv::Mat dH = mSrcImg(cv::Range::all(), cv::Range(1, mSrcImg.cols)) - mSrcImg(cv::Range::all(), cv::Range(0, mSrcImg.cols - 1));
			dH = dH.mul(dH);

			cv::Scalar fm = cv::mean(dH);
			mVal = fm[0] / (255.0*255.0);
		}

		return mVal;
	}

	/// <summary>
	/// Computes the energy of laplacian (Subbarao92a)
	/// </summary>
	/// <returns>The focus measure value</returns>
	double BasicFM::computeLAPE()
	{
		cv::Mat laImg;
		cv::Laplacian(mSrcImg, laImg, CV_32F);
		laImg = laImg.mul(laImg);

		cv::Scalar m, v;
		cv::meanStdDev(laImg, m, v);

		mVal = m[0] / 1040400.0;

		return mVal;
	}

	/// <summary>
	/// Computes the Variance of the Laplacians
	/// </summary>
	/// <returns>The focus measure value</returns>
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

	/// <summary>
	/// Computes Brenner's focus measure and determines the ratio of the median/mean.
	/// </summary>
	/// <returns>The focus measure value</returns>
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


            double r = 255.0*255.0;
			//mVal = r > 0 ? m[0] / r : m[0];
			mVal = m[0] / r;
		}

		return mVal;
	}

	/// <summary>
	/// Sets the img (will be converted to one channel and 32F.
	/// </summary>
	/// <param name="img">The source img.</param>
	void BasicFM::setImg(const cv::Mat & img)
	{
		mSrcImg = img;
	}

	/// <summary>
	/// Returns the fm value if calculated. Otherwise -1 is returned.
	/// </summary>
	/// <returns>The focus measure value</returns>
	double BasicFM::val() const
	{
		return mVal;
	}

	/// <summary>
	/// Sets the size of the window for focus measure which use a sliding window.
	/// Default value = 15.
	/// </summary>
	/// <param name="s">The size s of the sliding window.</param>
	void BasicFM::setWindowSize(int s)
	{
		mWindowSize = s;
	}

	/// <summary>
	/// Returns the window size for focus measure which use a sliding window.
	/// </summary>
	/// <returns>Window size</returns>
	int BasicFM::windowSize() const
	{
		return mWindowSize;
	}

	/// <summary>
	/// Checks the input and converts to grayscale and 32F if necessary
	/// </summary>
	/// <returns>False if the src image is empty, true otherwise.</returns>
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


	/// <summary>
	/// Initializes a new instance of the <see cref="FocusEstimation"/> class.
	/// </summary>
	FocusEstimation::FocusEstimation()
	{
	}

	/// <summary>
	/// Initializes a new instance of the <see cref="FocusEstimation"/> class.
	/// The source image of which the focus should be estimated is set.
	/// </summary>
	/// <param name="img">The source img.</param>
	FocusEstimation::FocusEstimation(const cv::Mat & img)
	{

		setImg(img);

	}

	/// <summary>
	/// Initializes a new instance of the <see cref="FocusEstimation"/> class.
	/// The source image of which the focus should be estimated is set and the patch window size (sliding window).
	/// </summary>
	/// <param name="img">The source img.</param>
	/// <param name="wSize">Size w of the sliding window.</param>
	FocusEstimation::FocusEstimation(const cv::Mat & img, int wSize)
	{

		setImg(img);

		mWindowSize = wSize;
	}

	/// <summary>
	/// Calculates for each image patch the focus measure value. Result is a vector of Patches (see class Patch).
	/// If the image is binary, the relative foreground (foreground pixel / patchSize) and the weight is stored for each Patch.
	/// </summary>
	/// <param name="fm">The specified focuse measure method fm (e.g. Brenner).</param>
	/// <param name="fmImg">The image fmImg to calculate the focus measure on. If empty, the src image is taken.</param>
	/// <param name="binary">if set to <c>true</c> [binary] the input image is binary, specifying the foreground image. The foreground area and the weight is saved to the image patch</param>
	/// <returns>True if the focus measure could be computed, false otherwise.</returns>
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
				case dsc::FocusEstimation::BREN:
					f = fmClass.computeBREN();
					break;
				case dsc::FocusEstimation::GLVA:
					f = fmClass.computeGLVA();
					break;
				case dsc::FocusEstimation::GLVN:
					f = fmClass.computeGLVN();
					break;
				case dsc::FocusEstimation::GLLV:
					f = fmClass.computeGLLV();
					break;
				case dsc::FocusEstimation::GRAT:
					f = fmClass.computeGRAT();
					break;
				case dsc::FocusEstimation::GRAS:
					f = fmClass.computeGRAS();
					break;
				case dsc::FocusEstimation::LAPE:
					f = fmClass.computeLAPE();
					break;
				case dsc::FocusEstimation::LAPV:
					f = fmClass.computeLAPV();
					break;
				case dsc::FocusEstimation::ROGR:
					f = fmClass.computeROGR();
					break;
				default:
					f = -1;
					break;
				}

				Patch r(cv::Point(col, row), mWindowSize, mWindowSize, f);

				if (binary) {
					cv::Scalar relArea = cv::sum(tile);
					r.setArea(relArea[0]);
					relArea[0] = relArea[0] / (double)(mWindowSize * mWindowSize);
					
					//area completely written with text ~ 0.1
					//normalize to 1
					relArea[0] *= 10.0;
					r.setWeight(relArea[0]);

					//weight with sigmoid function
					//-6: shift sigmoid to the right
					//*10: scale normalized Area
					//double a = 10.0;
					//double b = -6.0;
					//double weight = 1.0 / (1 + std::exp(-(relArea[0] * a + b)));
					//r.setWeight(weight);
				}


				mFmPatches.push_back(r);
			}
		}

		return true;
	}


	/// <summary>
	/// Computes the reference patches. The foreground is estimmated using Otsu.
	/// Based on the foreground image the fm for each patch is calculated.
	/// This can be seen as ideal reference, since only ideal edges exist (0-1).
	/// </summary>
	/// <param name="fm">The specified focuse measure method fm (e.g. Brenner).</param>
	/// <param name="binary">if set to <c>true</c> [binary] the input image is binary, specifying the foreground image. The foreground area and the weight is saved to the image patch in this case.</param>
	/// <returns></returns>
	bool FocusEstimation::computeRefPatches(FocusMeasure fm, bool binary)
	{
		if (mSrcImg.empty())
			return false;

//		testing out of memory:
//		cv::Mat m(1000000000, 1000000000, CV_8UC3);

		cv::Mat binImg;
		mSrcImg.convertTo(binImg, CV_8U);
		cv::threshold(binImg, binImg, 0, 255, CV_THRESH_BINARY_INV | CV_THRESH_OTSU);
		binImg.convertTo(binImg, CV_32F);

		return compute(fm, binImg, binary);
	}

	/// <summary>
	/// Returns the patches containing the fm value and optional the area and the weight.
	/// </summary>
	/// <returns>Returns the local image patches</returns>
	std::vector<Patch> FocusEstimation::fmPatches() const
	{
		return mFmPatches;
	}

	/// <summary>
	/// Sets the source img.
	/// If needed the image is converted to grayscale and 32F.
	/// </summary>
	/// <param name="img">The img.</param>
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

	/// <summary>
	/// Sets the size of the window (image patch) which is used to calculate a local focus measure.
	/// </summary>
	/// <param name="s">The local window (patch) size s.</param>
	void FocusEstimation::setWindowSize(int s)
	{
		mWindowSize = s;
	}

	/// <summary>
	/// Sets the split size (spacing) between local image patches (0 by default).
	/// </summary>
	/// <param name="s">The split size (spacing) s.</param>
	void FocusEstimation::setSplitSize(int s)
	{
		mSplitSize = s;
	}

	/// <summary>
	/// Returns the window size (local patch size).
	/// </summary>
	/// <returns>The local window size.</returns>
	int FocusEstimation::windowSize() const
	{
		return mWindowSize;
	}

	void FocusEstimation::setGlobalFMThreshold(double fmt)
	{
		mGlobalFMThresh = fmt;
	}

	double FocusEstimation::getGlobalFMThreshold() const
	{
		return mGlobalFMThresh;
	}

	void FocusEstimation::setTextThrshold(double t)	{
		mTextThreshold = t;
	}

	double FocusEstimation::textThr() const	{
		return mTextThreshold;
	}

	/// <summary>
	/// Applies the specified focus estimation.
	/// The window size is 1/5 min(width/height) of the src image.
	/// Currently, Brenner is used as fm value and the foreground is estimated (Otsu)
	/// to normalize the fm value.
	/// </summary>
	/// <param name="src">The source image.</param>
	/// <returns>A vector with image patches containing the fm value.</returns>
	std::vector<dsc::Patch> dsc::FocusEstimation::apply(const cv::Mat & src, const double globalFMThr) {

		static dsc::FocusEstimation fe;
		int w = src.cols < src.rows ? src.cols : src.rows;
		int ws = (int)ceil((double)w / 5.0);
		//int ws = 500;
		fe.setWindowSize(ws);

		fe.setImg(src);
		if (globalFMThr > 0) {
		    fe.setGlobalFMThreshold(globalFMThr);
		}

		////version 1
		//fe.compute(dsc::FocusEstimation::FocusMeasure::LAPV);
		//std::vector<dsc::Patch> resultP = fe.fmPatches();

		////version 2
		//fe.compute(dsc::FocusEstimation::FocusMeasure::LAPV);
		//std::vector<dsc::Patch> resultP = fe.fmPatches();

		//version 3 with foreground estimation
		fe.compute();
		std::vector<dsc::Patch> resultP = fe.fmPatches();
		fe.computeRefPatches(dsc::FocusEstimation::BREN, true);
		std::vector<dsc::Patch> refResults = fe.fmPatches();

		for (int i = 0; i < resultP.size(); i++) {
			dsc::Patch tmpPatch = resultP[i];
			dsc::Patch tmpPatchRef = refResults[i];
		    double fmV = tmpPatchRef.fm() > 0 ? tmpPatch.fm() / tmpPatchRef.fm() : 0;
		    //try this one
		    //double fmV = (tmpPatchRef.fm() > 0  && tmpPatch.fm() > tmpPatchRef.fm()) ? tmpPatch.fm() / tmpPatchRef.fm() : 0;
		    resultP[i].setFm(fmV);
			bool s = fmV < fe.getGlobalFMThreshold() ? false : true;
			resultP[i].setSharpness(s);

			bool text = tmpPatchRef.weight() < fe.textThr() ? false : true;
			//try this one
			//bool text = (tmpPatchRef.weight() < fe.textThr() || tmpPatchRef.weight() > 5) ? false : true;
			resultP[i].setForeground(text);

		}

		return resultP;


		//    std::ostringstream ss;
		//    ss << results[0].fm();
		//    __android_log_write(ANDROID_LOG_INFO, "FocusMeasureDummy", ss.str().c_str());
	}

	/// <summary>
	/// Initializes a new instance of the <see cref="Patch"/> class.
	/// </summary>
	Patch::Patch()
	{
	}

	/// <summary>
	/// Initializes a new instance of the <see cref="Patch"/> class.
	/// </summary>
	/// <param name="p">The upper left point of the patch.</param>
	/// <param name="w">The width of the patch.</param>
	/// <param name="h">The height of the patch.</param>
	/// <param name="f">The focus measure of the patch.</param>
	Patch::Patch(cv::Point p, int w, int h, double f)
	{
		mUpperLeft = p;
		mWidth = w;
		mHeight = h;
		mFm = f;
	}

	/// <summary>
	/// Initializes a new instance of the <see cref="Patch"/> class.
	/// </summary>
	/// <param name="p">The upper left point of the patch.</param>
	/// <param name="w">The width of the patch.</param>
	/// <param name="h">The height of the patch.</param>
	/// <param name="f">The focus measure of the patch.</param>
	/// <param name="fRef">The reference focus measure of the patch.</param>
	Patch::Patch(cv::Point p, int w, int h, double f, double fRef)
	{
		mUpperLeft = p;
		mWidth = w;
		mHeight = h;
		mFm = f;
		mFmReference = fRef;
	}

	/// <summary>
	/// Sets the position based on the upper left point, the height and the width.
	/// </summary>
	/// <param name="p">The upper left point of the patch.</param>
	/// <param name="w">The width of the patch.</param>
	/// <param name="h">The height of the patch.</param>
	void Patch::setPosition(cv::Point p, int w, int h)
	{
		mUpperLeft = p;
		mWidth = w;
		mHeight = h;
	}

	/// <summary>
	/// Returns the upper left point.
	/// </summary>
	/// <returns>The upper left point.</returns>
	cv::Point Patch::upperLeft() const
	{
		return mUpperLeft;
	}

	/// <summary>
	/// Returns the center of the patch.
	/// </summary>
	/// <returns>The center point.</returns>
	cv::Point Patch::center() const
	{
		cv::Point center(mUpperLeft.x+mWidth/2, mUpperLeft.y+mHeight/2);

		return center;
	}

	/// <summary>
	/// Sets the focus measure reference value.
	/// </summary>
	/// <param name="f">The fm reference value.</param>
	void Patch::setFmRef(double f)
	{
		mFmReference = f;
	}

	/// <summary>
	/// Sets the focus measure value.
	/// </summary>
	/// <param name="f">The f.</param>
	void Patch::setFm(double f)
	{
	    mFm = f;
	}

	/// <summary>
	/// Sets the weight of  the patch based on the ratio of the detected foreground and the area.
	/// </summary>
	/// <param name="w">The weight.</param>
	void Patch::setWeight(double w)
	{
		mWeight = w;
	}

	/// <summary>
	/// Sets the area of the patch (foreground).
	/// </summary>
	/// <param name="a">Foreground Area.</param>
	void Patch::setArea(double a)
	{
		mArea = a;
	}

	/// <summary>
	/// Returns the patch width.
	/// </summary>
	/// <returns>The Width.</returns>
	int Patch::width() const
	{
		return mWidth;
	}

	/// <summary>
	/// Returns the patch height.
	/// </summary>
	/// <returns>The height.</returns>
	int Patch::height() const
	{
		return mHeight;
	}

	bool Patch::isSharp() const
	{
		return mIsSharp;
	}

	void Patch::setSharpness(bool s)
	{
		mIsSharp = s;
	}

	bool Patch::foreground() const
	{
		return mForeground;
	}

	void Patch::setForeground(bool b)
	{
		mForeground = b;
	}

	/// <summary>
	// Returns the fm value
	/// </summary>
	/// <returns>The fm.</returns>
	double Patch::fm() const
	{
		return mFm;
	}

	/// <summary>
	/// Returns the weight based on the ratio of the foreground and patch size,
	/// </summary>
	/// <returns>The weight</returns>
	double Patch::weight() const
	{
		return mWeight;
	}

	/// <summary>
	/// Returns the area of the detected foreground.
	/// </summary>
	/// <returns>The foreground area.</returns>
	double Patch::area() const
	{
		return mArea;
	}

	/// <summary>
	/// Returns the fm value as string. NOT IMPLEMENTED.
	/// </summary>
	/// <returns>The fm value.</returns>
	std::string Patch::fmS() const
	{
		return std::string();
	}

	/// <summary>
	/// Returns the reference fm value.
	/// </summary>
	/// <returns>The reference fm value.</returns>
	double Patch::fmRef() const
	{
		return mFmReference;
	}

	/// <summary>
	/// The x coordinate of the upper left point of the patch.
	/// (allows for getting the coordinates without using OpenCV).
	/// </summary>
	/// <returns>The x coordinate of the upper left point.</returns>
	float Patch::centerX() const {

	    return (float) center().x;

	}

	/// <summary>
	/// The y coordinate of the upper left point of the patch.
	/// (allows for getting the coordinates without using OpenCV).
	/// </summary>
	/// <returns>The y coordinate of the upper left point.</returns>
    float Patch::centerY() const {

        return (float) center().y;

    }


}

