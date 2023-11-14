package com.dotcms.ai.workflow;

import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import io.vavr.control.Try;

import java.util.List;
import java.util.Map;

public class OpenAIGenerateImageActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        return List.of(
                new WorkflowActionletParameter("fieldToWrite", "The field where you want to include the image.", "Will default to the first binary field", false),
                new WorkflowActionletParameter("overwriteField", "Overwrite existing content (true|false)", "true", false),
                new WorkflowActionletParameter("openAIPrompt", "The prompt that will be sent to the AI", "Generate an abstract professional image about :\\n\\n${contentlet.blog}\\n\\n", true),
                new WorkflowActionletParameter("runDelay", "Update the content asynchronously, after X seconds. O means run in-process", "10", true)
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
        if (Try.of(() -> Integer.parseInt(params.get("runDelay").getValue())).getOrElse(5) == 0) {
            new GenerateImageRunner(processor, params).run();
        }
    }

    @Override
    public void executePreAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {


        if (Try.of(() -> Integer.parseInt(params.get("runDelay").getValue())).getOrElse(5) > 0) {
            OpenAIThreadPool.threadPool().submit(new GenerateImageRunner(processor, params));
        }
    }


}
