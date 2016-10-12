/*******************************************************************************************************
 DkMath.h
 Created on:	25.02.2010
 
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

#pragma once

#pragma warning(push, 0)	// no warnings from includes - begin
#include <cmath>
#include <cfloat>
#include <iostream>
#pragma warning(pop)		// no warnings from includes - end

#ifdef QT_NO_DEBUG_OUTPUT
#pragma warning(disable: 4127)		// no 'conditional expression is constant' if qDebug() messages are removed
#endif

#include "opencv2/core.hpp"
#include "opencv/cxcore.h"	// c functions e.g. dsc::round

#include "Utils.h"

#define DK_DEG2RAD	0.017453292519943
#define DK_RAD2DEG 	57.295779513082323

//// no min max macros for windows...
//#undef min
//#undef max

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

DllCoreExport int round(double x);

/** 
 * Provides useful mathematical functions.
 **/
class DkMath {

public:
	
	/** 
	 * Divides the integer by 2.
	 * @param val the integer value.
	 * @return the half integer (floor(val)).
	 **/
	static int halfInt(int val);

	/**
	 * Returns the greatest common divisor (GGT).
	 * Where a must be greater than b.
	 * @param a the greater number.
	 * @param b the smaller number.
	 * @return int the greatest common divisor.
	 **/ 
	static int gcd(int a, int b);

	/**
	 * Computes the normalized angle in radians.
	 * The normalized angle is in this case defined as the
	 * corresponding angle within [0 pi].
	 * @param angle an angle in radians.
	 * @return the normalized angle in radians within [0 pi].
	 **/
	static double normAngleRad(double angle);

	/**
	 * Computes the normalized angle within startIvl and endIvl.
	 * @param angle the angle in radians.
	 * @param startIvl the interval's lower bound.
	 * @param endIvl the interval's upper bound.
	 * @return the angle within [startIvl endIvl)
	 **/
	static double normAngleRad(double angle, double startIvl, double endIvl);

	/**
	 * Computes the normalized angle within startIvl and endIvl.
	 * @param angle the angle in radians.
	 * @param startIvl the interval's lower bound.
	 * @param endIvl the interval's upper bound.
	 * @return the angle within [startIvl endIvl)
	 **/
	static float normAngleRad(float angle, float startIvl, float endIvl);

	/**
	 * Computes the normalized angle in radians.
	 * The normalized angle is in this case defined as the
	 * corresponding angle within [0 pi].
	 * @param angle an angle in radians.
	 * @return the normalized angle in radians within [0 pi].
	 **/
	static float normAngleRad(float angle);

	static double distAngle(const double angle1, const double angle2);

	/**
	 * Check if a number is a power of two.
	 * @param ps a positive integer
	 * @return true if ps is a power of two.
	 **/
	static bool isPowerOfTwo(unsigned int ps);

	static float getNextPowerOfTwoDivisior(float factor);

	/**
	 * Returns the next power of two.
	 * @param val a number for which the next power of two needs to be computed.
	 * @return the next power of two for val.
	 **/
	static int getNextPowerOfTwo(int val);

	/**
	 * Returns the value of f(x,sigma) where f is a gaussian.
	 * @param sigma of the gaussian distribution.
	 * @param x param of the gaussian.
	 * @return f(x,sigma) .
	 **/
	static float getGaussian(float sigma, float x);

	template <typename numFmt>
	static numFmt sq(numFmt x) {
		
		return x*x;
	}

	template <typename numFmt>
	static double log2(numFmt x) {

		return log((double)x)/log(2.0);
	}


};

/**
 * A simple 2D vector class.
 */
class DllCoreExport DkVector {

public:
	
	union {
		float x;		/**< the vector's x-coordinate*/
		float width;	/**< the vector's x-coordinate*/
		float r;		/**< radius for log-polar coordinates or red channel*/
	};

	union {
		float y;		/**< the vector's y-coordinate*/
		float height;	/**< the vector's y-coordinate*/
		float theta;	/**< angle for log-polar coordinates*/
	};
	
	/** 
	 * Default constructor.
	 **/
	DkVector() : x(0.0f), y(0.0f) {};

	/** 
	 * Initializes an object.
	 * @param x the vector's x-coordinate.
	 * @param y the vector's y-coordinate.
	 **/
	DkVector(float x, float y) {
		this->x = x;
		this->y = y;
	};

	/**
	 * Initializes an object by means of the OpenCV size.
	 * @param s the size.
	 **/
	DkVector(cv::Size s) {
		this->width  = (float)s.width;
		this->height = (float)s.height;
	};

