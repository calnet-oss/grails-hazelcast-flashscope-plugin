Hazelcast Flash Scope Plugin for Grails
=======================================

This plugin offers a Grails `FlashScope` implementation that uses Hazelcast
for a distributed flash scope map that is cluster friendly.

It works by using a request filter to instantiate the Hazelcast-friendly
flash scope ahead of when Grails would normally establish a flash scope
object.  If Grails encounters an already-instantiated flash scope, it won't
overwrite it.

A filter is used and not an interceptor because of the need to run the
filter before the `GrailsWebRequestFilter`, which invokes interceptors.

It's important the filter runs before any other filter or internal Grails
operation that invokes
`org.grails.web.servlet.DefaultGrailsApplicationAttributes.getFlashScope()`. 
This is where Grails will internally instantiate a non-serializable flash
scope if one is not already set.  The serializable session filter will
detect if a non-serializable flash scope is already set and will log a
warning if it detects this situation, which would be an indicator to you
that the filter is not running early enough in the filter chain.  The filter
execution precedence is controlled by the
`hazelcast.flashscope.filter.highestPrecedenceOffset` configuration value. 
You can see how this is used in the code for
`edu.berkeley.grails.hazelcast.flash.HazelcastFlashScopeGrailsPlugin` in
`doWithSpring()`.  The `highestPrecedenceOffset` configuration value is an
offset added to `Ordered.HIGHEST_PRECEDENCE`.  The default is 5.  A
`highestPrecedenceOffset` of 0 is the highest precedence you can go.
