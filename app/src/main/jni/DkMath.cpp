/*******************************************************************************************************
 DkMath.cpp
 Created on:	22.03.2010
 
 nomacs is a fast and small image viewer with the capability of synchronizing multiple instances
 
 Copyright (C) 2011-2013 Markus Diem <markus@nomacs.org>
 Copyright (C) 2011-2013 Stefan Fiel <stefan@nomacs.org>
 Copyright (C) 2011-2013 Florian Kleber <florian@nomacs.org>

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

#include "DkMath.h"

#pragma warning(push, 0)	// no warnings from includes - begin
#pragma warning(pop)		// no warnings from includes - end


namespace dsc {

// DkMath --------------------------------------------------------------------

/** 
* Divides the integer by 2.
* @param val the integer value.
* @return the half integer (floor(val)).
**/
int DkMath::halfInt(int val) {
	return (val >> 1);
}

/**
* Returns the greatest common divisor (GGT).
* Where a must be greater than b.
* @param a the greater number.
* @param b the smaller number.
* @return int the greatest common divisor.
**/ 
int DkMath::gcd(int a, int b) {
	// zu deutsch: ggt

	if (b==0)
		return a;
	else
		return gcd(b, a%b);
}

/**
* Computes the normalized angle in radians.
* The normalized angle is in this case defined as the
* corresponding angle within [0 pi].
* @param angle an angle in radians.
* @return the normalized angle in radians within [0 pi].
**/
double DkMath::normAngleRad(double angle) {
	
	// this could be a bottleneck
	if (std::abs(angle) > 1000)
		return angle;

	while (angle < 0)
		angle += 2*CV_PI;
	while (angle >= 2*CV_PI)
		angle -= 2*CV_PI;

	return angle;
}

/**
* Computes the normalized angle within startIvl and endIvl.
* @param angle the angle in radians.
* @param startIvl the interval's lower bound.
* @param endIvl the interval's upper bound.
* @return the angle within [startIvl endIvl)
**/
double DkMath::normAngleRad(double angle, double startIvl, double endIvl) {

	// this could be a bottleneck
	if (std::abs(angle) > 1000)
		return angle;

	while(angle <= startIvl)
		angle += endIvl-startIvl;
	while(angle > endIvl)
		angle -= endIvl-startIvl;

	return angle;
}

/**
* Computes the normalized angle within startIvl and endIvl.
* @param angle the angle in radians.
* @param startIvl the interval's lower bound.
* @param endIvl the interval's upper bound.
* @return the angle within [startIvl endIvl)
**/
float DkMath::normAngleRad(float angle, float startIvl, float endIvl) {

	// this could be a bottleneck
	if (std::abs(angle) > 1000)
		return angle;

	while(angle <= startIvl)
		angle += endIvl-startIvl;
	while(angle > endIvl)
		angle -= endIvl-startIvl;

	return angle;
}

/**
* Computes the normalized angle in radians.
* The normalized angle is in this case defined as the
* corresponding angle within [0 pi].
* @param angle an angle in radians.
* @return the normalized angle in radians within [0 pi].
**/
float DkMath::normAngleRad(float angle) {

	// this could be a bottleneck
	if (std::fabs(angle) > 1000)
		return angle;

	while (angle < 0)
		angle += 2*(float)CV_PI;
	while (angle >= 2.0*CV_PI)
		angle -= 2*(float)CV_PI;

	return angle;
}

double DkMath::distAngle(const double angle1, const double angle2) {

	double nAngle1 = normAngleRad(angle1);
	double nAngle2 = normAngleRad(angle2);

	double angle = std::fabs(nAngle1 - nAngle2);

	return (angle > CV_PI) ? 2*CV_PI - angle : angle;
}

/**
* Check if a number is a power of two.
* @param ps a positive integer
* @return true if ps is a power of two.
**/
bool DkMath::isPowerOfTwo(unsigned int ps) {

	// count the bit set, see: http://tekpool.wordpress.com/category/bit-count/
	unsigned int bC;

	bC = ps - ((ps >> 1) & 033333333333) - ((ps >> 2) & 011111111111);
	bC = ((bC + (bC >> 3)) & 030707070707) % 63;

	return bC == 1;
}

float DkMath::getNextPowerOfTwoDivisior(float factor) {

	int iv = cvRound(1.0f/factor);
	int pt = getNextPowerOfTwo(iv);

	// if the number is not yet a power of two or pt is one
	if (pt != iv && pt != 1)
		pt = pt >> 1;

	return (float)pt;
}

/**
* Returns the next power of two.
* @param val a number for which the next power of two needs to be computed.
* @return the next power of two for val.
**/
int DkMath::getNextPowerOfTwo(int val) {

	int pt = 1;
	while (val > pt)
		pt = pt << 1;	// *2

	return pt;
}

/**
* Returns the value of f(x,sigma) where f is a gaussian.
* @param sigma of the gaussian distribution.
* @param x param of the gaussian.
* @return f(x,sigma) .
**/
float DkMath::getGaussian(float sigma, float x) {

	return 1/sqrt(2*(float)CV_PI*sigma*sigma) * exp(-(x*x)/(2*sigma*sigma));
}

}
