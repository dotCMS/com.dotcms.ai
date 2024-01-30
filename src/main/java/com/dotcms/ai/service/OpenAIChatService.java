package com.dotcms.ai.service;

import com.dotmarketing.util.json.JSONObject;

public interface OpenAIChatService {

    /**
     * Returns a JSONObject with the results of the text generation given the provided prompt
     * @param prompt a String representing the provided prompt to generate text
     * @return a JSONObject including the generated text and metadata
     */
    JSONObject sendTextPrompt(String prompt);

    /**
     * Returns a JSONObject with the results of the text generation given the provided prompt
     * @param prompt the provided prompt, as JSON, to generate text. The available properties
     *               for the JSON are:
     * <ul>
     * <li>{@code "prompt"}: the actual prompt text
     * <li>{@code "model"}: the model used for the generation
     * <li>{@code "temperature"}: Temperature ranges from 0 to 1. Low temperature (0 to 0.3): More focused, coherent, and conservative outputs. Medium temperature (0.3 to 0.7): Balanced creativity and coherence. High temperature (0.7 to 1): Highly creative and diverse, but potentially less coherent.
     * </ul>
     * @return a JSONObject including the generated text and metadata
     * <p>
     * Example of usage:
     * <p>
     * <pre>{@code
     *  JSONObject prompt = new JSONObject();
     *  prompt.put("model","gpt-3.5-turbo-16k");
     *  prompt.put("temperature",1);
     *  prompt.put("prompt","Short text about dotCMS");
     *  sendRawRequest(prompt);
     * }</pre>
     *
     */
    JSONObject sendRawRequest(JSONObject prompt);
}
