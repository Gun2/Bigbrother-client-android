package dju.teambabo.proj.bigbrother_client_android_102;

import android.app.Application;

import java.util.ArrayList;

/**
 * Created by chaegeonhui on 2018. 5. 15..
 */

public class GlobalValue extends Application {

    /**
     * 촬영 금지 항목 받아오기
     */
    private ArrayList<ArrayList<String>> guardListText = new ArrayList<>();
    private ArrayList<String> guardTempText = new ArrayList<>();
    private ArrayList<FilterList> guardListLabel = new ArrayList<>();
    private ArrayList<String> guardTempLabel = new ArrayList<>();

    public ArrayList<FilterList> getGlobalValueLabeldList(){

        return guardListLabel;
    }

    public ArrayList<ArrayList<String>> getGlobalValueTextList(){

        return guardListText;
    }

    public void setGlobalValueLabeldList(ArrayList<FilterList> mList){
        this.guardListLabel = mList;
    }

    public void setGlobalValueTextList(ArrayList<ArrayList<String>> mList){
        this.guardListText = mList;
    }
}
