package org.sarge.jove.demo.model;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.sarge.jove.common.TransientNativeObject;
import org.sarge.jove.control.RenderLoop;
import org.sarge.jove.control.RenderLoop.Task;
import org.sarge.jove.io.ClasspathDataSource;
import org.sarge.jove.io.DataSource;
import org.sarge.jove.io.FileDataSource;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class ModelDemo {
	@Bean
	public static DataSource classpath() {
		return new ClasspathDataSource();
	}

	@Bean
	public static DataSource data(ApplicationConfiguration cfg) {
		return new FileDataSource(cfg.getDataDirectory());
	}

	@Bean
	public static RenderLoop loop(List<Task> tasks) {
		return new RenderLoop(tasks);
	}

	@Component
	static class ApplicationLoop implements CommandLineRunner {
		@Autowired private RenderLoop app;
		@Autowired private LogicalDevice dev;

		@Override
		public void run(String... args) throws Exception {
			app.run();
			dev.waitIdle();
		}
	}

	@Bean
	static DestructionAwareBeanPostProcessor destroyer() {
		return new DestructionAwareBeanPostProcessor() {
			@Override
			public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
				if(bean instanceof TransientNativeObject obj) {
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

// https://github.com/SaschaWillems/Vulkan/blob/master/examples/texturecubemap/texturecubemap.cpp
// https://www.humus.name/
