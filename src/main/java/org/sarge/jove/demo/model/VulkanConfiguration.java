package org.sarge.jove.demo.model;

import org.sarge.jove.platform.desktop.Desktop;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.util.ValidationLayer;
import org.springframework.context.annotation.*;

@Configuration
class VulkanConfiguration {
	@Bean
	public static VulkanLibrary library() {
		return VulkanLibrary.create();
	}

	@Bean
	public static Instance instance(VulkanLibrary lib, Desktop desktop, ApplicationConfiguration cfg) {
		return new Instance.Builder()
				.name(cfg.getTitle())
				.extension(VulkanLibrary.EXTENSION_DEBUG_UTILS)
				.extensions(desktop.extensions())
				.layer(ValidationLayer.STANDARD_VALIDATION)
				.build(lib);
	}

	@Bean
	static Handler diagnostics(Instance instance) {
		return instance.handler().build();
	}
}
