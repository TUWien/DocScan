package at.ac.tuwien.caa.docscan.logic;

/**
 * Created by fabian on 20.12.2016.
 */
public class Settings {

    private static Settings mSettings = null;

    private boolean mUseFastPageSegmentation;

    private Settings() {

        mUseFastPageSegmentation = true;

    }

    public static Settings getInstance() {

        if (mSettings == null)
            mSettings = new Settings();

        return mSettings;

    }

    public boolean getUseFastPageSegmentation() {
        return mUseFastPageSegmentation;
    }

    public void setUseFastPageSegmentation(boolean fast) {
        mUseFastPageSegmentation = fast;
    }
}
