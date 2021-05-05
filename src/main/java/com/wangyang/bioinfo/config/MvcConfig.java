package com.wangyang.bioinfo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wangyang.bioinfo.interceptor.BioInterceptor;
import com.wangyang.bioinfo.interceptor.SpringWebSocketHandlerInterceptor;
import com.wangyang.bioinfo.util.SpringWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

/**
 * @author wangyang
 * @date 2021/4/24
 */
@Configuration
@EnableWebSocket
public class MvcConfig  extends WebMvcConfigurationSupport implements WebSocketConfigurer {
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("file:/home/wy/.bioinfo/")
                .addResourceLocations("classpath:/static/html/");
        super.addResourceHandlers(registry);
    }


    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new BioInterceptor()).addPathPatterns("/**");
    }

    /**
     * 将有ResponseBody注解的返回对象转换为json格式
     * @param converters
     */
    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        /**
         * 序列换成Json时,将所有的Long变成String
         * 因为js中得数字类型不能包括所有的java Long值
         */
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 所有的double类型返回保留三位小数
//        simpleModule.addSerializer(Double.class, CustomerDoubleSerialize.instance);
//        simpleModule.addSerializer(Double.TYPE, CustomerDoubleSerialize.instance);

        objectMapper.registerModule(simpleModule);
        jackson2HttpMessageConverter.setObjectMapper(objectMapper);
        converters.add(jackson2HttpMessageConverter);
//        StringHttpMessageConverter converter = new StringHttpMessageConverter(
//                Charset.forName("UTF-8"));
//        converters.add(responseBodyConverter());
    }

    /**
     * 解决Pageable pageable不能作为参数的问题
     * @param argumentResolvers
     */
    @Override
    protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add( new PageableHandlerMethodArgumentResolver());
    }


    @Override
    protected void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**").
                    allowedOriginPatterns("*").
                    allowedMethods("*"). //允许任何方法（post、get等）
                    allowedHeaders("*"). //允许任何请求头
                    allowCredentials(true); //带上cookie信息
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        //springwebsocket 4.1.5版本前默认支持跨域访问，之后的版本默认不支持跨域，需要设置.setAllowedOrigins("*")
        registry.addHandler(webSocketHandler(),"/websocket/socketServer.do").setAllowedOrigins("*")
                .addInterceptors(new SpringWebSocketHandlerInterceptor());
//
//        registry.addHandler(webSocketHandler(), "/sockjs/socketServer.do")
//                .addInterceptors(new SpringWebSocketHandlerInterceptor()).withSockJS();
    }

    @Bean
    public TextWebSocketHandler webSocketHandler() {
        return new SpringWebSocketHandler();
    }

}
