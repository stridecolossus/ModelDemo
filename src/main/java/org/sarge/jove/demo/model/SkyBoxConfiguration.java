package org.sarge.jove.demo.model;

import java.io.IOException;

import org.sarge.jove.common.Bufferable;
import org.sarge.jove.common.Dimensions;
import org.sarge.jove.common.ImageData;
import org.sarge.jove.common.Rectangle;
import org.sarge.jove.model.CubeBuilder;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.common.Command.Pool;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.Shader;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.image.ComponentMappingBuilder;
import org.sarge.jove.platform.vulkan.image.Image;
import org.sarge.jove.platform.vulkan.image.ImageCopyCommand;
import org.sarge.jove.platform.vulkan.image.ImageDescriptor;
import org.sarge.jove.platform.vulkan.image.ImageExtents;
import org.sarge.jove.platform.vulkan.image.SubResource;
import org.sarge.jove.platform.vulkan.image.View;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.pipeline.Barrier;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.pipeline.PipelineLayout;
import org.sarge.jove.platform.vulkan.render.RenderPass;
import org.sarge.jove.platform.vulkan.render.Sampler;
import org.sarge.jove.platform.vulkan.render.Sampler.Wrap;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.sarge.jove.util.DataSource;
import org.sarge.jove.util.TextureAtlas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

//@Configuration
public class SkyBoxConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private DataSource src;
	//private final Function<String, Shader> loader;

	public SkyBoxConfiguration(LogicalDevice dev, DataSource src) {
		this.dev = dev;
		//this.loader = ResourceLoader.of(src, new Shader.Loader(dev));
	}

	@Bean
	public static Model skybox() {
		return new CubeBuilder().build();
		// TODO - either use offset to skip texture coords and/or flag to builder
	}

	@Bean
	public Sampler cube() {
		return new Sampler.Builder(dev)
				.wrap(Wrap.EDGE, false)
				.build();
	}

	@Bean
	public View cubemap(AllocationService allocator, DataSource src, Pool graphics) throws IOException {
		// Determine image format
//		final VkFormat format = FormatBuilder.format(image.layout());
		final VkFormat format = VkFormat.R8G8B8A8_UNORM;

		// Create descriptor
		final ImageDescriptor descriptor = new ImageDescriptor.Builder()
				.type(VkImageType.IMAGE_TYPE_2D)
				.aspect(VkImageAspect.COLOR)
				.extents(new ImageExtents(2048, 2048))
				.format(format)
				.arrayLayers(6)
				.build();

		// Init image memory properties
		final var props = new MemoryProperties.Builder<VkImageUsage>()
				.usage(VkImageUsage.TRANSFER_DST)
				.usage(VkImageUsage.SAMPLED)
				.required(VkMemoryProperty.DEVICE_LOCAL)
				.build();

		// Create cube-map texture
		final Image texture = new Image.Builder()
				.descriptor(descriptor)
				.properties(props)
				.flag(VkImageCreateFlag.CUBE_COMPATIBLE)
				.build(dev, allocator);

		// Prepare texture
		new Barrier.Builder()
				.source(VkPipelineStage.TOP_OF_PIPE)
				.destination(VkPipelineStage.TRANSFER)
				.barrier(texture)
					.newLayout(VkImageLayout.TRANSFER_DST_OPTIMAL)
					.destination(VkAccess.TRANSFER_WRITE)
					.build()
				.build()
				.submitAndWait(graphics);

		// Load cube-map image
		final ImageData image = src.load(".jpg", new ImageData.Loader());

		// Load cube-map images
		final TextureAtlas atlas = TextureAtlas.cubemap(new Dimensions(104, 104));
		final Rectangle[] rectangles = atlas.values().toArray(Rectangle[]::new);
		for(int n = 0; n < 6; ++n) {
			// Create staging buffer
			final Bufferable data = Bufferable.of(image.bytes());
			final VulkanBuffer staging = VulkanBuffer.staging(dev, allocator, data);

			// Init image sub-resource
			final SubResource res = new SubResource.Builder(descriptor)
					.baseArrayLayer(n)
					.layerCount(1)
					.build();

			// Copy staging to texture
			// TODO - rectangles, combine into 6 x copies?
			new ImageCopyCommand.Builder(texture)
					.buffer(staging)
					.layout(VkImageLayout.TRANSFER_DST_OPTIMAL)
					.subresource(res)
					.build()
					.submitAndWait(graphics);

			// Release staging
			staging.close();
		}

		// Transition to sampled image
		new Barrier.Builder()
				.source(VkPipelineStage.TRANSFER)
				.destination(VkPipelineStage.FRAGMENT_SHADER)
				.barrier(texture)
					.oldLayout(VkImageLayout.TRANSFER_DST_OPTIMAL)
					.newLayout(VkImageLayout.SHADER_READ_ONLY_OPTIMAL)
					.source(VkAccess.TRANSFER_WRITE)
					.destination(VkAccess.SHADER_READ)
					.build()
				.build()
				.submitAndWait(graphics);

//		// Build component mapping for the image
//		final VkComponentMapping mapping = ComponentMappingBuilder.build(image.mapping());

//		final SubResource subresource = new SubResource.Builder(descriptor)
//				.layerCount(6)
//				.build();

		// Create texture view
		return new View.Builder(texture)
				.type(VkImageViewType.VIEW_TYPE_CUBE)
//				.subresource(subresource)
				.mapping(ComponentMappingBuilder.IDENTITY)
				.build();
	}

	@Bean
	public Shader skyboxVertex() throws IOException {
		return src.load("spv.skybox.vert", new Shader.Loader(dev));
	}

	@Bean
	public Shader skyboxFragment() throws IOException {
		return src.load("spv.skybox.frag", new Shader.Loader(dev));
	}

	@Bean
	public Pipeline skyboxPipeline(RenderPass pass, Swapchain swapchain, Shader skyboxVertex, Shader skyboxFragment, PipelineLayout layout, Model skybox) {
		final Rectangle viewport = new Rectangle(swapchain.extents());
		return new Pipeline.Builder()
				.layout(layout)
				.pass(pass)
				.viewport()
					.flip(true)
					.viewport(viewport, true)
					.build()
				.shader(VkShaderStage.VERTEX)
					.shader(skyboxVertex)
					.build()
				.shader(VkShaderStage.FRAGMENT)
					.shader(skyboxFragment)
					.build()
				.input()
					.add(skybox.header().layout())
					.build()
				.assembly()
					.topology(skybox.header().primitive())
					.build()
				.rasterizer()
					.cull(VkCullMode.FRONT)
					.build()
				.build(dev);
	}
}
