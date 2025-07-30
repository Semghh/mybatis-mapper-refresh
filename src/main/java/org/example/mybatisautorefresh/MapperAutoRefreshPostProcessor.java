package org.example.mybatisautorefresh;


import com.baomidou.mybatisplus.autoconfigure.SqlSessionFactoryBeanCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.lang.reflect.Field;


/**
 * @author SEMGHH
 * @date 2025/7/29 11:43
 */
@Slf4j
public class MapperAutoRefreshPostProcessor implements BeanPostProcessor, SqlSessionFactoryBeanCustomizer {

    MybatisSqlSessionFactoryBean factoryBean;

    MybatisConfiguration configuration;

    MapperWatchService mapperWatchService;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof SqlSessionFactory sqlSessionFactory) {
            org.apache.ibatis.session.Configuration c = sqlSessionFactory.getConfiguration();
            if (c instanceof MybatisConfiguration mybatisConfiguration) {
                configuration = mybatisConfiguration;
                mapperWatchService = new MapperWatchService(configuration);

                try {
                    Field resourceField = Permit.GetField(MybatisSqlSessionFactoryBean.class, "mapperLocations");
                    Resource[] mapperLocations = (Resource[]) resourceField.get(factoryBean);

                    for (Resource mapperLocation : mapperLocations) {
                        File file = mapperLocation.getFile();
                        mapperWatchService.WatchFile(file.getParentFile().toPath(), mapperLocation);
                    }
                } catch (Throwable e) {
                    log.error("mybatis-mapper-auto-refresh initialize fail",e);
                }
            }
        }
        return bean;
    }

    @Override
    public void customize(MybatisSqlSessionFactoryBean factoryBean) {
        this.factoryBean = factoryBean;
    }
}
