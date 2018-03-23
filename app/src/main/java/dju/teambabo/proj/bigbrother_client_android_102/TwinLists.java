package dju.teambabo.proj.bigbrother_client_android_102;

/**
 * Created by chaegeonhui on 2018. 3. 17..
 */

public class TwinLists {

    private  String _list;
    private  String _data;

    public TwinLists(String list,String data){
        _list = list;
        _data = data;
    }

    public String get_arrList(){

        return _list;
    }

    public String get_arrData(){



        return _data;
    }
}
