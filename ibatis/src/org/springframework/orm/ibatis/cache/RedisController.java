package org.springframework.orm.ibatis.cache;

import com.ibatis.sqlmap.engine.cache.CacheController;
import com.ibatis.sqlmap.engine.cache.CacheKey;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
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

    private static RedisTemplate<String, Object> redisTemplate;
    private static String IBATIS_ROOT = "ibatis";
    private static final String NULL_OBJECT = "SERIALIZABLE_NULL_OBJECT";


    /**
     * @see CacheController#getObject(CacheModel, Object)
     */
    @Override
    public Object getObject(CacheModel cacheModel, Object cacheKey) {
        String key = this.getKey(cacheModel, cacheKey);

        try {
            Object value = redisTemplate.opsForValue().get(key);
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
            redisTemplate.opsForValue().set(key, object, cacheModel.getFlushInterval(), TimeUnit.MILLISECONDS);

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
            Object value = redisTemplate.opsForValue().get(key);
            redisTemplate.delete(key);
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
            Set<String> keys = redisTemplate.keys(IBATIS_ROOT + ":" + cacheModel.getId() + "*");
            redisTemplate.delete(keys);

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
        try {
            if (cacheKey instanceof CacheKey) {
                String ckey = cacheKey.toString();
                ckey = ckey.replaceAll("^([\\-\\d]+\\|)([\\-\\d]+\\|)", "");
                ckey = ckey.replaceAll("^(.+\\|)*(\\w+\\.\\w+\\|)(\\d+\\|)(.*)", "$1$2$4");
                ckey = new BigInteger(1, MessageDigest.getInstance("MD5").digest(ckey.getBytes())).toString(16);

                String key = IBATIS_ROOT + ":" + cacheModel.getId() + ":" + ckey;

                if (logger.isDebugEnabled()) {
                    logger.debug("++++ ibatis缓存原始KEY:" + cacheKey + " +++");
                    logger.debug("++++ ibatis缓存去本地化的KEY:" + ckey + " +++");
                    logger.debug("++++ ibatis缓存最终使用的KEY:" + key + " +++");
                }

                return key;
            }

            return cacheKey.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置RedisConnectionFactory对象
     *
     * @param connectionFactory connectionFactory
     */
    public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
        RedisController.redisTemplate = new RedisTemplate<String, Object>();
        ;
        redisTemplate.setConnectionFactory(connectionFactory);

        //为key设置的序列化工具
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        //为value定制的序列化工具
        JdkSerializationRedisSerializer jdkSerializationRedisSerializer = new JdkSerializationRedisSerializer();
        redisTemplate.setValueSerializer(jdkSerializationRedisSerializer);
        redisTemplate.setHashValueSerializer(jdkSerializationRedisSerializer);

        redisTemplate.afterPropertiesSet();
    }

    /**
     * @param root
     */
    public void setRedisRootKey(String root) {
        if (!StringUtils.isEmpty(root))
            IBATIS_ROOT = root + ":" + IBATIS_ROOT;
    }

}
