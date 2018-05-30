package dju.teambabo.proj.bigbrother_client_android_102;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.webkit.CookieManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cz.msebera.android.httpclient.Header;
import dju.teambabo.proj.bigbrother_client_android_102.env.Logger;

public class RecognizeImageService extends Service {
    private static final Logger LOGGER = new Logger();
    /**
     * 브로드캐스트 전송 주소
     * BROADCAST_MESSAGE_POSTLOG 로그 전송용
     *
     */
    private final String BROADCAST_MESSAGE_POSTLOG = "dju.teambabo.proj.bigbrother_client_android_102.PostLog";
    /**
     * 인식 사진 사이즈
     */
    /**
     * Detector
     */
/*
    private static final int TF_OD_API_INPUT_SIZE = 600;
    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
*/
    /**
     * recognize
     */

    private static final int TF_OD_API_INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "Mul";
    private static final String OUTPUT_NAME = "final_result";


    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/rounded_graph.pb";
    private static final String TF_OD_API_LABELS_FILE =
            "file:///android_asset/retrained_labels.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();


    /**
     * 비교값
     */
    private ArrayList<FilterList> filterList = new ArrayList<>();
    /**
     * 송신값
     */
    private ArrayList<FilterList> resultList = new ArrayList<>();
    /**
     *
     */
    private ArrayList<String> labelList = new ArrayList<>();

    /**
     * 드랍 옵션 여부
     */
    private Boolean DropOption = false;
    /**
     * 사진 전송 여부
     */
    private Boolean PicRequest = true;
    /**
     * 사진 넓이
     */
    private int PicWidth;
    /**
     * 사진 높이
     */
    private int PicHeight;
    /*
     * 압축 메인 사이즈
     */
    private int MAIN_TF_OD_API_INPUT_SIZE;
    /**
     * 사용 가능 사이즈
     */
    private int usableRecognizeLevel;

    private void initTensorFlowAndLoadModel() {
        //불안한데 일단 오류 없음
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    /**
                     * detector
                     */
/*
                    classifier = TensorFlowObjectDetectionAPIModel.create(
                            getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
*/

                    /**
                     * recognize
                     */

                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);

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

