package dju.teambabo.proj.bigbrother_client_android_102;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.extras.Base64;


public class ImageViewDialogActivity extends Activity {

    /**
     * 이벤트 아이디 값 저장
     */

    private String POSTID;

    /**
     * 감지 사진
     */
    ImageView imageView;
    /**
     * 설명 textView
     */
    TextView CauseView;
    /**
     * 받은 이미지, 설명문구
     */

    private String pictureBase64;

    private  String CAUSE;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams  layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags  = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount  = 0.7f;
        getWindow().setAttributes(layoutParams);
        setContentView(R.layout.activity_image_view_dialog);

        TextView Keyword = (TextView)findViewById(R.id.keyword);



        POSTID = getIntent().getStringExtra("id");
        String keyword = getIntent().getStringExtra("keyword").toString();

        Keyword.setText(keyword);

        LoadAlertImage();



    }





    private void LoadAlertImage() {

        AsyncHttpClient loadAlertImage = new AsyncHttpClient();

        loadAlertImage.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));

        RequestParams params = new RequestParams();

        try {
            params.put("id", POSTID);

        } catch (Exception e) {

        }

        String loadAlertImageURL = getString(R.string.server_url) + getString(R.string.load_alert_image);
        loadAlertImage.post(this, loadAlertImageURL, params, new JsonHttpResponseHandler(){
            // 성공
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    int count;
                    for(count = 0; count< response.length(); count++) {
                        JSONArray ja = response;
                        JSONObject order = ja.optJSONObject(count);
                        pictureBase64 = order.getString("pictureBase64");
                        CAUSE = order.getString("cause");


                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                LoadBitmapToImageView(pictureBase64, CAUSE);
            }
                @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.d("TAG","request.err");
            }

        });


    }



    //base64 To img 작업과 내용 받아오기
    private void LoadBitmapToImageView(String img, String text){
        imageView = (ImageView)findViewById(R.id.imageView);
        CauseView = (TextView)findViewById(R.id.causeView);

        byte[] bytePlainOrg = Base64.decode(img, 0);
        ByteArrayInputStream inStream = new ByteArrayInputStream(bytePlainOrg);
        Bitmap bm = BitmapFactory.decodeStream(inStream) ;


        imageView.setImageBitmap(bm);
        CauseView.setText(text);

    }

}
