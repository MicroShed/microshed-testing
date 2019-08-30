/**
 *
 */
package org.microshed.testing.testcontainers;

import java.util.function.Consumer;

import org.testcontainers.containers.output.OutputFrame;

public class SystemOutLogConsumer implements Consumer<OutputFrame> {

    private final String prefix;

    public SystemOutLogConsumer() {
        this("");
    }

    public SystemOutLogConsumer(String prefix) {
        if (prefix != null && !prefix.isEmpty() && !prefix.endsWith(" "))
            this.prefix = prefix + " ";
        else
            this.prefix = prefix;
    }

    @Override
    public void accept(OutputFrame t) {
        System.out.print(prefix + t.getUtf8String());
    }

}
