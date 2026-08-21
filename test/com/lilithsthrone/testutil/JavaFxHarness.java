package com.lilithsthrone.testutil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javafx.application.Platform;

/**
 * Boots the JavaFX toolkit exactly once per JVM so tests can exercise logic that
 * transitively touches JavaFX. Apply with {@code @ExtendWith(JavaFxHarness.class)}.
 * A no-op if the toolkit is already running.
 */
public final class JavaFxHarness implements BeforeAllCallback {

	private static final AtomicBoolean STARTED = new AtomicBoolean(false);

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		if (!STARTED.compareAndSet(false, true)) {
			return;
		}
		CountDownLatch latch = new CountDownLatch(1);
		try {
			Platform.startup(latch::countDown);
		} catch (IllegalStateException alreadyRunning) {
			// Toolkit was booted elsewhere; nothing to wait for.
			return;
		}
		if (!latch.await(30, TimeUnit.SECONDS)) {
			throw new IllegalStateException("JavaFX toolkit failed to start within 30s");
		}
	}
}
