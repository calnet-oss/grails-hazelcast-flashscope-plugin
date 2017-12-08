/*
 * This source code is derived from GrailsFlashScope.java from the
 * Grails core source code, which is licensed under the Apache License,
 * Version 2.0.  See:
 * https://github.com/grails/grails-core/blob/master/grails-web-common/src/main/groovy/org/grails/web/servlet/GrailsFlashScope.java
 *
 * Copyright (c) 2004-2005, 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.berkeley.grails.hazelcast.flash;

import com.hazelcast.core.HazelcastInstance;
import grails.util.Holders;
import grails.web.mvc.FlashScope;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.grails.web.servlet.GrailsFlashScope;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.util.GrailsApplicationAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class HazelcastFlashScope implements FlashScope, Serializable {

    private transient Map _current;
    private transient Map _next;
    public transient static final String ERRORS_PREFIX = GrailsFlashScope.ERRORS_PREFIX;
    private transient static final String ERRORS_PROPERTY = "errors";
    private transient HazelcastInstance hazelcastInstance;
    private String mapName;
    private String sessionId;

    public static final String FLASH_LAST_MODIFIED_ATTR_NAME = "hazelcast.flash.lastModified";
    public static final String CURRENT_FLASH_MAP_ATTR_NAME = "hazelcast.flash.currentMap";
    public static final String NEXT_FLASH_MAP_ATTR_NAME = "hazelcast.flash.nextMap";

    public HazelcastFlashScope(String mapName, HttpSession session) {
        this.mapName = mapName;
        this.sessionId = session.getId();
    }

    protected HazelcastInstance getHazelcastInstance() {
        if (hazelcastInstance == null) {
            this.hazelcastInstance = (HazelcastInstance) Holders.getGrailsApplication().getMainContext().getBean("hazelcastClient");
            if (hazelcastInstance == null) {
                throw new RuntimeException("Could not find 'hazelcastClient' bean");
            }
        }
        return hazelcastInstance;
    }

    protected HttpSession getCurrentHazelcastSession() {
        return (HttpSession) getHazelcastInstance().getMap(mapName).get(sessionId);
    }

    protected Map getMapFromSession(String mapAttrName) {
        HttpSession session = (getCurrentGrailsSession() != null && getCurrentGrailsSession().getAttribute(mapAttrName) != null
                ? getCurrentGrailsSession() : getCurrentHazelcastSession());
        return session != null ? (Map) session.getAttribute(mapAttrName) : null;
    }

    @Override
    public Map getNow() {
        if (_current == null) {
            String mapAttrName = CURRENT_FLASH_MAP_ATTR_NAME;
            _current = getMapFromSession(mapAttrName);
            if (_current == null) {
                _current = new HashMap();
                if (getCurrentGrailsSession() != null) {
                    getCurrentGrailsSession().setAttribute(mapAttrName, _current);
                }
            }
        }
        return _current;
    }

    public Map getNext() {
        if (_next == null) {
            String mapAttrName = NEXT_FLASH_MAP_ATTR_NAME;
            _next = getMapFromSession(mapAttrName);
            if (_next == null) {
                _next = new HashMap();
                if (getCurrentGrailsSession() != null) {
                    getCurrentGrailsSession().setAttribute(mapAttrName, _next);
                }
            }
        }
        return _next;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void next() {
        getNow().clear();
        this._current = new HashMap(getNext());
        getCurrentGrailsSession().setAttribute(CURRENT_FLASH_MAP_ATTR_NAME, _current);
        getNext().clear();
        reassociateObjectsWithErrors(getNow());
    }

    private void reassociateObjectsWithErrors(Map scope) {
        for (Object key : scope.keySet()) {
            Object value = scope.get(key);
            if (value instanceof Map) {
                reassociateObjectsWithErrors((Map) value);
            }
            reassociateObjectWithErrors(scope, value);
        }
    }

    private void reassociateObjectWithErrors(Map scope, Object value) {
        if (value instanceof Collection) {
            Collection values = (Collection) value;
            for (Object val : values) {
                reassociateObjectWithErrors(scope, val);
            }
        } else {
            String errorsKey = ERRORS_PREFIX + System.identityHashCode(value);
            Object errors = scope.get(errorsKey);
            if (value != null && errors != null) {
                MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(value.getClass());
                if (mc.hasProperty(value, ERRORS_PROPERTY) != null) {
                    mc.setProperty(value, ERRORS_PROPERTY, errors);
                }
            }
        }
    }

    @Override
    public int size() {
        return getNow().size() + getNext().size();
    }

    @Override
    public void clear() {
        getNow().clear();
        getNext().clear();
        makeSessionDirty();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return (getNow().containsKey(key) || getNext().containsKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return (getNow().containsValue(value) || getNext().containsValue(value));
    }

    @Override
    public Collection<Object> values() {
        Collection<Object> c = new ArrayList<>();
        c.addAll(getNow().values());
        c.addAll(getNext().values());
        return c;
    }

    @Override
    public void putAll(Map<? extends String, ?> t) {
        for (Entry<? extends String, ?> entry : t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        makeSessionDirty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entrySet = new HashSet<>();
        entrySet.addAll(getNow().entrySet());
        entrySet.addAll(getNext().entrySet());
        return entrySet;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> keySet() {
        Set<String> keySet = new HashSet<>();
        keySet.addAll(getNow().keySet());
        keySet.addAll(getNext().keySet());
        return keySet;
    }

    @Override
    public Object get(Object key) {
        if (getNext().containsKey(key)) {
            return getNext().get(key);
        }
        if ("now".equals(key)) {
            return getNow();
        }
        return getNow().get(key);
    }

    @Override
    public Object remove(Object key) {
        try {
            if (getNow().containsKey(key)) {
                return getNow().remove(key);
            }

            return getNext().remove(key);
        } finally {
            makeSessionDirty();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(String key, Object value) {
        try {
            // create the session if it doesn't exist
            registerWithSessionIfNecessary();
            if (getNow().containsKey(key)) {
                getNow().remove(key);
            }
            storeErrorsIfPossible(getNext(), value);

            if (value == null) {
                return getNext().remove(key);
            }

            return getNext().put(key, value);
        } finally {
            makeSessionDirty();
        }
    }

    @SuppressWarnings("unchecked")
    private void storeErrorsIfPossible(Map scope, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Collection) {
            Collection values = (Collection) value;
            for (Object val : values) {
                storeErrorsIfPossible(scope, val);
            }
        } else if (value instanceof Map) {
            Map map = (Map) value;
            Collection keys = new LinkedList(map.keySet());
            for (Object key : keys) {
                Object val = map.get(key);
                storeErrorsIfPossible(map, val);
            }
        } else {
            MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(value.getClass());
            if (mc.hasProperty(value, ERRORS_PROPERTY) != null) {
                Object errors = mc.getProperty(value, ERRORS_PROPERTY);
                if (errors != null) {
                    scope.put(ERRORS_PREFIX + System.identityHashCode(value), errors);
                }
            }
        }
    }

    private void registerWithSessionIfNecessary() {
        HttpSession session = getCurrentGrailsSession();
        if (session.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE) == null) {
            session.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, this);
        }
    }

    private GrailsWebRequest getCurrentGrailsWebRequest() {
        return (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
    }

    /**
     * The grails session should be delegating to a Hazelcast session if the
     * HazelcastSessionManager is in use.
     */
    protected HttpSession getCurrentGrailsSession() {
        try {
            return getCurrentGrailsWebRequest().getCurrentRequest().getSession(true);
        } catch (IllegalStateException ignored) {
            // no-op
        }
        return null;
    }

    /**
     * Make a session dirty so that Hazelcast will persist changes.
     * Necessary because modifying the flash scope submap won't otherwise be
     * detected as a change to the session.
     *
     * @return true if there was a session available and it was dirtied
     */
    @SuppressWarnings("UnusedReturnValue")
    protected boolean makeSessionDirty() {
        HttpSession session = getCurrentGrailsSession();
        if (session != null) {
            session.setAttribute(FLASH_LAST_MODIFIED_ATTR_NAME, new Date().getTime());
            return true;
        }
        return false;
    }
}
