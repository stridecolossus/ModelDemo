package org.sarge.jove.demo.model;

import org.sarge.jove.control.ActionBindings;
import org.sarge.jove.control.Button;
import org.sarge.jove.control.RenderLoop;
import org.sarge.jove.control.RenderLoop.Task;
import org.sarge.jove.geometry.Matrix;
import org.sarge.jove.geometry.Rotation;
import org.sarge.jove.geometry.Vector;
import org.sarge.jove.platform.desktop.MouseDevice;
import org.sarge.jove.platform.desktop.Window;
import org.sarge.jove.platform.vulkan.VkBufferUsage;
import org.sarge.jove.platform.vulkan.VkMemoryProperty;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.sarge.jove.scene.Camera;
import org.sarge.jove.scene.OrbitalCameraController;
import org.sarge.jove.scene.Projection;
import org.sarge.jove.util.MathsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

	@Bean
	public ActionBindings bindings(Window window, RenderLoop loop) {
		// Bind stop action
		final ActionBindings bindings = new ActionBindings();
		final var keyboard = window.keyboard().source();
		bindings.bind(new Button("ESCAPE", keyboard), loop::stop);

		// Bind camera controller
		final MouseDevice mouse = window.mouse();
		bindings.bind(mouse.pointer(), controller::update);
		bindings.bind(mouse.wheel(), controller::zoom);

		// Init devices
		bindings.init();

		return bindings;
	}

	@Bean
	public Task matrix(VulkanBuffer uniform) {
		// TODO - can we bake these into the controller as offsets?
		final Matrix x = Rotation.matrix(Vector.X, MathsUtil.toRadians(90));
		final Matrix y = Rotation.matrix(Vector.Y, MathsUtil.toRadians(-120));
		final Matrix model = y.multiply(x);

		return () -> {
			final Matrix matrix = projection.multiply(cam.matrix()).multiply(model);
			uniform.load(matrix);
		};
	}

	@Bean
	public static VulkanBuffer uniform(LogicalDevice dev, AllocationService allocator) {
		final MemoryProperties<VkBufferUsage> props = new MemoryProperties.Builder<VkBufferUsage>()
				.usage(VkBufferUsage.UNIFORM_BUFFER)
				.required(VkMemoryProperty.HOST_VISIBLE)
				.required(VkMemoryProperty.HOST_COHERENT)
				.build();

		return VulkanBuffer.create(dev, allocator, Matrix.IDENTITY.length(), props);
	}
}
