package com.lilithsthrone.controller.xmlParsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the parallel world-generation XML parsing race: the shared static
 * {@code Main.getDocBuilder()} used to hand out ONE non-thread-safe {@link javax.xml.parsers.DocumentBuilder},
 * so concurrent parses (from {@code Generation.call()}'s parallelStream) threw
 * "FWK005 parse may not be called while parsing" and NPEs. Parsing files that load fine
 * sequentially must therefore also load fine under heavy concurrency.
 */
class XmlParsingConcurrencyTest {

	private static List<File> goodFiles;

	@BeforeAll
	static void collectParsableFiles() throws Exception {
		Path itemsDir = Path.of("res", "items", "innoxia");
		assumeTrue(Files.isDirectory(itemsDir), "res/items/innoxia not present in working dir");

		List<File> xmlFiles;
		try (Stream<Path> walk = Files.walk(itemsDir)) {
			xmlFiles = walk.filter(p -> p.toString().endsWith(".xml"))
					.map(Path::toFile)
					.limit(60)
					.collect(Collectors.toList());
		}

		// Baseline: keep only files that parse cleanly on their own, so the test isolates concurrency.
		goodFiles = new ArrayList<>();
		for (File f : xmlFiles) {
			try {
				Element.getDocumentRootElement(f);
				goodFiles.add(f);
			} catch (XMLLoadException ignored) {
				// A genuinely malformed file is not what we're testing.
			}
		}
		assumeTrue(goodFiles.size() >= 5, "not enough parsable item XML files found");
	}

	@Test
	@DisplayName("files that parse sequentially also parse under heavy concurrency")
	void concurrentParsingHasNoRace() {
		List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

		IntStream.range(0, 3000).parallel().forEach(i -> {
			File file = goodFiles.get(i % goodFiles.size());
			try {
				Element.getDocumentRootElement(file);
			} catch (Throwable t) {
				failures.add(t);
			}
		});

		assertThat(failures)
				.as("concurrent XML parse failures (shared DocumentBuilder race)")
				.isEmpty();
	}
}
