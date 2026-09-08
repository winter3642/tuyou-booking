package com.tuyou.interceptor;

/**
 * 当前登录用户持有者（ThreadLocal）
 * 请求线程内存取 userId/username，请求结束必须 clear() 防止内存泄漏
 */
public class LoginUserHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    public static void set(Long userId, String username) {
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    public static Long get() {
        return USER_ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}
