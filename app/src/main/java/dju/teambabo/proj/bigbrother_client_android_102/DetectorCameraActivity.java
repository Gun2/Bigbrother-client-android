package dju.teambabo.proj.bigbrother_client_android_102;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.os.Trace;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dju.teambabo.proj.bigbrother_client_android_102.OverlayView.DrawCallback;
import dju.teambabo.proj.bigbrother_client_android_102.env.BorderedText;
import dju.teambabo.proj.bigbrother_client_android_102.env.ImageUtils;
import dju.teambabo.proj.bigbrother_client_android_102.env.Logger;

public class DetectorCameraActivity extends Activity implements OnImageAvailableListener, Camera.
        PreviewCallback {
    /**
     * 서비스 종료
     */
    public static Activity contexta;
    /**
     * 실행 스레드풀
     */
    ThreadPoolExecutor threadPool = new ThreadPoolExecutor( 1, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()  );

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    protected boolean computing = false;
    protected Runnable postInferenceCallback;
    protected byte[][] yuvBytes = new byte[3][];
    protected int yRowStride;
    private boolean debug = false;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private int[] rgbBytes = null;

    TextView LocationTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams  layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags  = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount  = 0.7f;
        getWindow().setAttributes(layoutParams);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detector_camera);
        contexta = DetectorCameraActivity.this;
        LocationTextView = (TextView) findViewById(R.id.LocationTextView);
        Intent intent = new Intent(this, DetectorService.class);
        intent.putExtra("flag", false);
        startService(intent);

        _textviewRenewrHander.sendEmptyMessage(0);


        threadPool.execute( new Runnable()
        {
            public void run()
            {
                if (hasPermission()) {
                    setFragment();
                } else {
                    requestPermission();
                }


            }

        } );


    }


    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (computing) {
            return;
        }
        computing = true;
        yuvBytes[0] = bytes;
        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosenDetector(new Size(previewSize.width, previewSize.height), 90);
            }
            ImageUtils.convertYUV420SPToARGB8888(bytes, rgbBytes, previewWidth, previewHeight, false);
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }
        postInferenceCallback = new Runnable() {
            @Override
            public void run() {
                camera.addCallbackBuffer(bytes);
            }
        };
        processImageRGBbytes(rgbBytes);
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;
        //We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        rgbBytes = new int[previewWidth * previewHeight];
        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (computing) {
                image.close();
                return;
            }
            computing = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();
            ImageUtils.convertYUV420ToARGB8888(
                    yuvBytes[0],
                    yuvBytes[1],
                    yuvBytes[2],
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes);
            image.close();

        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        processImageRGBbytes(rgbBytes);
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

            if (!isFinishing()) {
                LOGGER.d("Requesting finish");
                finish();

        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
        //사물인식 서비스 종료
        Intent detectorService = new Intent(this, DetectorService.class);
        stopService(detectorService);
    }

    @Override
    public synchronized void onRestart() {
        LOGGER.d("onRestart " + this);
        super.onRestart();
        //사물인식 서비스 종료
        _textviewRenewrHander.removeMessages(0);
        Intent detectorService = new Intent(this, DetectorService.class);
        detectorService.putExtra("flag", false);
        stopService(detectorService);
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();

        Intent intent = new Intent(this, DetectorService.class);
        intent.putExtra("flag", true);
        startService(intent);
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    setFragment();
                } else {
                    requestPermission();
                }
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    boolean isHardwareLevelSupported(CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                useCamera2API = isHardwareLevelSupported(characteristics,
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();

        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    onPreviewSizeChosenDetector(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            fragment = new LegacyCameraConnectionFragment(this, getLayoutId());
        }

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commitAllowingStateLoss();
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void requestRender() {
        final OverlayView overlay = findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
        final OverlayView overlay = findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
    }



    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            debug = !debug;
            requestRender();
            onSetDebug(debug);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    /**
     *
     *
     * detector
     */


    private static final Logger LOGGER = new Logger();

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/frozen_inference_graph.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list_test.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = true;
    //텍스트 사이즈
    private static final float TEXT_SIZE_DIP = 25;
    private static final int[] COLORS = {
            Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE,
            Color.parseColor("#55FF55"), Color.parseColor("#FFA500"), Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"), Color.parseColor("#FFFFAA"), Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"), Color.parseColor("#0D0068")
    };
    private final Paint boxPaint = new Paint();
    private Integer sensorOrientation;
    private Classifier detector;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private Bitmap cropCopyBitmap;
    private BorderedText borderedText;
    private long lastProcessingTimeMs;
    private OverlayView detectionOverlay;
    private List<DetecteList.Recognition> mappedRecognitions =
            new LinkedList<>();

    public void onPreviewSizeChosenDetector(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        //텍스트 사이즈
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            LOGGER.e("Exception initializing classifier!", e);
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        final Display display = getWindowManager().getDefaultDisplay();
        final int screenOrientation = display.getRotation();

        LOGGER.i("Sensor orientation: %d, Screen orientation: %d", rotation, screenOrientation);

        sensorOrientation = rotation + screenOrientation;

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderDebug(canvas);
                    }
                });

        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Style.STROKE);
        boxPaint.setStrokeWidth(8.0f);
        detectionOverlay = findViewById(R.id.detection_overlay);
        detectionOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderOverlay(canvas);
                    }
                });
    }

    protected void processImageRGBbytes(int[] rgbBytes) {
        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }
        GlobalValue globalValue = (GlobalValue) getApplication();
        final ArrayList<FilterList> FilterList = globalValue.getGlobalValueLabeldList();
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {

                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        LOGGER.i("Detect: %s", results);
                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        mappedRecognitions.clear();

                        for (final Classifier.Recognition result : results) {
                            DetecteList.Recognition RESULT;
                            for (final FilterList Filter : FilterList){
                                if (result.getTitle().equals(Filter.get_label_value()))
                                {
                                    String Title = result.getTitle();
                                    String Id = result.getId();
                                    Float Confidence = result.getConfidence();
                                    RectF Location = result.getLocation();
                                    Boolean OptionFlag = Filter.get_drop_on_flag();

                                    RESULT = new DetecteList.Recognition(Id, Title, Confidence, Location, OptionFlag);

                                    final RectF location = RESULT.getLocation();
                                    if (location != null && RESULT.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                                        canvas.drawRect(location, paint);
                                        cropToFrameTransform.mapRect(location);
                                        RESULT.setLocation(location);

                                        mappedRecognitions.add(RESULT);

                                    }

                                }
                                //Log.d("TAG", Filter.get_label_value());
                            }

                        }

                        requestRender();
                        detectionOverlay.postInvalidate();
                        computing = false;
                        if (postInferenceCallback != null) {
                            postInferenceCallback.run();
                        }
                    }
                });
    }

    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_detection;
    }

    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    public void onSetDebug(final boolean debug) {
        detector.enableStatLogging(debug);
    }

    private void renderOverlay(final Canvas canvas) {
        final float multiplier =
                Math.min(canvas.getWidth() / (float) previewHeight, canvas.getHeight() / (float) previewWidth);
        Matrix frameToCanvasMatrix =
                ImageUtils.getTransformationMatrix(
                        previewWidth,
                        previewHeight,
                        (int) (multiplier * previewHeight),
                        (int) (multiplier * previewWidth),
                        sensorOrientation,
                        false);

        for (final DetecteList.Recognition recognition : mappedRecognitions) {
            final RectF location = new RectF(recognition.getLocation());

            frameToCanvasMatrix.mapRect(location);
            //색상
            boxPaint.setColor(recognition.getColor());

            final float cornerSize = Math.min(location.width(), location.height()) / 8.0f;

            canvas.drawRoundRect(location, cornerSize, cornerSize, boxPaint);

            //이름 부분
            final String labelString =
                    !TextUtils.isEmpty(recognition.getTitle())
                            ? String.format("%s %s", recognition.getTitle(), recognition.getOption_flag_string())
                            : String.format("%.2f", recognition.getConfidence());
            borderedText.drawText(canvas, location.left + cornerSize, location.bottom, labelString);
        }
    }

    private void renderDebug(final Canvas canvas) {
        if (!isDebug()) {
            return;
        }
        final Bitmap copy = cropCopyBitmap;
        if (copy == null) {
            return;
        }

        final int backgroundColor = Color.argb(100, 0, 0, 0);
        canvas.drawColor(backgroundColor);

        final Matrix matrix = new Matrix();
        final float scaleFactor = 2;
        matrix.postScale(scaleFactor, scaleFactor);
        matrix.postTranslate(
                canvas.getWidth() - copy.getWidth() * scaleFactor,
                canvas.getHeight() - copy.getHeight() * scaleFactor);
        canvas.drawBitmap(copy, matrix, new Paint());

        final Vector<String> lines = new Vector<>();
        if (detector != null) {
            final String statString = detector.getStatString();
            final String[] statLines = statString.split("\n");
            Collections.addAll(lines, statLines);
        }
        lines.add("");

        lines.add("Frame: " + previewWidth + "x" + previewHeight);
        lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
        lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
        lines.add("Rotation: " + sensorOrientation);
        lines.add("Inference time: " + lastProcessingTimeMs + "ms");

        borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
    }


    Handler _textviewRenewrHander = new Handler() {
        public void handleMessage(Message msg) {
            Log.d("TAG", "_filterRenewHandler");

            //필터 갱신
            GlobalValue globalValue = (GlobalValue) getApplication();
            ArrayList<FilterList> guardListLabel = globalValue.getGlobalValueLabeldList();
            ArrayList<String>location = new ArrayList<>();
            ArrayList<String>label = new ArrayList<>();
            location.add("위치 :");
            //label.add("");
            for (FilterList guarLabel: guardListLabel){
                label.add(guarLabel.get_label_value());
                if (location.indexOf(guarLabel.get_location())<0){
                    location.add(guarLabel.get_location());
                }
            }

            if (location.isEmpty()){
                LocationTextView.setText("");
            }
            else{
                LocationTextView.setText(location.toString()+"\n"+label.toString());
            }
            Log.d("TAG", guardListLabel.toString());
            _textviewRenewrHander.sendEmptyMessageDelayed(0, 5000);
        }
    };
}
