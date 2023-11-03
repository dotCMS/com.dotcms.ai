package com.dotcms.ai.rest.forms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class CompletionsFormTest {

    @Test
    public void testEquals() {
        CompletionsForm form1 = new CompletionsForm.Builder()
                .stream(true)
                .contentType("contentType")
                .fieldVar("fieldVar")
                .model("model")
                .operator("operator")
                .indexName("indexName")
                .language(100)
                .prompt("prompt")
                .site("site")
                .responseLengthTokens(500)
                .searchLimit(500)
                .searchOffset(1)
                .searchLimit(1)
                .temperature(.1f)
                .build();

        CompletionsForm form2 = new CompletionsForm.Builder()
                .stream(true)
                .contentType("contentType")
                .fieldVar("fieldVar")
                .model("model")
                .operator("operator")
                .indexName("indexName")
                .language(100)
                .prompt("prompt")
                .site("site")
                .responseLengthTokens(500)
                .searchLimit(500)
                .searchOffset(1)
                .searchLimit(1)
                .temperature(.1f)
                .build();

        System.out.println(form1);
        System.out.println(form2);
        Assertions.assertTrue(form1.equals(form2));


    }

    @Test
    public void testHashCode() {

        CompletionsForm form1 = new CompletionsForm.Builder()
                .stream(true)
                .contentType("contentType")
                .fieldVar("fieldVar")
                .model("model")
                .operator("operator")
                .indexName("indexName")
                .language(100)
                .prompt("prompt")
                .site("site")
                .responseLengthTokens(500)
                .searchLimit(500)
                .searchOffset(1)
                .searchLimit(1)
                .temperature(.1f)
                .build();

        CompletionsForm form2 = new CompletionsForm.Builder()
                .stream(true)
                .contentType("contentType")
                .fieldVar("fieldVar")
                .model("model")
                .operator("operator")
                .indexName("indexName")
                .language(100)
                .prompt("prompt")
                .site("site")
                .responseLengthTokens(500)
                .searchLimit(500)
                .searchOffset(1)
                .searchLimit(1)
                .temperature(.1f)
                .build();


        Assertions.assertTrue(form1.hashCode() == form2.hashCode());


    }

    @Test
    public void test_form_copy() {
        CompletionsForm form1 = new CompletionsForm.Builder()
                .stream(true)
                .contentType("contentType")
                .fieldVar("fieldVar")
                .model("model")
                .operator("operator")
                .indexName("indexName")
                //.fields("fields")
                .language(100)
                .prompt("prompt")
                .site("site")
                .responseLengthTokens(500)
                .searchLimit(500)
                .searchOffset(1)
                .searchLimit(1)
                .temperature(.1f)
                .build();

        CompletionsForm form2 = CompletionsForm.copy(form1).build();


        Arrays.equals(form1.fields, form2.fields);
        System.out.println(form1);
        System.out.println(form2);
        System.out.println(form1.fields);
        System.out.println(form2.fields);
        System.out.println(Arrays.equals(form1.fields, form2.fields));


        Assertions.assertTrue(form1.equals(form2));


    }
}