	/**
	 * Initializes a Vector by means of a OpenCV Point.
	 * @param p the point
	 **/
	DkVector(cv::Point2f p) {
		this->x = p.x;
		this->y = p.y;
	};

	/**
	 * Initializes a Vector by means of a OpenCV Point.
	 * @param p the point
	 **/
	DkVector(cv::Point p) {
		this->x = (float)p.x;
		this->y = (float)p.y;
	};

	/** 
	 * Default destructor.
	 **/
	virtual ~DkVector() {};

	/**
	 * Compares two vectors.
	 * @return true if both vectors have the same coordinates
	 */
	virtual bool operator== (const DkVector &vec) const {

		return (x == vec.x && y == vec.y);
	};

	/**
	 * Compares two vectors.
	 * @return true if both either the x or y coordinates of both
	 * vectors are not the same.
	 */
	virtual bool operator!= (const DkVector &vec) const {

		return (this->x != vec.x || this->y != vec.y);
	};

	/**
	 * Decides which vector is smaller.
	 * If y is < vec.y the function returns true.
	 * Solely if y == vec.y the x coordinates are compared.
	 * @param vec the vector to compare this instance with.
	 * @return true if y < vec.y or y == vec.y && x < vec.y.
	 **/
	virtual bool operator< (const DkVector &vec) const {

		if (y != vec.y)
			return y < vec.y;
		else
			return x < vec.x;
	};

	/**  
	 * Adds vector to the current vector.
	 * @param vec the vector to be added
	 */
	virtual void operator+= (const DkVector &vec) {

		this->x += vec.x;
		this->y += vec.y;
	};

	/** 
	 * Adds a scalar to the current vector.
	 * @param scalar the scalar which should be added
	 */
	virtual void operator+= (const float &scalar) {

		this->x += scalar;
		this->y += scalar;
	};

	/** 
	 * Computes the direction vector between this vector and vec.
	 * Computes the direction vector pointing to the current vector
	 * and replacing it.
	 */
	virtual void operator-= (const DkVector &vec) {

		this->x -= vec.x;
		this->y -= vec.y;
	};
	
	/** 
	 * Subtracts a scalar from the current vector.
	 * @param scalar the scalar which should be subtracted.
	 */
	virtual void operator-= (const float &scalar) {
		
		this->x -= scalar;
		this->y -= scalar;
	};

	/** 
	 * Scalar product.
	 * @param vec a vector which should be considered for the scalar product.
	 * @return the scalar product of vec and the current vector.
	 */ 
	virtual float operator* (const DkVector &vec) const {

		return this->x*vec.x + this->y*vec.y;
	};

	/** 
	 * Scalar multiplication.
	 * @param scalar a scalar.
	 */
	virtual void operator*= (const float scalar) {
		
		this->x *= scalar;
		this->y *= scalar;
	};

	/** 
	 * Scalar division.
	 * @param scalar a scalar.
	 */
	virtual void operator/= (const float scalar) {
		this->x /= scalar;
		this->y /= scalar;
	};

	// friends ----------------

	/** 
	 * Adds a vector to the current vector.
	 * @param vec the vector which should be added
	 * @return the addition of the current and the given vector.
	 */
	friend DkVector operator+ (const DkVector &vec, const DkVector &vec2) {

		return DkVector(vec.x+vec2.x, vec.y+vec2.y);
	};

	/** 
	 * Adds a scalar to the current vector.
	 * @param scalar the scalar which should be added
	 * @return the addition of the current vector and the scalar given.
	 */
	friend DkVector operator+ (const DkVector &vec, const float &scalar) {

		return DkVector(vec.x+scalar, vec.y+scalar);
	};

	/** 
	 * Adds a scalar to the current vector.
	 * @param scalar the scalar which should be added
	 * @return the addition of the current vector and the scalar given.
	 */
	friend DkVector operator+ (const float &scalar, const DkVector &vec) {

		return DkVector(vec.x+scalar, vec.y+scalar);
	};

	/** 
	 * Computes the direction vector between the given vector and vec.
	 * The direction vector C is computed by means of: C = B-A
	 * where B is the current vector.
	 * @param vec the basis vector A.
	 * @return a direction vector that points from @param vec to the 
	 * current vector.
	 */
	friend DkVector operator- (const DkVector &vec, const DkVector &vec2) {

		return DkVector(vec.x-vec2.x, vec.y-vec2.y);
	};

