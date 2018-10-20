package com.netease.mail.activity.multiDataSource.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * multiData anno
 * Created by hzlaojiaqi on 2017/12/26.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseDataSource {

     /**
      * use member in method to calculate hash key then choose the dataSource
      *
      * @return
      */
     boolean memberHash() default false;



     /**
      * use spel expression to calculate hash key then choose the dataSource
      * @return
      */
     String  hashExp() default "";


     /**
      * assign a dataSource key,this key is in {@link com.netease.mail.activity.multiDataSource.aop.DataSourceAsp}
      * targetDataSource map
      * @return
      */
     String source() default "";


}
