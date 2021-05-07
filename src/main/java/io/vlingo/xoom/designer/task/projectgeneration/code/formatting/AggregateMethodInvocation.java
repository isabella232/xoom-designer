// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.
package io.vlingo.xoom.designer.task.projectgeneration.code.formatting;

import io.vlingo.xoom.designer.task.projectgeneration.code.template.model.FieldDetail;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.model.MethodScope;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.model.aggregate.AggregateDetail;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.model.valueobject.ValueObjectDetail;
import io.vlingo.xoom.turbo.codegen.parameter.CodeGenerationParameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vlingo.xoom.designer.task.projectgeneration.code.template.DesignerTemplateStandard.DATA_OBJECT;
import static io.vlingo.xoom.designer.task.projectgeneration.code.template.Label.*;
import static java.util.stream.Collectors.toList;

public class AggregateMethodInvocation implements Formatters.Arguments {

  private final String carrier;
  private final String stageVariableName;
  private final boolean dataObjectHandling;
  private static final String FIELD_ACCESS_PATTERN = "%s.%s";

  public static AggregateMethodInvocation handlingDataObject(final String stageVariableName) {
    return new AggregateMethodInvocation(stageVariableName, "data", true);
  }

  public AggregateMethodInvocation(final String stageVariableName) {
    this(stageVariableName, "", false);
  }

  public AggregateMethodInvocation(final String stageVariableName,
                                   final String carrier,
                                   final boolean dataObjectHandling) {
    this.carrier = carrier;
    this.stageVariableName = stageVariableName;
    this.dataObjectHandling = dataObjectHandling;
  }

  @Override
  public String format(final CodeGenerationParameter method, final MethodScope scope) {
    final List<String> args = scope.isStatic() ?
            Arrays.asList(stageVariableName) : Arrays.asList();

    return Stream.of(args, formatMethodParameters(method))
            .flatMap(Collection::stream).collect(Collectors.joining(", "));
  }

  private List<String> formatMethodParameters(final CodeGenerationParameter method) {
    return method.retrieveAllRelated(METHOD_PARAMETER).map(this::resolveFieldAccess).collect(toList());
  }

  private String resolveFieldAccess(final CodeGenerationParameter parameter) {
    final CodeGenerationParameter stateField =
            AggregateDetail.stateFieldWithName(parameter.parent(AGGREGATE), parameter.value);

    final String fieldPath =
            carrier.isEmpty() ? parameter.value : String.format(FIELD_ACCESS_PATTERN, carrier, parameter.value);

    if(dataObjectHandling && FieldDetail.isValueObjectCollection(stateField)) {
      return ValueObjectDetail.translateDataObjectCollection(fieldPath, stateField);
    }
    if(ValueObjectDetail.isValueObject(stateField)) {
      return stateField.value;
    }
    return fieldPath;
  }

}
