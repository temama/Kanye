package com.bot.temama.kanye;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;


public class MainActivity extends ActionBarActivity {

    private boolean mArduinoConnected = false;

    EditText textStatus;

    private static final int targetVendorID = 9025;
    //private static final int targetProductID = 32828;
    UsbDevice deviceFound = null;
    UsbInterface usbInterfaceFound = null;
    UsbEndpoint endpointIn = null;
    UsbEndpoint endpointOut = null;

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    UsbInterface usbInterface;
    UsbDeviceConnection usbDeviceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = (EditText) findViewById(R.id.logText);

        //register the broadcast receiver
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        connectUsb();

        final Button button = (Button) findViewById(R.id.moveButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mArduinoConnected && usbDeviceConnection != null) {
                    byte[] bytesHello =
                            new byte[]{(byte) 'M', 'F', '5', '0', '0', '\n'};
                    int usbResult = usbDeviceConnection.bulkTransfer(
                            endpointOut,
                            bytesHello,
                            bytesHello.length,
                            1000); // timeout 1000

                    TextView txt = (TextView) findViewById(R.id.statusText);
                    txt.setText("bulkTransfer: " + usbResult);

                    EditText log = (EditText) findViewById(R.id.logText);
                    log.append("bulkTransfer: " + usbResult);

                    //if (usbResult == -1){
                    //    log.append("searching endpoint\r\n");
                    //    connectUsb();
                    //}

                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        releaseUsb();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbDeviceReceiver);
        super.onDestroy();
    }

    private void connectUsb() {
        textStatus.append("connectUsb()");

        searchEndPoint();

        if (usbInterfaceFound != null) {
            setupUsbComm();
            mArduinoConnected = true;
            TextView txt = (TextView) findViewById(R.id.arduinoConnectionTextView);
            txt.setTextColor(Color.GREEN);
            txt.setText(R.string.yes);
        }

    }

    private void releaseUsb() {
        textStatus.append("releaseUsb()");

        if (usbDeviceConnection != null) {
            if (usbInterface != null) {
                usbDeviceConnection.releaseInterface(usbInterface);
                usbInterface = null;
            }
            usbDeviceConnection.close();
            usbDeviceConnection = null;
        }

        deviceFound = null;
        usbInterfaceFound = null;
        endpointIn = null;
        endpointOut = null;
        mArduinoConnected = false;
        TextView txt = (TextView) findViewById(R.id.arduinoConnectionTextView);
        txt.setTextColor(Color.RED);
        txt.setText(R.string.no);
    }

    private void searchEndPoint() {
        usbInterfaceFound = null;
        endpointOut = null;
        endpointIn = null;

        //Search device for targetVendorID and targetProductID
        if (deviceFound == null) {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();

                if (device.getVendorId() == targetVendorID) {
                    deviceFound = device;
                }
            }
        }

        if (deviceFound == null) {
            textStatus.append("device not found");
        } else {
            String s = deviceFound.toString() + "\n" +
                    "DeviceID: " + deviceFound.getDeviceId() + "\n" +
                    "DeviceName: " + deviceFound.getDeviceName() + "\n" +
                    "DeviceClass: " + deviceFound.getDeviceClass() + "\n" +
                    "DeviceSubClass: " + deviceFound.getDeviceSubclass() + "\n" +
                    "VendorID: " + deviceFound.getVendorId() + "\n" +
                    "ProductID: " + deviceFound.getProductId() + "\n" +
                    "InterfaceCount: " + deviceFound.getInterfaceCount();
            textStatus.append(s);

            //Search for UsbInterface with Endpoint of USB_ENDPOINT_XFER_BULK,
            //and direction USB_DIR_OUT and USB_DIR_IN

            for (int i = 0; i < deviceFound.getInterfaceCount(); i++) {
                UsbInterface usbif = deviceFound.getInterface(i);
                textStatus.append("Interface Name: " + usbif.toString() + "; Interface N: " + usbif.getId() + "\r\n");
                UsbEndpoint tOut = null;
                UsbEndpoint tIn = null;

                int tEndpointCnt = usbif.getEndpointCount();
                if (tEndpointCnt >= 2) {
                    for (int j = 0; j < tEndpointCnt; j++) {
                        if (usbif.getEndpoint(j).getType() ==
                                UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (usbif.getEndpoint(j).getDirection() ==
                                    UsbConstants.USB_DIR_OUT) {
                                tOut = usbif.getEndpoint(j);
                            } else if (usbif.getEndpoint(j).getDirection() ==
                                    UsbConstants.USB_DIR_IN) {
                                tIn = usbif.getEndpoint(j);
                            }
                        }
                    }

                    if (tOut != null && tIn != null) {
                        //This interface have both USB_DIR_OUT
                        //and USB_DIR_IN of USB_ENDPOINT_XFER_BULK
                        usbInterfaceFound = usbif;
                        endpointOut = tOut;
                        endpointIn = tIn;
                    }
                }

            }

            if (usbInterfaceFound == null) {
                textStatus.append("No suitable interface found!");
            } else {
                textStatus.append(
                        "UsbInterface found: " + usbInterfaceFound.toString() + "\n\n" +
                                "Endpoint OUT: " + endpointOut.toString() + "\n\n" +
                                "Endpoint IN: " + endpointIn.toString());
            }
        }
    }

    private boolean setupUsbComm() {

        //for more info, search SET_LINE_CODING and
        //SET_CONTROL_LINE_STATE in the document:
        //"Universal Serial Bus Class Definitions for Communication Devices"
        //at http://adf.ly/dppFt
        final int RQSID_SET_LINE_CODING = 0x20;
        final int RQSID_SET_CONTROL_LINE_STATE = 0x22;

        boolean success = false;

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Boolean permitToRead = manager.hasPermission(deviceFound);

        if (permitToRead) {
            usbDeviceConnection = manager.openDevice(deviceFound);
            if (usbDeviceConnection != null) {
                textStatus.append("Start working with interface " + usbInterfaceFound.getId() + "\n");

                if (!usbDeviceConnection.claimInterface(usbInterfaceFound, true)) {
                    textStatus.append("Can't claim interface"+ "\n");
                }

                //showRawDescriptors(); //skip it if you no need show RawDescriptors
                //SystemClock.sleep(5000);

                //if (!usbDeviceConnection.setInterface(usbInterfaceFound)){
                    //textStatus.append("Can't set interface"+ "\n");
                //}

                int usbResult;
                usbResult = usbDeviceConnection.controlTransfer(
                        0x21,        //requestType
                        RQSID_SET_CONTROL_LINE_STATE, //SET_CONTROL_LINE_STATE
                        0x01,     //value
                        0,     //index
                        null,    //buffer
                        0,     //length
                        0);    //timeout

                textStatus.append("controlTransfer(SET_CONTROL_LINE_STATE): " + usbResult+ "\n");

                //baud rate = 9600
                //8 data bit
                //1 stop bit
                byte[] encodingSetting =
                        new byte[]{(byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08};

                usbResult = usbDeviceConnection.controlTransfer(
                        0x21,       //requestType
                        RQSID_SET_LINE_CODING,   //SET_LINE_CODING
                        0,      //value
                        0,      //index
                        encodingSetting,  //buffer
                        7,      //length
                        0);     //timeout

                textStatus.append("controlTransfer(RQSID_SET_LINE_CODING): " + usbResult+ "\n");

               /* byte[] bytesHello =
                        new byte[]{(byte) 'H', 'e', 'l', 'l', 'o', ' ',
                                'f', 'r', 'o', 'm', ' ',
                                'A', 'n', 'd', 'r', 'o', 'i', 'd', '\n'};
                usbResult = usbDeviceConnection.bulkTransfer(
                        endpointOut,
                        bytesHello,
                        bytesHello.length,
                        10000);*/

                //textStatus.append("bulkTransfer: " + usbResult);
            }

        } else {
            manager.requestPermission(deviceFound, mPermissionIntent);
            textStatus.setText("Permission: " + permitToRead+ "\n");
        }


        return success;
    }

    private void showRawDescriptors() {
        final int STD_USB_REQUEST_GET_DESCRIPTOR = 0x06;
        final int LIBUSB_DT_STRING = 0x03;

        byte[] buffer = new byte[255];
        int indexManufacturer = 14;
        int indexProduct = 15;
        String stringManufacturer = "";
        String stringProduct = "";

        byte[] rawDescriptors = usbDeviceConnection.getRawDescriptors();

        int lengthManufacturer = usbDeviceConnection.controlTransfer(
                UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_STANDARD,   //requestType
                STD_USB_REQUEST_GET_DESCRIPTOR,         //request ID for this transaction
                (LIBUSB_DT_STRING << 8) | rawDescriptors[indexManufacturer], //value
                0,   //index
                buffer,  //buffer
                0xFF,  //length
                0);   //timeout
        try {
            stringManufacturer = new String(buffer, 2, lengthManufacturer - 2, "UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            textStatus.setText(e.toString());
        }

        int lengthProduct = usbDeviceConnection.controlTransfer(
                UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_STANDARD,
                STD_USB_REQUEST_GET_DESCRIPTOR,
                (LIBUSB_DT_STRING << 8) | rawDescriptors[indexProduct],
                0,
                buffer,
                0xFF,
                0);
        try {
            stringProduct = new String(buffer, 2, lengthProduct - 2, "UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        textStatus.setText("Manufacturer: " + stringManufacturer + "\n" +
                "Product: " + stringProduct);
    }

    private final BroadcastReceiver mUsbReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (ACTION_USB_PERMISSION.equals(action)) {
                        textStatus.setText("ACTION_USB_PERMISSION");

                        synchronized (this) {
                            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                if (device != null) {
                                    connectUsb();
                                }
                            } else {
                                textStatus.setText("permission denied for device " + device);
                            }
                        }
                    }
                }
            };

    private final BroadcastReceiver mUsbDeviceReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

                        deviceFound = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        textStatus.setText("ACTION_USB_DEVICE_ATTACHED: \n" +
                                deviceFound.toString());

                        connectUsb();

                    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        textStatus.setText("ACTION_USB_DEVICE_DETACHED: \n" +
                                device.toString());

                        if (device != null) {
                            if (device == deviceFound) {
                                releaseUsb();
                            }
                        }
                    }
                }

            };
}