        Uri uri = intent.getParcelableExtra("uri");
        String mCurrentPhotoPath = intent.getStringExtra("mCurrentPhotoPath");
        //filterList = intent.getSerializableExtra("filterList");
        Bitmap bitmap = null;
        //Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        try{
            Bitmap mBitmap = BitmapUtils.rotateBitmapOrientation(mCurrentPhotoPath);
            bitmap = mBitmap;
        }catch (final Exception e){
            stopSelf();
        }

        //키값 생성
        double Random = Math.random();

        String RandomKey = ToSha256(String.valueOf(Random));

        //파일 암호화
        try {
            StreamImageFileEncode(mCurrentPhotoPath, RandomKey, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        GlobalValue globalValue = (GlobalValue) getApplication();



        if (bitmap.getWidth()!=PicWidth||bitmap.getHeight()!=PicHeight){

            int recognizeLevel = globalValue.getRecognizeLevel();

            int Size = TF_OD_API_INPUT_SIZE*recognizeLevel;

            for (int size=Size; bitmap.getWidth()<=Size || bitmap.getHeight()<=Size; size-=TF_OD_API_INPUT_SIZE){
                --recognizeLevel;
            }

            MAIN_TF_OD_API_INPUT_SIZE = TF_OD_API_INPUT_SIZE*recognizeLevel;
            usableRecognizeLevel = recognizeLevel;
            PicWidth = bitmap.getWidth();
            PicHeight = bitmap.getHeight();
        }

        bitmap = Bitmap.createScaledBitmap(bitmap, MAIN_TF_OD_API_INPUT_SIZE, MAIN_TF_OD_API_INPUT_SIZE, false);


        Bitmap finBitmap = bitmap;
        if (globalValue.getRecognizeState()){
            //서버인식
            requestRecognize(bitmap, usableRecognizeLevel, mCurrentPhotoPath, RandomKey);




        }

        else {
            //내장인식

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bitmap bitmap = finBitmap;


                        ArrayList<String> tmpList = new ArrayList<String>();

                        if (usableRecognizeLevel != 1)
                        {
                            List<Classifier.Recognition> results;
                            for (int num = 0; num < usableRecognizeLevel; num++) {
                                for (int col = 0; col < usableRecognizeLevel; col++) {
                                    Bitmap subBitmap = Bitmap.createBitmap(bitmap
                                            , TF_OD_API_INPUT_SIZE * num
                                            , TF_OD_API_INPUT_SIZE * col
                                            , TF_OD_API_INPUT_SIZE
                                            , TF_OD_API_INPUT_SIZE);

                                    results = classifier.recognizeImage(subBitmap);

                                    for (Classifier.Recognition result : results) {
                                        if (tmpList.indexOf(result.getTitle()) < 0) {
                                            tmpList.add(result.getTitle());
                                        }
                                    }
                                }
                            }
                        }


                        bitmap = Bitmap.createScaledBitmap(bitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, false);
                        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                        for (Classifier.Recognition result: results){
                            if(tmpList.indexOf(result.getTitle())<0){
                                tmpList.add(result.getTitle());
                            }
                        }



                        LOGGER.i("Detect: %s", tmpList);
                        SearchResult(tmpList);
                        //superviseFilter(bitmap,mCurrentPhotoPath,RandomKey);




                    }catch (final Exception e) {
                        throw new RuntimeException("Error  TensorFlow!", e);
                    }

                }
            });

        }




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
     *
     * @param option
     * 0번 인코딩
     * 그 외 번호 디코딩
     */

    public void StreamImageFileEncode(String path, String key, int option) throws IOException {
        File file1;
        File file2;
        if(option==0) {
            file1 = new File(path);
            file2 = new File(path + getString(R.string.locked_file_name));
        }
        else{
            file1 = new File(path + getString(R.string.locked_file_name));
            file2 = new File(path);
        }

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


    /**
     * 단어 비교
     * @param results
     */
    private void SearchResult(ArrayList<String> results){
        GlobalValue globalValue = (GlobalValue) getApplication();
        filterList = new ArrayList<>(globalValue.getGlobalValueLabeldList());
        Log.d("TAG", filterList.toString());


        for (FilterList filter : filterList){
            if (results.indexOf(filter.get_label_value()) >= 0){
                resultList.add(filter);
                if (filter.get_drop_on_flag()==true){
                    DropOption = true;
                }
                if (filter.get_picRequest()==false){
                    PicRequest = false;
                }
            }


        }
        Log.d("TAG","resultList.toString"+ resultList.toString());


    }


    /**
     * 규칙 실행 //암호 풀기 및 파일 삭제 자가인식
     */
    private void superviseFilter(Bitmap bitmap, String path, String key) throws IOException {


        if(resultList.size()>0){
            String SearchLabel="";
            for (FilterList result : resultList){
                SearchLabel += " "+result.get_label_value();
            }


            //파일 삭제
            if (DropOption){
                File removeFile = new File(path + getString(R.string.locked_file_name));
                if(removeFile.delete()){
                    Log.d("TAG","삭제완");
                }
                else{
                    Log.d("TAG","삭제 ㄴㄴ");
                }
                sendToastMessage("촬영한 사진에"+SearchLabel+"이(가) 발견되었습니다.\n유출 사고를 막기 위해 사진을 삭제합니다.");
            }
            //복구
            else{
                StreamImageFileEncode(path, key, 1);
                AddPic(path);

                sendToastMessage("촬영한 사진에"+SearchLabel+"이(가) 발견되었습니다.");

            }

            //사진 전송
            if(PicRequest){
                postFilterLog(bitmap);
            }
            else {
                postFilterLog(null);
            }

        }
        else{
            StreamImageFileEncode(path, key, 1);
            AddPic(path);
            sendToastMessage("위해 요소가 없어 사진을 저장하였습니다.");

        }

    }






    /**
     * 규칙 실행 //암호 풀기 및 파일 삭제 서버인식
     */
    private void superviseFilterServerRecognizeVer(Bitmap bitmap, String path, String key) throws IOException {


        if(!labelList.get(0).equals("[]")){
            String SearchLabel=labelList.get(0);



            //파일 삭제
            if (DropOption){
                File removeFile = new File(path + getString(R.string.locked_file_name));
                if(removeFile.delete()){
                    Log.d("TAG","삭제완");
                }
                else{
                    Log.d("TAG","삭제 ㄴㄴ");
                }
                sendToastMessage("촬영한 사진에"+SearchLabel+"이(가) 발견되었습니다.\n유출 사고를 막기 위해 사진을 삭제합니다.");
            }
            //복구
            else{
                StreamImageFileEncode(path, key, 1);
                AddPic(path);

                sendToastMessage("촬영한 사진에"+SearchLabel+"이(가) 발견되었습니다.");

            }

        }
        else{
            StreamImageFileEncode(path, key, 1);
            AddPic(path);
            sendToastMessage("위해 요소가 없어 사진을 저장하였습니다.");

        }

    }
    /**
     *
     * 알림 로그 전달
     */

    private void postFilterLog(Bitmap bitmap) {


        SyncHttpClient filteringListPost = new SyncHttpClient();

        filteringListPost.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));
        filteringListPost.getThreadPool();
        RequestParams params = new RequestParams();
        ArrayList<Integer> pk = new ArrayList<>();
        ArrayList<String> label_value = new ArrayList<>();


        for (FilterList resultlist : resultList){
            pk.add(resultlist.get_pk());
            label_value.add(resultlist.get_label_value());
        }

        try {

            params.put("pk", pk);
            params.put("drop_on_flag", DropOption);
            params.put("label_value", label_value);
            params.put("pictureBase64", BitmapUtils.BitmapToBase64(bitmap));

        } catch (Exception e) {

        }
