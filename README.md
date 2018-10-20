# 多数据源中间件
这是我的blog，https://blog.csdn.net/laojiaqi/article/details/78964862

### 1. 配置
#### 1.1 添加依赖(暂时不可用，等我上传到maven库)
```
<dependency>
    <groupId>com.netease.mail.act</groupId>
    <artifactId>MultiDataSource</artifactId>
    <version>1.0</version>
</dependency>
```
#### 1.2 整合多数据源
- 假设当前有2个数据源`childDataSource1`和`childDataSource2`

	``` java
	<bean id="childDataSource1" parent="dataSource">
		<property name="url" value="${jdbc.url1}" />
	</bean>
	
	<bean id="childDataSource2" parent="dataSource">
		<property name="url" value="${jdbc.url2}" />
	</bean>
	
	```
- **配置数据源切换类**，将上面的数据源放入targetDataSources这个map中，key值可以自定义
``` java
	<bean id="dataSourceSwitcher" class="com.netease.mail.activity.multiDataSource.util.DataSourceSwitcher">
		<property name="targetDataSources">
			<map>
				<entry key="ds1" value-ref="childDataSource1"/>
				<entry key="ds2" value-ref="childDataSource2"/>
			</map>
		</property>
		<property name="defaultTargetDataSource" ref="childDataSource1"/>
	</bean>
```
- **配置数据源切面**
``` java
	<bean id="multiDataSourceAsp" class="com.netease.mail.activity.multiDataSource.aop.DataSourceAsp">
	        <property name="mDataSourceSwitcher" ref="dataSourceSwitcher"/>
	</bean>
```

### 2.使用
有`四种`指定数据源的方式
- 第一种：**直接指定**数据源,使用方法如下所示,直接在source上指定数据源的key值即可
``` java
    @UseDataSource(source = "ds1")
    public AjaxResult insert(@RequestParam String uid){
        return new AjaxResult(RetCode.SUCCESS);
    }
```

- 第二种:使用方法的**成员变量**(使用@DSKey进行标注)作为hash值，进行计算后再选中数据源。注意，目前只支持单个参数。
``` java
   @UseDataSource(memberHash = true)
   public AjaxResult insert2(@RequestParam @DSKey String uid){
       return new AjaxResult(RetCode.SUCCESS);
   }
```
- 第三种:**使用spel表达式**作为hash值进行计算，然后再选择数据源
``` java
    @UseDataSource(hashExp = "'test_'+#uid")
    public AjaxResult insert3(@RequestParam String uid){
        return new AjaxResult(RetCode.SUCCESS);
    }
```
上述例子中，如果uid的值为1,则计算test_1的hash值，再选择数据源
- 第四种:显示指定数据源，通过调用`DataSourceSwitcher`类中的`setDataSourceKey`方法
``` java
    DataSourceSwitcher.setDataSourceKey(dsKey);//该key为targetDataSources中的key值
```
上述例子中，可以使用`DataSourceSwitcher.setDataSourceKey("ds1")`，将数据源切换到childDataSource1
## 3.注意事项
- 若对同一个方法同时指定了3种数据源指定方式，则按照次序 **成员变量>spel表达式>直接指定** 的方式判断
- 需启用切面的注解支持
```
    <context:annotation-config/>
    <aop:aspectj-autoproxy/>
```
