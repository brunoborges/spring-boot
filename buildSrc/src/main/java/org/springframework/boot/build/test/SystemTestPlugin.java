/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build.test;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

/**
 * A {@link Plugin} to configure system testing support in a {@link Project}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class SystemTestPlugin implements Plugin<Project> {

	private static final Spec<Task> NEVER = (task) -> false;

	/**
	 * Name of the {@code systemTest} task.
	 */
	public static String SYSTEM_TEST_TASK_NAME = "systemTest";

	/**
	 * Name of the {@code systemTest} source set.
	 */
	public static String SYSTEM_TEST_SOURCE_SET_NAME = "systemTest";

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> configureSystemTesting(project));
	}

	private void configureSystemTesting(Project project) {
		SourceSet systemTestSourceSet = createSourceSet(project);
		createTestTask(project, systemTestSourceSet);
		project.getPlugins().withType(EclipsePlugin.class, (eclipsePlugin) -> {
			EclipseModel eclipse = project.getExtensions().getByType(EclipseModel.class);
			eclipse.classpath((classpath) -> classpath.getPlusConfigurations()
				.add(project.getConfigurations()
					.getByName(systemTestSourceSet.getRuntimeClasspathConfigurationName())));
		});
		project.getDependencies()
			.add(systemTestSourceSet.getRuntimeOnlyConfigurationName(), "org.junit.platform:junit-platform-launcher");
	}

	private SourceSet createSourceSet(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
		SourceSet systemTestSourceSet = sourceSets.create(SYSTEM_TEST_SOURCE_SET_NAME);
		SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		systemTestSourceSet
			.setCompileClasspath(systemTestSourceSet.getCompileClasspath().plus(mainSourceSet.getOutput()));
		systemTestSourceSet
			.setRuntimeClasspath(systemTestSourceSet.getRuntimeClasspath().plus(mainSourceSet.getOutput()));
		return systemTestSourceSet;
	}

	private TaskProvider<Test> createTestTask(Project project, SourceSet systemTestSourceSet) {
		return project.getTasks().register(SYSTEM_TEST_TASK_NAME, Test.class, (task) -> {
			task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
			task.setDescription("Runs system tests.");
			task.setTestClassesDirs(systemTestSourceSet.getOutput().getClassesDirs());
			task.setClasspath(systemTestSourceSet.getRuntimeClasspath());
			task.shouldRunAfter(JavaPlugin.TEST_TASK_NAME);
			if (isCi()) {
				task.getOutputs().upToDateWhen(NEVER);
				task.getOutputs().doNotCacheIf("System tests are always rerun on CI", (spec) -> true);
			}
		});
	}

	private boolean isCi() {
		return Boolean.parseBoolean(System.getenv("CI"));
	}

}
