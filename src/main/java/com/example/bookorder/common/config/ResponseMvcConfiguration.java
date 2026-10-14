package com.example.bookorder.common.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.bookorder.controller.dto.response.BaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** Allow MVC to invoke response advice for void and null Object results. */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class ResponseMvcConfiguration implements WebMvcConfigurer {
	private final ObjectMapper objectMapper;

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(new EmptyResponseConverter(objectMapper));
	}

	private static final class EmptyResponseConverter extends MappingJackson2HttpMessageConverter {
		private EmptyResponseConverter(ObjectMapper objectMapper) {
			super(objectMapper);
		}

		@Override
		public boolean canWrite(Class<?> type, MediaType mediaType) {
			return (type == void.class || type == Void.class || type == Object.class)
					&& super.canWrite(BaseResponse.class, mediaType);
		}

		@Override
		public boolean canRead(Class<?> type, MediaType mediaType) {
			return false;
		}

		@Override
		public boolean canRead(java.lang.reflect.Type type, Class<?> contextClass, MediaType mediaType) {
			return false;
		}
	}
}
