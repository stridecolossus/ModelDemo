package org.sarge.jove.demo.model;

import org.sarge.jove.common.Handle;
import org.sarge.jove.platform.vulkan.VkQueueFlag;
import org.sarge.jove.platform.vulkan.common.Queue;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.core.LogicalDevice.RequiredQueue;
import org.sarge.jove.platform.vulkan.core.PhysicalDevice.Selector;
import org.sarge.jove.platform.vulkan.util.*;
import org.springframework.context.annotation.*;

@Configuration
class DeviceConfiguration {
	private final Selector graphics = Selector.of(VkQueueFlag.GRAPHICS);
	private final Selector presentation;
	private final DeviceFeatures features;

	public DeviceConfiguration(Handle surface, ApplicationConfiguration cfg) {
		this.presentation = Selector.of(surface);
		this.features = DeviceFeatures.of(cfg.getFeatures());
	}

	@Bean
	public PhysicalDevice physical(Instance instance) {
		return new PhysicalDevice.Enumerator(instance)
				.devices()
				.filter(graphics)
				.filter(presentation)
				.filter(PhysicalDevice.Enumerator.features(features))
				.findAny()
				.orElseThrow(() -> new RuntimeException("No suitable physical device available"));
	}

	@Bean
	public LogicalDevice device(PhysicalDevice dev) {
		return new LogicalDevice.Builder(dev)
				.extension(VulkanLibrary.EXTENSION_SWAP_CHAIN)
				.layer(ValidationLayer.STANDARD_VALIDATION)
				.queue(new RequiredQueue(graphics.select(dev)))
				.queue(new RequiredQueue(presentation.select(dev)))
				.features(features)
				.build();
	}

	private static Command.Pool pool(LogicalDevice dev, Selector selector) {
		final Queue.Family family = selector.select(dev.parent());
		final Queue queue = dev.queue(family);
		return Command.Pool.create(dev, queue);
	}

	@Bean
	public Command.Pool graphics(LogicalDevice dev) {
		return pool(dev, graphics);
	}

	@Bean
	public Command.Pool presentation(LogicalDevice dev) {
		return pool(dev, presentation);
	}
}
