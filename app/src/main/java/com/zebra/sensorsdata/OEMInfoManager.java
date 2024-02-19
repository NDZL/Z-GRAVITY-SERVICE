package com.zebra.sensorsdata;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class OEMInfoManager implements EMDKManager.EMDKListener  {

    public static volatile String OEMINFO_DEVICE_SERIAL ="N/A";

    String TAG = "DeviceID";
    String URI_SERIAL = "content://oem_info/oem.zebra.secure/build_serial";
    String URI_IMEI = "content://oem_info/wan/imei";
    String URI_BT_MAC = "content://oem_info/oem.zebra.secure/bt_mac";
    String URI_WIFI_MAC = "content://oem_info/oem.zebra.secure/wifi_mac";


    //Assign the profile name used in EMDKConfig.xml
    private String profileName = "";

    //Declare a variable to store ProfileManager object
    private ProfileManager profileManager = null;

    //Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;
    EditText txtServiceIdentifier = null;
    EditText txtPackageName = null;
    // Provides the error type for characteristic-error
    private String errorType = "";
    // Provides the parm name for parm-error
    private String parmName = "";
    // Provides error description
    private String errorDescription = "";
    // Provides error string with type/name + description
    private String errorString = "";

    private String mToken = "";

    Context context;
    public OEMInfoManager(Context ctx){
        context =ctx;
        EMDKResults results = EMDKManager.getEMDKManager(context.getApplicationContext(), this);
        if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            //EMDKManager object creation success
        }else {
            //EMDKManager object creation failed
        }

    }


    private void RetrieveOEMInfo(Uri uri, boolean isIMEI) {
        //  For clarity, this code calls ContentResolver.query() on the UI thread but production code should perform queries asynchronously.
        //  See https://developer.android.com/guide/topics/providers/content-provider-basics.html for more information
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        if (cursor == null || cursor.getCount() < 1)
        {
            String errorMsg = "Error: This app does not have access to call OEM service. " +
                    "Please assign access to " + uri + " through MX.  See ReadMe for more information";
            Log.d(TAG, errorMsg);
            OEMINFO_DEVICE_SERIAL = errorMsg;
            return;
        }
        Log.i(TAG, "Records available =" + cursor.getCount());
        while (cursor.moveToNext()) {
            if (cursor.getColumnCount() == 0)
            {
                //  No data in the cursor.  I have seen this happen on non-WAN devices
                String errorMsg = "Error: " + uri + " does not exist on this device";
                Log.d(TAG, errorMsg);
                if (isIMEI)
                    errorMsg = "Error: Could not find IMEI.  Is device WAN capable?";
                OEMINFO_DEVICE_SERIAL = errorMsg;
            }
            else{
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    Log.v(TAG, "column " + i + "=" + cursor.getColumnName(i));
                    try {
                        String data = cursor.getString(cursor.getColumnIndex(cursor.getColumnName(i)));
                        Log.i(TAG, "Column Data " + i + "=" + data);
                        OEMINFO_DEVICE_SERIAL = data;
                    }
                    catch (Exception e)
                    {
                        Log.i(TAG, "Exception reading data for column " + cursor.getColumnName(i));
                    }
                }
            }
        }
        cursor.close();
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;

        profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);

        try {
            allowThisPackageToCallService("content://oem_info/oem.zebra.secure/build_serial", "content://oem_info/oem.zebra.secure/wifi_mac");
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onClosed() {

        //This callback will be issued when the EMDK closes unexpectedly.
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }

    }


    public void allowThisPackageToCallService(String service1, String service2) throws ExecutionException, InterruptedException {
        profileName = "AllowCallerToCallService";
        String packagename = context.getApplicationContext().getPackageName();
        //Log.i("working on package", packagename);
        String signature = getCallerSignatureBase64Encoded(packagename);

        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "  <characteristic type=\"Profile\">\n" +
                "  <parm name=\"ProfileName\" value=\"AllowCallerToCallService\"/>" +

                "   <characteristic type=\"AccessMgr\" version=\"8.3\">\n" +
                "      <parm name=\"emdk_name\" value=\"\"/>\n" +
                "      <parm name=\"OperationMode\" value=\"1\"/>\n" +
                "      <parm name=\"ServiceAccessAction\" value=\"4\"/>\n" +
                "      <parm name=\"ServiceIdentifier\" value=\""+  service1  +"\"/>\n" +
                "      <parm name=\"CallerPackageName\" value=\""+  packagename  +"\"/>\n" +
                "      <parm name=\"CallerSignature\" value=\""+signature+"\"/>\n" +
                "    </characteristic>\n"

/*                +


                "   <characteristic type=\"AccessMgr\" version=\"8.3\">\n" +
                "      <parm name=\"emdk_name\" value=\"\"/>\n" +
                "      <parm name=\"OperationMode\" value=\"1\"/>\n" +
                "      <parm name=\"ServiceAccessAction\" value=\"4\"/>\n" +
                "      <parm name=\"ServiceIdentifier\" value=\""+  service2  +"\"/>\n" +
                "      <parm name=\"CallerPackageName\" value=\""+  packagename  +"\"/>\n" +
                "      <parm name=\"CallerSignature\" value=\""+signature+"\"/>\n" +
                "    </characteristic>\n"
                */
                +

                "  </characteristic>";
        EMDKResults er = new ProcessProfileTask().execute(xml).get();

    }

    String getCallerSignatureBase64Encoded(String packageName){
        String callerSignature = null;

        try {
            Signature sig = context.getApplicationContext().getPackageManager().getPackageInfo(packageName, 64).signatures[0];
            if (sig != null) {
                byte[] data = Base64.encode(sig.toByteArray(), 0);
                String signature = new String(data, StandardCharsets.UTF_8);
                callerSignature = signature.replaceAll("\\s+", "");
                Log.d("SignatureVerifier", "caller signature:" + callerSignature);
            }
        } catch (Exception var6) {
            Log.e("SignatureVerifier", "exception in getting application signature:" + var6.toString());
            Log.e( TAG,"EXCP:"+var6.toString());
        }

        return callerSignature;
    }

    private class ProcessProfileTask extends AsyncTask<String, Void, EMDKResults> {
        @Override
        protected EMDKResults doInBackground(String... params) {

            parmName = "";
            errorDescription = "";
            errorType = "";
            mToken = "";

            EMDKResults resultsReset = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.RESET, params);

            EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, params);

            RetrieveOEMInfo(Uri.parse(URI_SERIAL), false);       //  Build.getSerial()
            return results;

        }

        @Override
        protected  void onPostExecute(EMDKResults results) {

            super.onPostExecute(results);

            String resultString = "";

            //Check the return status of processProfile
            if(results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {

                // Get XML response as a String
                String statusXMLResponse = results.getStatusString();

                try {
                    // Create instance of XML Pull Parser to parse the response
                    XmlPullParser parser = Xml.newPullParser();
                    // Provide the string response to the String Reader that reads
                    // for the parser
                    parser.setInput(new StringReader(statusXMLResponse));
                    // Call method to parse the response
                    //parseXML(parser);

                    if (TextUtils.isEmpty(parmName) && TextUtils.isEmpty(errorType) && TextUtils.isEmpty(errorDescription) ) {

                        resultString = "Profile update success.";
                        if(!TextUtils.isEmpty(mToken))
                        {
                            resultString += "\nToken: " + mToken;
                            txtPackageName.setText(mToken);
                        }
                    }
                    else {

                        resultString = "Profile update failed." + errorString;
                    }

                } catch (XmlPullParserException e) {
                    resultString =  e.getMessage();
                }
            }



            if (emdkManager != null) {
                emdkManager.release();
                emdkManager = null;
            }

        }
    }

}
