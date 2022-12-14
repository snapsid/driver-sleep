package com.sid.esleep.snapsid;



import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;







class OverlayView extends View {
    private static final String TAG = OverlayView.class.getSimpleName();

    private static final float GUIDE_FONT_SIZE = 26.0f;
    private static final float GUIDE_LINE_PADDING = 8.0f;
    private static final float GUIDE_LINE_HEIGHT = GUIDE_FONT_SIZE + GUIDE_LINE_PADDING;
    private static final float CARD_NUMBER_MARKUP_FONT_SIZE = GUIDE_FONT_SIZE + 2;

    private static final GradientDrawable.Orientation[] GRADIENT_ORIENTATIONS = { GradientDrawable.Orientation.TOP_BOTTOM,
            GradientDrawable.Orientation.LEFT_RIGHT, GradientDrawable.Orientation.BOTTOM_TOP, GradientDrawable.Orientation.RIGHT_LEFT };

    private static final int GUIDE_STROKE_WIDTH = 17;

    private static final float CORNER_RADIUS_SIZE = 1 / 15.0f;

    private static final int TORCH_WIDTH = 70;
    private static final int TORCH_HEIGHT = 50;

    private static final int LOGO_MAX_WIDTH = 100;
    private static final int LOGO_MAX_HEIGHT = TORCH_HEIGHT;

    private static final int BUTTON_TOUCH_TOLERANCE = 20;

    private final WeakReference<CameraActivity> mScanActivityRef;

    private Bitmap mBitmap;
    GradientDrawable mScanLineDrawable;
    private Rect mGuide;
    private int mRotation;
    private int mState;
    private int guideColor;

    private boolean hideCardIOLogo;
    private String scanInstructions;

    // Keep paint objects around for high frequency methods to avoid re-allocating them.
    private GradientDrawable mGradientDrawable;
    private final Paint mGuidePaint;
    private final Paint mLockedBackgroundPaint;
    private Path mLockedBackgroundPath;
    private Rect mCameraPreviewRect;
    private Rect mTorchRect, mLogoRect;
    private final boolean mShowTorch;
    private int mRotationFlip;
    private float mScale = 1;

    public OverlayView(CameraActivity captureActivity, AttributeSet attributeSet, boolean showTorch) {
        super(captureActivity, attributeSet);

        mShowTorch = showTorch;
        mScanActivityRef = new WeakReference<CameraActivity>(captureActivity);

        mRotationFlip = 1;

        // card.io is designed for an hdpi screen (density = 1.5);
        mScale = getResources().getDisplayMetrics().density / 1.5f;


        mGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLockedBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLockedBackgroundPaint.clearShadowLayer();
        mLockedBackgroundPaint.setStyle(Paint.Style.FILL);
        mLockedBackgroundPaint.setColor(0xbb000000); // 75% black

    }

    public int getGuideColor() {
        return guideColor;
    }

    public void setGuideColor(int color) {
        guideColor = color;
    }

    public boolean getHideCardIOLogo() {
        return hideCardIOLogo;
    }

    public void setHideCardIOLogo(boolean hide) {
        hideCardIOLogo = hide;
    }

    public String getScanInstructions() {
        return scanInstructions;
    }

    public void setScanInstructions(String scanInstructions) {
        this.scanInstructions = scanInstructions;
    }

    // Public methods used by CardIOActivity
    public void setGuideAndRotation(Rect rect, int rotation) {
        mRotation = rotation;
        mGuide = rect;
        invalidate();

        Point topEdgeUIOffset;
        if (mRotation % 180 != 0) {
            topEdgeUIOffset = new Point((int) (40 * mScale), (int) (60 * mScale));
            mRotationFlip = -1;
        } else {
            topEdgeUIOffset = new Point((int) (60 * mScale), (int) (40 * mScale));
            mRotationFlip = 1;
        }
        if (mCameraPreviewRect != null) {
            Point torchPoint = new Point(mCameraPreviewRect.left + topEdgeUIOffset.x,
                    mCameraPreviewRect.top + topEdgeUIOffset.y);

            // mTorchRect used only for touch lookup, not layout
            mTorchRect = Util.rectGivenCenter(torchPoint, (int) (TORCH_WIDTH * mScale),
                    (int) (TORCH_HEIGHT * mScale));

            // mLogoRect used only for touch lookup, not layout
            Point logoPoint = new Point(mCameraPreviewRect.right - topEdgeUIOffset.x,
                    mCameraPreviewRect.top + topEdgeUIOffset.y);
            mLogoRect = Util.rectGivenCenter(logoPoint, (int) (LOGO_MAX_WIDTH * mScale),
                    (int) (LOGO_MAX_HEIGHT * mScale));

            int[] gradientColors = { Color.WHITE, Color.BLACK };
            GradientDrawable.Orientation gradientOrientation = GRADIENT_ORIENTATIONS[(mRotation / 90) % 4];
            mGradientDrawable = new GradientDrawable(gradientOrientation, gradientColors);
            mGradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            mGradientDrawable.setBounds(mGuide);
            mGradientDrawable.setAlpha(50);

            mLockedBackgroundPath = new Path();
            mLockedBackgroundPath.addRect(new RectF(mCameraPreviewRect), Path.Direction.CW);
            mLockedBackgroundPath.addRect(new RectF(mGuide), Path.Direction.CCW);
        }
    }

