package com.synq.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BaseEntity 의 @CreatedDate/@LastModifiedDate 를 채우려면 감사(auditing)를 활성화해야 한다.
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
