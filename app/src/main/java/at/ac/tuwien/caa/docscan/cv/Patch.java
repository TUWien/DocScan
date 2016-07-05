package at.ac.tuwien.caa.docscan.cv;

/**
 * Created by fabian on 22.06.2016.
 */
public class Patch {

    private float mPX, mPY;
    private int mWidth, mHeight;
    private double mFm;
    private float mDrawViewPX, mDrawViewPY;


    public Patch(float pX, float pY, int width, int height, double fM) {

        mPX = pX;
        mPY = pY;
        mWidth = width;
        mHeight = height;
        mFm = fM;

    }

    public float getPX() {

        return mPX;

    }

    public float getPY() {

        return mPY;

    }

    public int getWidth() {

        return mWidth;

    }

    public int getHeight() {

        return mHeight;

    }

    public double getFM() {

        return mFm;

    }

    public void setDrawViewPX(float drawViewPX) {

        mDrawViewPX = drawViewPX;
    }

    public float getDrawViewPX() {

        return mDrawViewPX;

    }

    public float getDrawViewPY() {

        return mDrawViewPY;

    }

    public void setDrawViewPY(float drawViewPY) { mDrawViewPY = drawViewPY; }

}

