// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.designer.task;

import io.vlingo.xoom.actors.Logger;
import io.vlingo.xoom.codegen.parameter.CodeGenerationParameter;
import io.vlingo.xoom.codegen.parameter.CodeGenerationParameters;
import io.vlingo.xoom.designer.task.projectgeneration.DeploymentType;
import io.vlingo.xoom.designer.task.projectgeneration.Label;
import io.vlingo.xoom.designer.task.projectgeneration.code.java.DeploymentSettings;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static io.vlingo.xoom.designer.task.projectgeneration.Label.DEPLOYMENT_SETTINGS;
import static io.vlingo.xoom.designer.task.projectgeneration.Label.TARGET_FOLDER;

public class TaskExecutionContext {

  public final String executionId;
  private final CodeGenerationParameters parameters;
  private final List<OptionValue> optionValues = new ArrayList<>();
  private final Map<TaskOutput, Object> taskOutput = new HashMap<>();
  private Logger logger;

  public static TaskExecutionContext empty() {
    return new TaskExecutionContext(Collections.emptyList());
  }

  public static TaskExecutionContext withOptions(final List<OptionValue> optionValues) {
    return new TaskExecutionContext(optionValues);
  }

  public static TaskExecutionContext withOutput(final TaskOutput taskOutput,
                                                final Object output) {
    return empty().addOutput(taskOutput, output);
  }

  private TaskExecutionContext(final List<OptionValue> optionValues) {
    this.executionId = UUID.randomUUID().toString();
    this.parameters = CodeGenerationParameters.empty();
    this.optionValues.addAll(optionValues);
  }

  public TaskExecutionContext with(final CodeGenerationParameters parameters) {
    this.parameters.addAll(parameters);
    return this;
  }

  public TaskExecutionContext addOutput(final TaskOutput taskOutput, final Object content) {
    this.taskOutput.put(taskOutput, content);
    return this;
  }

  @SuppressWarnings("unchecked")
  public <T> T retrieveOutput(final TaskOutput taskOutput) {
    if(!this.taskOutput.containsKey(taskOutput)) {
      return null;
    }
    return (T) this.taskOutput.get(taskOutput);
  }

  public String optionValueOf(final OptionName optionName) {
    return optionValues.stream()
            .filter(optionValue -> optionValue.hasName(optionName))
            .map(optionValue -> optionValue.value()).findFirst().get();
  }

  public boolean hasOption(final OptionName optionName) {
    return optionValues.stream().anyMatch(optionValue -> optionValue.hasName(optionName));
  }

  @SuppressWarnings("unchecked")
  public <T> T codeGenerationParameterOf(final Label label) {
    return (T) parameters.retrieveValue(label);
  }

  public Stream<CodeGenerationParameter> codeGenerationParametersOf(final Label label) {
    return parameters.retrieveAll(label);
  }

  public CodeGenerationParameters codeGenerationParameters() {
    return parameters;
  }

  public DeploymentType deploymentType() {
    return ((DeploymentSettings) parameters.retrieveObject(DEPLOYMENT_SETTINGS)).type;
  }

  public String targetFolder() {
    return Paths.get(parameters.retrieveValue(TARGET_FOLDER)).toString();
  }

  public TaskExecutionContext logger(final Logger logger) {
    this.logger = logger;
    return this;
  }

  public Logger logger() {
    return logger;
  }

}
