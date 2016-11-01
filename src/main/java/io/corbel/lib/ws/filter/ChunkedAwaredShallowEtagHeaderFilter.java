package io.corbel.lib.ws.filter;

import org.eclipse.jetty.server.Response;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class ChunkedAwaredShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isChucked(response)) {
            filterChain.doFilter(request, response);
        }
        else {
            super.doFilterInternal(request, response, filterChain);
        }
    }

    @Override
    protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response, int responseStatusCode, InputStream inputStream) {
        return !isChucked(response) && super.isEligibleForEtag(request, response, responseStatusCode, inputStream);
    }

    protected boolean isChucked(HttpServletResponse response) {
        return response instanceof  Response && ((Response) response).getContentLength() == -1;
    }
}
