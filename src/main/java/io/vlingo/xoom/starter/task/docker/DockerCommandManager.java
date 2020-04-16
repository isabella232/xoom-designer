package io.vlingo.xoom.starter.task.docker;

import io.vlingo.xoom.starter.task.CommandNotFoundException;
import io.vlingo.xoom.starter.task.SubTask;
import io.vlingo.xoom.starter.task.TaskExecutionContext;
import io.vlingo.xoom.starter.task.TaskManager;
import io.vlingo.xoom.starter.task.docker.steps.DockerSettingsLoadStep;
import io.vlingo.xoom.starter.task.option.OptionValue;
import io.vlingo.xoom.starter.task.steps.CommandExecutionStep;
import io.vlingo.xoom.starter.task.steps.LoggingStep;
import io.vlingo.xoom.starter.task.steps.StatusHandlingStep;
import io.vlingo.xoom.starter.task.steps.TaskExecutionStep;

import java.util.Arrays;
import java.util.List;

import static io.vlingo.xoom.starter.task.Task.DOCKER;

public class DockerCommandManager implements TaskManager {

    private static final int SUB_TASK_INDEX = 1;

    @Override
    public void run(final List<String> args) {
        validateArgs(args);
        final String command = args.get(SUB_TASK_INDEX);
        final SubTask subTask = DOCKER.subTaskOf(command);
        runSteps(subTask, args);
    }

    private void runSteps(final SubTask subTask, final List<String> args) {
        final List<OptionValue> optionValues = subTask.findOptionValues(args);

        final TaskExecutionContext context =
                TaskExecutionContext.withOptions(optionValues);

        final List<TaskExecutionStep> steps =
                Arrays.asList(new DockerSettingsLoadStep(),
                        subTask.commandResolverStep(),
                        new CommandExecutionStep(),
                        new LoggingStep(),
                        new StatusHandlingStep());

        steps.forEach(step -> step.process(context));
    }

    private void validateArgs(final List<String> args) {
        if(args.size() < 2) {
            throw new CommandNotFoundException();
        }
    }

}