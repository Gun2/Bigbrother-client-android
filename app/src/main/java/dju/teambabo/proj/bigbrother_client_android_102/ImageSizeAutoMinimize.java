package dju.teambabo.proj.bigbrother_client_android_102;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by chaegeonhui on 2018. 3. 24..
 */

public class ImageSizeAutoMinimize {


    /** Get Bitmap's Width **/
    public static int getBitmapOfWidth( String fileName ){
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, options);
            return options.outWidth;
        } catch(Exception e) {
            return 0;
        }
    }

    /** Get Bitmap's height **/
    public static int getBitmapOfHeight( String fileName ) {

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, options);

            return options.outHeight;
        } catch (Exception e) {
            return 0;
        }
    }

    //해상도별 압축
    public static Bitmap AutominimizeBitmap(Bitmap srcBitmap, int size) {

        if (size>12000000){
            return Bitmap.createScaledBitmap(srcBitmap, srcBitmap.getWidth()/6, srcBitmap.getHeight()/6, true);
        }
        else if (size>9000000){
            return Bitmap.createScaledBitmap(srcBitmap, srcBitmap.getWidth()/5, srcBitmap.getHeight()/5, true);
        }
        else if(size>6000000){
            return Bitmap.createScaledBitmap(srcBitmap, srcBitmap.getWidth()/4, srcBitmap.getHeight()/4, true);
        }
        else if(size>3000000){
            return Bitmap.createScaledBitmap(srcBitmap, srcBitmap.getWidth()/3, srcBitmap.getHeight()/3, true);
        }
        else{
            return srcBitmap;
        }
    }
}
