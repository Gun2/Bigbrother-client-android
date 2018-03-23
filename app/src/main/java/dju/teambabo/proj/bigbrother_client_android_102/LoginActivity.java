package dju.teambabo.proj.bigbrother_client_android_102;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * A login screen that offers login via email/password.
 */


public class LoginActivity extends AppCompatActivity {
    /**
     *
     *  브로드캐스트 설정
     */
    private final String BROADCAST_MESSAGE_POSTLOG = "dju.teambabo.proj.bigbrother_client_android_102.PostLog";

    private BroadcastReceiver mReceiver = null;

    private int ACTIVITYINFO;

    Button _loginButton;

    Button _listButton;

    EditText _usernameEditText;

    EditText _passwordEditText;

    private static final int MY_PERMISSION_CAMERA = 1111;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);
        initViewElements();

        //동적 브로드캐스트 실행
        createReceiverPostLog();

        //권한 체크
        checkPermission();

    }


    private void initViewElements() {
        _loginButton = (Button)findViewById(R.id.login_button);
        _loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ACTIVITYINFO=0;
                loginStart();
            }
        });

        _listButton = (Button)findViewById(R.id.list_button);
        _listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ACTIVITYINFO=1;
                loginStart();
            }
        });

        _usernameEditText = (EditText)findViewById(R.id.username_editText);
        _passwordEditText = (EditText)findViewById(R.id.password_editText);

    }


    /**
     *  서버 알림 전송 값
     */

    private String GUARDKEYWORD;
    private  String DROPFLAG;

    /***
     * 로그인 시작
     */
    private void loginStart() {
        if ( checkblank() ){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("로그인 실패");
            alertDialogBuilder.setMessage("아이디 또는 비밀번호가 누락됐습니다.");
            alertDialogBuilder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //확인을 눌렀을 때
                }
            });
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.show();
            return;
        }


        AttemptLogin();
    }

    /**
     * 로그인 요청
     */
    private void AttemptLogin() {
        //alias this
        final LoginActivity self = this;

        AsyncHttpClient loginClient = new AsyncHttpClient();

        JSONObject jsonParams = new JSONObject();
        StringEntity entity = null;
        try {
            jsonParams.put("username", _usernameEditText.getText().toString());
            jsonParams.put("password", _passwordEditText.getText().toString());
            entity = new StringEntity(jsonParams.toString());
        } catch (Exception e) {

        }

        String loginURL = getString(R.string.server_url) + getString(R.string.user_auth_url);
        loginClient.post(this, loginURL, entity, "application/json", new JsonHttpResponseHandler(){
            //로그인 성공
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                try {
                    String rawToken = response.getString("token");
                    CookieManager.getInstance().setCookie(getString(R.string.token_key), "JWT" + " " + rawToken);
                    loginSucessHandle();
                } catch (Exception e) {
                    //로그인은 성공하였으나 토큰값을 못 받아올 때.
                    e.printStackTrace();
                }
            }
            //로그인 실패
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(self);
                alertDialogBuilder.setTitle("로그인 실패");
                alertDialogBuilder.setMessage("아이디 또는 비밀번호를 확인하세요.");
                alertDialogBuilder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //확인을 눌렀을 때
                    }
                });
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder.show();
            }
        });
    }


    /**
     * 로그인 성공 후
     */
    private void loginSucessHandle() {
        if (ACTIVITYINFO==0){
            Intent cameraIntent = new Intent(this, CameraLuncherActivity.class);
            startActivity(cameraIntent);
        }
        else{
            Intent alertIntent = new Intent(this, AlertListViewActivity.class);
            startActivity(alertIntent);
        }

    }

    /**
     *
     * 아이디 비밀번호 공백 확인
     */
    private boolean checkblank() {
        if (_usernameEditText.getText().toString().isEmpty()) return true;
        if (_passwordEditText.getText().toString().isEmpty()) return true;

        return false;
    }
    /**
     *
     * 권한 체크
     */

    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 처음 호출시엔 if()안의 부분은 false로 리턴 됨 -> else{..}의 요청으로 넘어감
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                    (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))) {
                new AlertDialog.Builder(this)
                        .setTitle("알림")
                        .setMessage("저장소 권한이 거부되었습니다. 사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                        .setNeutralButton("설정", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, MY_PERMISSION_CAMERA);
                //captureCamera();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_CAMERA:
                for (int i = 0; i < grantResults.length; i++) {
                    // grantResults[] : 허용된 권한은 0, 거부한 권한은 -1
                    if (grantResults[i] < 0) {
                        Toast.makeText(LoginActivity.this, "해당 권한을 활성화 하셔야 합니다.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                }
                // 허용

                //카메라 작동
                //captureCamera();

                break;
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        detroyReceiver();

    }


    /**
     *  브로드 캐스트
     **/
    private void createReceiverPostLog(){

        if(mReceiver != null) return;

        final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction(BROADCAST_MESSAGE_POSTLOG);

        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(BROADCAST_MESSAGE_POSTLOG)){
                    //Uri passUri = intent.getParcelableExtra("uri");

                    //정상사진 촬영 시 사진 복호화 동기화 하고 알람 x
                    if (intent.getStringExtra("guardKeywordLabel").equals("")&&intent.getStringExtra("guardKeywordText").equals("")){
                        try {
                            galleryAddPic(intent.getStringExtra("path"), intent.getStringExtra("key"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        GUARDKEYWORD=intent.getStringExtra("guardKeywordLabel")+intent.getStringExtra("guardKeywordText");
                        DROPFLAG=intent.getStringExtra("dropFlag");
                        //drop일때 사진 삭제o 동기화  x 알람o
                        if (DROPFLAG.equals("true")) {
                            Toast.makeText(context, "[위험] 보안 규칙에 심각하게 위배되는 다음과 같은 형태를 촬영하셨습니다.\n"+GUARDKEYWORD+"\n사진을 삭제합니다.", Toast.LENGTH_LONG).show();
                            try {
                                StreamImageFileDecode(intent.getStringExtra("path"), intent.getStringExtra("key"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        //alert일때 사진 복호화 동기화o 알람o
                        else{
                            Toast.makeText(context, "[경고] 보안 규칙에 위배되는 다음과 같은 형태를 촬영하셨습니다.\n"+intent.getStringExtra("guardKeywordLabel")+intent.getStringExtra("guardKeywordText"), Toast.LENGTH_LONG).show();
                            try {
                                galleryAddPic(intent.getStringExtra("path"), intent.getStringExtra("key"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        postFilterLog(intent.getStringExtra("guardKeywordLabel")+intent.getStringExtra("guardKeywordText"),intent.getStringExtra("dropFlag"), intent.getStringExtra("path"));

                    }
                }

            }
        };

        this.registerReceiver(this.mReceiver, theFilter);

    }

    /** 동적으로(코드상으로) 브로드 캐스트를 종료한다. **/
    private void detroyReceiver() {
        if(mReceiver != null){
            this.unregisterReceiver(mReceiver);
            mReceiver = null;
        }

    }


    private void postFilterLog(String guardKeyword, String dropFlag, String path) {

        Bitmap bitmap = BitmapUtils.rotateBitmapOrientation(path);

        bitmap = BitmapUtils.minimizeBitmap(bitmap);

        //bitmap = BitmapUtils.minimizeBitmap(bitmap);

        AsyncHttpClient filteringListPost = new AsyncHttpClient();

        filteringListPost.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));

        RequestParams params = new RequestParams();

        try {
            params.put("keyword", guardKeyword);
            params.put("drop_on_flag", dropFlag);
            params.put("pictureBase64", BitmapUtils.BitmapToBase64(bitmap));

        } catch (Exception e) {

        }

        if (dropFlag.equals("true")){
            File removeFile = new File(path);
            if(removeFile.delete()){
                Log.d("TAG","삭제완");
            }
            else{
                Log.d("TAG","삭제 ㄴㄴ");
            }
        }


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
     * 사진 동기화 및 복호화 (StreamImageFileDecode(path, key))
     * @param path
     */
    private void galleryAddPic(String path, String key) throws IOException {
        StreamImageFileDecode(path, key);
        Log.i("galleryAddPic", "Call");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        // 해당 경로에 있는 파일을 객체화(새로 파일을 만든다는 것으로 이해하면 안 됨)
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    /**
     * 사진 암호 해제용
     * @param path
     * @param key
     * @throws IOException
     */
    public void StreamImageFileDecode(String path, String key) throws IOException {

        File file1 = new File(path+"(IS_LOOK)");
        File file2 = new File(path);

        FileInputStream fis = new FileInputStream(file1);
        FileOutputStream fos = new FileOutputStream(file2);

        int input=0;

        byte[] data = new byte[1024];
        byte[] encode = key.getBytes();
        //fis.read(data);
        //i 는 1~sha256.length 암호 강도
        while((input=fis.read(data))!=-1){
            for (int i = 0 ; i<5; i++){
                data[i] = (byte)(data[i]^encode[i]);
            }
            //Log.d(TAG, Arrays.toString(data));
            fos.write(data, 0, input);



        }
        fos.close();
        fis.close();
        IsLookImageDelete(path);
    }

    public void IsLookImageDelete(String path){

        File removeFile = new File(path+"(IS_LOOK)");
        if(removeFile.delete()){
            Log.d("TAG","삭제완료");
        }
        else{
            Log.d("TAG","삭제실패");
        }

    }
}
