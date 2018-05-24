package dju.teambabo.proj.bigbrother_client_android_102;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class DetectorService extends Service {

    private ImageView mPopupView;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    private float START_X, START_Y;
    private int PREV_X, PREV_Y;
    private int MAX_X = -1, MAX_Y = -1;

    private boolean PreviewMode = true;

    private View.OnClickListener mViewTouchListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (!PreviewMode) {
                mPopupView.setImageResource(R.drawable.preview_on);
                PreviewMode = true;
                DetectorCameraActivity contexta = (DetectorCameraActivity) DetectorCameraActivity.contexta;
                if(contexta != null) {
                    contexta.finish();
                }

            }
            else{
                mPopupView.setImageResource(R.drawable.preview_off);
                Intent intent = new Intent(DetectorService.this, DetectorCameraActivity.class);

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(intent);
                PreviewMode = false;

            }
            mWindowManager.updateViewLayout(mPopupView, mParams);






            return ;
        }
    };

    public DetectorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mPopupView = new ImageView(this);
        mPopupView.setImageResource(R.drawable.preview_on);
        mPopupView.setOnClickListener(mViewTouchListener);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mPopupView, mParams);
        mWindowManager.updateViewLayout(mPopupView, mParams);

    }
    private void setMaxPosition() {
        DisplayMetrics matrix = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(matrix);

        MAX_X = matrix.widthPixels - mPopupView.getWidth();
        MAX_Y = matrix.heightPixels - mPopupView.getHeight();
    }


    private void optimizePosition() {
        //√÷¥Î∞™ ≥—æÓ∞°¡ˆ æ ∞‘ º≥¡§
        if(mParams.x > MAX_X) mParams.x = MAX_X;
        if(mParams.y > MAX_Y) mParams.y = MAX_Y;
        if(mParams.x < 0) mParams.x = 0;
        if(mParams.y < 0) mParams.y = 0;
    }





    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setMaxPosition();
        optimizePosition();
    }
    @Override
    public void onDestroy() {
        if(mWindowManager != null) {        //서비스 종료시 뷰 제거. *중요 : 뷰를 꼭 제거 해야함.
            if(mPopupView != null) mWindowManager.removeView(mPopupView);
        }
        stopSelf();
/*
        if (!PreviewMode) {
            mPopupView.setImageResource(R.drawable.preview_on);
            PreviewMode = true;
            DetectorCameraActivity contexta = (DetectorCameraActivity) DetectorCameraActivity.contexta;
            if(contexta != null) {
                contexta.finish();
            }

        }
        else{
            mPopupView.setImageResource(R.drawable.preview_off);

            Intent intent = new Intent(DetectorService.this, DetectorCameraActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
            PreviewMode = false;

        }
        mWindowManager.updateViewLayout(mPopupView, mParams);
*/
        super.onDestroy();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

/*
        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflater.inflate(R.layout.camera_connection_fragment_detection, null);

        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,

                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,

                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,

                PixelFormat.TRANSLUCENT);

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);
        mManager.updateViewLayout(mView, mParams);
*/

        //mappedRecognitions = (ArrayList<RecognitionSerializable>) intent.getSerializableExtra("mappedRecognitions");
        PreviewMode = intent.getBooleanExtra("flag", false);
        if (PreviewMode){
            mPopupView.setImageResource(R.drawable.preview_on);
        }else {
            mPopupView.setImageResource(R.drawable.preview_off);
        }
        mWindowManager.updateViewLayout(mPopupView, mParams);

        //Log.d("TAG",mappedRecognitions.toString());
        return startId;
    }
}
