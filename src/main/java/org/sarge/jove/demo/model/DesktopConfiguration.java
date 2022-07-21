package org.sarge.jove.demo.model;

import org.sarge.jove.common.*;
import org.sarge.jove.platform.desktop.*;
import org.sarge.jove.platform.vulkan.core.Instance;
import org.sarge.jove.scene.RenderLoop.Task;
import org.springframework.context.annotation.*;

@Configuration
class DesktopConfiguration {
	@Bean
	public static Desktop desktop() {
		final Desktop desktop = Desktop.create();
		desktop.setErrorHandler(System.err::println);
		if(!desktop.isVulkanSupported()) throw new RuntimeException("Vulkan not supported");
		return desktop;
	}

	@Bean
	public static Window window(Desktop desktop, ApplicationConfiguration cfg) {
		return new Window.Builder()
				.title(cfg.getTitle())
				.size(new Dimensions(1024, 768))
				.hint(Window.Hint.DISABLE_OPENGL)
				.build(desktop);
	}

	@Bean
	public static Handle surface(Instance instance, Window window) {
		return window.surface(instance.handle());
	}

	@Bean
	public static Task poll(Desktop desktop) {
		return desktop::poll;
	}
}
