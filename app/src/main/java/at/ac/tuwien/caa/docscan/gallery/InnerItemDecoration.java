package at.ac.tuwien.caa.docscan.gallery;

//
// Source code recreated from a GreedoSpacingItemDecoration.class file by IntelliJ IDEA
// This class removes the item decoration on the outer screen regions (compared to GreedoSpacingItemDecoration ).
// (powered by Fernflower decompiler)
//

/**
 * Created by Julian Villella on 15-07-30. Changed by Fabian Hollaus
 */

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.fivehundredpx.greedolayout.GreedoLayoutManager;
import com.fivehundredpx.greedolayout.GreedoLayoutSizeCalculator;

public class InnerItemDecoration extends RecyclerView.ItemDecoration {

    public static int DEFAULT_SPACING = 64;
    private int mSpacing;

    public InnerItemDecoration() {
        this(DEFAULT_SPACING);
    }

    public InnerItemDecoration(int spacing) {
        this.mSpacing = spacing;
    }

    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if(!(parent.getLayoutManager() instanceof GreedoLayoutManager)) {
            throw new IllegalArgumentException(String.format("The %s must be used with a %s", new Object[]{InnerItemDecoration.class.getSimpleName(), GreedoLayoutManager.class.getSimpleName()}));
        } else {
            GreedoLayoutManager layoutManager = (GreedoLayoutManager)parent.getLayoutManager();
            int childIndex = parent.getChildAdapterPosition(view);
            if(childIndex != -1) {
                outRect.top = 0;
                outRect.bottom = this.mSpacing;
                outRect.left = 0;
                outRect.right = this.mSpacing;

                //                Remove the right spacing if this is the last item in the row:
                if (isRightChild(childIndex, layoutManager))
                    outRect.right = 0;

//                Remove the bottom spacing if this is the last row:
                if (isBottomChild(childIndex, layoutManager))
                    outRect.bottom = 0;

            }
        }
    }


    private static boolean isRightChild(int position, GreedoLayoutManager layoutManager) {
        boolean isFirstViewHeader = layoutManager.isFirstViewHeader();
        if(isFirstViewHeader && position == 0) {
            return true;
        } else {
            if(isFirstViewHeader && position > 0) {
                --position;
            }

            GreedoLayoutSizeCalculator sizeCalculator = layoutManager.getSizeCalculator();
            int rowForPosition = sizeCalculator.getRowForChildPosition(position);
            int rowForPositionPlus1 = sizeCalculator.getRowForChildPosition(position+1);
            return rowForPositionPlus1 == rowForPosition + 1;
        }
    }

    private static boolean isBottomChild(int position, GreedoLayoutManager layoutManager) {


        GreedoLayoutSizeCalculator sizeCalculator = layoutManager.getSizeCalculator();
        int rowForPosition = sizeCalculator.getRowForChildPosition(position);
        int lastRow = sizeCalculator.getRowForChildPosition(layoutManager.getItemCount());
        return rowForPosition == lastRow;

    }

}
