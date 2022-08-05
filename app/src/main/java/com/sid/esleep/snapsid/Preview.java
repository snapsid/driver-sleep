package com.sid.esleep.snapsid;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;



class Preview extends ViewGroup {
    private static final String TAG = Preview.class.getSimpleName();

    private int mPreviewWidth;
    private int mPreviewHeight;

    SurfaceView mSurfaceView;

    public Preview(Context context, AttributeSet attributeSet, int previewWidth, int previewHeight) {
        super(context, attributeSet);


        mPreviewWidth = previewHeight;
        mPreviewHeight = previewWidth;

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
    }

    public SurfaceView getSurfaceView() {
        assert mSurfaceView != null;
        return mSurfaceView;
    }

    SurfaceHolder getSurfaceHolder() {
        SurfaceHolder holder = getSurfaceView().getHolder();
        assert holder != null;
        return holder;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawARGB(255, 255, 0, 0);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            assert mSurfaceView != null;

            final int width = r - l;
            final int height = b - t;

            // Center the child SurfaceView within the parent, making sure that the preview is
            // *always* fully contained on the device screen.
            if (width * mPreviewHeight > height * mPreviewWidth) {
                final int scaledChildWidth = mPreviewWidth * height / mPreviewHeight;
                mSurfaceView.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = mPreviewHeight * width / mPreviewWidth;
                mSurfaceView.layout(0, (height - scaledChildHeight) / 2, width,
                        (height + scaledChildHeight) / 2);
            }
        }
    }

}
