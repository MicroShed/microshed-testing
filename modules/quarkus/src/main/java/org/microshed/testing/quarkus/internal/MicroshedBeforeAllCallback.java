/**
 *
 */
package org.microshed.testing.quarkus.internal;

import io.quarkus.test.junit.callback.QuarkusTestBeforeClassCallback;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.jupiter.MicroShedTestExtension;
import org.microshed.testing.quarkus.QuarkusConfiguration;

public class MicroshedBeforeAllCallback implements QuarkusTestBeforeClassCallback {

    @Override
    public void beforeClass(Class<?> testClass) {
        ApplicationEnvironment env = ApplicationEnvironment.Resolver.load();
        if (env instanceof QuarkusConfiguration) {
            QuarkusConfiguration quarkusEnv = (QuarkusConfiguration) env;
            quarkusEnv.preConfigure(testClass);
            MicroShedTestExtension.postConfigure(testClass, quarkusEnv);
        }
    }
}
