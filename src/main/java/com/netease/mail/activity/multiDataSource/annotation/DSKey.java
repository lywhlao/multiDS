package com.netease.mail.activity.multiDataSource.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * use the member to calculate hash key
 * only one member can be used this annotation
 * Created by hzlaojiaqi on 2017/12/26.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface DSKey {

    String value() default "";
}