	/** 
	 * Subtracts a scalar from the current vector.
	 * @param scalar the scalar which should be subtracted.
	 * @return the subtraction of the current vector and the scalar given.
	 */
	friend DkVector operator- (const DkVector vec, const float &scalar) {

		return DkVector(vec.x-scalar, vec.y-scalar);
	};

	/** 
	 * Subtracts the vector from a scalar.
	 * @param scalar the scalar which should be subtracted.
	 * @return the subtraction of the current vector and the scalar given.
	 */
	friend DkVector operator- (const float &scalar, const DkVector vec) {

		return DkVector(scalar-vec.x, scalar-vec.y);
	};

	/** 
	 * Scalar multiplication.
	 * @param scalar a scalar.
	 * @return the current vector multiplied by a scalar.
	 */
	friend DkVector operator* (const DkVector& vec, const float scalar) {

		return DkVector(vec.x*scalar, vec.y*scalar);
	};

	/** 
	 * Scalar multiplication.
	 * @param scalar a scalar.
	 * @return the current vector multiplied by a scalar.
	 */
	friend DkVector operator* (const float &scalar, const DkVector& vec) {

		return DkVector(vec.x*scalar, vec.y*scalar);
	};

	/** 
	 * Scalar division.
	 * @param vec a vector which shall be divided.
	 * @param scalar a scalar.
	 * @return the current vector divided by a scalar.
	 */
	friend DkVector operator/ (const DkVector &vec, const float &scalar) {

		return DkVector(vec.x/scalar, vec.y/scalar);
	};

	/** 
	 * Scalar division.
	 * @param scalar a scalar.
	 * @param vec a vector which shall be divided.
	 * @return the current vector divided by a scalar.
	 */
	friend DkVector operator/ (const float &scalar, const DkVector &vec) {

		return DkVector(scalar/vec.x, scalar/vec.y);
	};

	/**
	 * Writes the vector r coordinates to the outputstream s.
	 * @param s the outputstream
	 * @param r the vector
	 * @return friend std::ostream& the modified outputstream
	 **/ 
	friend std::ostream& operator<<(std::ostream& s, const DkVector& r){

		return r.put(s);
	};

	/**
	 * Writes the vector coordinates to the stream s.
	 * @param s the output stream
	 * @return std::ostream& the output stream with the coordinates.
	 **/ 
	virtual std::ostream& put(std::ostream& s) const {

		return s << "[" << x << ", " << y << "]";
	};

	bool isEmpty() const {

		return x == 0 && y == 0;
	};


	/**
	 * Returns the largest coordinate.
	 * @return float the largest coordinate
	 **/ 
	virtual float maxCoord() {

		return std::max(x, y);
	};

	/**
	 * Returns the largest coordinate.
	 * @return float the largest coordinate.
	 **/ 
	virtual float minCoord() const {

		return std::min(x, y);
	};

	/**
	 * Creates a new vector having the
	 * maximum coordinates of both vectors.
	 * Thus: n.x = max(this.x, vec.x).
	 * @param vec the second vector.
	 * @return a vector having the maximum 
	 * coordinates of both vectors.
	 **/
	virtual DkVector maxVec(const DkVector vec) const {

		return DkVector(std::max(x, vec.x), std::max(y, vec.y));
	}

	/**
	 * Creates a new vector having the
	 * minimum coordinates of both vectors.
	 * Thus: n.x = min(this.x, vec.x).
	 * @param vec the second vector.
	 * @return a vector having the minimum
	 * coordinates of both vectors.
	 **/
	virtual DkVector minVec(const DkVector vec) const{

		return DkVector(std::min(x, vec.x), std::min(y, vec.y));
	}

	/**
	 * Swaps the coordinates of a vector.
	 **/
	void swap() {
		float xtmp = x;
		x = y;
		y = xtmp;
	}

	/** 
	* Returns the angle between two vectors
	*  @param vec vector
	*  @return the angle between two vectors
	*/
	double angle(const DkVector &vec) const {
		return acos(cosv(vec));
	};

	double cosv(const DkVector& vec) const {

		return (x*vec.x + y*vec.y) / (sqrt(x*x + y*y)*sqrt(vec.x*vec.x + vec.y*vec.y));
	};

	/**
	 * Returns the vector's angle in radians.
	 * The angle is computed by: atan2(y,x).
	 * @return the vector's angle in radians.
	 **/
	double angle() const {
		return atan2(y, x);
	};

