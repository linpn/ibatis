package org.springframework.orm.ibatis.cache;

import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.cache.CacheKey;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Cache implementation for using Memcached with iBATIS
 *
 * @author Linpn
 */
public class MemcachedController implements CacheController {

    protected final static Log logger = LogFactory.getLog(MemcachedController.class);

    private static MemcachedClient memcached;
    private static final String IBATIS_MEMCACHED_KEYS = "IBATIS_MEMCACHED_KEYS";
    private static final String IBATIS_MEMCACHED_DATA = "IBATIS_MEMCACHED_DATA";
    private static final String NULL_OBJECT = "SERIALIZABLE_NULL_OBJECT";


    /**
     * @see CacheController#getObject(CacheModel, Object)
     */
    @Override
    public Object getObject(CacheModel cacheModel, Object cacheKey) {
        String key = this.getKey(cacheModel, cacheKey);

        try {
            Object value = memcached.get(key);
            if (NULL_OBJECT.equals(value))
                value = null;

            //判断keyset有没有这个key，如果没有，如果没有就加入这个key，避免缓存泄漏无法实时清除的问题
            if (value != null && !this.hasKey(cacheModel, key)) {
                this.putKey(cacheModel, key);
            }

            return value;

        } catch (Exception e) {
            logger.error("ibatis memcached 获取键为" + key + "的缓存失败，系统将直接查询数据。 异常信息如下，请及时排查", e);
        }

        return null;
    }

    /**
     * @see CacheController#putObject(CacheModel, Object, Object)
     */
    @Override
    public void putObject(CacheModel cacheModel, Object cacheKey, Object object) {
        String key = this.getKey(cacheModel, cacheKey);

        try {
            memcached.set(key, (int) (cacheModel.getFlushInterval() / 1000), object);
            this.putKey(cacheModel, key);

        } catch (Exception e) {
            logger.error("ibatis memcached 插入键为" + key + "的缓存失败。 异常信息如下，请及时排查", e);
        }
    }

    /**
     * @see CacheController#removeObject(CacheModel, Object)
     */
    @Override
    public Object removeObject(CacheModel cacheModel, Object cacheKey) {
        String key = this.getKey(cacheModel, cacheKey);

        try {
            Object value = memcached.get(key);
            if (value == null) {
                this.removeKey(cacheModel, key);
            } else {
                //删除不成功，重试3次
                for (int i = 0; i < 3; i++) {
                    if (memcached.delete(key)) {
                        this.removeKey(cacheModel, key);
                        break;
                    } else {
                        logger.error("ibatis memcached 键为" + key + "的缓存删除不成功，正在重试 " + i + "...");
                        Thread.sleep(500);
                    }
                }
            }

            return value;

        } catch (Exception e) {
            logger.error("ibatis memcached 删除键为" + key + "的缓存失败。 异常信息如下，请及时排查", e);
        }

        return null;
    }

    /**
     * @see CacheController#flush(CacheModel)
     */
    @Override
    public void flush(CacheModel cacheModel) {
        try {
            Set<String> keySet = this.getKeySet(cacheModel);

            if (keySet != null) {
                //如果Mencached删除成功，则移除key
                for (Iterator<String> it = keySet.iterator(); it.hasNext(); ) {
                    String key = it.next();
                    this.removeObject(cacheModel, key);
                }
            }

        } catch (Exception e) {
            logger.error("ibatis memcached flush 清除缓存异常。 异常信息如下，请及时排查", e);
        }
    }

    /**
     * @see CacheController#setProperties(Properties)
     */
    @Override
    public void setProperties(Properties props) {
    }


    /**
     * 获取转化后的key， 如果参数cacheKey为已经转化过的key，则直接返回
     */
    private String getKey(CacheModel cacheModel, Object cacheKey) {
        if (cacheKey instanceof CacheKey) {
            String ckey = cacheKey.toString();
            ckey = ckey.replaceAll("^([\\-\\d]+\\|)([\\-\\d]+\\|)", "");
            ckey = ckey.replaceAll("^(.+\\|)*(\\w+\\.\\w+\\|)(\\d+\\|)(.*)", "$1$2$4");

            String key = IBATIS_MEMCACHED_DATA + "|" + cacheModel.getId() + "|" + DigestUtils.md5Hex(ckey);

            if (logger.isDebugEnabled()) {
                logger.debug("++++ ibatis缓存原始KEY:" + cacheKey + " +++");
                logger.debug("++++ ibatis缓存去本地化的KEY:" + ckey + " +++");
                logger.debug("++++ ibatis缓存最终使用的KEY:" + key + " +++");
            }

            return key;
        }

        return cacheKey.toString();
    }

    private void putKey(CacheModel cacheModel, Object cacheKey) throws TimeoutException, InterruptedException, MemcachedException {
        Set<String> keySet = this.getKeySet(cacheModel);

        if (keySet == null) {
            keySet = new HashSet<String>();
        }

        String key = this.getKey(cacheModel, cacheKey);
        keySet.add(key);
        this.setKeySet(cacheModel, keySet);
    }

    private void removeKey(CacheModel cacheModel, Object cacheKey) throws TimeoutException, InterruptedException, MemcachedException {
        Set<String> keySet = this.getKeySet(cacheModel);

        if (keySet != null) {
            String key = this.getKey(cacheModel, cacheKey);
            for (Iterator<String> it = keySet.iterator(); it.hasNext(); ) {
                if (key.equals(it.next())) {
                    it.remove();
                    this.setKeySet(cacheModel, keySet);
                    break;
                }
            }
        }
    }

    private boolean hasKey(CacheModel cacheModel, Object cacheKey) throws InterruptedException, MemcachedException, TimeoutException {
        String key = this.getKey(cacheModel, cacheKey);
        Set<String> keySet = this.getKeySet(cacheModel);

        if (keySet != null) {
            for (Iterator<String> it = keySet.iterator(); it.hasNext(); ) {
                if (key.equals(it.next())) {
                    return true;
                }
            }
        }

        return false;
    }

    private Set<String> getKeySet(CacheModel cacheModel) throws InterruptedException, MemcachedException, TimeoutException {
        Set<String> keySet = memcached.get(IBATIS_MEMCACHED_KEYS + "|" + cacheModel.getId());
        return keySet;
    }

    private void setKeySet(CacheModel cacheModel, Set<String> keySet) throws InterruptedException, MemcachedException, TimeoutException {
        memcached.set(IBATIS_MEMCACHED_KEYS + "|" + cacheModel.getId(), 0, keySet);
    }


    /**
     * 获取MemcachedClient对象
     *
     * @return MemcachedClient
     */
    public MemcachedClient getMemcached() {
        return MemcachedController.memcached;
    }

    /**
     * 设置MemcachedClient对象
     *
     * @param memcached MemcachedClient
     */
    public void setMemcached(MemcachedClient memcached) {
        MemcachedController.memcached = memcached;
    }

}
