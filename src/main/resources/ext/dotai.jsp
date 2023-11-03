<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>dotAI</title>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>

    <script type="application/javascript">
        <%@include file = "dotai.js" %>
    </script>
    <style>
        <%@include file = "dotai.css" %>
    </style>
</head>


<body>
<div id="openAIKeyWarn"
     style="display: none;padding:20px; border-radius: 10px;color:indianred;border:1px solid indianred;margin:20px auto;max-width: 800px;text-align: center">
    Your OpenAI API key is not set. Please add a valid API key in your <a
        href="/dotAdmin/#/apps/dotAI/edit/SYSTEM_HOST"
        target="_top">App screen</a>.
</div>
<div id="container">
    <input id="tab-1" type="radio" name="tab-group" checked="checked" onclick="changeTabs()"/>
    <label for="tab-1" class="p-tabview p-tabview-nav p-tabview-nav-link">Search and Chat with dotCMS</label>

    <input id="tab-2" type="radio" name="tab-group" onclick="changeTabs()"/>
    <label for="tab-2">Manage Embeddings/Indexes</label>

    <input id="tab-3" type="radio" name="tab-group" onclick="changeTabs();"/>
    <label for="tab-3">Config Values</label>

</div>

<div id="content">
    <div id="content-1">
        <h2>Semantic Content Search and Chat</h2>

        <div style="display: grid;grid-template-columns: 45% 55%;">
            <div style="border-right:1px solid #eeeeee;margin-right:40px;padding-right: 40px">

                <table >
                    <form action="POST" id="chatForm" onsubmit="return false;">
                        <tr>
                            <th>
                                Content index to search:
                            </th>
                            <td>
                                <select name="indexName" id="indexNameChat" style="min-width:400px;">
                                    <option disabled="true" placeholder="Select an Index">Select an Index</option>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Model:
                            </th>
                            <td>
                                <select name="model" id="modelName" style="min-width:400px;">
                                    <option disabled="true" placeholder="Select a Model">Select a Model</option>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <th>
                                <label>Response Type:</label>
                            </th>
                            <td>
                                <div style="padding-bottom:10px;">
                                    <input type="radio" checked="true" id="searchResponseType" name="responseType"
                                           value="search" onchange="showResultTables()">
                                    <label for="searchResponseType">Semantic Search &nbsp; &nbsp; (dotCMS Only)</label>
                                </div>
                                <div style="padding-bottom:10px;">
                                    <input type="radio" id="streamingResponseType" name="responseType" value="stream"
                                           onchange="showResultTables()">
                                    <label for="streamingResponseType">Streaming Chat &nbsp; &nbsp; (OpenAI + dotCMS
                                        Supporting Content)</label>
                                </div>
                                <div>
                                    <input type="radio" id="restJsonResponseType" name="responseType" value="json"
                                           onchange="showResultTables()">
                                    <label for="restJsonResponseType">REST/JSON Chat &nbsp; &nbsp; (OpenAI + dotCMS
                                        Supporting Content)</label>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Temperature:
                            </th>
                            <td>
                                <input name="temperature" type="number" step="0.1" value="1" min="0" max="2" style="min-width:100px;"><br>
                                (determines the randomness of the response. 0 = deterministic, 2 = most random
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Response length:
                            </th>
                            <td>
                                <input  type="number" step="1" value="500" min="10" max="2048"  style="min-width:100px;" name="responseLengthTokens" id="responseLengthTokens"><br>
                                The general length of response you would like to generate. 75 words ~= 100 tokens
                            </td>
                        </tr>

                        <tr>
                            <th>Vector Operator:</th>
                            <td>

                                <input type="radio" name="operator" id="cosine" checked="true" value="cosine">
                                <label for="cosine">Cosine Similarity</label>
                                &nbsp; &nbsp;
                                <input type="radio" name="operator" id="distance" value="distance">
                                <label for="distance">Distance</label>
                                &nbsp; &nbsp;
                                <input type="radio" name="operator" id="product" value="product">
                                <label for="product">Inner Product</label>
                                <br>
                                Search stored embeddings using this operator<br>(probably best to leave it alone).
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Distance Threshold:
                            </th>
                            <td>
                                <input type="number" step="0.05" value=".25" name="threshold" min="0.05"  max="100"  style="min-width:100px;"><br>
                                the lower this number, the more semantically similar the results
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Site:
                            </th>
                            <td>
                                <input type="text" value="" name="site"><br>
                                Site id on which the content lives - leave blank for all
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Content Types:
                            </th>
                            <td>
                                <input type="text" value="" name="contentType" id="contentTypeSearch"><br>
                                Comma separated list of content types to include in the results
                            </td>
                        </tr>

                    </form>
                </table>
            </div>
            <div>
                <table style="margin-bottom:20px;">
                    <tr>
                        <th style="width:20%">
                            <b>Prompt:</b>
                        </th>
                        <td>
                            <textarea class="prompt" name="prompt" id="searchQuery"
                                      placeholder="Search text or phrase"></textarea>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" style="text-align: center">
                            <div style="padding:10px;height:75px; text-align: center">
                                <div class="loader" style="display:none;height:40px;padding:10px;"
                                     id="loaderChat"></div>
                                <button id="submitChat" class="button dijit dijitReset dijitInline dijitButton"
                                        onclick="doSearchChatJson()">
                                    Submit
                                </button>
                            </div>
                        </td>
                    </tr>
                </table>

                <div>
                    <textarea id="answerChat" style="overflow: auto;white-space: pre-wrap;"></textarea>
                    <div id="semanticSearchResults"></div>
                </div>
            </div>
        </div>
    </div>


    <div id="content-2">
        <h2>Manage Embeddings / Indexes</h2>

        <div style="display: grid;grid-template-columns: 45% 55%;">
            <div style="border-right:1px solid #eeeeee;margin-right:40px;padding-right: 40px">

                <form id="createUpdateIndex" onsubmit="return false;">

                    <table style="width:100%">
                        <tr>
                            <th style="width:30%">
                                Index Name
                            </th>
                            <td>
                                <input type="text" name="indexName" value="default"><br>
                                Index Name to create or append
                            </td>
                        </tr>
                        <tr>
                            <th style="width:30%">
                                Content Query to Index:
                            </th>
                            <td>
                                    <textarea class="prompt" name="query"
                                              placeholder="e.g. +contentType:blog"></textarea>
                            </td>
                        </tr>
                    </table>
                    <fieldset style="margin:20px 20px 20px 0px;border-bottom:0px;border-left: 0px;border-right: 0px;">
                        <legend style="background-color:rgba(0, 0, 0, 0);">What To Embed (Optional)</legend>
                        <table style="width:100%">
                            <tr>
                                <td colspan="2" style="text-align: center">
                                    Optionally, you can specify what field or fields of your content you want to include in the embeddings.  Leave these blank and dotCMS will try to guess what fields to use when generating embedddings.
                                </td>
                            </tr>
                            <tr>
                                <th style="width:30%">
                                    Velocity Template to embed:
                                </th>
                                <td>
                                <textarea class="prompt" name="velocityTemplate" placeholder="e.g.&#10;$contentlet.shortDescription&#10;$contentlet.body.toHtml()"></textarea>
                                    <br>
                                    Use velocity to build exactly how you want to embed your content.
                                </td>
                            </tr>
                            <tr>
                                <th style="width:30%">
                                    Or Field Variable(s)
                                </th>
                                <td>
                                    <input type="text" value="" name="fields">
                                    <br>
                                    If you just specify a comma separated list of fields variables, dotCMS will use them to generate the embedding.
                                </td>
                            </tr>
                        </table>
                    </fieldset>
                    <table style="width:100%">
                        <tr>
                            <td colspan="2" style="text-align: center">
                                <button onclick="doBuildIndex()"
                                        class="button dijit dijitReset dijitInline dijitButton">Build Index
                                </button>
                            </td>
                        </tr>
                    </table>

                </form>
                <div id="buildResponse"></div>
            </div>

            <div>
                <h3>Indexes &nbsp;</h3>
                <table id="indexManageTable" style="width:80%">

                </table>


            </div>
        </div>
    </div>


    <div id="content-3">
        <h2>AI/Embeddings Config</h2>
        <div id="configTable" style="max-width: 800px">


        </div>

        <div style="padding:20px;border:1px solid darkgray;max-width:800px;margin:30px;">
            These values can be changed by adding/editing them in the <a
                href="/dotAdmin/#/apps/dotAI/edit/SYSTEM_HOST"
                target="_top">App screen</a> either as a
            setting or
            as a custom property.
        </div>

    </div>
</div>

</body>
</html>
