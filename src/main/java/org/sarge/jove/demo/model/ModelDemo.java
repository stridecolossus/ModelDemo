package org.sarge.jove.demo.model;

import java.nio.file.Paths;
import java.util.concurrent.*;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.builder.*;
import org.sarge.jove.common.TransientObject;
import org.sarge.jove.io.*;
import org.sarge.jove.platform.desktop.Desktop;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ModelDemo {
	@Autowired private LogicalDevice dev;

	@Bean
	public static DataSource classpath() {
		return new ClasspathDataSource();
	}

	@Bean
	public static DataSource data() {
		return FileDataSource.home(Paths.get("workspace/Demo/Data"));
	}

	@Bean
	public static ScheduledExecutorService executor() {
		return Executors.newSingleThreadScheduledExecutor();
	}

	@Bean
	static CommandLineRunner runner(Desktop desktop) {
		return args -> {
			while(true) {
				desktop.poll();
			}
		};
	}

	@PreDestroy
	void destroy() {
		dev.waitIdle();
	}

	@Bean
	static DestructionAwareBeanPostProcessor destroyer() {
		return new DestructionAwareBeanPostProcessor() {
			@Override
			public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
				if(bean instanceof TransientObject obj && obj.isDestroyed()) {
					obj.destroy();
				}
			}
		};
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws InterruptedException {
		ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);

		new SpringApplicationBuilder(ModelDemo.class)
				.headless(false)
				.run(args);
	}
}

