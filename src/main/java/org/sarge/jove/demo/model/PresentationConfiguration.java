package org.sarge.jove.demo.model;

import java.util.*;

import org.sarge.jove.common.Handle;
import org.sarge.jove.control.Frame;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.image.View;
import org.sarge.jove.platform.vulkan.render.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;

@Configuration
class PresentationConfiguration {
	@Autowired private ApplicationConfiguration cfg;

	@Bean
	public static Surface surface(Handle surface, PhysicalDevice dev) {
		return new Surface(surface, dev).cached();
	}

	@Bean
	public Swapchain swapchain(LogicalDevice dev, Surface surface) {
	    // Select presentation mode
	    final VkPresentModeKHR mode = surface.mode(VkPresentModeKHR.MAILBOX_KHR);

	    // Select SRGB surface format
	    final VkSurfaceFormatKHR format = surface.format(VkFormat.B8G8R8_UNORM, VkColorSpaceKHR.SRGB_NONLINEAR_KHR, null);

	    return new Swapchain.Builder(surface)
				.count(cfg.getFrameCount())
				.presentation(mode)
				.format(format)
				.clear(cfg.getBackground())
				.build(dev);
	}

	@Bean
	static FrameBuffer.Group frames(Swapchain swapchain, RenderPass pass, View depth) {
		return new FrameBuffer.Group(swapchain, pass, List.of(depth));
	}

	@Bean
	static FrameBuilder builder(FrameBuffer.Group frames, @Qualifier("graphics") Command.Pool pool) {
		return new FrameBuilder(frames::buffer, pool::allocate, VkCommandBufferUsage.ONE_TIME_SUBMIT);
	}

	@Bean
	FrameProcessor processor(Swapchain swapchain, FrameBuilder builder, Collection<Frame.Listener> listeners) {
		final var proc = new FrameProcessor(swapchain, builder, cfg.getFrameCount());
		listeners.forEach(proc::add);
		return proc;
	}
}
