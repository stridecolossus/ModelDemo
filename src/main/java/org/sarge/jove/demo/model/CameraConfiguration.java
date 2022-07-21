package org.sarge.jove.demo.model;

import java.nio.ByteBuffer;

import org.sarge.jove.control.ActionBindings;
import org.sarge.jove.geometry.*;
import org.sarge.jove.platform.desktop.*;
import org.sarge.jove.platform.vulkan.pipeline.*;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.sarge.jove.scene.*;
import org.sarge.jove.scene.RenderLoop.Task;
import org.sarge.jove.util.*;
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

	@Bean
	public ActionBindings bindings(Window window, RenderLoop loop) {
		// Bind stop action
		final ActionBindings bindings = new ActionBindings();
		final KeyboardDevice keyboard = window.keyboard();
		keyboard.bind(bindings);
		bindings.bind(keyboard.key("ESCAPE"), loop::stop);

		// Bind camera controller
		final MouseDevice mouse = window.mouse();
		bindings.bind(mouse.pointer(), controller::update);
		bindings.bind(mouse.wheel(), controller::zoom);

		return bindings;
	}

//	@Bean
//	public static VulkanBuffer uniform(LogicalDevice dev, AllocationService allocator) {
//		final MemoryProperties<VkBufferUsage> props = new MemoryProperties.Builder<VkBufferUsage>()
//				.usage(VkBufferUsage.UNIFORM_BUFFER)
//				.required(VkMemoryProperty.HOST_VISIBLE)
//				.required(VkMemoryProperty.HOST_COHERENT)
//				.build();
//
//		return VulkanBuffer.create(dev, allocator, 3 * Matrix.IDENTITY.length(), props);
//	}
//
//	@Bean
//	public Task matrix(PipelineLayout layout) {
//		// Init model rotation
//		// TODO - can we bake these into the controller as offsets?
//		final Matrix x = Rotation.matrix(Vector.X, MathsUtil.toRadians(-90));
//		final Matrix y = Rotation.matrix(Vector.Y, MathsUtil.toRadians(120));
//		final Matrix model = y.multiply(x);
//
//		// Add projection matrix
//		final BufferWrapper buffer = new BufferWrapper(uniform.buffer());
//		buffer.insert(2, projection);
//
//		// Update modelview matrix
//		return () -> {
//			buffer.rewind();
//			buffer.append(model);
//			buffer.append(cam.matrix());
//		};
//	}

	@Bean
	public static PushConstantUpdateCommand update(PipelineLayout layout) {
		return PushConstantUpdateCommand.of(layout);
	}

	@Bean
	public Task matrix(PushConstantUpdateCommand update) {
		// Init model rotation
		// TODO - can we bake these into the controller as offsets?
		final Matrix x = Rotation.matrix(Vector.X, MathsUtil.toRadians(-90));
		final Matrix y = Rotation.matrix(Vector.Y, MathsUtil.toRadians(120));
		final Matrix model = y.multiply(x);

		// Add projection matrix
		BufferHelper.insert(2, projection, update.data());

		// Update modelview matrix
		return () -> {
			final ByteBuffer bb = update.data();
			bb.rewind();
			model.buffer(bb);
			cam.matrix().buffer(bb);
		};
	}
}
