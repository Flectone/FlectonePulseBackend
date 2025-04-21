package net.flectone.pulse.backend.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import net.flectone.pulse.backend.wrapper.GzipServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GzipDecompressionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        if ("gzip".equalsIgnoreCase(request.getHeader("Content-Encoding"))) {
            HttpServletRequest wrappedRequest = new GzipServletRequestWrapper(request);
            chain.doFilter(wrappedRequest, res);
        } else {
            chain.doFilter(req, res);
        }
    }
}

