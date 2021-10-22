package org.sarge.jove.demo.model;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.sarge.jove.control.Application;
import org.sarge.jove.geometry.Matrix;
import org.sarge.jove.platform.desktop.Desktop;
import org.sarge.jove.platform.desktop.DesktopLibraryDevice.KeyListener;
import org.sarge.jove.platform.desktop.Window;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.util.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class ModelDemo {
	@Bean
	public static DataSource source() {
		return DataSource.of("./src/main/resources");
	}

//	@Bean
//	public static Runnable update(Matrix matrix, VulkanBuffer uniform, ApplicationConfiguration cfg) {
//		final long period = cfg.getPeriod();
//		final long start = System.currentTimeMillis();
//		return () -> {
//			final long time = System.currentTimeMillis() - start;
//			final float angle = (time % period) * MathsUtil.TWO_PI / period;
//			final Matrix h = Matrix.rotation(Vector.Y, angle);
//			final Matrix v = Matrix.rotation(Vector.X, MathsUtil.toRadians(30));
//			final Matrix model = h.multiply(v);
//			final Matrix m = matrix.multiply(model);
//			uniform.load(m);
//		};
//	}

	@Bean
	public static Runnable update(Matrix matrix, VulkanBuffer uniform) {
		return () -> {
			uniform.load(matrix);
		};
	}

	@Bean
	public static Application application(List<Runnable> tasks) {
		return new Application(tasks);
	}

	@Component
	static class ApplicationLoop implements CommandLineRunner {
		private final Application app;
		private final LogicalDevice dev;

		public ApplicationLoop(Application app, LogicalDevice dev) {
			this.app = app;
			this.dev = dev;
		}

		@Override
		public void run(String... args) throws Exception {
			app.run();
			dev.waitIdle();
		}
	}

	@Bean
	public static KeyListener listener(Window window, Application app) {
		// Create key listener
		final KeyListener listener = (ptr, key, scancode, action, mods) -> {
		    if(key == 256) {
		    	app.stop();
		    }
		};

		// Register listener
		final Desktop desktop = window.desktop();
		desktop.library().glfwSetKeyCallback(window.handle(), listener);

		return listener;
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws InterruptedException {
		ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
		SpringApplication.run(ModelDemo.class, args);
	}
}
