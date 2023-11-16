package com.dotcms.ai.workflow;

import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.MultiKeyValue;
import com.dotmarketing.portlets.workflows.model.MultiSelectionWorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Try;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OpenAIGenerateImageActionlet extends WorkFlowActionlet {






    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        WorkflowActionletParameter overwriteParameter = new MultiSelectionWorkflowActionletParameter(OpenAIParams.OVERWRITE_FIELDS.key,
                "Overwrite existing content (true|false)", Boolean.toString(true), true,
                () -> ImmutableList.of(
                        new MultiKeyValue(Boolean.toString(false), Boolean.toString(false)),
                        new MultiKeyValue(Boolean.toString(true), Boolean.toString(true)))
        );

        return List.of(
                new WorkflowActionletParameter(OpenAIParams.FIELD_TO_WRITE.key, "The field where you want to include the image.", "Will default to the first binary field", false),
                overwriteParameter,
                new WorkflowActionletParameter(OpenAIParams.OPEN_AI_PROMPT.key, "The prompt that will be sent to the AI", "Generate an abstract professional image about :\\n\\n${contentlet.blog}\\n\\n", true),
                new WorkflowActionletParameter(OpenAIParams.RUN_DELAY.key, "Update the content asynchronously, after X seconds. O means run in-process", "10", true)
        );
    }

    @Override
    public String getName() {
        return "AI Generate Image";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will send the value of the 'openAIPrompt' field to AI to generate an image.  The prompt can also take velocity (content can be referenced as $dotContentMap)";
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        int delay= Try.of(() -> Integer.parseInt(params.get(OpenAIParams.RUN_DELAY.key).getValue())).getOrElse(5);

        Runnable task = new AsyncWorkflowRunnerWrapper(new OpenAIGenerateImageRunner(processor, params));
        if (delay > 0) {
            OpenAIThreadPool.schedule(task, delay , TimeUnit.SECONDS);
        } else {
            task.run();
        }
    }




}
