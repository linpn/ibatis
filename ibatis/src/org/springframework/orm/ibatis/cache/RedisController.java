package org.springframework.orm.ibatis.cache;

import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.cache.CacheKey;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Cache implementation for using Redis with iBATIS
 *
 * @author Linpn
 */
public class RedisController implements CacheController {

    protected final static Log logger = LogFactory.getLog(RedisController.class);

    private static RedisTemplate<String, Object> redis;
    private static final String IBATIS_ROOT = "ibatis";
    private static final String NULL_OBJECT = "SERIALIZABLE_NULL_OBJECT";


    /**
     * @see CacheController#getObject(CacheModel, Object)
     */
    @Override
    public Object getObject(CacheModel cacheModel, Object cacheKey) {
        String key = this.getKey(cacheModel, cacheKey);

        try {
            Object value = redis.opsForValue().get(key);
            if (NULL_OBJECT.equals(value))
                value = null;

            return value;

        } catch (Exception e) {
            logger.error("ibatis redis 获取键为" + key + "的缓存失败，系统将直接查询数据。 异常信息如下，请及时排查", e);
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
            redis.opsForValue().set(key, object, cacheModel.getFlushInterval(), TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            logger.error("ibatis redis 插入键为" + key + "的缓存失败。 异常信息如下，请及时排查", e);
        }
    }

    /**
     * @see CacheController#removeObject(CacheModel, Object)
     */
    @Override
    public Object removeObject(CacheModel cacheModel, Object cacheKey) {
        String key = this.getKey(cacheModel, cacheKey);

        try {
            Object value = redis.opsForValue().get(key);
            redis.delete(key);
            return value;

        } catch (Exception e) {
            logger.error("ibatis redis 删除键为" + key + "的缓存失败。 异常信息如下，请及时排查", e);
        }

        return null;
    }

    /**
     * @see CacheController#flush(CacheModel)
     */
    @Override
    public void flush(CacheModel cacheModel) {
        try {
            Set<String> keys = redis.keys(IBATIS_ROOT + ":" + cacheModel.getId());
            redis.delete(keys);

        } catch (Exception e) {
            logger.error("ibatis redis flush 清除缓存异常。 异常信息如下，请及时排查", e);
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

            String key = IBATIS_ROOT + ":" + cacheModel.getId() + ":" + DigestUtils.md5Hex(ckey);

            if (logger.isDebugEnabled()) {
                logger.debug("++++ ibatis缓存原始KEY:" + cacheKey + " +++");
                logger.debug("++++ ibatis缓存去本地化的KEY:" + ckey + " +++");
                logger.debug("++++ ibatis缓存最终使用的KEY:" + key + " +++");
            }

            return key;
        }

        return cacheKey.toString();
    }


    /**
     * 获取RedisTemplate对象
     *
     * @return RedisTemplate
     */
    public RedisTemplate<String, Object> getRedis() {
        return RedisController.redis;
    }

    /**
     * 设置RedisTemplate对象
     *
     * @param redis RedisTemplate
     */
    public void setRedis(RedisTemplate<String, Object> redis) {
        RedisController.redis = redis;
    }

}