/*
    private static final int targetVendorID= 9025;

    private boolean mArduinoConnected = false;

    UsbDevice deviceFound = null;
    UsbInterface usbInterfaceFound = null;
    UsbEndpoint endpointIn = null;
    UsbEndpoint endpointOut = null;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    UsbInterface usbInterface;
    UsbDeviceConnection usbDeviceConnection;

    private void onArduinoConnected(boolean connected) {
        mArduinoConnected = connected;

        Button btn = (Button) findViewById(R.id.startButton);
        btn.setEnabled(connected);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.startButton);
        btn.setEnabled(false);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));


        final Button button = (Button) findViewById(R.id.moveButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mArduinoConnected && usbDeviceConnection != null) {
                    byte[] bytesHello =
                            new byte[] {(byte)'F', '5', '0', '0', '\n'};
                    int usbResult = usbDeviceConnection.bulkTransfer(
                            endpointOut,
                            bytesHello,
                            bytesHello.length,
                            100); // timeout 100

                    TextView txt = (TextView) findViewById(R.id.statusText);
                    txt.setText("bulkTransfer: " + usbResult);

                    EditText log = (EditText) findViewById(R.id.logText);
                    log.append("bulkTransfer: " + usbResult);
                }
            }
        });

        // if already connected
        connectUsb();
    }

    @Override
    protected void onDestroy() {
        releaseUsb();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbDeviceReceiver);
        super.onDestroy();
    }

    private void connectUsb(){
        searchEndPoint();

        if(usbInterfaceFound != null){

            mArduinoConnected = true;

            Button btn = (Button) findViewById(R.id.startButton);
            btn.setEnabled(true);

            TextView txt = (TextView) findViewById(R.id.arduinoConnectionTextView);
            txt.setTextColor(Color.GREEN);
            txt.setText(R.string.yes);

            setupUsbComm();
        }

    }

    private void releaseUsb(){
        mArduinoConnected = false;

        Button btn = (Button) findViewById(R.id.startButton);
        btn.setEnabled(false);

        TextView txt = (TextView) findViewById(R.id.arduinoConnectionTextView);
        txt.setTextColor(Color.RED);
        txt.setText(R.string.no);

        if(usbDeviceConnection != null){
            if(usbInterface != null){
                usbDeviceConnection.releaseInterface(usbInterface);
                usbInterface = null;
            }
            usbDeviceConnection.close();
            usbDeviceConnection = null;
        }

        deviceFound = null;
        usbInterfaceFound = null;
        endpointIn = null;
        endpointOut = null;
    }

    private void searchEndPoint() {
        usbInterfaceFound = null;
        endpointOut = null;
        endpointIn = null;

        //Search device for targetVendorID and targetProductID
        if (deviceFound == null) {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();

                if (device.getVendorId() == targetVendorID) {
                    deviceFound = device;
                }
            }
        }

        if (deviceFound == null)
            return;

        for(int i=0; i<deviceFound.getInterfaceCount(); i++){
            UsbInterface usbif = deviceFound.getInterface(i);

            UsbEndpoint tOut = null;
            UsbEndpoint tIn = null;

            int tEndpointCnt = usbif.getEndpointCount();
            if(tEndpointCnt>=2){
                for(int j=0; j<tEndpointCnt; j++){
                    if(usbif.getEndpoint(j).getType() ==
                            UsbConstants.USB_ENDPOINT_XFER_BULK){
                        if(usbif.getEndpoint(j).getDirection() ==
                                UsbConstants.USB_DIR_OUT){
                            tOut = usbif.getEndpoint(j);
                        }else if(usbif.getEndpoint(j).getDirection() ==
                                UsbConstants.USB_DIR_IN){
                            tIn = usbif.getEndpoint(j);
                        }
                    }
                }

                if(tOut!=null && tIn!=null){
                    //This interface have both USB_DIR_OUT
                    //and USB_DIR_IN of USB_ENDPOINT_XFER_BULK
                    usbInterfaceFound = usbif;
                    endpointOut = tOut;
                    endpointIn = tIn;
                }
            }
        }
    }

    private boolean setupUsbComm(){

        //for more info, search SET_LINE_CODING and
        //SET_CONTROL_LINE_STATE in the document:
        //"Universal Serial Bus Class Definitions for Communication Devices"
        //at http://adf.ly/dppFt
        final int RQSID_SET_LINE_CODING = 0x20;
        final int RQSID_SET_CONTROL_LINE_STATE = 0x22;

        boolean success = false;

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Boolean permitToRead = manager.hasPermission(deviceFound);

        if(permitToRead){
            usbDeviceConnection = manager.openDevice(deviceFound);
            if(usbDeviceConnection != null){
                usbDeviceConnection.claimInterface(usbInterfaceFound, true);

                //showRawDescriptors(); //skip it if you no need show RawDescriptors

                int usbResult;
                usbResult = usbDeviceConnection.controlTransfer(
                        0x21,        //requestType
                        RQSID_SET_CONTROL_LINE_STATE, //SET_CONTROL_LINE_STATE
                        0,     //value
                        0,     //index
                        null,    //buffer
                        0,     //length
                        0);    //timeout

                TextView txt = (TextView) findViewById(R.id.statusText);
                txt.setText("controlTransfer(SET_CONTROL_LINE_STATE): " + usbResult);

                EditText log = (EditText) findViewById(R.id.logText);
                log.append("controlTransfer(SET_CONTROL_LINE_STATE): " + usbResult);

                //baud rate = 9600
                //8 data bit
                //1 stop bit
                byte[] encodingSetting =
                        new byte[] {(byte)0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
                usbResult = usbDeviceConnection.controlTransfer(
                        0x21,       //requestType
                        RQSID_SET_LINE_CODING,   //SET_LINE_CODING
                        0,      //value
                        0,      //index
                        encodingSetting,  //buffer
                        7,      //length
                        0);     //timeout
                txt.setText("controlTransfer(RQSID_SET_LINE_CODING): " + usbResult);
                log.append("controlTransfer(RQSID_SET_LINE_CODING): " + usbResult);

                usbResult = usbDeviceConnection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 0);
                txt.setText("controlTransfer(RQSID_SET_LINE_CODING): " + usbResult);
                log.append("controlTransfer(RQSID_SET_LINE_CODING): " + usbResult);
            }

        }else{
            manager.requestPermission(deviceFound, mPermissionIntent);
            TextView txt = (TextView) findViewById(R.id.statusText);
            txt.setText("Permission: " + permitToRead);

            EditText log = (EditText) findViewById(R.id.logText);
            log.append("Permission: " + permitToRead);
        }



        return success;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver mUsbReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (ACTION_USB_PERMISSION.equals(action)) {
                        synchronized (this) {
                            UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                if(device != null){
                                    connectUsb();
                                }
                            }
                            else {
                                TextView txt = (TextView) findViewById(R.id.statusText);
                                txt.setText("permission denied for device " + device);

                                EditText log = (EditText) findViewById(R.id.logText);
                                log.append("permission denied for device " + device);
                            }
                        }
                    }
                }
            };

    private final BroadcastReceiver mUsbDeviceReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

                        deviceFound = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        TextView txt = (TextView) findViewById(R.id.statusText);
                        txt.setText("ACTION_USB_DEVICE_ATTACHED: \n" +
                                deviceFound.toString());

                        EditText log = (EditText) findViewById(R.id.logText);
                        log.append("ACTION_USB_DEVICE_ATTACHED: \n" +
                                deviceFound.toString());

                        connectUsb();

                    }else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        TextView txt = (TextView) findViewById(R.id.statusText);
                        txt.setText("ACTION_USB_DEVICE_DETACHED: \n" +
                                device.toString());

                        EditText log = (EditText) findViewById(R.id.logText);
                        log.append("ACTION_USB_DEVICE_DETACHED: \n" +
                                device.toString());

                        if(device!=null){
                            if(device == deviceFound){
                                releaseUsb();
                            }
                        }
                    }
                }

            };
}
*/
