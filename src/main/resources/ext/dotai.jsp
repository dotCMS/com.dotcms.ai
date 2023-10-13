<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>dotAI</title>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <link rel="icon" type="image/x-icon" href="favicon.ico"/>
    <script>

        const changeTabs = async () => {
            var ele = document.getElementsByName('tab-group');
            for (i = 0; i < ele.length; i++) {
                document.getElementById('content-' + (i + 1)).style.display = ele[i].checked ? "block" : "none";
            }
        };

        const getText = async (callback) => {

            const prompt = document.getElementById("prompt").value
            document.getElementById("submit").style.display = "none";
            document.getElementById("loader").style.display = "block";
            document.getElementById("answer").innerText = "";

            const temperature = document.getElementById("temperature").value;

            const streaming = document.getElementById("streamingResponseType").checked;
            const indexName = document.getElementById("indexName").value;

            const response = await fetch('/api/v1/ai/completions', {

                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    query: prompt,
                    indexName: indexName,
                    stream: streaming,
                    temperature: temperature
                }),
            });


            if (!streaming) {

                response.json().then(json => {
                    console.log("json", json)
                    document.getElementById("answer").innerText = json.results.choices[0].message.content
                        +  "\n\n------\nJSON Response\n------\n" + JSON.stringify(json, null, 2);
                });
                document.getElementById("loader").style.display = "none";
                document.getElementById("submit").style.display = "";
                return;


            }


            const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();

            document.getElementById("loader").style.display = "none";
            document.getElementById("submit").style.display = "";

            if (!reader) return;
            // eslint-disable-next-line no-constant-condition
            while (true) {
                // eslint-disable-next-line no-await-in-loop
                const {value, done} = await reader.read();
                if (done) break;
                let dataDone = false;
                const arr = value.split('\n');
                arr.forEach((data) => {
                    if (data.trim().length === 0) return; // ignore empty message
                    if (data.startsWith(':')) return; // ignore sse comment message
                    if (data === 'data: [DONE]') {
                        dataDone = true;
                        return;
                    }

                    const json = JSON.parse(data.substring(6));
                    if (json.choices[0].delta.content == null) return;
                    document.getElementById("answer").innerText += json.choices[0].delta.content;
                });
                if (dataDone) break;
            }
        };


        const displayOption = async () => {
            const indexName = document.getElementById("indexName");
            const indexToSearch = document.getElementById("indexToSearch");


            fetch("/api/v1/ai/embeddings/indexCount")
                .then(response => response.json())
                .then(options => {

                    for (const [key, value] of Object.entries(options.indexCount)) {
                        const newOption = document.createElement("option");
                        newOption.value = key;
                        newOption.text = `${key}   - (content:${value.contents}, fragments:${value.fragments})`

                        indexToSearch.appendChild(newOption.cloneNode(true));

                        indexName.appendChild(newOption);


                    }
                })
        };

        const loadConfigs = async () => {
            const configTable = document.getElementById("configTable")


            configTable.innerHTML = "";
            const headerDiv = document.createElement("h2");
            headerDiv.innerHTML = "OpenAi Configuration";
            configTable.appendChild(headerDiv);
            const table = document.createElement("table");
            table.className = "propTable";
            configTable.appendChild(table);
            await fetch("/api/v1/ai/completions/config")
                .then(response => response.json())
                .then(configProps => {

                    for (const [key, value] of Object.entries(configProps)) {

                        const tr = document.createElement("tr");

                        const th = document.createElement("th");
                        th.className = "propTh";
                        const td = document.createElement("td");
                        td.className = "propTd";
                        table.appendChild(tr)
                        tr.appendChild(th);
                        tr.appendChild(td);
                        th.innerHTML = key;
                        td.innerHTML = value;

                    }
                })
        };

        window.addEventListener('load', function () {
            displayOption();
            loadConfigs();
        });


    </script>


    <style>

        #container {
            margin: 18px auto 0px auto;
            width: 98%;
            overflow: hidden;

        }

        #container input[type="radio"] {

            visibility: hidden;
        }

        #container label {
            background: #f9f9f9;
            border-radius: .25em .25em 0 0;
            color: #888;
            cursor: pointer;
            display: block;
            float: left;
            font-size: 1em;
            height: 2.5em;
            line-height: 2.5em;
            margin-right: .25em;
            padding: 0 1.5em;
            text-align: center;

        }

        #container input:hover + label {
            background: #ddd;
            color: #666;
        }

        #container input:checked + label {
            background: #f1f1f1;
            color: #444;
            position: relative;
            z-index: 6;
        }

        #content {
            background: #f1f1f1;
            border-radius: 0 .25em .25em .25em;
            min-height: 20em;
            width: 98%;
            margin: 0 auto;
            text-align: left;
            z-index: 5;
        }

        #content-1 {
            display: block;
            padding:20px;
        }

        #content-2 {
            display: none;
            padding:20px;
        }

        #content-3 {
            display: none;
            padding:20px;
        }

        #submit {
            padding: 20px 40px;

        }

        input[type="number"] {
            font-size: large;
            padding: 10px;
            min-width: 100px;
        }

        input[type="radio"] {

            padding: 10px;

        }

        textarea {
            font-size: large;
            padding: 10px;
        }

        select {
            font-size: large;
            padding: 10px;
        }

        button {
            font-size: large;
            padding: 5px 20px;
            border: 1px solid #9D9D9D;
            border-radius: .25em;

        }
        th{
            padding:1em;
            vertical-align: top;
            text-align: right;
            margin-right:20px;
        }
        td{
            padding:1em;
            vertical-align: top;
        }


        .propTh {
            padding: 5px;
            font-weight: bold;


        }

        .propTd {
            max-width: 400px;
            overflow-wrap: break-word;
            padding: 5px;

        }
    </style>
