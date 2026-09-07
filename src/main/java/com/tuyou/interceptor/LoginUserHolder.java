package com.tuyou.interceptor;

/**
 * 当前登录用户持有者（ThreadLocal）
 * 请求线程内存取 userId，请求结束必须 clear() 防止内存泄漏
 */
public class LoginUserHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
