package com.uni.gamesever.domain.rest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public class WifiIPv4 {
    public static String getWifiIPv4() throws SocketException {
        String ipAddress = "";
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {

            // typische WLAN-Namen: wlan, wi-fi, wireless
            String name = ni.getDisplayName().toLowerCase();
            if (!ni.isUp() || ni.isLoopback() || !name.contains("wi")) {
                continue;
            }

            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    System.out.println("WLAN IPv4: " + addr.getHostAddress());
                    ipAddress=addr.getHostAddress();
                }
            }
        }
        return ipAddress;
    }
}