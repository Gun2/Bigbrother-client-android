package dju.teambabo.proj.bigbrother_client_android_102;

import android.graphics.Bitmap;

public class RecognizeQue {


    private String _path;
    private Bitmap _bitmap;
    private String _key;

    public RecognizeQue(String path ,Bitmap bitmap, String key){
        _path = path;
        _bitmap = bitmap;
        _key = key;
    }

    @Override
    public String toString() {
        String resultString = "";
        if (_path != null) {
            resultString += "[" + _path + "] ";
        }

        if (_bitmap != null) {
            resultString += _bitmap + " ";
        }

        if (_key != null) {
            resultString += _key.toString()+"";
        }

        return resultString.trim();
    }

    public String get_path(){
        return _path;
    }

    public Bitmap get_bitmap(){
        return _bitmap;
    }

    public String get_key(){
        return _key;
    }



}
