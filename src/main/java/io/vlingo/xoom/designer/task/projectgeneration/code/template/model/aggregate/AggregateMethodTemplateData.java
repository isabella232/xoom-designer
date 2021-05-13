// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.designer.task.projectgeneration.code.template.model.aggregate;

import io.vlingo.xoom.codegen.parameter.CodeGenerationParameter;
import io.vlingo.xoom.codegen.template.TemplateData;
import io.vlingo.xoom.codegen.template.TemplateParameters;
import io.vlingo.xoom.codegen.template.TemplateStandard;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.DesignerTemplateStandard;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.projections.ProjectionSourceTypesDetail;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.projections.ProjectionType;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.storage.StorageType;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static io.vlingo.xoom.designer.task.projectgeneration.code.formatting.Formatters.Arguments.AGGREGATE_METHOD_INVOCATION;
import static io.vlingo.xoom.designer.task.projectgeneration.code.formatting.Formatters.Arguments.SIGNATURE_DECLARATION;
import static io.vlingo.xoom.designer.task.projectgeneration.code.template.Label.*;
import static io.vlingo.xoom.designer.task.projectgeneration.code.template.TemplateParameter.STORAGE_TYPE;
import static io.vlingo.xoom.designer.task.projectgeneration.code.template.TemplateParameter.*;
import static java.util.stream.Collectors.toList;

public class AggregateMethodTemplateData extends TemplateData {

  private final TemplateParameters parameters;

  public static List<TemplateData> from(final TemplateParameters parentParameters,
                                        final CodeGenerationParameter aggregate,
                                        final StorageType storageType,
                                        final ProjectionType projectionType) {
    return aggregate.retrieveAllRelated(AGGREGATE_METHOD)
            .map(method -> new AggregateMethodTemplateData(parentParameters, method, storageType, projectionType))
            .collect(toList());
  }

  private AggregateMethodTemplateData(final TemplateParameters parentParameters,
                                      final CodeGenerationParameter method,
                                      final StorageType storageType,
                                      final ProjectionType projectionType) {
    this.parameters =
            TemplateParameters.with(METHOD_NAME, method.value).and(STORAGE_TYPE, storageType)
                    .and(DOMAIN_EVENT_NAME, method.retrieveRelatedValue(DOMAIN_EVENT))
                    .and(METHOD_INVOCATION_PARAMETERS, AGGREGATE_METHOD_INVOCATION.format(method))
                    .and(METHOD_PARAMETERS, SIGNATURE_DECLARATION.format(method))
                    .and(SOURCED_EVENTS, SourcedEvent.from(method.parent()))
                    .and(OPERATION_BASED, projectionType.isOperationBased())
                    .and(PROJECTION_SOURCE_TYPES_NAME, resolveProjectionSourceTypesName(projectionType))
                    .and(STATE_NAME, DesignerTemplateStandard.AGGREGATE_STATE.resolveClassname(method.parent(AGGREGATE).value));

    parentParameters.addImports(resolveImports(method));
  }

  private Set<String> resolveImports(final CodeGenerationParameter method) {
    final Stream<CodeGenerationParameter> involvedStateFields =
            AggregateDetail.findInvolvedStateFields(method.parent(), method.value);

    return AggregateDetail.resolveImports(involvedStateFields);
  }

  private String resolveProjectionSourceTypesName(final ProjectionType projectionType) {
    return ProjectionSourceTypesDetail.resolveClassName(projectionType);
  }

  @Override
  public TemplateParameters parameters() {
    return parameters;
  }

  @Override
  public TemplateStandard standard() {
    return DesignerTemplateStandard.AGGREGATE_METHOD;
  }

}