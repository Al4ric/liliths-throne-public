package com.lilithsthrone.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.lilithsthrone.testutil.JavaFxHarness;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Validates the canonical asynchronous WebView content pipeline that the tooltip (and every game
 * panel) now relies on after the {@code document.write} hack was removed: content set via
 * {@link WebEngine#loadContent(String)} eventually reaches {@link Worker.State#SUCCEEDED}, renders
 * the supplied HTML, and is measurable (non-zero {@code scrollHeight}).
 *
 * <p>This is a durable behavioural test — it asserts the observable outcome of the async load
 * contract, not any implementation detail — and runs headless via {@link JavaFxHarness} (no window,
 * no game boot).
 */
@ExtendWith(JavaFxHarness.class)
class TooltipWebViewLoadTest {

	private static final String TOOLTIP_HTML =
			"<div id='sizing-box' style='width:100%;'>"
			+ "<div class='title'>Async tooltip</div>"
			+ "<div class='description'>Loaded canonically via loadContent.</div>"
			+ "</div>";

	@Test
	void loadContentRendersAndIsMeasurable() throws Exception {
		AtomicReference<String> bodyText = new AtomicReference<>();
		AtomicReference<Integer> scrollHeight = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		CountDownLatch done = new CountDownLatch(1);

		Platform.runLater(() -> {
			try {
				WebEngine engine = new WebView().getEngine();
				engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
					if (newState == Worker.State.SUCCEEDED) {
						try {
							bodyText.set((String) engine.executeScript("document.body.innerText"));
							scrollHeight.set((Integer) engine.executeScript(
									"document.getElementById('sizing-box').scrollHeight"));
						} catch (Throwable t) {
							failure.set(t);
						} finally {
							done.countDown();
						}
					} else if (newState == Worker.State.FAILED) {
						failure.set(engine.getLoadWorker().getException());
						done.countDown();
					}
				});
				engine.loadContent(TOOLTIP_HTML);
			} catch (Throwable t) {
				failure.set(t);
				done.countDown();
			}
		});

		assertThat(done.await(20, TimeUnit.SECONDS))
				.as("WebEngine load worker should reach a terminal state")
				.isTrue();
		assertThat(failure.get()).as("no failure during async load").isNull();
		assertThat(bodyText.get()).contains("Async tooltip").contains("Loaded canonically");
		assertThat(scrollHeight.get()).as("rendered content must have a measurable height").isGreaterThan(0);
	}
}
