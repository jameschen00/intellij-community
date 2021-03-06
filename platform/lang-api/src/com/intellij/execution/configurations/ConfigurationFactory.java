/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.configurations;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author dyoma
 */
public abstract class ConfigurationFactory {
  public static final Icon ADD_ICON = IconUtil.getAddIcon();

  private final ConfigurationType myType;

  protected ConfigurationFactory(@NotNull final ConfigurationType type) {
    myType = type;
  }

  public RunConfiguration createConfiguration(String name, RunConfiguration template) {
    RunConfiguration newConfiguration = template.clone();
    newConfiguration.setName(name);
    return newConfiguration;
  }

  public abstract RunConfiguration createTemplateConfiguration(Project project);

  public RunConfiguration createTemplateConfiguration(Project project, RunManager runManager) {
    return createTemplateConfiguration(project);
  }

  public String getName() {
    return myType.getDisplayName();
  }

  public Icon getAddIcon() {
    return ADD_ICON;
  }

  public Icon getIcon(@NotNull final RunConfiguration configuration) {
    return getIcon();
  }

  public Icon getIcon() {
    return myType.getIcon();
  }

  @NotNull
  public ConfigurationType getType() {
    return myType;
  }

  /**
   * In this method you can configure defaults for the task, which are preferable to be used for your particular configuration type
   * @param providerID
   * @param task
   */
  public void configureBeforeRunTaskDefaults(Key<? extends BeforeRunTask> providerID, BeforeRunTask task) {
  }

  public boolean isConfigurationSingletonByDefault() {
    return false;
  }

  public boolean canConfigurationBeSingleton() {
    return true; // Configuration may be marked as singleton by default
  }
}