</head>
<body>

<div id="container">
    <input id="tab-1" type="radio" name="tab-group" checked="checked" onclick="changeTabs()"/>
    <label for="tab-1">Ask dotCMS.com a question</label>

    <input id="tab-2" type="radio" name="tab-group" onclick="changeTabs()"/>
    <label for="tab-2">Indexes</label>

    <input id="tab-3" type="radio" name="tab-group" onclick="changeTabs()"/>
    <label for="tab-3">Config Values</label>
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
                            <input type="number" step="0.1" value="1" id="temperature" min="0" max="2" ><br>
                            (determines the randomness of the response.  0 = deterministic, 2 = most random
                        </td>
                    </tr>



                    <tr>
                        <td colspan="2">
                            <textarea name="prompt" id="prompt" style="width:100%;min-height:200px;"
                                      placeholder="Ask me a question">What are nice beaches?</textarea>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <div style="padding:10px;height:75px; text-align: center">
                                <div class="loader" style="display:none;height:40px;padding:10px;" id="loader"></div>
                                <button id="submit" class="dijit dijitReset dijitInline dijitButton"
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

    <div id="content-2" style="padding:20px">
        <form action="POST" onsubmit="return false;">
            <h2>Semantic Searching</h2>
            <table style="width:80%">
                <tr>
                    <td>Index to Search:</td>
                    <td>
                        <select name="indexToSearch" id="indexToSearch">
                            <option disabled="true" placeholder="Select an Index">Select an Index</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>Search Text or Phrase:</td>
                    <td>
                        <textarea placeholder="Search text or phrase"></textarea>
                    </td>
                </tr>
                <tr>
                    <th>Operator:</th>
                    <td>
                        <div style="height:30px;"><input type="radio" name="operator" id="cosine" value="cosine"><label
                                for="cosine">Cosine Similarity</label></div>
                        <div style="height:30px;"><input type="radio" name="operator" id="ltDev" value="dev"
                                                         onchange="setExpire()"><label for="ltDev"">Dev &nbsp; &nbsp;
                            (default 1 year)</label></div>
                        <div style="height:30px;"><input type="radio" name="operator" id="ltprod" value="prod"
                                                         onchange="setExpire()"><label for="ltprod">Prod &nbsp; &nbsp;
                            (default 1 year)</label><br></div>
                    </td>
                </tr>
            </table>
        </form>

    </div>


    <div id="content-3" style="padding:20px">
        <div id="configTable" style="max-width: 800px">


        </div>

        <div style="padding:20px;border:1px solid darkgray;max-width:800px;margin:30px;">
            These values can be changed by adding/editing them in the <a href="/dotAdmin/#/apps/dotAI/edit/SYSTEM_HOST"
                                                                         target="_top">App screen</a> either as a
            setting or
            as a custom property.
        </div>

    </div>
</div>


</body>
</html>
