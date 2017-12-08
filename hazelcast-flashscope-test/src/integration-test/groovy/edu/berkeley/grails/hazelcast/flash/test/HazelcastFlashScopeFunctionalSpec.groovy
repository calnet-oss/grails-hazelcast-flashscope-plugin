package edu.berkeley.grails.hazelcast.flash.test

import edu.berkeley.grails.hazelcast.flash.HazelcastFlashScope
import geb.spock.GebSpec
import grails.test.mixin.integration.Integration
import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.Base64

// Geb: http://www.gebish.org/manual/current/

@Integration
class HazelcastFlashScopeFunctionalSpec extends GebSpec {

    void "test hazelcast flash scope"() {
        when:
        go '/test'
        def bytes = downloadBytes { HttpURLConnection connection ->
            connection.setRequestProperty("Accept", "application/json")
        }
        def json = new JsonSlurper().parse(bytes)

        def lastModified = json.flashLastModified

        // Deserialize the flash scope bytes that should be part of the HTTP JSON response.
        assert json.flashBytesBase64
        byte[] flashScopeBytes = Base64.decodeBase64((String) json.flashBytesBase64)
        HazelcastFlashScope flash = deserializeFlashScope(flashScopeBytes)

        then:
        lastModified
        flash.test == "hello world"
    }

    /**
     * Deserialize a byte array into a SerializableFlashScope object.
     *
     * @param serializedFlashScopeBytes Byte array of a serialized SerializableFlashScope object.
     * @return The deserialized SerializableFlashScope object.
     * @throws IOException If an I/O error occurs.
     */
    static HazelcastFlashScope deserializeFlashScope(byte[] serializedFlashScopeBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedFlashScopeBytes)
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)
            try {
                return (HazelcastFlashScope) objectInputStream.readObject()
            } catch (ClassNotFoundException e) {
                throw new IOException(e)
            } finally {
                objectInputStream.close()
            }
        } finally {
            byteArrayInputStream.close()
        }
    }
}
