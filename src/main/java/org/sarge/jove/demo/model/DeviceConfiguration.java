package org.sarge.jove.demo.model;

import org.sarge.jove.common.Handle;
import org.sarge.jove.platform.vulkan.VkQueueFlag;
import org.sarge.jove.platform.vulkan.common.Queue;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.core.LogicalDevice.RequiredQueue;
import org.sarge.jove.platform.vulkan.core.PhysicalDevice.Selector;
import org.sarge.jove.platform.vulkan.memory.*;
import org.sarge.jove.platform.vulkan.render.Surface;
import org.sarge.jove.platform.vulkan.util.*;
import org.springframework.context.annotation.*;

@Configuration
class DeviceConfiguration {
	private final Selector graphics = Selector.of(VkQueueFlag.GRAPHICS);
	private final Selector presentation;

	public DeviceConfiguration(Handle surface) {
		presentation = Selector.of(surface);
	}

	@Bean
	static DeviceFeatures features(ApplicationConfiguration cfg) {
		return DeviceFeatures.of(cfg.getFeatures());
	}

	@Bean
	public PhysicalDevice physical(Instance instance, DeviceFeatures features) {
		return new PhysicalDevice.Enumerator(instance)
				.devices()
				.filter(graphics)
				.filter(presentation)
				.filter(PhysicalDevice.Enumerator.features(features))
				.findAny()
				.orElseThrow(() -> new RuntimeException("No suitable physical device available"));
	}

	@Bean
	public static Surface surface(Handle surface, PhysicalDevice dev) {
		return new Surface(surface, dev);
	}

	@Bean
	public LogicalDevice device(PhysicalDevice dev, DeviceFeatures features) {
		return new LogicalDevice.Builder(dev)
				.extension(VulkanLibrary.EXTENSION_SWAP_CHAIN)
				.layer(ValidationLayer.STANDARD_VALIDATION)
				.queue(new RequiredQueue(graphics.select(dev)))
				.queue(new RequiredQueue(presentation.select(dev)))
				.features(features)
				.build();
	}

	@Bean
	public Queue graphics(LogicalDevice dev) {
		return dev.queue(graphics.select(dev.parent()));
	}

	@Bean
	public Queue presentation(LogicalDevice dev) {
		return dev.queue(presentation.select(dev.parent()));
	}

	@Bean
	public static Command.Pool transfer(LogicalDevice dev, Queue graphics) {
		return Command.Pool.create(dev, graphics);
	}

	@Bean
	public static MemorySelector selector(LogicalDevice dev) {
		return MemorySelector.create(dev);
	}

	@Bean
	public static Allocator allocator(LogicalDevice dev) {
		final Allocator allocator = new DefaultAllocator(dev);
		// TODO - pagination, pool, expanding
		//return new PoolAllocator(allocator, Integer.MAX_VALUE);		// TODO
		return allocator;
	}

	@Bean
	public static AllocationService service(MemorySelector selector, Allocator allocator) {
		return new AllocationService(selector, allocator);
	}
}
