package org.example.mybatisautorefresh;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author SEMGHH
 * @date 2025/7/30 17:03
 */
@Configuration
@ConditionalOnProperty(value = "mybatis-mapper-auto-refresh.enable", havingValue = "true")
@Import(MapperAutoRefreshPostProcessor.class)
public class MapperRefreshAutoConfiguration {

}
