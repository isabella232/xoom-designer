// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.designer.task.projectgeneration.code.template.bootstrap;

import io.vlingo.xoom.codegen.CodeGenerationContext;
import io.vlingo.xoom.codegen.content.ContentQuery;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.Label;
import io.vlingo.xoom.designer.task.projectgeneration.code.template.storage.StorageType;

import java.util.Set;

import static io.vlingo.xoom.designer.task.projectgeneration.code.template.DesignerTemplateStandard.*;
import static io.vlingo.xoom.designer.task.projectgeneration.code.template.Label.CQRS;
import static io.vlingo.xoom.designer.task.projectgeneration.code.template.TemplateParameter.*;

public class DefaultBootstrapTemplateData extends BootstrapTemplateData {

  @Override
  protected void enrichParameters(final CodeGenerationContext context) {
    final Boolean useCQRS = context.parameterOf(CQRS, Boolean::valueOf);
    final StorageType storageType = context.parameterOf(Label.STORAGE_TYPE, StorageType::valueOf);

    final Set<String> qualifiedNames =
            ContentQuery.findFullyQualifiedClassNames(context.contents(),
                    STORE_PROVIDER, PROJECTION_DISPATCHER_PROVIDER, REST_RESOURCE,
                    AUTO_DISPATCH_RESOURCE_HANDLER, EXCHANGE_BOOTSTRAP);

    parameters().and(REST_RESOURCES, RestResource.from(context.contents()))
            .and(EXCHANGE_BOOTSTRAP_NAME, EXCHANGE_BOOTSTRAP.resolveClassname())
            .and(HAS_EXCHANGE, ContentQuery.exists(EXCHANGE_BOOTSTRAP, context.contents()))
            .addImports(qualifiedNames).addImports(storageType.resolveTypeRegistryQualifiedNames(useCQRS));
  }

  @Override
  protected boolean support(final CodeGenerationContext context) {
    return !context.parameterOf(Label.USE_ANNOTATIONS, Boolean::valueOf);
  }

}