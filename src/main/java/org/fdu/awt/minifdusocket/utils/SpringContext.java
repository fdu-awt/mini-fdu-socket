package org.fdu.awt.minifdusocket.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @author ZMark
 * @date 2024/5/15 上午10:46
 */
@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext context;

    /**
     * 设置应用上下文
     *
     * @param applicationContext 上下文
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        context = applicationContext;
    }
    
    public static ApplicationContext getApplicationContext() {
        return context;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}

