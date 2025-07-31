# Mybatis-mapper-auto-refresh

用于 "开发环境"。自动监听mapper.xml ，自动刷新其中的sql语句。

注意: 请勿用于"生产环境",仅可用于 “开发调试使用”。因为它是线程不安全的。




## 如何使用

4步：

1. 克隆本项目
```shell
git clone https://github.com/Semghh/mybatis-mapper-refresh.git
```

2. 打包构建本项目

```shell
    mvn clean install
```

3. 引入依赖
```xml
<dependency>
    <groupId>org.semghh</groupId>
    <artifactId>mapper-refresh-spring-boot-starter</artifactId>
    <version>x.x.x</version>
</dependency>
```

4. 修改ymal配置开启：
```yaml
#application.yaml
mybatis-mapper-auto-refresh:
  enable: true
```


## 使用说明

在开发环境中,mybatis实际使用的是 `${project_home}/target/`路径下编译后的XML文件资源。

为了开发方便,我们会通过target下的资源找到对应源码监听。
因此无需修改`${project_home}/target/`下的XML。



## 运行环境
需求环境：SpringBoot , Mybatis-plus ,lombok
