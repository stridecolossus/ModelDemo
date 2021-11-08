package org.sarge.jove.demo.model;

import java.io.IOException;

import org.sarge.jove.common.Rectangle;
import org.sarge.jove.io.DataSource;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkShaderStage;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.Shader;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.pipeline.PipelineCache;
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
		return src.load("chalet.vert.spv", new Shader.Loader(dev));
	}

	@Bean
	Shader fragment() throws IOException {
		return src.load("chalet.frag.spv", new Shader.Loader(dev));
	}

	@Bean
	PipelineLayout pipelineLayout(DescriptorSet.Layout layout) {
		return new PipelineLayout.Builder()
				.add(layout)
				.build(dev);
	}

	@Bean
	PipelineCache cache() throws IOException {
//		// TODO
////		return wrapper.src.load(CacheWrapper.FILENAME, null)
		return PipelineCache.create(dev, null);
	}

//	@Component
//	class CacheWrapper {
//		private static final String FILENAME = "pipeline.cache";
//
//		private final DataSource src;
//
//		public CacheWrapper(ApplicationConfiguration cfg) throws IOException {
//			// Create application data folder
//			final String home = System.getProperty("user.home");
//			final Path dir = Paths.get(home).resolve("JOVE").resolve(cfg.getTitle());
//			Files.createDirectories(dir);
//
//			// Create pipeline cache file
//			final Path file = dir.resolve(FILENAME);
//			if(!Files.exists(file)) {
//				Files.createFile(file);
//			}
//
//			// Load pipeline cache
//			this.src = new DataSource(dir);
////			this.cache = src.load(filename, new PipelineCache.Loader(dev));
//		}
//
//		@PreDestroy
//		void close() throws IOException {
//			final var loader = new PipelineCache.Loader(dev);
//			loader.save(cache, Files.newOutputStream(file));
//		}
//	}

	@Bean
	public Pipeline pipeline(RenderPass pass, Swapchain swapchain, Shader vertex, Shader fragment, PipelineLayout layout, PipelineCache cache, Model.Header model) {
		final Rectangle viewport = new Rectangle(swapchain.extents());
		return new Pipeline.Builder()
				.layout(layout)
				.cache(cache)
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
