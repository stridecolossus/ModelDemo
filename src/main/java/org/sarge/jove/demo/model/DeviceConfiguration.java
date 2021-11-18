package org.sarge.jove.demo.model;

import org.sarge.jove.platform.vulkan.VkCommandPoolCreateFlag;
import org.sarge.jove.platform.vulkan.VkQueueFlag;
import org.sarge.jove.platform.vulkan.common.Queue;
import org.sarge.jove.platform.vulkan.core.Command.Pool;
import org.sarge.jove.platform.vulkan.core.Instance;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.PhysicalDevice;
import org.sarge.jove.platform.vulkan.core.PhysicalDevice.Selector;
import org.sarge.jove.platform.vulkan.core.Surface;
import org.sarge.jove.platform.vulkan.core.VulkanLibrary;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.Allocator;
import org.sarge.jove.platform.vulkan.memory.MemorySelector;
import org.sarge.jove.platform.vulkan.util.ValidationLayer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DeviceConfiguration {
	private final Selector graphics = Selector.of(VkQueueFlag.GRAPHICS);
	private final Selector presentation;

	public DeviceConfiguration(Surface surface) {
		presentation = Selector.of(surface);
	}

	@Bean
	public PhysicalDevice physical(Instance instance) {
		return PhysicalDevice
				.devices(instance)
				.filter(graphics)
				.filter(presentation)
				.findAny()
				.orElseThrow(() -> new RuntimeException("No suitable physical device available"));
	}

	@Bean
	public LogicalDevice device(PhysicalDevice dev) {
		return new LogicalDevice.Builder(dev)
				.extension(VulkanLibrary.EXTENSION_SWAP_CHAIN)
				.layer(ValidationLayer.STANDARD_VALIDATION)
				.queue(graphics.family())
				.queue(presentation.family())
				.build();
	}

	private static Pool pool(LogicalDevice dev, Selector selector) {
		final Queue queue = dev.queue(selector.family());
		return Pool.create(dev, queue, VkCommandPoolCreateFlag.RESET_COMMAND_BUFFER); // TODO
	}

	@Bean
	public Pool graphics(LogicalDevice dev) {
		return pool(dev, graphics);
	}

	@Bean
	public Pool presentation(LogicalDevice dev) {
		return pool(dev, presentation);
	}

	@Bean
	public static MemorySelector selector(LogicalDevice dev) {
		return MemorySelector.create(dev);
	}

	@Bean
	public static Allocator allocator(LogicalDevice dev) {
		final Allocator allocator = Allocator.allocator(dev);
		// TODO - pagination, pool, expanding
		//return new PoolAllocator(allocator, Integer.MAX_VALUE);		// TODO
		return allocator;
	}

	@Bean
	public static AllocationService service(MemorySelector selector, Allocator allocator) {
		return new AllocationService(selector, allocator);
	}
}
