package dju.teambabo.proj.bigbrother_client_android_102;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class AlertListViewActivity extends AppCompatActivity  {

    TripleLists temp;

    ArrayList<TripleLists> list = new ArrayList<>();

    int Position;

    private ArrayList<String> mList;

    private ListView mListView;

    private ArrayAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_list_view);

        mList = new ArrayList<String>();
        mListView= (ListView) findViewById(R.id.listview1);
        loadAlertList();
        mAdapter =  new ArrayAdapter(this, R.layout.listview_theme , mList);
        mListView.setAdapter(mAdapter);

        // SwipeDismiss 기능으로 리스트뷰 슬라이드 삭제 기능
        SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(mListView,
                new SwipeDismissListViewTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(int position) {
                        return true;
                    }

                    @Override
                    public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions) {
                            mAdapter.remove(mAdapter.getItem(position));
                            postAlertList(list.get(position).get_id());
                            //loadAlertList();

                            Log.d("TAG", "밀었다"+list.get(position).toString());

                        }
                        mAdapter.notifyDataSetChanged();
                    }

                });
        mListView.setOnTouchListener(touchListener);
        mListView.setOnScrollListener(touchListener.makeScrollListener());





        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, int position, long l) {
                Position = position;
                itemCheckEvent(list, Position);
                Log.d("TAG", "클릭"+list.get(position).get_id());

            }
        });

    }




    private void loadAlertList() {
        AsyncHttpClient alertListLoad = new AsyncHttpClient();

        alertListLoad.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));

        String loadAlerUTL = getString(R.string.server_url) + getString(R.string.load_alert_list);


        alertListLoad.get(loadAlerUTL, new JsonHttpResponseHandler(){
            //불러오기 성공
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    int count;
                    list.clear();
                    for(count = 0; count< response.length(); count++) {
                        JSONArray ja = response;
                        JSONObject order = ja.optJSONObject(count);
                        String keyword = order.getString("keyword");
                        String id = order.getString("id");
                        String recordTime = order.getString("recordTime");
                        String onDropFlag = order.getString("drop_on_flag");

                        temp = new TripleLists(id, keyword, recordTime, onDropFlag);
                        //
                        list.add(temp);
                    }

                    final Handler mainHandler = new Handler(AlertListViewActivity.this.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mList.clear();
                            for(int i=0; i<list.size();i++) {
                               //
                                mList.add(list.get(i).get_flag()+"\n{키워드}"+list.get(i).get_keyword()+"\n기록-"+list.get(i).get_time());

                            }
                            mAdapter.notifyDataSetChanged();
                        }
                    });

                } catch (Exception e) {
                    //응답은 성공하였으나 값이 올바르지 않음
                    e.printStackTrace();

                }
            }
            //불러오기 실패
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }
    private void itemCheckEvent(ArrayList<TripleLists> data, int position) {
        String id =  data.get(position).get_id();
        String keyword =  data.get(position).get_keyword();


        Intent intent = new Intent(getApplicationContext(),ImageViewDialogActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("keyword", keyword);
        startActivity(intent);
    }


    private void postAlertList(String id) {

        AsyncHttpClient postAlertImage = new AsyncHttpClient();

        postAlertImage.addHeader(getString(R.string.auth_key), CookieManager.getInstance().getCookie(getString(R.string.token_key)));

        RequestParams params = new RequestParams();

        try {
            params.put("id", id);

        } catch (Exception e) {

        }

        String loadAlertImageURL = getString(R.string.server_url) + getString(R.string.post_alert_list);
        postAlertImage.post(this, loadAlertImageURL, params, new JsonHttpResponseHandler(){
            // 성공
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                loadAlertList();
            }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Log.d("TAG","request.err");
                }

            });


        }

}
