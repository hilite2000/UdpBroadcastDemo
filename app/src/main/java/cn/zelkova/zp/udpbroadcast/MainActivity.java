package cn.zelkova.zp.udpbroadcast;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.etMsg = (EditText) findViewById(R.id.etMsg);

        Button btn = (Button) findViewById(R.id.btnSend);
        btn.setOnClickListener(myClickHandle);

        btn = (Button) findViewById(R.id.btnReceive);
        btn.setOnClickListener(myClickHandle);

        showMsgLine("app started");

    }


    Thread thdReceive;
    EditText etMsg;

    private View.OnClickListener myClickHandle = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (v.getId() == R.id.btnSend) {
                Thread thd = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendUDP();
                    }
                });
                thd.start();
            }

            if (v.getId() == R.id.btnReceive) {
                receiveUDP();
            }
        }


        private void receiveUDP() {
            if (thdReceive != null) {
                showMsgLine("UDP receiver is running");
                return;
            }

            thdReceive = new Thread(new Runnable() {
                @Override
                public void run() {
                    DatagramSocket rSocket = null;

                    try {
                        rSocket = new DatagramSocket(1661);
                        rSocket.setBroadcast(true);

                    } catch (SocketException e) {
                        e.printStackTrace();
                    }


                    while (true) {

                        try {

                            byte[] buffer = new byte[512];
                            final DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                            rSocket.receive(dp);

                            showMsgLine("[Rx]" + new String(dp.getData(), 0, dp.getLength()));

                        } catch (SocketException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });

            thdReceive.start();

            showMsgLine("UDP receiver started");
        }

        private void sendUDP() {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String content = sdf.format(new Date());
                content = "[" + Build.MODEL + "]" + content;
                byte[] sendBuffer = content.getBytes();

                InetAddress bcIP = Inet4Address.getByAddress(getWifiBroadcastIP());

                DatagramSocket udp = new DatagramSocket();
                udp.setBroadcast(true);
                DatagramPacket dp = new DatagramPacket(sendBuffer, sendBuffer.length, bcIP, 1661);
                udp.send(dp);

                showMsgLine("[Tx]" + content);
                Log.d("UDP", "[Tx]" + content);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private byte[] getWifiBroadcastIP() {

        WifiManager wifiMng = ((WifiManager) getSystemService(Context.WIFI_SERVICE));
        DhcpInfo dhcpInfo = wifiMng.getDhcpInfo();

        int bcIp = ~dhcpInfo.netmask | dhcpInfo.ipAddress;
        byte[] retVal = new byte[4];
        retVal[0] = (byte) (bcIp & 0xff);
        retVal[1] = (byte) (bcIp >> 8 & 0xff);
        retVal[2] = (byte) (bcIp >> 16 & 0xff);
        retVal[3] = (byte) (bcIp >> 24 & 0xff);

        return retVal;
    }

    private void showMsgLine(final String msg) {
        showMsg(msg + "\n");
    }

    private void showMsg(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                etMsg.append(msg);
            }
        });
    }
}
