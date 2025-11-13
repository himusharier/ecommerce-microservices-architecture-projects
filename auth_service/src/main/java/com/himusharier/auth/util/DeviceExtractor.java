package com.himusharier.auth.util;

import jakarta.servlet.http.HttpServletRequest;

public class DeviceExtractor {

    public static String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown Device";
        }

        // Simple device detection
        if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
            if (userAgent.contains("Android")) {
                return "Android Mobile";
            } else if (userAgent.contains("iPhone")) {
                return "iPhone";
            } else if (userAgent.contains("iPad")) {
                return "iPad";
            }
            return "Mobile Device";
        } else if (userAgent.contains("Windows")) {
            return "Windows Desktop";
        } else if (userAgent.contains("Mac")) {
            return "Mac Desktop";
        } else if (userAgent.contains("Linux")) {
            return "Linux Desktop";
        }

        return "Unknown Device";
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // In case of multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }
}

