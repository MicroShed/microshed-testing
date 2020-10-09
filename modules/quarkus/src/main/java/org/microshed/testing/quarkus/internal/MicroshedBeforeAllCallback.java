/**
 *
 */
package org.microshed.testing.quarkus.internal;

import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.jupiter.MicroShedTestExtension;
import org.microshed.testing.quarkus.QuarkusConfiguration;

import io.quarkus.test.junit.callback.QuarkusTestBeforeAllCallback;

public class MicroshedBeforeAllCallback implements QuarkusTestBeforeAllCallback {

    @Override
    public void beforeAll(Object testInstance) {
        ApplicationEnvironment env = ApplicationEnvironment.Resolver.load();
        if (env instanceof QuarkusConfiguration) {
            QuarkusConfiguration quarkusEnv = (QuarkusConfiguration) env;
            quarkusEnv.preConfigure(testInstance.getClass());
            MicroShedTestExtension.postConfigure(testInstance.getClass(), quarkusEnv);
        }
    }

}
