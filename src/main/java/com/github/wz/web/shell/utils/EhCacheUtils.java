package com.github.wz.web.shell.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@Slf4j
public final class EhCacheUtils {
    private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);

    public static Cache getCache() {
        return CACHE_MANAGER.getCache("myCache");
    }

    public static void put(String key, Object value) {
        try {
            Cache cache = getCache();
            cache.put(key, value);
        } catch (Exception e) {
            log.error("添加缓存失败：{}", e.getMessage());
        }
    }

    public static <T> T get(String key) {
        try {
            Cache cache = getCache();
            return (T) cache.get(key).get();
        } catch (Exception e) {
            log.error("获取缓存数据失败：", e);
            return null;
        }
    }

    public static void delete(String key) {
        try {
            Cache cache = getCache();
            cache.evict(key);
        } catch (Exception e) {
            log.error("删除缓存数据失败：", e);
        }
    }

    private EhCacheUtils() {
    }
}
