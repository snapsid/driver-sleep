package com.sid.esleep.snapsid;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;



public class FaceOverlayView extends View {


    private LocationManager locationManager;
    private LocationListener locationListener;
    private final long MIN_TIME = 1000;
    private final long MIN_DIST = 5;


    private Bitmap mBitmap;
    private SparseArray<Face> mFaces;

    private double leftEyeOpenProbability = -1.0;
    private double rightEyeOpenProbability = -1.0;
    private double leftopenRatio = 1;
    private static int blinkCount = 0;
    private static int mes=0;

    private MediaPlayer player;

    private FaceDetector detector = new FaceDetector.Builder( getContext() )
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .build();

    public FaceOverlayView(Context context) {
        this(context, null);
    }

    public FaceOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBitmap( Bitmap bitmap ) {

        mBitmap = bitmap;
       // ActivityCompat.requestPermissions(,new String[]{Manifest.permission.SEND_SMS}, PackageManager.PERMISSION_GRANTED);
        if (!detector.isOperational()) {
            //Handle contingency
        } else {
            //Log.d("time1", SystemClock.currentThreadTimeMillis()+"");
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            mFaces = detector.detect(frame);
        }

        if(isEyeBlinked()){
            Log.d("isEyeBlinked","eye blink is observed");

            blinkCount++;
            mes++;

            if(blinkCount>10){
                Log.d("sound", "played");
                player=new MediaPlayer();
                player=MediaPlayer.create(getContext(), R.raw.dit);
                if(player.isPlaying())
                {
                    player.pause();
                    //ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS}, PackageManager.PERMISSION_GRANTED);
                }
                else
                {
                    player.start();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                    String username = prefs.getString("keyurl","xyz");
                    String phoneNumber = "6265305328";
                    String locString="EMERGENCY!! My location is: ";

                    Log.d("url2", username);

                    SmsManager smsManager = SmsManager.getDefault();
                 smsManager.sendTextMessage(phoneNumber, null, locString+"\n"+username, null, null);
                }

                blinkCount=0;


                Face face = mFaces.valueAt(0);
                float left=face.getIsLeftEyeOpenProbability();
                float right=face.getIsRightEyeOpenProbability();

                if(left==0.9||right==0.9){

                    Log.d("stop", "music stop");
                    if(player!=null&&player.isPlaying())
                    {
                        player.stop();
                        player.release();
                    }
                }





            }

            CameraActivity.showScore(blinkCount);
        }

        invalidate();
    }

    private boolean isEyeBlinked(){



        if(mFaces.size()==0)
            return false;

        Face face = mFaces.valueAt(0);
        float currentLeftEyeOpenProbability = face.getIsLeftEyeOpenProbability();
        float currentRightEyeOpenProbability = face.getIsRightEyeOpenProbability();
        if(currentLeftEyeOpenProbability== -1.0 || currentRightEyeOpenProbability == -1.0){
            return false;
        }

        if(leftEyeOpenProbability>0.13 || rightEyeOpenProbability > 0.13){
            boolean blinked = true;
            if(currentLeftEyeOpenProbability>0.6 || rightEyeOpenProbability> 0.6){
                blinked = false;
            }
            leftEyeOpenProbability = currentLeftEyeOpenProbability;
            rightEyeOpenProbability = currentRightEyeOpenProbability;
            return blinked;
        }else{
            leftEyeOpenProbability = currentLeftEyeOpenProbability;
            rightEyeOpenProbability = currentRightEyeOpenProbability;
            return true;
        }
    }


    private boolean isEyeToggled() {

        if (mFaces.size() == 0)
            return false;

        Face face = mFaces.valueAt(0);
        float currentLeftEyeOpenProbability = face.getIsLeftEyeOpenProbability();
        float currentRightEyeOpenProbability = face.getIsRightEyeOpenProbability();
        if (currentLeftEyeOpenProbability == -1.0 || currentRightEyeOpenProbability == -1.0) {
            return false;
        }

        double currentLeftOpenRatio = currentLeftEyeOpenProbability / currentRightEyeOpenProbability;
        if (currentLeftOpenRatio > 3) currentLeftOpenRatio = 3;
        if (currentLeftOpenRatio < 0.33) currentLeftOpenRatio = 0.33;

        Log.d("probs",currentLeftOpenRatio+" "+leftopenRatio );
        if(currentLeftOpenRatio==0.33|| currentLeftOpenRatio ==3.0){
            if(leftopenRatio==1){
                leftopenRatio = currentLeftOpenRatio;
            }

            if(leftopenRatio*currentLeftOpenRatio==0.99){
                leftopenRatio = currentLeftOpenRatio;
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if ((mBitmap != null) && (mFaces != null)) {
            double scale = drawBitmap(canvas);
            drawFaceLandmarks(canvas, scale);
        }
    }

    private double drawBitmap(Canvas canvas) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));
        canvas.drawBitmap(mBitmap, null, destBounds, null);
        return scale;
    }



    private void drawFaceLandmarks( Canvas canvas, double scale ) {
        Paint paint = new Paint();
        paint.setColor( Color.GREEN );
        paint.setStyle( Paint.Style.STROKE );
        paint.setStrokeWidth( 5 );

        for( int i = 0; i < mFaces.size(); i++ ) {
            Face face = mFaces.valueAt(i);

            for ( Landmark landmark : face.getLandmarks() ) {
                int cx = (int) ( landmark.getPosition().x * scale );
                int cy = (int) ( landmark.getPosition().y * scale );
                canvas.drawCircle( cx, cy, 10, paint );
            }
        }
    }

    private void logFaceData() {
        float smilingProbability;
        float leftEyeOpenProbability;
        float rightEyeOpenProbability;
        float eulerY;
        float eulerZ;
        for( int i = 0; i < mFaces.size(); i++ ) {
            Face face = mFaces.valueAt(i);
            smilingProbability = face.getIsSmilingProbability();
            leftEyeOpenProbability = face.getIsLeftEyeOpenProbability();
            rightEyeOpenProbability = face.getIsRightEyeOpenProbability();
            eulerY = face.getEulerY();
            eulerZ = face.getEulerZ();
            Log.e( "Tuts+ Face Detection", "Smiling: " + smilingProbability );
            Log.d( "Tuts+ Face Detection", "Left eye open: " + leftEyeOpenProbability );
            Log.d( "Tuts+ Face Detection", "Right eye open: " + rightEyeOpenProbability );
            Log.e( "Tuts+ Face Detection", "Euler Y: " + eulerY );
            Log.e( "Tuts+ Face Detection", "Euler Z: " + eulerZ );
        }
    }
}

