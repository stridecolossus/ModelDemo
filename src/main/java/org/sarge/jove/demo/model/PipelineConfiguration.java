package org.sarge.jove.demo.model;

import java.io.IOException;

import org.sarge.jove.common.Rectangle;
import org.sarge.jove.io.DataSource;
import org.sarge.jove.io.ResourceLoaderAdapter;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkShaderStage;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.Shader;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.pipeline.PipelineLayout;
import org.sarge.jove.platform.vulkan.render.DescriptorSet;
import org.sarge.jove.platform.vulkan.render.RenderPass;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PipelineConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private DataSource src;

	@Bean
	Shader vertex() throws IOException {
		final var loader = new ResourceLoaderAdapter<>(src, new Shader.Loader(dev));		// TODO
		return loader.load("chalet.vert.spv");
	}

	@Bean
	Shader fragment() throws IOException {
		final var loader = new ResourceLoaderAdapter<>(src, new Shader.Loader(dev));		// TODO
		return loader.load("chalet.frag.spv");
	}

	@Bean
	PipelineLayout pipelineLayout(DescriptorSet.Layout layout) {
		return new PipelineLayout.Builder()
				.add(layout)
				.build(dev);
	}

	@Bean
	public Pipeline pipeline(RenderPass pass, Swapchain swapchain, Shader vertex, Shader fragment, PipelineLayout pipelineLayout, Model.Header model) {
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
					.add(model.layout())
					.build()
				.assembly()
					.topology(model.primitive())
					.build()
				.depth()
					.enable(true)
					.build()
				.build(dev);
	}
}
