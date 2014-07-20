package com.fasterxml.jackson.datatype.hibernate4;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Id;

import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature;

/**
 * Factory to create instances of persistentClass with {@link Id} property set to given value. <p/>
 * Used by {@link HibernateProxySerializer} in case {@link Feature#SERIALIZE_IDENTIFIER_USE_PERSISTENT_CLASS} is enabled.
 * @see #createInstanceWithIdValue(Class, String, Object)
 * @author arthupka
 * @since 2.4
 */
public class IdentifierTypeFactory {
    static final ConcurrentHashMap<CacheKey, Field> _idFieldCache = new ConcurrentHashMap<CacheKey, Field>();

    Object createInstanceWithIdValue(Class<?> persistentClass, final String idName, final Object idValue) {
        try {
            Constructor<?> defaultConstructor = persistentClass.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
            Object instance = defaultConstructor.newInstance();
            Field idField = getIdField(persistentClass, idName, instance);
            if(!idField.isAccessible()){
                idField.setAccessible(true);
            }
            idField.set(instance, idValue);
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Error creating identifier class [" + persistentClass.getSimpleName() + "] and "
                    + "setting idValue with [" + idName + "=" + idValue + "]. Default constructor present and field available?",
                    e);
        }
    }

    private Field getIdField(Class<?> persistentClass, String idName, Object instance) throws NoSuchMethodException {
        CacheKey cacheKey = new CacheKey(persistentClass.getCanonicalName(), idName);
        Field field = _idFieldCache.get(cacheKey);
        if (field != null) {
            return field;
        }
        else {
            synchronized (_idFieldCache) {
                field = _idFieldCache.get(cacheKey);
                if (field == null) {
                    field = findField(persistentClass, idName, instance);
                    _idFieldCache.put(cacheKey, field);
                }
                return field;
            }
        }
    }

    private Field findField(Class<?> persistentClass, String idName, Object instance) throws NoSuchMethodException {
        if (persistentClass == null) {
            return null;
        }
        Field field;
        try {
            field = persistentClass.getDeclaredField(idName);
        } catch (NoSuchFieldException e) {
            field = findField(persistentClass.getSuperclass(), idName, instance);
        }
        return field;
    }

    private static class CacheKey {
        private final String clazz;
        private final String idName;

        public CacheKey(String clazz, String idName) {
            super();
            this.clazz = clazz;
            this.idName = idName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
            result = prime * result + ((idName == null) ? 0 : idName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheKey other = (CacheKey) obj;
            if (clazz == null) {
                if (other.clazz != null)
                    return false;
            } else if (!clazz.equals(other.clazz))
                return false;
            if (idName == null) {
                if (other.idName != null)
                    return false;
            } else if (!idName.equals(other.idName))
                return false;
            return true;
        }

    }

}
