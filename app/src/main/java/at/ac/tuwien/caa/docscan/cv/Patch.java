package at.ac.tuwien.caa.docscan.cv;

/**
 * Created by fabian on 22.06.2016.
 */
public class Patch {

    // TODO: check if type int is appropriate - at the moment it seems so:
    private int mPX, mPY;
    private int mWidth, mHeight;
    private double mFm;


    public Patch(int pX, int pY, int width, int height, double fM) {

        mPX = pX;
        mPY = pY;
        mWidth = width;
        mHeight = height;
        mFm = fM;

    }

    public int getPX() {
        return mPX;
    }

    public int getPY() {
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

}

