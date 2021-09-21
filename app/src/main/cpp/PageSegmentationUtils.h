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

#include "DkMath.h"

#pragma warning(push, 0)    // no warnings from includes - begin

#include <opencv2/core/core.hpp>

#pragma warning(pop)        // no warnings from includes - end

// hide dll-interface warning
#pragma warning(disable: 4251)

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

/**
* Box class DK_CORE_API, defines a non-skewed rectangle e.g. Bounding Box
**/
    class DkBox {

    public:

        /**
        * Default constructor.
        * All values are initialized with zero.
        **/
        DkBox() : uc(), lc() {};

        /**
        * Constructor.
        * @param uc the upper left corner of the box.
        * @param size the size of the box.
        **/
        DkBox(DkVector uc, DkVector size) {

            this->uc = uc;
            this->lc = uc + size;

            //if (size.width < 0 || size.height < 0)
            //std::cout << "the size is < 0: " << size << std::endl;
        };

        /**
        * Constructor.
        * @param x value of the upper left corner.
        * @param y value of the upper left corner.
        * @param width of the box.
        * @param height of the box.
        **/
        DkBox(float x, float y, float width, float height) {

            DkVector size = DkVector(width, height);

            uc = DkVector(x, y);
            lc = uc + size;

            //if (size.width < 0 || size.height < 0)
            //std::cout << "the size is < 0: " << size << std::endl;

        };

        /**
        * Constructor.
        * @param r box as rect with upperleft corner and width and height.
        **/
        DkBox(cv::Rect r) {

            DkVector size((float) r.width, (float) r.height);

            uc.x = (float) r.x;
            uc.y = (float) r.y;

            lc = uc + size;

            //if (size.width < 0 || size.height < 0)
            //	std::cout << "the size is < 0: " << size << std::endl;

        };

        /**
        * Constructor.
        * @param b box as DkBox.
        **/
        DkBox(const DkBox &b) {

            this->uc = b.uc;
            this->lc = b.uc + b.size();

            //if (size().width < 0 || size().height < 0)
            //	std::cout << "the size is < 0: " << size() << std::endl;
        }

        /**
        * Default destructor.
        **/
        ~DkBox() {};

        void getStorageBuffer(char **buffer, size_t &length) const {


            size_t newBufferLength = length + 4 * sizeof(float);
            char *newStream = new char[newBufferLength];

            if (*buffer) {

                // copy old stream & clean it
                memcpy(newStream, *buffer, length);
                delete *buffer;
            }

            float *newFStream = (float *) newStream;

            int pos = 0;
            newFStream[pos] = uc.x;
            pos++;
            newFStream[pos] = uc.y;
            pos++;
            newFStream[pos] = lc.x;
            pos++;
            newFStream[pos] = lc.y;
            pos++;

            *buffer = newStream;
            length = newBufferLength;
        }

        const char *setSorageBuffer(const char *buffer) {

            const float *fBuffer = (const float *) buffer;
            int pos = 0;
            uc.x = fBuffer[pos];
            pos++;
            uc.y = fBuffer[pos];
            pos++;
            lc.x = fBuffer[pos];
            pos++;
            lc.y = fBuffer[pos];
            pos++;

            return buffer + sizeof(float) * pos;    // update buffer position
        }

        //friend std::ostream& operator<<(std::ostream& s, DkBox& b) - original
        friend std::ostream &operator<<(std::ostream &s, const DkBox &b) {

            // this makes the operator<< virtual (stroustrup)
            return s << "uc: " << b.uc << " lc:" << b.lc;
        };

        void moveBy(const DkVector &dxy) {

            uc += dxy;
            lc += dxy;
        };

        bool isEmpty() const {

            return uc.isEmpty() && lc.isEmpty();
        };

        /**
        * Returns the box as opencv Rect.
        * @return a box as opencv Rect.
        **/
        cv::Rect getCvRect() const {

            return cv::Rect(dsc::round(uc.x), dsc::round(uc.y), dsc::round(size().width),
                            dsc::round(size().height));
        }

        static DkBox contour2BBox(const std::vector<std::vector<cv::Point> > &pts) {

            if (pts.empty())
                return DkBox();

            int ux = INT_MAX, uy = INT_MAX;
            int lx = 0, ly = 0;

            for (int cIdx = 0; cIdx < (int) pts.size(); cIdx++) {

                const std::vector<cv::Point> &cont = pts[cIdx];

                for (int idx = 0; idx < (int) cont.size(); idx++) {

                    cv::Point p = cont[idx];

                    if (p.x < ux)
                        ux = p.x;
                    if (p.x > lx)
                        lx = p.x;
                    if (p.y < uy)
                        uy = p.y;
                    if (p.y > ly)
                        ly = p.y;
                }
            }
            DkBox rect((float) ux, (float) uy, (float) lx - ux, (float) ly - uy);

            return rect;
        }

        /**
        * Enlarges the box by the given offset, and the upperleft corner is recalculated.
        * @param offset by which the box is expanded.
        **/
        void expand(float offset) {

            uc -= (offset * 0.5f);
        }

        /**
        * Clips the box according the vector s (the box is only clipped but not expanded).
        * @param s the clip vector.
        **/
        void clip(DkVector s) {

            uc.round();
            lc.round();

            uc.clipTo(s);
            lc.clipTo(s);

            //if (lc.x > s.x || lc.y > s.y)
            //	mout << "I did not clip..." << dkendl;
        };

        bool within(const DkVector &p) const {

            return (p.x >= uc.x && p.x < lc.x &&
                    p.y >= uc.y && p.y < lc.y);
        };

        DkVector center() const {
            return uc + size() * 0.5f;
        };

        void scaleAboutCenter(float s) {

            DkVector c = center();

            uc = DkVector(uc - c) * s + c;
            lc = DkVector(lc - c) * s + c;
        };

        /**
        * Returns the x value of the upper left corner.
        * @return x value in pixel of the upperleft corner.
        **/
        int getX() const {
            return dsc::round(uc.x);
        };

        /**
        * Returns the y value of the upper left corner.
        * @return y value in pixel of the upperleft corner.
        **/
        int getY() const {
            return dsc::round(uc.y);
        };

        /**
        * Returns the width of the box.
        * @return the width in pixel of the box.
        **/
        int getWidth() const {
            return dsc::round(lc.x - uc.x);
        };

        /**
        * Returns the width of the box.
        * @return float the width in pixel fo the box.
        **/
        double getWidthF() const {
            return lc.x - uc.x;
        };

        /**
        * Returns the height of the box.
        * @return the height in pixel of the box.
        **/
        int getHeight() const {
            return dsc::round(lc.y - uc.y);
        };

        /**
        * Returns the height of the box as float
        * @return float height in pixel of the box.
        **/
        double getHeightF() const {
            return lc.y - uc.y;
        };

        /**
        * Returns the size of the box.
        * @return size of the box as opencv Size.
        **/
        cv::Size getSize() const {
            return cv::Size(getWidth(), getHeight());
        };

        DkVector size() const {

            return lc - uc;
        };

        void setSize(DkVector size) {

            lc = uc + size;
        };

        double area() const {

            DkVector s = size();
            return s.width * s.height;
        };

        double intersectArea(const DkBox &box) const {

            DkVector tmp1 = lc.maxVec(box.lc);
            DkVector tmp2 = uc.maxVec(box.uc);

            // no intersection?
            if (lc.x < uc.x || lc.y < lc.y)
                return 0;

            tmp1 = tmp2 - tmp1;

            return tmp1.width * tmp1.height;
        };

        DkVector uc;        /**< upper left corner of the box **/
        DkVector lc;        /**< lower right corner of the box **/
    };

