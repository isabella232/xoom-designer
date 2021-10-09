// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.
package io.vlingo.xoom.designer.codegen.e2e;

import io.vlingo.xoom.actors.Logger;
import io.vlingo.xoom.cli.task.TaskExecutionStep;
import io.vlingo.xoom.terminal.CommandExecutor;
import io.vlingo.xoom.cli.task.TaskExecutionContext;
import io.vlingo.xoom.terminal.DefaultCommandExecutionProcess;
import io.vlingo.xoom.terminal.ObservableCommandExecutionProcess;
import io.vlingo.xoom.terminal.Terminal;

public class SupportingServicesManager {

  public static final String SCHEMATA = "schemata";
  public static final String RABBIT_MQ = "rabbitmq";

  public static void run() {
    if(shouldManage()) {
      if(SupportingServicesStart.run()) {
        Logger.basicLogger().info("Supporting services running...");
      } else {
        Logger.basicLogger().warn("Unable to run Supporting Services");
      }
    }
  }

  public static void shutdown() {
    if(shouldManage()) {
      Logger.basicLogger().info("Stopping Supporting services...");
      SupportingServicesShutdown.handle();
    }
  }

  private static boolean shouldManage() {
    return Boolean.valueOf(System.getProperty("enable-supporting-services", "false"));
  }

  public static int findPortOf(final String serviceName) {
    final String port = System.getProperty(serviceName + "-port");
    if(port == null) {
      throw new IllegalArgumentException("Unknown port for service " + serviceName);
    }
    return Integer.valueOf(port);
  }

  private static class SupportingServicesStart extends CommandExecutor implements TaskExecutionStep {

    private final CommandObserver observer;

    public static boolean run() {
      final SupportingServicesStart starter =
              new SupportingServicesStart(new CommandObserver());

      starter.processTask();

      return starter.isRunning();
    }

    private SupportingServicesStart(final CommandObserver observer) {
      super(new ObservableCommandExecutionProcess(observer));
      this.observer = observer;
    }

    @Override
    protected String formatCommands(final TaskExecutionContext context) {
      final String directoryChangeCommand =
              Terminal.supported().resolveDirectoryChangeCommand(ProjectGenerationTest.e2eResourcesPath);

      return String.format("%s && docker-compose up -d", directoryChangeCommand);
    }

    private boolean isRunning() {
      return observer.status.equals(ExecutionStatus.SUCCEEDED);
    }
  }

  private static class SupportingServicesShutdown extends CommandExecutor implements TaskExecutionStep {

    static void handle() {
      new SupportingServicesShutdown().processTask();
    }

    private SupportingServicesShutdown() {
      super(new DefaultCommandExecutionProcess());
    }

    @Override
    protected String formatCommands(final TaskExecutionContext context) {
      final String directoryChangeCommand =
              Terminal.supported().resolveDirectoryChangeCommand(ProjectGenerationTest.e2eResourcesPath);

      return String.format("%s && docker-compose stop && docker-compose rm -v -f", directoryChangeCommand);
    }
  }

}
