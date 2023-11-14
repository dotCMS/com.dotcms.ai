package com.dotcms.ai.workflow;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;

import java.io.Serializable;
import java.util.Map;

interface AsyncWorkflowRunner extends Runnable, Serializable {

    WorkflowProcessor getProcessor();
    Map<String, WorkflowActionClassParameter> getParams();

    void runInternal() ;

    default void run(){
        throw new DotRuntimeException("not implemented");
    }

}
