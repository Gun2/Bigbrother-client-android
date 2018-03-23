package dju.teambabo.proj.bigbrother_client_android_102;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by chaegeonhui on 2018. 3. 18..
 */

public class BigbrotherBroadcastReceiverMain extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        String actionName = intent.getAction();

        Toast.makeText(context,"toast" +actionName, Toast.LENGTH_SHORT).show();
    }
}
