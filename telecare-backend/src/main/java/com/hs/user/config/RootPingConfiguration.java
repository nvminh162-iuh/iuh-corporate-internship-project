package com.hs.user.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerMapping;

import com.hs.user.filter.RequestIdFilter;

@Configuration(proxyBeanMethods = false)
public class RootPingConfiguration {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> rootPingWebServerCustomizer() {
        return factory -> factory.addContextCustomizers(applicationContext -> {
            Context rootContext = applicationContext;
            boolean separateContext = !applicationContext.getPath().isEmpty();
            if (separateContext) {
                // A controller cannot serve outside server.servlet.context-path.
                // This context has only an exact-root servlet, no API or static files.
                StandardContext context = new StandardContext();
                context.setName("");
                context.setPath("");
                context.setDocBase(applicationContext.getDocBase());
                context.setParentClassLoader(RootPingConfiguration.class.getClassLoader());
                context.addLifecycleListener(new Tomcat.FixContextListener());
                rootContext = context;
            }

            Tomcat.addServlet(rootContext, "root-ping", new RootPingServlet()).setLoadOnStartup(1);
            rootContext.addServletMappingDecoded("", "root-ping");
            if (separateContext) {
                // Spring's application filters do not cover this separate servlet context.
                FilterDef loggingFilter = new FilterDef();
                loggingFilter.setFilterName("root-request-log");
                loggingFilter.setFilter(new RequestIdFilter());
                rootContext.addFilterDef(loggingFilter);
                FilterMap mapping = new FilterMap();
                mapping.setFilterName("root-request-log");
                mapping.addURLPattern("/*");
                rootContext.addFilterMap(mapping);
                applicationContext.getParent().addChild(rootContext);
            }
        });
    }

    private static final class RootPingServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/");
            byte[] body = "ping - pong".getBytes(StandardCharsets.UTF_8);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
        }

        @Override
        protected void doHead(HttpServletRequest request, HttpServletResponse response) {
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLength("ping - pong".length());
        }
    }
}
