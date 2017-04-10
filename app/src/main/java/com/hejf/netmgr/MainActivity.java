package com.hejf.netmgr;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.LinkAddress;
import android.net.ProxyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ExpandedMenuView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ToggleButton button = (ToggleButton) findViewById(R.id.toggleButton);
        final ToggleButton button2 = (ToggleButton) findViewById(R.id.toggleButton2);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration currentConf = getCurrentWifiConfiguration(wifiManager);
        String ipStrategy = getIpAssignmentStrategy(currentConf);
        if ("DHCP".equals(ipStrategy)) {
            button.setChecked(false);
        } else {
            button.setChecked(true);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiConfiguration currentConf = getCurrentWifiConfiguration(wifiManager);
                DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                String ipAddr = long2ip(dhcpInfo.ipAddress);
                String gateway = long2ip(dhcpInfo.gateway);
                if (button.isChecked()) {
                    try {
                        Object ipAssignment = getEnumValue("android.net.IpConfiguration$IpAssignment", "STATIC");
                        Class ipAssignmentClass = Class.forName("android.net.IpConfiguration$IpAssignment");
                        Method assignMethod = getMethod("setIpAssignment", ipAssignmentClass);
                        assignMethod.invoke(currentConf, ipAssignment);

                        Class laClass = Class.forName("android.net.LinkAddress");
                        Constructor laConstructor = laClass.getConstructor(new Class[]{InetAddress.class, int.class});
                        LinkAddress linkAddress = (LinkAddress) laConstructor.newInstance(InetAddress.getByName(ipAddr), 24);

                        Class staticIpConfClass = Class.forName("android.net.StaticIpConfiguration");
                        Object staticIpConf = staticIpConfClass.getConstructor().newInstance();
                        setField(staticIpConfClass, "ipAddress", staticIpConf, linkAddress);
                        setField(staticIpConfClass, "gateway", staticIpConf, InetAddress.getByName(gateway));

                        ArrayList<InetAddress> dnsServers = new ArrayList<InetAddress>();
                        dnsServers.add(InetAddress.getByName("10.185.240.85"));
                        setField(staticIpConfClass, "dnsServers", staticIpConf, dnsServers);

                        Method m = getMethod("setStaticIpConfiguration", staticIpConfClass);
                        m.invoke(currentConf, staticIpConf);
                        wifiManager.updateNetwork(currentConf); //apply the setting
                        wifiManager.saveConfiguration(); //Save it

                        Toast toast = Toast.makeText(getApplicationContext(), "设置成功", Toast.LENGTH_LONG);
                        toast.show();
                    } catch (Exception e) {
                        Toast toast = Toast.makeText(getApplicationContext(), "设置代理失败", Toast.LENGTH_LONG);
                        toast.show();
                    }
                } else {
                    try {
                        Object ipAssignment = getEnumValue("android.net.IpConfiguration$IpAssignment", "DHCP");
                        Class ipAssignmentClass = Class.forName("android.net.IpConfiguration$IpAssignment");
                        Method assignMethod = getMethod("setIpAssignment", ipAssignmentClass);
                        assignMethod.invoke(currentConf, ipAssignment);
                        wifiManager.updateNetwork(currentConf); //apply the setting
                        wifiManager.saveConfiguration(); //Save it
                        Toast toast = Toast.makeText(getApplicationContext(), "设置成功", Toast.LENGTH_LONG);
                        toast.show();
                    } catch (Exception e) {
                        Toast toast = Toast.makeText(getApplicationContext(), "取消代理失败", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            }
        });


        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiConfiguration currentConf = getCurrentWifiConfiguration(wifiManager);
                try {
                    if (button2.isChecked()) {
                        Object ipAssignment = getEnumValue("android.net.IpConfiguration$IpAssignment", "DHCP");
                        Class ipAssignmentClass = Class.forName("android.net.IpConfiguration$IpAssignment");
                        Method assignMethod = getMethod("setIpAssignment", ipAssignmentClass);
                        assignMethod.invoke(currentConf, ipAssignment);

                        ProxyInfo httpProxy = ProxyInfo.buildDirectProxy("192.168.2.64", 8888);
                        Method m = getMethod("setHttpProxy", ProxyInfo.class);
                        m.invoke(currentConf, httpProxy);

                        Class staticIpConfClass = Class.forName("android.net.StaticIpConfiguration");
                        Method staticM = getMethod("setStaticIpConfiguration", staticIpConfClass);
                        m.invoke(currentConf, new Object[]{null});

                        wifiManager.updateNetwork(currentConf); //apply the setting
                        wifiManager.saveConfiguration(); //Save it
                    } else {
                        Object ipAssignment = getEnumValue("android.net.IpConfiguration$IpAssignment", "DHCP");
                        Class ipAssignmentClass = Class.forName("android.net.IpConfiguration$IpAssignment");
                        Method assignMethod = getMethod("setIpAssignment", ipAssignmentClass);
                        assignMethod.invoke(currentConf, ipAssignment);

                        Method m = getMethod("setHttpProxy", ProxyInfo.class);
                        m.invoke(currentConf, new Object[]{null});

                        Class staticIpConfClass = Class.forName("android.net.StaticIpConfiguration");
                        Method staticM = getMethod("setStaticIpConfiguration", staticIpConfClass);
                        m.invoke(currentConf, new Object[]{null});

                        wifiManager.updateNetwork(currentConf); //apply the setting
                        wifiManager.saveConfiguration(); //Save it
                    }
                } catch (Exception e) {
                    Toast toast = Toast.makeText(getApplicationContext(), "设置失败", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    private WifiConfiguration getCurrentWifiConfiguration(WifiManager wifiManager) {
        String ssid = wifiManager.getConnectionInfo().getSSID();
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        WifiConfiguration currentConf = null;
        for (WifiConfiguration wc : list) {
            if (wc.SSID.equals(ssid)) {
                currentConf = wc;
                break;
            }
        }
        return currentConf;
    }

    private static String getIpAssignmentStrategy(WifiConfiguration conf) {
        Object mIpConfiguration = getField(conf, "mIpConfiguration");
        Object ipAssignment = getField(mIpConfiguration, "ipAssignment");
        return ((Enum) ipAssignment).name();
    }

    private static <T> T getField(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    String long2ip(long ip) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf((int) (ip & 0xff)));
        sb.append('.');
        sb.append(String.valueOf((int) ((ip >> 8) & 0xff)));
        sb.append('.');
        sb.append(String.valueOf((int) ((ip >> 16) & 0xff)));
        sb.append('.');
        sb.append(String.valueOf((int) ((ip >> 24) & 0xff)));
        return sb.toString();
    }

    public static void setField(Class<?> clazz, String fname, Object obj, Object fval) throws Exception {
        Field f = clazz.getDeclaredField(fname);
        f.setAccessible(true);
        f.set(obj, fval);
    }

    public static void setEnumField(Object obj, String value, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getField(name);
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }

    private static Object getEnumValue(String enumClassName, String enumValue) throws ClassNotFoundException {
        Class<Enum> enumClz = (Class<Enum>) Class.forName(enumClassName);
        return Enum.valueOf(enumClz, enumValue);
    }

    private static void callMethod(Object object, String methodName, String[] parameterTypes, Object[] parameterValues) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++)
            parameterClasses[i] = Class.forName(parameterTypes[i]);

        Method method = object.getClass().getDeclaredMethod(methodName, parameterClasses);
        method.invoke(object, parameterValues);
    }

    public static Method getMethod(String mname, Class... classes) {
        try {
            Method method = WifiConfiguration.class.getDeclaredMethod(mname, classes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}
