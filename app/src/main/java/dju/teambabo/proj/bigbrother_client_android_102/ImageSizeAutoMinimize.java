package dju.teambabo.proj.bigbrother_client_android_102;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.IOException;

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

    public static Bitmap AutominimizeWithRotateBitmapOrientation(String photoFilePath) {

        // Create and configure BitmapFactory
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoFilePath, bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(photoFilePath, opts);
        // Read EXIF Data
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(photoFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;
        // Rotate Bitmap
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);

        int ImageSize = getBitmapOfWidth(photoFilePath)*getBitmapOfHeight(photoFilePath);


        Bitmap rotatedBitmap = Bitmap.createBitmap(AutominimizeBitmap(bm,ImageSize), 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
        // Return result
        return rotatedBitmap;
    }
}