	/**
	 * Rotates the vector by a specified angle in radians.
	 * The rotation matrix is: R(-theta) = [cos sin; -sin cos]
	 * @param angle the rotation angle in radians.
	 **/
	void rotate(double angle) {
		
		float xtmp = x;
		x = (float) ( xtmp*cos(angle)+y*sin(angle));
		y = (float) (-xtmp*sin(angle)+y*cos(angle));
	};

	/**
	 * Computes the absolute value of both coordinates.
	 **/
	virtual void abs() {

		x = fabs(x);
		y = fabs(y);
	};

	/**
	 * Clips the vector's coordinates to the bounds given.
	 * @param maxBound the maximum bound.
	 * @param minBound the minimum bound.
	 **/
	virtual void clipTo(float maxBound = 1.0f, float minBound = 0.0f) {

		if (minBound > maxBound) {
			std::cout << "[DkVector3] maxBound < minBound: " << maxBound << " < " << minBound << std::endl;
			return;
		}

		if (x > maxBound)		x = maxBound;
		else if (x < minBound)	x = minBound;
		if (y > maxBound)		y = maxBound;
		else if (y < minBound)	y = minBound;

	};

	/**
	* Clips the vector's coordinates to the bounds given.
	* @param maxBound the maximum bound.
	* @param minBound the minimum bound.
	**/
	virtual void clipTo(const DkVector& maxBound) {

		if (maxBound.x < 0  || maxBound.y < 0) {

			DkVector nonConst = maxBound;
			//qWarning() << "[WARNING] clipTo maxBound < 0: " << nonConst.toString();
			return;
		}

		maxVec(DkVector(0.0f,0.0f));
		minVec(maxBound);
	};
	
	/** 
	 * Normal vector.
	 * @return a vector which is normal to the current vector
	 * (rotated by 90� counter clockwise).
	 */
	DkVector normalVec() const {

		return DkVector(-y, x);
	};

	/** 
	 * The vector norm.
	 * @return the vector norm of the current vector.
	 */
	virtual float norm() const{
		
		return sqrt(this->x*this->x + this->y*this->y);
	}

	/** 
	 * Normalizes the vector.
	 * After normalization the vector's magnitude is |v| = 1
	 */
	virtual void normalize() {
		float n = norm();
		x /= n; 
		y /= n;
	};

	/** Returns euclidean distance between two vectors
	 *  @param vec vector
	 *  @return the euclidean distance
	 */
	virtual float euclideanDistance(const DkVector &vec) {
		return sqrt((this->x - vec.x)*(this->x - vec.x) + (this->y - vec.y)*(this->y - vec.y));
	};
	

	/** 
	 * Scalar product.
	 * @param vec a vector which should be considered for the scalar product.
	 * @return the scalar product of vec and the current vector.
	 */ 
	virtual float scalarProduct(const DkVector vec) const {

		return this->x*vec.x + this->y*vec.y;
	};

	virtual DkVector round() const {
		return DkVector((float)dsc::round(x), (float)dsc::round(y));
	} 

	///** 
	// * String containing the vector's values.
	// * @return a String representing the vector's coordinates: <x, y>
	// */
	//virtual std::string toString();

	/** 
	 * Slope of a line connecting two vectors. 
	 * start point is the actual vector, end point the parameter vector
	 * @param vec a vector which should be considered for the slope.
	 * @return the slope between the two points.
	 */ 
	float slope(DkVector vec) {
		return (vec.x - this->x) != 0 ? (vec.y - this->y) / (vec.x - this->x) : FLT_MAX;
	}

	DkVector mul(const DkVector& vec) const {
		return DkVector(x*vec.x, y*vec.y);
	};

	/**
	 * Convert DkVector to cv::Point.
	 * @return a cv::Point having the vector's coordinates.
	 **/
	virtual cv::Point getCvPoint32f() const {

		return cv::Point_<float>(x, y);
	};

	/**
	 * Convert DkVector to cv::Point.
	 * The vectors coordinates are rounded.
	 * @return a cv::Point having the vector's coordinates.
	 **/
	virtual cv::Point getCvPoint() const {

		return cv::Point(dsc::round(x), dsc::round(y));
	};

	/**
	 * Convert DkVector to cv::Size.
	 * The vector coordinates are rounded.
	 * @return a cv::Size having the vector's coordinates.
	 **/
	cv::Size getCvSize() const {

		return cv::Size(dsc::round(width), dsc::round(height));
	}
};

}
