package dju.teambabo.proj.bigbrother_client_android_102;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class CameraLuncherActivity extends AppCompatActivity{
    /**
     * 촬영 금지 항목 받아오기
     */

    private ArrayList<ArrayList<String>> guardListText = new ArrayList<>();
    private ArrayList<ArrayList<String>> guardListLabel = new ArrayList<>();

    /**
     * 일치하는 사물 및 텍스트
     */
    private String guardKeywordLabel;

    private String guardKeywordText;

    private Boolean dropFlag=false;//드랍 속성이 있으면 활성화

    /**
     * 사진 촬영 응답
     */
    private static final int REQUEST_TAKE_PHOTO = 2222;

    /***
     * 구글 클라우드 비전 설정값
     */
    private static final String CLOUD_VISION_API_KEY = "AIzaSyDR44-TtVnCXlvois2QuGhPuZsOpHsJrYk\n";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    /**
     * 로그 출력용
     */
    private static final String TAG = CameraLuncherActivity.class.getSimpleName();

    /**
     * 촬영한 사진 Uri값, 경로값
     */
    Uri imageUri;

    String mCurrentPhotoPath;

    /**
     * 브로드캐스트 전송 주소
     * BROADCAST_MESSAGE_POSTLOG 로그 전송용
     *
     */
    private final String BROADCAST_MESSAGE_POSTLOG = "dju.teambabo.proj.bigbrother_client_android_102.PostLog";

    /***
     *
     *
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_luncher);

        //감시할 값 가져오기
        RequestGuardLabel();
        RequestGuardText();

        //사용 현황 로그
        _userConnectionHandler.sendEmptyMessage(0);

        //필터 갱신 핸들러
        _filterRenewHandler.sendEmptyMessage(0);

        //카메라 촬영
        captureCamera();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        processCommand(intent);
        super.onNewIntent(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();

        _userConnectionHandler.removeMessages(0);
        _filterRenewHandler.removeMessages(0);

        File removeFile = new File(mCurrentPhotoPath);
        if(removeFile.delete()){
            Log.d("TAG","삭제완");
        }
        else{
            Log.d("TAG","삭제 ㄴㄴ");
        }
    }



    private void processCommand(Intent intent) {

        if(intent != null){
            String command = intent.getStringExtra("command");
            Toast.makeText(this, "서비스로 부터 전달받은 데이터 :" + command, Toast.LENGTH_SHORT).show();
        }

    }

    /****
     * 사진에서 검색 할 텍스트 문자 가져오기
     */

    private void RequestGuardText() {
        AsyncHttpClient profileLoadClient = new AsyncHttpClient();

        profileLoadClient.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));

        String profileURL = getString(R.string.server_url) + getString(R.string.text_guard_list);


        profileLoadClient.get(profileURL, new JsonHttpResponseHandler() {
            //불러오기 성공
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                try {

                    int count;

                    guardListText.clear();
/*
                    for (count = 0; count < response.length(); count++) {
                        JSONArray ja = response;
                        JSONObject order = ja.optJSONObject(count);
                        String text_value = order.getString("text_value");
                        String drop_on_flag = order.getString("drop_on_flag");
                        guardTempText = new TwinLists(text_value, drop_on_flag);
                        guardListText.add(guardTempText);
                    }
*/
                    //String test001 = guardListText.get(0).get_arrData();


                } catch (Exception e) {
                    //응답은 성공하였으나 값이 올바르지 않음
                    e.printStackTrace();
                }
            }

            //불러오기 실패
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(CameraLuncherActivity.this, "감시 목록을 불러오지 못하였습니다. 관리자에게 문의해주세요.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /****
     * 사진에서 검색 할 라벨 목록 가져오기
     */

    private void RequestGuardLabel() {
        AsyncHttpClient profileLoadClient = new AsyncHttpClient();

        profileLoadClient.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));

        String profileURL = getString(R.string.server_url) + getString(R.string.label_guard_list);


        profileLoadClient.get(profileURL, new JsonHttpResponseHandler() {
            //불러오기 성공
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                try {

                    int count;
                    guardListLabel.clear();
                    /*
                    for (count = 0; count < response.length(); count++) {
                        JSONArray ja = response;
                        JSONObject order = ja.optJSONObject(count);
                        String text_value = order.getString("label_value");
                        String drop_on_flag = order.getString("drop_on_flag");
                        guardTempLabel = new TwinLists(text_value, drop_on_flag);
                        guardListLabel.add(guardTempLabel);
                    }
                    */

                } catch (Exception e) {
                    //응답은 성공하였으나 값이 올바르지 않음
                    e.printStackTrace();
                }
            }

            //불러오기 실패
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(CameraLuncherActivity.this, "감시 목록을 불러오지 못하였습니다. 관리자에게 문의해주세요.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    /**
     * 사물인식 반응 결과
     */
    private String convertResponseToStringLabel(BatchAnnotateImagesResponse responseLabel,BatchAnnotateImagesResponse responseText, String path, String key) {
        String messageLabel = "";
        String messageText = "";//인식 메시지 담을 곳

        List<EntityAnnotation> labels = responseLabel.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {//사물인식 수행

            for (EntityAnnotation label : labels) {

                messageLabel += String.format(label.getDescription());
                messageLabel += "\n";
            }
            //사물인식 대치되는 물체 검색

        }
        List<EntityAnnotation> texts = responseText.getResponses().get(0).getTextAnnotations();
        if (texts != null) {
            messageText += texts.get(0).getDescription();

        }
        Log.d("TAG", messageText);
        Log.d("TAG", messageLabel);

        SearchGuardListLabel(messageLabel ,messageText, path, key);

        return messageLabel+messageText;
    }

    /***
     *
     * 구글 클라우드 비전에 값 전송
     */
    private void callCloudVisionLabel(final Bitmap bitmap, final String path, final  String key) throws IOException {
        // Switch text to loading
        //mImageDetails.setText(R.string.loading_message);

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequestLabel =
                            new BatchAnnotateImagesRequest();
                    BatchAnnotateImagesRequest batchAnnotateImagesRequestText =
                            new BatchAnnotateImagesRequest();

                    final Image base64EncodedImage = new Image();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                    final byte[] imageBytes = byteArrayOutputStream.toByteArray();
                    base64EncodedImage.encodeContent(imageBytes);


                    batchAnnotateImagesRequestLabel.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequestLabel = new AnnotateImageRequest();

                        annotateImageRequestLabel.setImage(base64EncodedImage);

                        annotateImageRequestLabel.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(20);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequestLabel);
                    }});

                    batchAnnotateImagesRequestText.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequestText = new AnnotateImageRequest();

                        annotateImageRequestText.setImage(base64EncodedImage);

                        annotateImageRequestText.setFeatures(new ArrayList<Feature>() {{
                            Feature textDetection = new Feature();
                            textDetection.setType("TEXT_DETECTION");
                            textDetection.setMaxResults(10);
                            add(textDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequestText);
                    }});




                    Vision.Images.Annotate annotateRequestLabel =
                            vision.images().annotate(batchAnnotateImagesRequestLabel);

                    Vision.Images.Annotate annotateRequestText =
                            vision.images().annotate(batchAnnotateImagesRequestText);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequestLabel.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse responseLabel = annotateRequestLabel.execute();
                    BatchAnnotateImagesResponse responseText = annotateRequestText.execute();
                    return convertResponseToStringLabel(responseLabel,responseText, path, key);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {
                //mImageDetails.setText(result);
            }
        }.execute();
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


    /***
     *
     * 사진 가져오기 및 변형
     */
    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {

                //키값 생성
                double Random = Math.random();
               //Log.d(TAG, ToSha256(String.valueOf(Random)));

                String RandomKey = ToSha256(String.valueOf(Random));

                //Log.d(TAG,"getBitmapOfWidth"+BitmapUtils.getBitmapOfWidth(mCurrentPhotoPath));
                Bitmap bitmap = BitmapUtils.rotateBitmapOrientation(mCurrentPhotoPath);

                //이미지 해상도 구하기
                int ImageSize = ImageSizeAutoMinimize.getBitmapOfWidth(mCurrentPhotoPath)*ImageSizeAutoMinimize.getBitmapOfHeight(mCurrentPhotoPath);
                //이미지 파일 암호화
                StreamImageFileEncode(mCurrentPhotoPath, RandomKey);


                bitmap = ImageSizeAutoMinimize.AutominimizeBitmap(bitmap, ImageSize);


                callCloudVisionLabel(bitmap, mCurrentPhotoPath, RandomKey);
                //callCloudVisionText(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                //Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            //Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * 사진촬영 결과 반영값
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Log.i("REQUEST_TAKE_PHOTO", "OK");
                        uploadImage(imageUri);



                        //galleryAddPic(); //사진저장


                    } catch (Exception e) {
                        Log.e("REQUEST_TAKE_PHOTO", e.toString());
                    }

                    RequestGuardLabel();
                    RequestGuardText();
                    captureCamera();//카메라 다시 시작
                } else {
                    Toast.makeText(CameraLuncherActivity.this, "사진찍기를 취소하였습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

        }
    }


    /****
     *
     * 사진으로 찍은 이미지파일 저장
     */
    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "bigbrother");

        if (!storageDir.exists()) {
            //Log.i("mCurrentPhotoPath1", storageDir.toString());
            storageDir.mkdirs();
        }

        imageFile = new File(storageDir, imageFileName);
        mCurrentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;


    }

    /**
     * 사진엑티비티 호출 가능여부 검사
     */
    private void captureCamera() {

        String state = Environment.getExternalStorageState();
        // 외장 메모리 검사
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e("captureCamera Error", ex.toString());
                }
                if (photoFile != null) {
                    // getUriForFile의 두 번째 인자는 Manifest provier의 authorites와 일치해야 함

                    Uri providerURI = FileProvider.getUriForFile(this, "dju.teambabo.proj.bigbrother_client_android_102.fileprovider", photoFile);
                    imageUri = providerURI;

                    // 인텐트에 전달할 때는 FileProvier의 Return값인 content://로만!!, providerURI의 값에 카메라 데이터를 넣어 보냄
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI);

                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        } else {
            Toast.makeText(this, "저장공간이 접근 불가능한 기기입니다", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    /*
     *단어비교
     */

    private void SearchGuardListLabel(String responseMessageLabel, String responseMessageText, String path, String key){
        guardKeywordLabel="사물(형체) : ";// 알림창 앞에 넣을 문자
        guardKeywordText="\n텍스트 : ";
        dropFlag=false;
        //사물 확인
        for (int count = 0; count<guardListLabel.size(); count++){
            /*물체에 일치 하는 단어 있으면*/


            if(responseMessageLabel.contains(guardListLabel.get(count).get(0))){
                guardKeywordLabel += ("["+guardListLabel.get(count).get(0)+"] ");
                if(guardListLabel.get(count).get(1)=="true"){
                    dropFlag=true;
                    // 전체 검색 하려면 주석 break;
                }
                Log.d("TAG","일치");
                Log.d("TAG","키워드"+guardKeywordLabel);
            }
            else Log.d("TAG","불일치");
        }
        if (responseMessageText!="") {//텍스트는 메시지가 있어야만 검색
            //텍스트 비교
            for (int count = 0; count < guardListText.size(); count++) {
            /*일치 하는 단어 있으면*/

                if (responseMessageText.contains(guardListText.get(count).get(0))) {
                    guardKeywordText += ("[" + guardListText.get(count).get(0) + "] ");
                    if (guardListText.get(count).get(1) == "true") {
                        dropFlag = true;

                        //전체검색 하려면 주석처리 break;
                    }
                    Log.d("TAG","일치"+guardKeywordText);
                }
            }
        }




        if (guardKeywordLabel.length()==9)guardKeywordLabel="";
        if (guardKeywordText.length()==7)guardKeywordText="";

        callReceivePostLog(guardKeywordLabel, guardKeywordText, dropFlag.toString(), path, key);


    }

    /**
     * 알림 로그 전달을 위해 리시버에게 요청
     */
    private void callReceivePostLog(String guardKeywordLabel,String guardKeywordText,String dropFlag, String path, String key){
        Intent intent = new Intent(BROADCAST_MESSAGE_POSTLOG);
        intent.putExtra("guardKeywordLabel",guardKeywordLabel);
        intent.putExtra("guardKeywordText",guardKeywordText);
        intent.putExtra("dropFlag",dropFlag);
        intent.putExtra("path",path);
        intent.putExtra("key", key);
        sendBroadcast(intent);

    }


    /**
     * uri to path
     */
    public String getPathFromUri(Uri uri){

        Cursor cursor = getContentResolver().query(uri, null, null, null, null );

        cursor.moveToNext();

        String path = cursor.getString( cursor.getColumnIndex( "_data" ) );

        cursor.close();



        return path;
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


    /**
     * 사용자 연결 현황 체크
     */
    public void connectionRequestHttpPost(){

        AsyncHttpClient connectionRequest = new AsyncHttpClient();

        connectionRequest.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));





        String postAlertLogURL = getString(R.string.server_url) + getString(R.string.connection_Request);
        connectionRequest.get(this, postAlertLogURL, new JsonHttpResponseHandler(){
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

    Handler _userConnectionHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d("TAG", "_userConnectionHandler");


            //사용자 연결상태
            connectionRequestHttpPost();


            _userConnectionHandler.sendEmptyMessageDelayed(0, 5000);
        }
    };


    Handler _filterRenewHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d("TAG", "_filterRenewHandler");

            //필터 갱신
            GlobalValue globalValue = (GlobalValue) getApplication();
            guardListText = globalValue.getGlobalValueLabeldList();
            _filterRenewHandler.sendEmptyMessageDelayed(0, 10000);
        }
    };
}