/*
        if (dropFlag.equals("true")){
            File removeFile = new File(path);
            if(removeFile.delete()){
                Log.d("TAG","삭제완");
            }
            else{
                Log.d("TAG","삭제 ㄴㄴ");
            }
        }

*/
        String postAlertLogURL = getString(R.string.server_url) + getString(R.string.post_alert_log);
        filteringListPost.post(this, postAlertLogURL, params, new JsonHttpResponseHandler(){
            // 성공
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                //Log.d("TAG","postFilter.success");

                //성공
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                //실패
                //Log.d("TAG","postFilter.err");
            }
        });
    }




    /**
     *
     * 사물 인식 요청
     */

    private void requestRecognize(Bitmap bitmap, int size, String path, String randomKey) {


        AsyncHttpClient requestRecognizePost = new AsyncHttpClient();

        requestRecognizePost.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));
        requestRecognizePost.getThreadPool();
        RequestParams params = new RequestParams();

        GlobalValue globalValue = (GlobalValue) getApplication();
        filterList = new ArrayList<>(globalValue.getGlobalValueLabeldList());
        ArrayList<Boolean> drop_on_flag = new ArrayList<>();
        ArrayList<String> location = new ArrayList<>();
        ArrayList<String> label_value = new ArrayList<>();


        for (FilterList filter : filterList ){
            drop_on_flag.add(filter.get_drop_on_flag());
            location.add(filter.get_location());
            label_value.add(filter.get_label_value());

        }
        try {

            params.put("base64Image", BitmapUtils.BitmapToBase64(bitmap));
            params.put("drop_on_flag", drop_on_flag);
            params.put("location", location);
            params.put("label_value", label_value);
            params.put("size", size);



        } catch (Exception e) {

        }

        String postAlertLogURL = getString(R.string.server_url) + getString(R.string.recognize_request);
        requestRecognizePost.post(this, postAlertLogURL, params, new JsonHttpResponseHandler(){
            // 성공
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    labelList = new ArrayList<String>();
                    JSONArray Single = response;
                    JSONObject SingleOrder = Single.optJSONObject(0);
                    DropOption = SingleOrder.getBoolean("DropFlag");
                    labelList.add(SingleOrder.getString("label_value"));


                    superviseFilterServerRecognizeVer(bitmap, path, randomKey);


                } catch (Exception e) {
                    //응답은 성공하였으나 값이 올바르지 않음
                    labelList = new ArrayList<String>();
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);

            }
        });



    }


    /**
     * 사진 동기화
     * @param path
     * @throws IOException
     */
    private void AddPic(String path){

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }



    private void sendToastMessage(String message){
        Intent intent = new Intent(BROADCAST_MESSAGE_POSTLOG);
        intent.putExtra("message",message);
        sendBroadcast(intent);

    }
}