/**
* A simple point class DK_CORE_API.
* This class DK_CORE_API is needed for a fast computation
* of the polygon overlap.
**/
    class DkIPoint {

    public:
        int x;
        int y;

        DkIPoint() : x(0), y(0) {};

        DkIPoint(int x, int y) {
            this->x = x;
            this->y = y;
        };
    };


/**
* A simple vertex class DK_CORE_API.
* This class DK_CORE_API is needed for a fast computation
* of the polygon overlap.
**/
    class DkVertex {

    public:
        DkIPoint ip;
        DkIPoint rx;
        DkIPoint ry;
        int in;

        DkVertex() {};

        DkVertex(DkIPoint ip, DkIPoint rx, DkIPoint ry) {
            this->ip = ip;
            this->rx = rx;
            this->ry = ry;
            in = 0;
        };
    };


    class DkIntersectPoly {

        // this class DK_CORE_API is based on a method proposed by norman hardy
        // see: http://www.cap-lore.com/MathPhys/IP/aip.c

    public:

        DkIntersectPoly();

        DkIntersectPoly(std::vector<DkVector> vecA, std::vector<DkVector> vecB);

        double compute();

    private:

        std::vector<DkVector> vecA;
        std::vector<DkVector> vecB;
        int64 interArea;
        DkVector mMaxRange;
        DkVector mMinRange;
        DkVector scale;
        float gamut;

        void inness(std::vector<DkVertex> ipA, std::vector<DkVertex> ipB);

        void cross(DkVertex a, DkVertex b, DkVertex c, DkVertex d, double a1, double a2, double a3,
                   double a4);

        void cntrib(int fx, int fy, int tx, int ty, int w);

        int64 area(DkIPoint a, DkIPoint p, DkIPoint q);

        bool ovl(DkIPoint p, DkIPoint q);

        void getVertices(const std::vector<DkVector> &vec, std::vector<DkVertex> *ip, int noise);

        void computeBoundingBox(const std::vector<DkVector> &vec, DkVector *minRange,
                                DkVector *maxRange);
    };

// data class
    class DllCoreExport DkPolyRect {

    public:
        //DkPolyRect(DkVector p1, DkVector p2, DkVector p3, DkVector p4);
        DkPolyRect(const std::vector<cv::Point> &pts = std::vector<cv::Point>());

        DkPolyRect(const std::vector<DkVector> &pts);

        bool empty() const;

        double getMaxCosine() const { return maxCosine; };

        void draw(cv::Mat &img, const cv::Scalar &col = cv::Scalar(0, 100, 255)) const;

        std::vector<cv::Point> toCvPoints() const;

        std::vector<DkVector> getCorners() const;

        DkBox getBBox() const;

        double intersectArea(const DkPolyRect &pr) const;

        double getArea();

        double getAreaConst() const;

        void scale(float s);

        void scaleCenter(float s);

        bool inside(const DkVector &vec) const;

        float maxSide() const;

        DkVector center() const;

        static bool compArea(const DkPolyRect &pl, const DkPolyRect &pr);

        void setThreshold(int thr);

        int threshold() const;

        void setChannel(int chl);

        int channel() const;

    protected:
        std::vector<DkVector> mPts;
        double maxCosine;
        double area;

        double mChlIndex = -1;    // last channel
        double mThrIndex = -1;    // last threshold

        void toDkVectors(const std::vector<cv::Point> &pts, std::vector<DkVector> &dkPts) const;

        void computeMaxCosine();
    };

};
