package com.dotcms.ai.workflow;


import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentPromptRunnerTest {


    JSONObject json = new JSONObject();


    @Test
    public void test_setting_content_props() {

        Contentlet contentlet = new Contentlet();



        json.put("blog", "<h1>Understanding the Superiority of Server Side Rendered A/B Tests</h1>            <p>The landscape of web development is constantly evolving</p>");

        json.put("blog2", "<h1>Understanding the Superiority of Server Side Rendered A/B Tests<\\/h1>            <p>The landscape of web development is constantly evolving<\\/p>");


        System.out.println("blog");

        System.out.println(json.get("blog"));

        System.out.println("blog2");
        System.out.println(json.get("blog2"));

    }







}
