package org.sarge.jove.demo.model;

import org.sarge.jove.control.Frame;
import org.sarge.jove.geometry.*;
import org.sarge.jove.geometry.Rotation.AxisAngle;
import org.sarge.jove.platform.desktop.*;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.render.*;
import org.sarge.jove.scene.*;
import org.sarge.jove.util.MathsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

@Configuration
public class CameraConfiguration {
	private final Matrix projection;
	private final Camera cam = new Camera();
	private final OrbitalCameraController controller;

	public CameraConfiguration(Swapchain swapchain) {
		projection = Projection.DEFAULT.matrix(0.1f, 100, swapchain.extents());
		controller = new OrbitalCameraController(cam, swapchain.extents());
		controller.radius(3);
		controller.scale(0.25f);
	}

	@Autowired
	void stop(Window window) {
		window.keyboard().keyboard().bind(e -> System.exit(0));
	}

	@Autowired
	void controller(Window window) {
		final MouseDevice mouse = window.mouse();
		mouse.pointer().bind(controller::update);
		mouse.wheel().bind(controller::zoom);
	}

	@Bean
	public ResourceBuffer uniform(LogicalDevice dev) {
		final var props = new MemoryProperties.Builder<VkBufferUsageFlag>()
				.usage(VkBufferUsageFlag.UNIFORM_BUFFER)
				.required(VkMemoryProperty.HOST_VISIBLE)
				.build();

		final long len = Matrix.IDENTITY.length();
		final VulkanBuffer buffer = VulkanBuffer.create(dev, len, props);
		return new ResourceBuffer(buffer, VkDescriptorType.UNIFORM_BUFFER, 0);
	}

	@Bean
	public Frame.Listener update(ResourceBuffer uniform) {
		return () -> {
			final Matrix tilt = AxisAngle.of(Axis.X, MathsUtil.toRadians(-90)).matrix();
			final Matrix rot = AxisAngle.of(Axis.Y, MathsUtil.toRadians(120)).matrix();
			final Matrix model = rot.multiply(tilt);
			final Matrix matrix = projection.multiply(cam.matrix()).multiply(model);
			matrix.buffer(uniform.buffer());
		};
	}
}
