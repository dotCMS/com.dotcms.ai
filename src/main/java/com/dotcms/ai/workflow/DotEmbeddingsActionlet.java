package com.dotcms.ai.workflow;


import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DotEmbeddingsActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    private static final String DOT_EMBEDDING_TYPES_FIELDS = "dotEmbeddingTypes";
    private static final String DOT_EMBEDDING_ACTION = "dotEmbeddingAction";
    private static final String DOT_EMBEDDING_INDEX = "default";
    @Override
    public List<WorkflowActionletParameter> getParameters() {

        final List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter(DOT_EMBEDDING_TYPES_FIELDS, "List of {contentType}.{fieldVar} to use to generate the embeddings.  Each type.field should be on its own line, e.g. blog.title<br>blog.blogContent", "", false));
        params.add(new WorkflowActionletParameter(DOT_EMBEDDING_INDEX, "Index Name", "", false));
        params.add(new WorkflowActionletParameter(DOT_EMBEDDING_ACTION, "INSERT|DELETE", "INSERT", false));

        return params;
    }

    @Override
    public String getName() {
        return "OpenAI Embeddings";
    }

    @Override
    public String getHowTo() {
        return "List of {contentType}.{fieldVar} to use to generate the embeddings.  Each type.field should be on its own line, e.g. blog.title<br>blog.blogContent.  If no contentType.fields are specified here, dotCMS will attempt to guess how the content should be indexed based on its content type and/or its fields.";
    }


    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final ContentType type = processor.getContentlet().getContentType();


        final String updateOrDelete = "DELETE".equalsIgnoreCase(params.get(DOT_EMBEDDING_ACTION).getValue()) ? "DELETE" : "INSERT";
        final Host host = Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false)).getOrNull();
        final String indexName = UtilMethods.isSet(params.get(DOT_EMBEDDING_INDEX).getValue()) ? params.get(DOT_EMBEDDING_INDEX).getValue(): "default";
        HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if(request == null){
            return;
        }

        final List<Optional<Field>> fields = parseTypesAndFields(params.get(DOT_EMBEDDING_TYPES_FIELDS).getValue());

        if (fields.isEmpty()) {
            return;
        }


        for (Optional<Field> field : fields){
            EmbeddingsAPI.impl().generateEmbeddingsforContent(contentlet, field, indexName);
        }


    }


    private List<Optional<Field>> parseTypesAndFields(final String typeAndFieldParam) {

        if (UtilMethods.isEmpty(typeAndFieldParam)) {
            return List.of();
        }


        final Map<String, List<Optional<Field>>> typesAndFields = new HashMap<>();
        final String[] typeFieldArr = typeAndFieldParam.trim().split("\\r?\\n");
        List<Optional<Field>> fields = List.of();
        for (String typeField : typeFieldArr) {
            String[] typeOptField = typeField.trim().split("\\.");
            Optional<ContentType> type = Try.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser()).find(typeOptField[0])).toJavaOptional();
            if (type.isEmpty()) {
                continue;
            }
            fields = typesAndFields.getOrDefault(type.get().variable(),new ArrayList<>());

            if(typeOptField.length>1) {
                Optional<Field> field = Try.of(() -> type.get().fieldMap().get(typeOptField[1])).toJavaOptional();
                if(field.isPresent()) {
                    fields.add(field);
                }
            }
            else{
                fields.add(Optional.empty());
            }
            typesAndFields.put(type.get().variable(), fields);

        }


        return fields;
    }


}
