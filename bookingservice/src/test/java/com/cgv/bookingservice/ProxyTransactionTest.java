package com.cgv.bookingservice;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import static org.junit.jupiter.api.Assertions.*;

class ProxyTransactionTest {
    @Configuration @EnableTransactionManagement(proxyTargetClass = true)
    static class Config {
        @Bean DriverManagerDataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:proxy;DB_CLOSE_DELAY=-1", "sa", ""); }
        @Bean JdbcTemplate jdbc(DriverManagerDataSource ds) { JdbcTemplate j=new JdbcTemplate(ds); j.execute("create table if not exists marker(id int)"); j.update("delete from marker"); return j; }
        @Bean PlatformTransactionManager transactionManager(DriverManagerDataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean DemoService demoService(JdbcTemplate jdbc) { return new DemoService(jdbc); }
    }
    static class DemoService {
        final JdbcTemplate jdbc; DemoService(JdbcTemplate j) { jdbc=j; }
        @Transactional public void writeThenFail() { jdbc.update("insert into marker values(1)"); throw new IllegalStateException("forced rollback"); }
        public void selfInvocation() { try { this.writeThenFail(); } catch (IllegalStateException ignored) {} }
    }
    @Test void proxyRollsBackButSelfInvocationBypassesTransaction() {
        try (var context=new AnnotationConfigApplicationContext(Config.class)) {
            DemoService bean=context.getBean(DemoService.class); JdbcTemplate jdbc=context.getBean(JdbcTemplate.class);
            assertTrue(AopUtils.isCglibProxy(bean));
            assertThrows(IllegalStateException.class,bean::writeThenFail);
            assertEquals(0,jdbc.queryForObject("select count(*) from marker",Integer.class));
            bean.selfInvocation();
            assertEquals(1,jdbc.queryForObject("select count(*) from marker",Integer.class));
        }
    }
}
