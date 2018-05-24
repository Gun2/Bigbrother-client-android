package dju.teambabo.proj.bigbrother_client_android_102;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dju.teambabo.proj.bigbrother_client_android_102.env.Logger;

public class RecognizeImageService extends Service {
    private static final Logger LOGGER = new Logger();

    private static final int TF_OD_API_INPUT_SIZE = 600;
    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();


    private void initTensorFlowAndLoadModel() {
        //불안한데 일단 오류 없음
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowObjectDetectionAPIModel.create(
                            getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }


    public RecognizeImageService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {

        initTensorFlowAndLoadModel();


    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId)
    {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri uri = intent.getParcelableExtra("uri");
                    String mCurrentPhotoPath = intent.getStringExtra("mCurrentPhotoPath");

                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    //키값 생성
                    double Random = Math.random();
                    //Log.d(TAG, ToSha256(String.valueOf(Random)));

                    String RandomKey = ToSha256(String.valueOf(Random));


                    StreamImageFileEncode(mCurrentPhotoPath, RandomKey);
                    bitmap = Bitmap.createScaledBitmap(bitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, false);
                    final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                    LOGGER.i("Detect: %s", results);


                }catch (final Exception e) {
                    throw new RuntimeException("Error  TensorFlow!", e);
                }
            }
        });



        return Service.START_REDELIVER_INTENT;
    }



    @Override
    public void onDestroy() {
        Log.d("TAG","onPause " + this);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
        stopSelf();
        super.onDestroy();
    }

    /***
     *
     * 사진 사이즈 다운 uri용
     */
    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }


    /**
     * 스트림암호화
     */

    public void StreamImageFileEncode(String path, String key) throws IOException {

        File file1 = new File(path);
        File file2 = new File(path+getString(R.string.locked_file_name));

        FileInputStream fis = new FileInputStream(file1);
        FileOutputStream fos = new FileOutputStream(file2);



        int input=0;


        byte[] data = new byte[1024];
        byte[] encode = key.getBytes();

        int flag = 0;
        //i 는 1~sha256.length 암호 강도
        while((input=fis.read(data))!=-1){
            if (flag>1) {
                for (int i = 0; i < encode.length; i++) {
                    data[i] = (byte) (data[i] ^ encode[i]);
                }
            }
            flag ++;
            //Log.d(TAG, Arrays.toString(data));
            fos.write(data, 0, input);

        }
        fos.close();
        fis.close();
        File removeFile = new File(path);
        if(removeFile.delete()){
            Log.d("TAG","삭제완");
        }
        else{
            Log.d("TAG","삭제 ㄴㄴ");
        }

    }


    /**
     * sha256 생성기
     */

    private String ToSha256(String base) {
        String SHA = "";

        try {

            MessageDigest sh = MessageDigest.getInstance("SHA-256");

            sh.update(base.getBytes());

            byte byteData[] = sh.digest();

            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < byteData.length; i++) {

                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));

            }

            SHA = sb.toString();


        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();

            SHA = null;

        }

        return SHA;
    }


}
