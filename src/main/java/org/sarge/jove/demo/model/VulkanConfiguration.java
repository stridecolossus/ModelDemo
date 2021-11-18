package org.sarge.jove.demo.model;

import org.sarge.jove.platform.desktop.Desktop;
import org.sarge.jove.platform.vulkan.core.Instance;
import org.sarge.jove.platform.vulkan.core.VulkanLibrary;
import org.sarge.jove.platform.vulkan.util.ValidationLayer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VulkanConfiguration {
	@Bean
	public static VulkanLibrary library() {
		return VulkanLibrary.create();
	}

	@Bean
	public static Instance instance(VulkanLibrary lib, Desktop desktop, ApplicationConfiguration cfg) {
		// Create instance
		final Instance instance = new Instance.Builder()
				.name(cfg.getTitle())
				.extension(VulkanLibrary.EXTENSION_DEBUG_UTILS)
				.extensions(desktop.extensions())
				.layer(ValidationLayer.STANDARD_VALIDATION)
				.build(lib);

		// Attach diagnostics handler
		instance
				.manager()
				.builder()
				.init()
				.build();

		return instance;
	}
}
