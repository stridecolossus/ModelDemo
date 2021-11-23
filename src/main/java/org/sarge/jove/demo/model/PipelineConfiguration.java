package org.sarge.jove.demo.model;

import java.io.IOException;
import java.util.Set;

import org.sarge.jove.common.Rectangle;
import org.sarge.jove.geometry.Matrix;
import org.sarge.jove.io.DataSource;
import org.sarge.jove.io.ResourceLoaderAdapter;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkShaderStage;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.pipeline.PipelineLayout;
import org.sarge.jove.platform.vulkan.pipeline.PipelineLayout.PushConstantRange;
import org.sarge.jove.platform.vulkan.pipeline.Shader;
import org.sarge.jove.platform.vulkan.render.DescriptorLayout;
import org.sarge.jove.platform.vulkan.render.RenderPass;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PipelineConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private DataSource classpath;

	@Bean
	Shader vertex() throws IOException {
		final var loader = new ResourceLoaderAdapter<>(classpath, new Shader.Loader(dev));		// TODO
		return loader.load("chalet.vert.spv");
	}

	@Bean
	Shader fragment() throws IOException {
		final var loader = new ResourceLoaderAdapter<>(classpath, new Shader.Loader(dev));		// TODO
		return loader.load("chalet.frag.spv");
	}

	@Bean
	PipelineLayout pipelineLayout(DescriptorLayout layout) {
		final int len = 3 * Matrix.IDENTITY.length();

		return new PipelineLayout.Builder()
				.add(layout)
				.add(new PushConstantRange(0, len, Set.of(VkShaderStage.VERTEX)))
				.build(dev);
	}

	@Bean
	public Pipeline pipeline(RenderPass pass, Swapchain swapchain, Shader vertex, Shader fragment, PipelineLayout pipelineLayout, Model model) {
		final Rectangle viewport = new Rectangle(swapchain.extents());
		return new Pipeline.Builder()
				.layout(pipelineLayout)
				.pass(pass)
				.viewport()
					.flip(true)
					.viewport(viewport, true)
					.build()
				.shader(VkShaderStage.VERTEX)
					.shader(vertex)
					.build()
				.shader(VkShaderStage.FRAGMENT)
					.shader(fragment)
					.build()
				.input()
					.add(model.header().layout())
//					.binding()
//						.rate(VkVertexInputRate.INSTANCE)
//						.stride(Point.LAYOUT.length())
//						.attribute()
//							.location(2)
//							.format(FormatBuilder.format(Point.LAYOUT))
//							.build()
//						.build()
					.build()
				.assembly()
					.topology(model.header().primitive())
					.build()
				.depth()
					.enable(true)
					.build()
				.build(dev);
	}
}
