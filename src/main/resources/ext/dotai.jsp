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
    <label for="tab-1">Chatting with dotCMS</label>

    <input id="tab-2" type="radio" name="tab-group" onclick="changeTabs()"/>
    <label for="tab-2">Semantic Searching</label>

    <input id="tab-3" type="radio" name="tab-group" onclick="changeTabs()"/>
    <label for="tab-3">Manage Embeddings/Indexes</label>

    <input id="tab-4" type="radio" name="tab-group" onclick="changeTabs();loadConfigs();"/>
    <label for="tab-4">Config Values</label>

</div>

<div id="content">
    <div id="content-1">
        <div style="display: grid;grid-template-columns: 50% 50%;">
            <div>
                <h2>Chatting with dotCMS</h2>
                <table style="width:80%">
                    <tr>
                        <th>
                            Index to chat with:
                        </th>
                        <td>
                            <select name="indexName" id="indexName">
                                <option disabled="true" placeholder="Select an Index">Select an Index</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <th>
                            <label>Response Type:</label>
                        </th>
                        <td>
                            <input type="radio" checked="true" id="streamingResponseType" name="responseType">
                            <label for="streamingResponseType">Streaming</label>
                            &nbsp; &nbsp; &nbsp;
                            <input type="radio" id="restJsonResponseType" name="responseType">
                            <label for="restJsonResponseType">REST/JSON</label>

                        </td>
                    </tr>
                    <tr>
                        <th>
                            Temperature:
                        </th>
                        <td>
                            <input type="number" step="0.1" value="1" id="temperature" min="0" max="2"><br>
                            (determines the randomness of the response. 0 = deterministic, 2 = most random
                        </td>
                    </tr>


                    <tr>
                        <th>Search Text or Phrase:</th>
                        <td>
                            <textarea class="prompt" name="prompt" placeholder="Search text or phrase"></textarea>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" style="text-align: center">
                            <div style="padding:10px;height:75px; text-align: center">
                                <div class="loader" style="display:none;height:40px;padding:10px;" id="loader"></div>
                                <button id="submit" class="button dijit dijitReset dijitInline dijitButton"
                                        onclick="getText()">
                                    Submit
                                </button>
                            </div>
                        </td>
                    </tr>

                </table>
            </div>
            <div>
                <pre id="answer" style="overflow: auto;white-space: pre-wrap;"></pre>
            </div>
        </div>


    </div>

    <div id="content-2">

        <h2>Semantic Searching</h2>
        <div style="display: grid;grid-template-columns: 50% 50%;">
            <div>
                <form action="POST" id="searchForm" onsubmit="return false;">
                    <table style="width:80%">
                        <tr>
                            <th>Index to Search:</th>
                            <td>
                                <select name="indexName" id="indexToSearch">
                                    <option disabled="true" placeholder="Select an Index">Select an Index</option>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <th>Search Text or Phrase:</th>
                            <td>
                                <textarea class="prompt" name="query" placeholder="Search text or phrase"></textarea>
                            </td>
                        </tr>
                        <tr>
                            <th>Operator:</th>
                            <td>

                                <input type="radio" name="operator" id="cosine" value="cosine">
                                <label for="cosine">Cosine Similarity</label>
                                &nbsp; &nbsp;
                                <input type="radio" name="operator" id="distance" value="distance">
                                <label for="distance">Distance</label>
                                &nbsp; &nbsp;
                                <input type="radio" name="operator" id="product" value="product">
                                <label for="product">Inner Product</label>

                            </td>
                        </tr>
                        <tr>
                            <th>
                                Threshold:
                            </th>
                            <td>
                                <input type="number" step="0.05" value=".25" name="threshold" id="threshold" min="0.05"
                                       max="100"><br>
                                (the lower this number, the more semantically similar the results)
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Site:
                            </th>
                            <td>
                                <input type="text" value="" name="site" ><br>
                                (site id on which the content lives - leave blank for all)
                            </td>
                        </tr>

                        <tr>
                            <th>
                                Content Type Var:
                            </th>
                            <td>
                                <input type="text" value="" name="contentType" id="contentTypeSearch"><br>
                                (variable for the content type you would like to search - leave blank for all)
                            </td>
                        </tr>
                        <tr>
                            <th>
                                Field Var:
                            </th>
                            <td>
                                <input type="text" value="" name="fieldVar" id="fieldVarSearch"><br>
                                (variable for the field var you would like to search - leave blank for all)
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="text-align: center">


                                <button onclick="doSearch()" class="button dijit dijitReset dijitInline dijitButton">
                                    Submit
                                </button>

                            </td>
                        </tr>

                    </table>
                </form>
            </div>
            <div>
                <div id="semanticSearchResults">


                </div>


            </div>

        </div>


        <div id="content-3" style="padding:20px">
            <h2>Manage Embeddings / Indexes</h2>
        </div>


        <div id="content-4" style="padding:20px">
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


</body>
</html>
