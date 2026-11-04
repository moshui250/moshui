package com.codeagent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 原生 MyBatis 配置：只扫描 Mapper，分页由业务 SQL LIMIT 控制。
 */
@Configuration
@MapperScan("com.codeagent.mapper")
public class MybatisConfig {
}
