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
package edu.berkeley.grails.hazelcast.flash

import edu.berkeley.grails.hazelcast.flash.filter.GrailsHazelcastFlashScopeFilter
import grails.plugins.Plugin
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered

class HazelcastFlashscopeGrailsPlugin extends Plugin {

    def grailsVersion = "3.2.11 > *"

    def title = "Grails Hazelcast Flash Scope Plugin"
    def author = "Brian Koehmstedt"
    def authorEmail = "bkoehmstedt@berkeley.edu"
    def description = '''A plugin to persist flash scope values in a distributed Hazelcast map.  Useful for clustering.'''
    def profiles = ['web']

    def documentation = "https://github.com/calnet-oss/grails-hazelcast-flashscope-plugin"

    def license = "BSD"

    def organization = [name: "University of California, Berkeley", url: "http://www.berkeley.edu/"]

//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    def issueManagement = [system: "GitHub", url: "https://github.com/calnet-oss/grails-hazelcast-flashscope-plugin/issues"]

    def scm = [url: "https://github.com/calnet-oss/grails-hazelcast-flashscope-plugin"]

    static int DEFAULT_HIGHEST_PRECEDENCE_OFFSET = 5

    protected static class HazelcastFlashScopeGrailsPluginHelper {
        static Integer getHighestPrecedenceOffset(def config) {
            def value = config.hazelcast?.flashscope?.filter?.highestPrecedenceOffset
            if (value instanceof Number) {
                return value.intValue()
            } else if (value instanceof String) {
                return value as Integer
            }
            return null
        }
    }

    Closure doWithSpring() {
        { ->
            grailsHazelcastFlashScopeFilter(GrailsHazelcastFlashScopeFilter) {
                if (config.hazelcast?.flashscope?.filter?.mapName) {
                    mapName = config.hazelcast.flashscope.filter.mapName
                }
            }

            grailsHazelcastFlashScopeFilterRegistrationBean(FilterRegistrationBean) {
                def highestPrecedenceOffset = HazelcastFlashScopeGrailsPluginHelper.getHighestPrecedenceOffset(config)
                name = "Grails Hazelcast Flash Scope Filter"
                filter = ref("grailsHazelcastFlashScopeFilter")
                order = Ordered.HIGHEST_PRECEDENCE + (highestPrecedenceOffset != null ? highestPrecedenceOffset : DEFAULT_HIGHEST_PRECEDENCE_OFFSET)
            }

            hazelcastClientFactory(HazelcastClientFactory) {
                if (config.hazelcast?.flashscope?.clientConfigLocation) {
                    configFileLocation = config.hazelcast.flashscope.clientConfigLocation
                }
            }

            hazelcastClient(hazelcastClientFactory: "newHazelcastClientInstance")
        }
    }

    void doWithDynamicMethods() {
    }

    void doWithApplicationContext() {
    }

    void onChange(Map<String, Object> event) {
    }

    void onConfigChange(Map<String, Object> event) {
    }

    void onShutdown(Map<String, Object> event) {
    }
}