    public void setBitmap(Bitmap bitmap) {
        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = bitmap;
        if (mBitmap != null) {
            decorateBitmap();
        }
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }


    public int getCardX() {
        return mGuide.centerX() - mBitmap.getWidth() / 2;
    }

    public int getCardY() {
        return mGuide.centerY() - mBitmap.getHeight() / 2;
    }

    public Bitmap getCardImage() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            return Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        } else {
            return null;
        }
    }

    // Drawing methods
    private Rect guideStrokeRect(int x1, int y1, int x2, int y2) {
        Rect r;
        int t2 = (int) (GUIDE_STROKE_WIDTH / 2 * mScale);
        r = new Rect();

        r.left = Math.min(x1, x2) - t2;
        r.right = Math.max(x1, x2) + t2;

        r.top = Math.min(y1, y2) - t2;
        r.bottom = Math.max(y1, y2) + t2;

        return r;
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (mGuide == null || mCameraPreviewRect == null) {
            return;
        }
        canvas.save();
        int tickLength;

        // Draw background rect

        mGradientDrawable.draw(canvas);

        if ((mRotation == 0) || (mRotation == 180)) {
            tickLength = (mGuide.bottom - mGuide.top) / 4;
        } else {
            tickLength = (mGuide.right - mGuide.left) / 4;
        }

        // Draw guide lines
        mGuidePaint.clearShadowLayer();
        mGuidePaint.setStyle(Paint.Style.FILL);
        mGuidePaint.setColor(guideColor);

        // top left
        canvas.drawRect(
                guideStrokeRect(mGuide.left, mGuide.top, mGuide.left + tickLength, mGuide.top),
                mGuidePaint);
        canvas.drawRect(
                guideStrokeRect(mGuide.left, mGuide.top, mGuide.left, mGuide.top + tickLength),
                mGuidePaint);

        // top right
        canvas.drawRect(
                guideStrokeRect(mGuide.right, mGuide.top, mGuide.right - tickLength, mGuide.top),
                mGuidePaint);
        canvas.drawRect(
                guideStrokeRect(mGuide.right, mGuide.top, mGuide.right, mGuide.top + tickLength),
                mGuidePaint);

        // bottom left
        canvas.drawRect(
                guideStrokeRect(mGuide.left, mGuide.bottom, mGuide.left + tickLength, mGuide.bottom),
                mGuidePaint);
        canvas.drawRect(
                guideStrokeRect(mGuide.left, mGuide.bottom, mGuide.left, mGuide.bottom - tickLength),
                mGuidePaint);

        // bottom right
        canvas.drawRect(
                guideStrokeRect(mGuide.right, mGuide.bottom, mGuide.right - tickLength,
                        mGuide.bottom), mGuidePaint);
        canvas.drawRect(
                guideStrokeRect(mGuide.right, mGuide.bottom, mGuide.right, mGuide.bottom
                        - tickLength), mGuidePaint);

        canvas.restore();

        // draw logo
        if (!hideCardIOLogo) {
            canvas.save();
            canvas.translate(mLogoRect.exactCenterX(), mLogoRect.exactCenterY());
            canvas.rotate(mRotationFlip * mRotation);
            canvas.restore();
        }

        if (mShowTorch) {
            // draw torch
            canvas.save();
            canvas.translate(mTorchRect.exactCenterX(), mTorchRect.exactCenterY());
            canvas.rotate(mRotationFlip * mRotation);
            canvas.restore();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            int action;
            action = event.getAction() & MotionEvent.ACTION_MASK;
            if (action == MotionEvent.ACTION_DOWN) {

                Point p = new Point((int) event.getX(), (int) event.getY());
                Rect r = Util.rectGivenCenter(p, BUTTON_TOUCH_TOLERANCE, BUTTON_TOUCH_TOLERANCE);
                if (mShowTorch && mTorchRect != null && Rect.intersects(mTorchRect, r)) {
                    mScanActivityRef.get().toggleFlash();
                } else {
                    mScanActivityRef.get().triggerAutoFocus();
                }
            }
        } catch (NullPointerException e) {

            Log.d(TAG, "NullPointerException caught in onTouchEvent method");
        }
        return false;
    }

    /* create the card image with inside a rounded rect */
    private void decorateBitmap() {

        RectF roundedRect = new RectF(2, 2, mBitmap.getWidth() - 2, mBitmap.getHeight() - 2);
        float cornerRadius = mBitmap.getHeight() * CORNER_RADIUS_SIZE;

        // Alpha canvas with white rounded rect
        Bitmap maskBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas maskCanvas = new Canvas(maskBitmap);
        maskCanvas.drawColor(Color.TRANSPARENT);
        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.BLACK);
        maskPaint.setStyle(Paint.Style.FILL);
        maskCanvas.drawRoundRect(roundedRect, cornerRadius, cornerRadius, maskPaint);

        Paint paint = new Paint();
        paint.setFilterBitmap(false);

        // Draw mask onto mBitmap
        Canvas canvas = new Canvas(mBitmap);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(maskBitmap, 0, 0, paint);

        // Now re-use the above bitmap to do a shadow.
        paint.setXfermode(null);

        maskBitmap.recycle();
    }

    // TODO - move this into RequestTask, so we just get back a card image ready to go
    public boolean isAnimating() {
        return (mState != 0);
    }

    public void setCameraPreviewRect(Rect rect) {
        mCameraPreviewRect = rect;
    }

    // for test
    public Rect getTorchRect() {
        return mTorchRect;
    }
}
