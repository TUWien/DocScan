package at.ac.tuwien.caa.docscan.logic;

import android.app.Activity;
import android.text.InputFilter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

import java.util.Date;

import at.ac.tuwien.caa.docscan.extensions.DateExtensionKt;


/**
 * Created by fabian on 26.09.2017.
 */
public class Helper {

    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'";

    public static int getDPI(double cameraDistance, float horizontalViewAngle, int imgW) {

        double thetaH = Math.toRadians(horizontalViewAngle);

        //Size in inches
        double printWidth = 2 * cameraDistance * Math.tan(thetaH / 2);

        return (int) Math.round((double) imgW / printWidth);

    }

    /**
     * @return an input filter for edit text that prevents that the user enters non valid characters
     * for directories (under Windows and Unix).
     */
    public static InputFilter[] getDocumentInputFilters() {

        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (source.length() < 1)
                return null;
            char last = source.charAt(source.length() - 1);
            if (RESERVED_CHARS.indexOf(last) > -1)
                return source.subSequence(0, source.length() - 1);

            return null;
        };

        return new InputFilter[]{new InputFilter.LengthFilter(100), filter};

    }

    /**
     * @return a file name prefix for a given timestamp document name and page num.
     */
    @NonNull
    public static String getFileNamePrefix(String timeStamp, String docName, int pageNum) {
        String leadingZeros = "00000";
        String page = Integer.toString(pageNum);
        int shift = page.length();
        if (shift < leadingZeros.length())
            page = leadingZeros.substring(0, leadingZeros.length() - shift) + page;

        return docName + "_" + page + '-' + timeStamp;
    }

    /**
     * @return a time stamp that is used for the file name generation.
     */
    public static String getFileTimeStamp() {
        return DateExtensionKt.getTimeStamp(new Date());
    }

    public static void hideKeyboard(Activity activity) {

        if (activity == null)
            return;

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
