package dju.teambabo.proj.bigbrother_client_android_102;

/**
 * Created by chaegeonhui on 2018. 3. 20..
 */
//4개됨;;
public class TripleLists {

    private  String _id;
    private  String _keyword;
    private  String _time;
    private  String _flag;

    public TripleLists(String id ,String keyword, String time, String flag){
        _id = id;
        _keyword = keyword;
        _time = time;
        _flag = flag;
    }

    public String get_id(){
        return _id;
    }

    public String get_keyword(){
        return _keyword;
    }

    public String get_time(){
        return _time;
    }

    public String get_flag() { return  _flag; }
}
