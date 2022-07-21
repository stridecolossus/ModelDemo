package org.sarge.jove.demo.model;

import java.io.IOException;
import java.util.List;

import org.sarge.jove.common.Rectangle;
import org.sarge.jove.io.*;
import org.sarge.jove.model.*;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.core.Command.Pool;
import org.sarge.jove.platform.vulkan.image.*;
import org.sarge.jove.platform.vulkan.image.Sampler.Wrap;
import org.sarge.jove.platform.vulkan.memory.*;
import org.sarge.jove.platform.vulkan.pipeline.*;
import org.sarge.jove.platform.vulkan.render.*;
import org.sarge.jove.platform.vulkan.render.FrameBuilder.Recorder;
import org.sarge.jove.platform.vulkan.util.FormatBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

@Configuration
public class SkyBoxConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private DataSource classpath;
//	@Autowired private ApplicationConfiguration cfg;

	@Bean
	public static Model skybox() {
		return new CubeBuilder()
				.build();
				//.transform(Component.POSITION, Component.COORDINATE);
		// TODO - transform
	}

	@Bean
	public Sampler cubeSampler() {
		return new Sampler.Builder()
				.wrap(Wrap.EDGE.mode(false))
				.build(dev);
	}

	@Bean
	public static Recorder skyboxRecorder(Pipeline skyboxPipeline, List<DescriptorSet> skyboxDescriptors, VertexBuffer skyboxVertexBuffer, Model skybox) {
		final DescriptorSet ds = skyboxDescriptors.get(0);
		final DrawCommand draw = DrawCommand.of(skybox);

		return buffer -> {
			buffer
					.add(skyboxPipeline.bind())
					.add(ds.bind(skyboxPipeline.layout()))
					.add(skyboxVertexBuffer.bind(0))
					.add(draw);
		};
	}

	@Bean
	public View cubemap(AllocationService allocator, DataSource data, Pool transfer) throws IOException {
//		final String[] filenames = {"posx", "negx", "posy", "negy", "posz", "negz"};
//		final ImageData[] images = new ImageData[6];
//		final var loader3 = new ResourceLoaderAdapter<>(data, new NativeImageLoader());
//		for(int n = 0; n < filenames.length; ++n) {
//			images[n] = loader3.load(filenames[n] + ".jpg");
//		}
//		final ImageData array = ImageData.array(Arrays.asList(images));
//		final ImageLoader loader2 = new ImageLoader();
//		loader2.save(array, new DataOutputStream(new FileOutputStream("../Data/skybox.image")));

		// Load cubemap image
		final var loader = new ResourceLoaderAdapter<>(data, new VulkanImageLoader());
		final ImageData image = loader.load("cubemap_vulkan.ktx2");
		if(image.layers() != Image.CUBEMAP_ARRAY_LAYERS) throw new IllegalArgumentException("Invalid cubemap image");

		// Determine image format
		final VkFormat format = FormatBuilder.format(image);

		// Create descriptor
		final ImageDescriptor descriptor = new ImageDescriptor.Builder()
				.type(VkImageType.TWO_D)
				.aspect(VkImageAspect.COLOR)
				.extents(image.extents())
				.format(format)
				.arrayLayers(Image.CUBEMAP_ARRAY_LAYERS)
				.mipLevels(image.levels().size())
				.build();

		// Init image memory properties
		final var props = new MemoryProperties.Builder<VkImageUsageFlag>()
				.usage(VkImageUsageFlag.TRANSFER_DST)
				.usage(VkImageUsageFlag.SAMPLED)
				.required(VkMemoryProperty.DEVICE_LOCAL)
				.build();

		// Create cube-map texture
		final Image texture = new Image.Builder()
				.descriptor(descriptor)
				.properties(props)
				.cubemap()
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
				.submitAndWait(transfer);

		// Load to staging buffer
		// TODO - either load each layer or add helper
		final VulkanBuffer staging = VulkanBuffer.staging(dev, allocator, image.data());

		// Create copy command
//		final var copy =
		new ImageCopyCommand.Builder()
				.image(texture)
				.buffer(staging)
				.layout(VkImageLayout.TRANSFER_DST_OPTIMAL)
				.region(image)
				.build()
				.submitAndWait(transfer);

//		// Add copy region for each image
//		for(int n = 0; n < Image.CUBEMAP_ARRAY_LAYERS; ++n) {
//			final SubResource res = new SubResource.Builder(descriptor)
//					.baseArrayLayer(n)
//					.build();
//
//			final CopyRegion region = new CopyRegion.Builder()
//// TODO					.offset(image.offset(n))
//					.extents(texture.descriptor().extents())
//					.subresource(res)
//					.build();
//
//			copy.region(region);
//		}
//
//		// Copy staging to texture
//		copy.build().submitAndWait(transfer);
		staging.destroy();

		// TODO - 500 -> 190 ms

		/*
		final var loader = new ResourceLoaderAdapter<>(src, new NativeImageLoader());
		for(int n = 0; n < filenames.length; ++n) {
			// Load image
			final ImageData image = loader.load(filenames[n] + ".jpg");

			// Copy image to staging buffer
			final VulkanBuffer staging = VulkanBuffer.staging(dev, allocator, image.data());

			// Init image sub-resource
			final SubResource res = new SubResource.Builder(descriptor)
					.baseArrayLayer(n)
					.build();

			final CopyRegion region = new CopyRegion.Builder()
					.extents(texture.descriptor().extents())
					.subresource(res)
					.build();

			// Create copy command
			final var copy = new ImageCopyCommand.Builder()
					.image(texture)
					.buffer(staging)
					.layout(VkImageLayout.TRANSFER_DST_OPTIMAL)
					.region(region)
					.build();

			// Copy staging to texture
			copy.submitAndWait(graphics);
			staging.destroy();
		}
		*/

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
				.submitAndWait(transfer);

		final SubResource subresource = new SubResource.Builder(descriptor)
				.layerCount(Image.CUBEMAP_ARRAY_LAYERS)
				.build();

		// Create texture view
		return new View.Builder(texture)
				.type(VkImageViewType.CUBE)
				.subresource(subresource)
				.mapping(ComponentMapping.of(image.components()))
				.build();
	}

	@Bean
	public Shader skyboxVertex() throws IOException {
		final var loader = new ResourceLoaderAdapter<>(classpath, new Shader.Loader(dev));	// TODO
		return loader.load("skybox.vert.spv");
	}

	@Bean
	public Shader skyboxFragment() throws IOException {
		final var loader = new ResourceLoaderAdapter<>(classpath, new Shader.Loader(dev));	// TODO
		return loader.load("skybox.frag.spv");
	}

	@Bean
	public Pipeline skyboxPipeline(RenderPass pass, Swapchain swapchain, Shader skyboxVertex, Shader skyboxFragment, PipelineLayout pipelineLayout, Model skybox) {
		final Rectangle viewport = new Rectangle(swapchain.extents());
		return new Pipeline.Builder()
				.layout(pipelineLayout)
				.pass(pass)
				.viewport(viewport)
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
				.depth()
					.enable(true)
					.write(false)
					.build()
//				.rasterizer()
//					.cull(VkCullMode.FRONT)
//					.build()
				.build(null, dev);
	}
}
