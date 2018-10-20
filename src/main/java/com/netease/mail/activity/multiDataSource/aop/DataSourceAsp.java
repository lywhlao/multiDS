package com.netease.mail.activity.multiDataSource.aop;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netease.mail.activity.multiDataSource.annotation.DSKey;
import com.netease.mail.activity.multiDataSource.annotation.UseDataSource;
import com.netease.mail.activity.multiDataSource.util.CustomSpelParser;
import com.netease.mail.activity.multiDataSource.util.DataSourceSwitcher;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Multiple DataSource Aspect
 * Created by hzlaojiaqi on 2017/12/26.
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class DataSourceAsp implements InitializingBean {

    /**
     * dataSourceSwitcher
     */
    private AbstractRoutingDataSource mDataSourceSwitcher;
    /**
     * DataSource List
     */
    private List<String> mDataSourceKeys = new CopyOnWriteArrayList<>();

    /**
     * mDataSourceKeys's size
     */
    private int mKeySize;
    /**
     * local cache for speed up to find @DsKey parameter's position
     */
    private static Cache<String, Integer> LOCAL_CACHE = CacheBuilder
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(50)
            .build();


    @Override
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() throws Exception {
        Preconditions.checkArgument(this.mDataSourceSwitcher != null, "mDataSourceSwitcher is null");
        Field resolvedDataSources = FieldUtils.getField(mDataSourceSwitcher.getClass(), "resolvedDataSources", true);
        Preconditions.checkArgument(resolvedDataSources != null, "resolvedDataSources is null");
        Map<Object, DataSource> dataSourceMap = (Map<Object, DataSource>) resolvedDataSources.get(mDataSourceSwitcher);
        dataSourceMap.forEach((k, v) -> {
            this.mDataSourceKeys.add(String.valueOf(k));
        });
        this.mKeySize = mDataSourceKeys.size();
        Preconditions.checkArgument(mKeySize != 0, "dataSource size is 0!");
    }


    /**
     * pointcut
     */
    @Pointcut("@annotation(com.netease.mail.activity.multiDataSource.annotation.UseDataSource)")
    public void useDataSource() {
    }

    /**
     * core method
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("useDataSource() && @annotation(anno)")
    public Object dataSourceSwitcher(ProceedingJoinPoint joinPoint, UseDataSource anno) throws Throwable {
        //1.get dataSource
        String ds = getDsKey(anno, joinPoint);
        //2.save origin dataSource
        String originTag = DataSourceSwitcher.getDataSourceKey();
        //3.change dataSource
        DataSourceSwitcher.setDataSourceKey(ds);
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            //4.reset origin dataSource
            DataSourceSwitcher.setDataSourceKey(originTag);
        }
    }


    /**
     * get key from joinPoint for hash
     *
     * @param joinPoint
     * @return
     */
    public String getHashKeyFromMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = MethodSignature.class.cast(joinPoint.getSignature());
        Method method = signature.getMethod();
        Integer positionFromCache = getPositionFromCache(method);
        Object[] args = joinPoint.getArgs();
        if (positionFromCache != null) {
            return String.valueOf(args[positionFromCache]);
        }
        Parameter[] declaredFields = method.getParameters();
        int index = 0;
        for (Parameter temp : declaredFields) {
            Annotation[] annotations = temp.getAnnotations();
            for (Annotation anTemp : annotations) {
                if (anTemp instanceof DSKey) {
                    putToCache(method, index);
                    return String.valueOf(args[index]);
                }
            }
            index++;
        }
        throw new IllegalArgumentException("can not get field with @DsKey annotation");
    }

    /**
     * get DataSource Key
     *
     * @param anno
     * @param joinPoint
     * @return
     */
    private String getDsKey(UseDataSource anno, ProceedingJoinPoint joinPoint) {
        String ds = "";
        String source = anno.source();
        String spel = anno.hashExp();
        //use method member
        if (anno.memberHash()) {
            int i = Math.abs(getHashKeyFromMethod(joinPoint).hashCode()) % mKeySize;
            ds = mDataSourceKeys.get(i);
        }
        // use spel expression
        else if (!StringUtils.isEmpty(spel)) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String dynamicValue = CustomSpelParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), spel);
            int i = Math.abs(dynamicValue.hashCode()) % mKeySize;
            ds = mDataSourceKeys.get(i);
            // direct use dataSource
        } else if (!StringUtils.isEmpty(source)) {
            ds = source;
            if (!isInKeyList(ds)) {
                throw new IllegalArgumentException(String.format("dataSource key %s is not in key List %s", ds, mDataSourceKeys));
            }
        }
        if (StringUtils.isEmpty(ds)) {
            throw new IllegalArgumentException("dataSource is empty!");
        }
        return ds;
    }


    /**
     * check dataSource is in source list
     *
     * @param ds
     * @return false not in,otherwise true
     */
    private boolean isInKeyList(String ds) {
        Preconditions.checkArgument(mDataSourceKeys != null, "mDataSourceKeys is not init!");
        for (String temp : mDataSourceKeys) {
            if (temp.equals(ds)) {
                return true;
            }
        }
        return false;
    }

    /**
     * put position to cache
     *
     * @param method
     * @param pos
     */
    private void putToCache(Method method, Integer pos) {
        LOCAL_CACHE.put(method.toString(), pos);
    }


    /**
     * get position from cache
     *
     * @param method
     */
    private Integer getPositionFromCache(Method method) {
        Integer value = LOCAL_CACHE.getIfPresent(method.toString());
        return value;
    }

    public void setmDataSourceSwitcher(AbstractRoutingDataSource mDataSourceSwitcher) {
        this.mDataSourceSwitcher = mDataSourceSwitcher;
    }


}
