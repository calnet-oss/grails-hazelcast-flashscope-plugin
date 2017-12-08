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
package edu.berkeley.grails.hazelcast.flash.test

import edu.berkeley.grails.hazelcast.flash.HazelcastFlashScope
import grails.converters.JSON
import org.apache.commons.codec.binary.Base64

class TestController {

    def index() {
        flash.test = "hello world"

        byte[] flashBytes = serializeFlashScope(8192, (Serializable) flash)
        String base64Encoded = Base64.encodeBase64String(flashBytes)
        def result = [flashLastModified: session.getAttribute(HazelcastFlashScope.FLASH_LAST_MODIFIED_ATTR_NAME), flashBytesBase64: base64Encoded]
        render result as JSON
    }

    /**
     * Serialize into a byte array.
     *
     * @param allocationSize The size of the byte array to allocate, which must be big enough to hold the serialized object.
     * @param flashScope The flash scope instance.
     * @return The serialized object as a byte array.
     * @throws IOException If an I/O error occurs.
     */
    static byte[] serializeFlashScope(int allocationSize, Serializable flashScope) throws IOException {
        ByteArrayOutputStream serializedByteStream = new ByteArrayOutputStream(allocationSize)
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(serializedByteStream)
            try {
                objectOutputStream.writeObject(flashScope)
                return serializedByteStream.toByteArray()
            } finally {
                objectOutputStream.close()
            }
        } finally {
            serializedByteStream.close()
        }
    }
}
