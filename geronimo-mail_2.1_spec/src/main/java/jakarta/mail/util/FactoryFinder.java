/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jakarta.mail.util;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Simple service lookup helper for locating a {@link StreamProvider}
 * implementation.  The lookup order is:
 * <ol>
 *   <li>a system property naming the implementation class (the property
 *       name is the factory interface name),</li>
 *   <li>the standard {@link ServiceLoader} mechanism, tried with the
 *       thread context class loader, the class loader of the factory
 *       interface and the system class loader,</li>
 *   <li>the built-in default implementation shipped with this bundle.</li>
 * </ol>
 */
class FactoryFinder {

    private static final String DEFAULT_PROVIDER = "org.apache.geronimo.mail.util.MailStreamProvider";

    private FactoryFinder() {
    }

    /**
     * Finds an implementation of the given factory type.
     *
     * @param factoryClass factory abstract class or interface to be found
     * @return an instance of the factory implementation
     * @throws IllegalStateException if no implementation can be located or instantiated
     */
    static <T> T find(final Class<T> factoryClass) {
        // a system property naming the implementation class always wins.
        final String className = System.getProperty(factoryClass.getName());
        if (className != null) {
            final T result = newInstance(className, factoryClass);
            if (result != null) {
                return result;
            }
        }

        // regular ServiceLoader lookup, using the different candidate class loaders.
        final ClassLoader[] loaders = new ClassLoader[] {
            Thread.currentThread().getContextClassLoader(),
            factoryClass.getClassLoader(),
            ClassLoader.getSystemClassLoader()
        };

        for (final ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            final T result = fromServiceLoader(factoryClass, loader);
            if (result != null) {
                return result;
            }
        }

        // fall back to the default implementation included in this bundle.  This keeps
        // the API functional in environments where the ServiceLoader mechanism does not
        // work (e.g. some OSGi containers).
        final T result = newInstance(DEFAULT_PROVIDER, factoryClass);
        if (result != null) {
            return result;
        }

        throw new IllegalStateException("No provider of " + factoryClass.getName() + " was found");
    }

    private static <T> T newInstance(final String className, final Class<T> factoryClass) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = factoryClass.getClassLoader();
            }
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            Class<?> clazz;
            try {
                clazz = Class.forName(className, false, loader);
            } catch (final ClassNotFoundException e) {
                // retry with the loader of the factory class itself (e.g. the TCCL
                // cannot see the implementation classes).
                clazz = Class.forName(className, false, factoryClass.getClassLoader());
            }
            return clazz.asSubclass(factoryClass).getConstructor().newInstance();
        } catch (final ClassCastException wrongLoader) {
            return null;
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate " + className, e);
        }
    }

    private static <T> T fromServiceLoader(final Class<T> factoryClass, final ClassLoader loader) {
        try {
            final ServiceLoader<T> serviceLoader = ServiceLoader.load(factoryClass, loader);
            final Iterator<T> iterator = serviceLoader.iterator();
            if (iterator.hasNext()) {
                return factoryClass.cast(iterator.next());
            }
            return null;
        } catch (final ClassCastException wrongLoader) {
            return null;
        } catch (final Throwable t) {
            // e.g. a ServiceConfigurationError; ignore and try the next mechanism.
            return null;
        }
    }
}
