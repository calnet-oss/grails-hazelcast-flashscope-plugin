/*
 * Copyright (c) 2017, Regents of the University of California and
 * contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.grails.hazelcast.flash.filter;

import edu.berkeley.grails.hazelcast.flash.HazelcastFlashScope;
import grails.core.GrailsApplication;
import grails.core.support.GrailsApplicationAware;
import org.grails.web.util.GrailsApplicationAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;

/**
 * A filter that must execute before the {@link
 * org.grails.web.servlet.mvc.GrailsWebRequestFilter} and before anything
 * calls {@link
 * org.grails.web.servlet.DefaultGrailsApplicationAttributes#getFlashScope}. 
 * This filter will set a serializable flash scope object in the session
 * before Grails has a chance to instantiate an unserializable flash scope
 * object in {@link
 * org.grails.web.servlet.DefaultGrailsApplicationAttributes#getFlashScope}. 
 * Grails won't set its own flash scope object if it's already been set, so
 * we take advantage of that fact by creating our own serializable flash
 * scope early in the filter chain.  
 *
 * <p> This filter will warn if a nonserializable flash scope has been set
 * before this filter is executed, indicating this filter isn't running
 * early enough in the filter chain.  See {@link
 * edu.berkeley.grails.hazelcast.flash.HazelcastFlashscopeGrailsPlugin#doWithSpring}
 * for how the execution order is set for this filter.
 */
public class GrailsHazelcastFlashScopeFilter implements Filter, GrailsApplicationAware {

    protected static Logger log = LoggerFactory.getLogger(GrailsHazelcastFlashScopeFilter.class);
    private GrailsApplication grailsApplication;

    @Override
    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest) request).getSession(/*false*/true);
        Object existingFlashScope = (session != null ? session.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE) : request.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE));
        if (existingFlashScope != null) {
            if (!(existingFlashScope instanceof Serializable)) {
                log.warn(GrailsApplicationAttributes.FLASH_SCOPE + " has already been set and it's not serializable!  This is an indication this GrailsSerializableSessionFilter is not running early enough in the filter chain.");
            } else {
                log.warn("There's an existing serializable flash scope of type " + existingFlashScope.getClass().getName());
            }
        } else {
            if (session != null) {
                // TODO: The mapName can be configured in HazelcastSessionManager
                String contextPath = request.getServletContext().getContextPath();
                String mapName;
                if (contextPath == null || contextPath.equals("/") || contextPath.equals("")) {
                    mapName = "empty_session_replication";
                } else {
                    mapName = contextPath.substring(1, contextPath.length()) + "_session_replication";
                }
                log.info("Setting serializable flash scope in the session using mapName " + mapName);

                session.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, new HazelcastFlashScope(mapName, session));

            } else {
                log.warn("New session could not be established");
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // no-op
    }
}
