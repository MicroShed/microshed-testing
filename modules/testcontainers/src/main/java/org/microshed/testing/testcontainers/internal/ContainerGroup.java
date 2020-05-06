/**
 *
 */
package org.microshed.testing.testcontainers.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.SharedContainerConfiguration;
import org.microshed.testing.internal.InternalLogger;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

public class ContainerGroup {

    private static final InternalLogger LOG = InternalLogger.get(ContainerGroup.class);

    public final Class<?> testClass;
    public final Class<? extends SharedContainerConfiguration> sharedConfigClass;
    public final Set<GenericContainer<?>> unsharedContainers;
    public final Set<GenericContainer<?>> sharedContainers;
    public final Set<GenericContainer<?>> allContainers;
    public final ApplicationContainer app;

    public ContainerGroup(Class<?> testClass) {
        this.testClass = testClass;
        sharedConfigClass = testClass.isAnnotationPresent(SharedContainerConfig.class) ? //
                        testClass.getAnnotation(SharedContainerConfig.class).value() : null;
        unsharedContainers = Collections.unmodifiableSet(discoverContainers(testClass));
        sharedContainers = hasSharedConfig() ? //
                        Collections.unmodifiableSet(discoverContainers(sharedConfigClass)) : //
                        Collections.emptySet();
        Set<GenericContainer<?>> all = new HashSet<>(unsharedContainers);
        all.addAll(sharedContainers);
        allContainers = Collections.unmodifiableSet(all);

        Set<ApplicationContainer> apps = allContainers.stream()
                        .filter(c -> c instanceof ApplicationContainer)
                        .map(c -> (ApplicationContainer) c)
                        .collect(Collectors.toSet());
        if (apps.size() == 0) {
            app = null;
        } else if (apps.size() == 1) {
            app = apps.iterator().next();
        } else {
            app = null;
            // Error: Multiple ApplicationContainers were found
            String appString = apps.stream()
                            .map(app -> app.toStringSimple())
                            .collect(Collectors.joining(", "));
            throw new ExtensionConfigurationException("Only 1 ApplicationContainer may be used, but multiple were defined: " +
                                                      appString);
        }
    }

    public boolean hasSharedConfig() {
        return sharedConfigClass != null;
    }

    private Set<GenericContainer<?>> discoverContainers(Class<?> clazz) {
        Set<GenericContainer<?>> discoveredContainers = new HashSet<>();
        for (Field containerField : AnnotationSupport.findAnnotatedFields(clazz, Container.class)) {
            if (!Modifier.isPublic(containerField.getModifiers()))
                throw new ExtensionConfigurationException("@Container annotated fields must be public visibility");
            if (!Modifier.isStatic(containerField.getModifiers()))
                throw new ExtensionConfigurationException("@Container annotated fields must be static");
            boolean isStartable = GenericContainer.class.isAssignableFrom(containerField.getType());
            if (!isStartable)
                throw new ExtensionConfigurationException("@Container annotated fields must be a subclass of " + GenericContainer.class);
            try {
                GenericContainer<?> startableContainer = (GenericContainer<?>) containerField.get(null);
                discoveredContainers.add(startableContainer);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                LOG.warn("Unable to access field " + containerField, e);
            }
        }
        return discoveredContainers;
    }

}
