<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>dotAI</title>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <link rel="icon" type="image/x-icon" href="favicon.ico"/>
    <script type="application/javascript">
        <%@include file = "dotai.js" %>
    </script>
    <style>
        <%@include file = "dotai.css" %>
    </style>
</head>


<body>

<div id="container">
    <input id="tab-1" type="radio" name="tab-group" checked="checked" onclick="changeTabs()"/>
    <label for="tab-1">Search and Chat with dotCMS</label>

    <input id="tab-3" type="radio" name="tab-group" onclick="changeTabs()"/>
    <label for="tab-3">Manage Embeddings/Indexes</label>

    <input id="tab-4" type="radio" name="tab-group" onclick="changeTabs();"/>
    <label for="tab-4">Config Values</label>

</div>

<div id="content">
    <div id="content-1">
        <div style="display: grid;grid-template-columns: 50% 50%;">
            <div>
                <h2>Semantic Search and Chat with dotCMS</h2>
                <table style="width:80%">
                    <form action="POST" id="chatForm" onsubmit="return false;">
                        <tr>
                            <th>
                                Source content index to use:
                            </th>
                            <td>
                                <select name="indexName" id="indexNameChat">
                                    <option disabled="true" placeholder="Select an Index">Select an Index</option>
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
                                    <input type="radio" id="streamingResponseType" name="responseType" value="stream" onchange="showResultTables()">
                                    <label for="streamingResponseType">Streaming Chat &nbsp; &nbsp; (OpenAI + dotCMS Supporting Content)</label>
                                </div>
                                <div>
                                    <input type="radio" id="restJsonResponseType" name="responseType" value="json" onchange="showResultTables()">
                                    <label for="restJsonResponseType">REST/JSON Chat &nbsp; &nbsp; (OpenAI + dotCMS Supporting Content)</label>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Temperature:
                            </th>
                            <td>
                                <input name="temperature" type="number" step="0.1" value="1" min="0" max="2"><br>
                                (determines the randomness of the response. 0 = deterministic, 2 = most random
                            </td>
                        </tr>


                        <tr>
                            <th>Search Query:</th>
                            <td>
                            <textarea class="prompt" name="query"
                                      placeholder="Search text or phrase">best beach</textarea>
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
                                Search stored embeddings using this operator. (probably best to leave it alone)
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Distance Threshold:
                            </th>
                            <td>
                                <input type="number" step="0.05" value=".25" name="threshold" min="0.05"
                                       max="100"><br>
                                the lower this number, the more semantically similar the results
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Site:
                            </th>
                            <td>
                                <input type="text" value="" name="site"><br>
                                site id on which the content lives - leave blank for all
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Content Type Var:
                            </th>
                            <td>
                                <input type="text" value="" name="contentType" id="contentTypeSearch"><br>
                                content type var you would like to search - leave blank for all
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Field Var:
                            </th>
                            <td>
                                <input type="text" value="" name="fieldVar" id="fieldVarSearch"><br>
                                field var you would like to search - leave blank for all
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="text-align: center">
                                <div style="padding:10px;height:75px; text-align: center">
                                    <div class="loader" style="display:none;height:40px;padding:10px;" id="loaderChat"></div>
                                    <button id="submitChat" class="button dijit dijitReset dijitInline dijitButton"
                                            onclick="doSearchChatJson()">
                                        Submit
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </form>
                </table>
            </div>
            <div>
                <div style="padding-bottom:10px">
                    <h2>Results</h2>
                </div>
                <div>
                    <textarea id="answerChat" style="overflow: auto;white-space: pre-wrap;"></textarea>
                    <div id="semanticSearchResults"></div>
                </div>
            </div>
        </div>
    </div>


    <div id="content-2">
        <h2>Manage Embeddings / Indexes</h2>

        <div style="display: grid;grid-template-columns: 40% 60%;">
            <div>
                <h3>Indexes</h3>
                <table id="indexManageTable" style="width:80%">

                </table>
            </div>
            <div>
                <h3>Create/Update Index</h3>
                <form id="createUpdateIndex" onsubmit="return false;">
                    <table>
                        <tr>
                            <th>
                                Index Name
                            </th>
                            <td>
                                <input type="text" value="" name="indexName"><br>
                                index to create or append
                            </td>
                        </tr>
                        <tr>
                            <th>Content to index:</th>
                            <td>
                            <textarea class="prompt" name="query"
                                      placeholder="lucene content search"></textarea>
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Field Name
                            </th>
                            <td>
                                <input type="text" value="" name="fieldVar"><br>
                               specific field to index (optional)
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="text-align: center">
                                <button onclick="doBuildIndex()" class="button dijit dijitReset dijitInline dijitButton">Build Index</button>
                            </td>
                        </tr>
                    </table>
                </form>
            </div>
            <div id="buildResponse"></div>
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
