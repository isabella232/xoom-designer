// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.
package io.vlingo.xoom.designer.task.projectgeneration.code.java;

import io.restassured.specification.RequestSpecification;
import io.vlingo.xoom.actors.Logger;
import io.vlingo.xoom.codegen.content.CodeElementFormatter;
import io.vlingo.xoom.codegen.dialect.Dialect;
import io.vlingo.xoom.codegen.dialect.ReservedWordsHandler;
import io.vlingo.xoom.designer.Profile;
import io.vlingo.xoom.designer.infrastructure.HomeDirectory;
import io.vlingo.xoom.designer.infrastructure.Infrastructure;
import io.vlingo.xoom.designer.infrastructure.userinterface.UserInterfaceBootstrapStep;
import io.vlingo.xoom.designer.infrastructure.userinterface.XoomInitializer;
import io.vlingo.xoom.designer.task.TaskExecutionContext;
import io.vlingo.xoom.designer.task.projectgeneration.GenerationTarget;
import io.vlingo.xoom.turbo.ComponentRegistry;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static io.vlingo.xoom.designer.task.Property.DESIGNER_SERVER_PORT;
import static io.vlingo.xoom.http.Response.Status.Created;
import static io.vlingo.xoom.http.Response.Status.Ok;

public abstract class ProjectGenerationTest {

  private static final Logger logger = Logger.basicLogger();
  private static final PortDriver portDriver = PortDriver.init();

  public static void init() {
    Infrastructure.clear();
    ComponentRegistry.clear();
    Profile.enableTestProfile();
    ComponentRegistry.register(GenerationTarget.class, GenerationTarget.FILESYSTEM);
    ComponentRegistry.register(DESIGNER_SERVER_PORT.literal(), portDriver.findAvailable());
    ComponentRegistry.register("defaultCodeFormatter", CodeElementFormatter.with(Dialect.findDefault(), ReservedWordsHandler.usingSuffix("_")));
    Infrastructure.resolveInternalResources(HomeDirectory.fromEnvironment());
    new UserInterfaceBootstrapStep().process(TaskExecutionContext.bare());
    releasePortsOnShutdown();
  }

  public void generateAndRun(final Project project) {
    generate(project);
    compile(project);
    run(project);
  }

  public RequestSpecification apiOf(final Project project) {
    return given().port(project.appPort).accept(JSON).contentType(JSON);
  }

  private void generate(final Project project){
    removeTargetFolder(project.generationPath.path);

    final int designerPort = Infrastructure.DesignerServer.url().getPort();

    final int pathCreationStatusCode = given().port(designerPort).accept(JSON)
            .contentType(JSON).body(project.generationPath).post("/api/generation-settings/paths").statusCode();

    Assertions.assertEquals(Created.code, pathCreationStatusCode, "Error creating generation path for " + project);

    final int generationStatusCode = given().port(Infrastructure.DesignerServer.url().getPort())
            .accept(JSON).contentType(JSON).body(project.generationSettings).post("/api/generation-settings").statusCode();

    Assertions.assertEquals(Ok.code, generationStatusCode, "Error generating " + project);
  }

  private void compile(final Project project) {
    final JavaCompilationCommand compilationCommand =
            JavaCompilationCommand.at(project.generationPath.path);

    compilationCommand.process();

    Assertions.assertEquals(CommandStatus.SUCCEEDED, compilationCommand.status(), "Error compiling " + project);
  }

  private void run(final Project project) {
    JavaAppInitializationCommand.from(project.generationSettings, project.appPort).process();
    Assertions.assertEquals(false, portDriver.isPortAvailable(project.appPort, 300, 30, false), "Error initializing app " + project);
  }

  private void removeTargetFolder(final String generationPath) {
    try {
      FileUtils.deleteDirectory(new File(generationPath));
    } catch (final IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Unable to remove target folder", e);
    }
  }

  public static void clear() throws Exception {
    Infrastructure.clear();
    ComponentRegistry.clear();
    Profile.disableTestProfile();
    XoomInitializer.instance().stopServer();
  }

  public static void releasePortsOnShutdown() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      Project.all().forEach(project -> {
        if(!portDriver.release(project.appPort)) {
          logger.error("Unable to release port " + project.appPort);
        } else {
          logger.info("Port " + project.appPort + " released");
        }
      });
      Project.clear();
    }));
  }

  private String resolveGenerationPath(final String model) {
    return Paths.get(System.getProperty("user.dir"), "target", "e2e-tests", model)
            .toString().replace("\\", "\\\\");
  }

}