package com.netease.mail.activity.multiDataSource.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 *
 * change data source
 *
 * Created by hzlaojiaqi on 2017/10/16.
 */
public class DataSourceSwitcher extends AbstractRoutingDataSource{

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceSwitcher.class);

    private static final ThreadLocal<String> dataSourceKey = new ThreadLocal<String>();


    public static void clearDataSourceType() {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("thread:{},remove,dataSource:{}", Thread.currentThread().getName());
        }
        dataSourceKey.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String s = dataSourceKey.get();
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("thread:{},determine,dataSource:{}", Thread.currentThread().getName(), s);
        }
        return s;
    }

    public static void setDataSourceKey(String dataSource) {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("thread:{},set,dataSource:{}", Thread.currentThread().getName(), dataSource);
        }
        dataSourceKey.set(dataSource);
    }

    public static String getDataSourceKey() {
        String s = dataSourceKey.get();
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("thread:{},get,dataSource:{}", Thread.currentThread().getName(), s);
        }
        return s;
    }
}
