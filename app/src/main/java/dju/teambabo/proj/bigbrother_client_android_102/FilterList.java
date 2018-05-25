package dju.teambabo.proj.bigbrother_client_android_102;

/**
 * Created by chaegeonhui on 2018. 5. 23..
 */

public class FilterList{

    private  String _location;
    private  String _label_value;
    private  Boolean _drop_on_flag;
    private  Boolean _picRequest;
    private  int _pk;

    public FilterList(String location ,String label_value, Boolean drop_on_flag, Boolean picRequest, int pk){
        _location = location;
        _label_value = label_value;
        _drop_on_flag = drop_on_flag;
        _picRequest = picRequest;
        _pk = pk;
    }
    @Override
    public String toString() {
        String resultString = "";
        if (_location != null) {
            resultString += "[" + _location + "] ";
        }

        if (_label_value != null) {
            resultString += _label_value + " ";
        }

        if (_drop_on_flag != null) {
            resultString += _drop_on_flag.toString()+"";
        }

        if (_picRequest != null) {
            resultString += _picRequest.toString() + " ";
        }

        if (_pk >= 0) {
            resultString += (_pk) + " ";
        }

        return resultString.trim();
    }

    public String get_label_value(){
        return _label_value;
    }

    public Boolean get_drop_on_flag(){
        return _drop_on_flag;
    }

    public String get_location(){
        return _location;
    }

    public Boolean get_picRequest(){
        return _picRequest;
    }

    public int get_pk(){ return _pk; }

}
