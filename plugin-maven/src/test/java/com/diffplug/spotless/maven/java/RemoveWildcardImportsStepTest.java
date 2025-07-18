/*
 * Copyright 2025 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.maven.java;

import static org.assertj.core.api.Assertions.assertThat;

import com.diffplug.spotless.FormatterStep;

import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.kotlin.KtLintStep;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class RemoveWildcardImportsStepTest extends MavenIntegrationHarness {

	private static final String ERROR = "Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this";

	@BeforeEach
	void init() throws Exception {
		writePomWithJavaSteps("<removeWildcardImports/>");
	}

	@Test
	void testRemoveWildcardImports() throws Exception {
		setFile(PATH).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");
		assertFile(PATH).sameAsResource("java/removewildcardimports/JavaCodeWildcardsFormatted.test");
		AbstractStringAssert<?> abstractStringAssert = assertThat(mavenRunner().withArguments("spotless:apply").runHasError().stdOutUtf8());
		abstractStringAssert
			.contains(ERROR)
//			.contains("11111")
		;

		FormatterStep step = KtLintStep.create("0.49.0", TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step)
			.testResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test", "java/removewildcardimports/JavaCodeWildcardsFormatted.test")
			.expectLintsOfResource("kotlin/ktlint/unsolvable.dirty").toBe("L1 ktlint(standard:no-wildcard-imports) Wildcard import");
	}

	@Test
	void testRemoveWildcardImportsNoError() throws Exception {
		setFile(PATH).toResource("java/removewildcardimports/JavaCodeNoWildcardsUnformatted.test");
		assertFile(PATH).sameAsResource("java/removewildcardimports/JavaCodeNoWildcardsUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
	}
}
