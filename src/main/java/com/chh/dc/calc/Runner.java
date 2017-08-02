package com.chh.dc.calc;

import com.chh.dc.calc.trigger.TriggerManager;
import com.chh.dc.calc.task.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Created by Niow on 2016/6/27.
 */
@Configuration
@EnableAutoConfiguration
@ImportResource({"classpath:spring.xml"})
public class Runner {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        log.info("OBD数据汇总启动");
        applicationContext = SpringApplication.run(Runner.class, args);
        TriggerManager triggerManager = (TriggerManager)applicationContext.getBean("triggerManager");
        triggerManager.start();
        TaskManager taskManager = (TaskManager)applicationContext.getBean("taskManager");
        taskManager.start();
    }

    public static final <T> T getBean(String beanName, Class<T> clazz) {
        return applicationContext.getBean(beanName, clazz);
    }

}
