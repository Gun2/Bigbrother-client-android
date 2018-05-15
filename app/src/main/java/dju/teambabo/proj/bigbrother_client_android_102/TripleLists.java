package dju.teambabo.proj.bigbrother_client_android_102;

/**
 * Created by chaegeonhui on 2018. 3. 20..
 */

public class TripleLists {

    private  String _id;
    private  String _keyword;
    private  String _time;
    private  String _flag;
    private  String _label_value;
    private  String _drop_on_flag;
    private  String _location;

    //4개됨;;
    public TripleLists(String id ,String keyword, String time, String flag){
        _id = id;
        _keyword = keyword;
        _time = time;
        _flag = flag;
    }

    public TripleLists(String label_value ,String drop_on_flag, String location){
        _label_value = label_value;
        _drop_on_flag = drop_on_flag;
        _location = location;
    }

    public String get_label_value(){
        return _label_value;
    }

    public String get_drop_on_flag(){
        return _drop_on_flag;
    }

    public String get_location(){
        return _location;
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
