const changeTabs = async () => {
    let ele = document.getElementsByName('tab-group');
    for (i = 0; i < ele.length; i++) {
        document.getElementById('content-' + (i + 1)).style.display = ele[i].checked ? "block" : "none";
        document.getElementById('content-' + (i + 1)).className = ele[i].checked  ? + " dijitTabChecked" : "";

    }
    if (ele[0].checked) {
        tab1()
    } else if (ele[1].checked) {
        tab2()
    } else if (ele[2].checked) {
        tab3()
    } else if (ele[3].checked) {
        tab4()
    }
};

const dotAiState = {};


const refreshIndexes = async () => {
    dotAiState.indexes = [];
    return fetch("/api/v1/ai/embeddings/indexCount")
        .then(response => response.json())
        .then(options => {
            for (const [key, value] of Object.entries(options.indexCount)) {
                let entry = {}
                entry.name = key;
                entry.contents = value.contents;
                entry.fragments = value.fragments;
                entry.tokenTotal = value.tokenTotal;
                entry.tokensPerChunk=value.tokensPerChunk;
                dotAiState.indexes.push(entry);

            }
        })

};

const refreshConfigs = async () => {
    dotAiState.config = {};

    return fetch("/api/v1/ai/completions/config")
        .then(response => response.json())
        .then(configProps => {
            const entity = {}
            for (const [key, value] of Object.entries(configProps)) {
                entity[key] = value
            }
            dotAiState.config = entity;
        });
};

const refreshTypesAndFields = async () => {
    const contentTypes = [];

    return fetch("/api/v1/contenttype?orderby=modDate&direction=DESC&per_page=40")
        .then(response => response.json())
        .then(options => {

            for (i = 0; i < options.entity.length; i++) {
                let type = options.entity[i];
                let entry = {};
                entry

            }
        })
};

const reinitializeDatabase = async () => {
    if(!confirm("Are you sure you want to recreate the whole db?  You will lose all saved embeddings.")){
        return;
    }
    const contentTypes = [];

    return fetch("/api/v1/ai/embeddings/db", {
        method: 'DELETE',
        headers: {
            'Content-type': 'application/json'
        }} )
        .then(response => response.json())
        .then(data => {
            alert("DB dropped/created:" + data.created);

        })
};

const writeIndexesToDropdowns = async () => {
    const indexName = document.getElementById("indexNameChat");
    let options = indexName.getElementsByTagName('option');

    //console.log("options", options)
    for (i = 1; i < options.length; i++) {
        indexName.removeChild(options[i]);
    }

    for (i = 0; i < dotAiState.indexes.length; i++) {
        if(dotAiState.indexes[i].name==="cache"){
            continue;
        }
        const newOption = document.createElement("option");
        newOption.value = dotAiState.indexes[i].name;
        newOption.text = `${dotAiState.indexes[i].name}   - (contents:${dotAiState.indexes[i].contents})`

        indexName.appendChild(newOption);
    }
};

const writeModelToDropdown = async () => {
    const modelName = document.getElementById("modelName");
    let options = modelName.getElementsByTagName('option');

    for (i = 1; i < options.length; i++) {
        indexName.removeChild(options[i]);
    }

    for (i = 0; i < dotAiState.config.availableModels.length; i++) {

        const newOption = document.createElement("option");
        newOption.value = dotAiState.config.availableModels[i];
        newOption.text = `${dotAiState.config.availableModels[i]}`
        if(dotAiState.config.availableModels[i]===dotAiState.config.model){
            newOption.selected=true;
            newOption.text = `${dotAiState.config.availableModels[i]} (default)`
        }


        modelName.appendChild(newOption);
    }
};



const writeConfigTable = async () => {

    const configTable = document.getElementById("configTable")
    //console.log("config", dotAiState.config)

    configTable.innerHTML = "";

    const table = document.createElement("table");
    table.className = "propTable";
    configTable.appendChild(table);

    for (const [key, value] of Object.entries(dotAiState.config)) {
        //console.log(key)
        const tr = document.createElement("tr");
        tr.style.borderBottom = "1px solid #cccccc"
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

};
const writeIndexManagementTable = async () => {
    const indexTable = document.getElementById("indexManageTable")

    indexTable.innerHTML = "";

    let tr = document.createElement("tr");
    tr.style.fontWeight = "bold";
    tr.style.textAlign = "left"
    tr.style.borderBottom = "1px solid black"
    let td1 = document.createElement("th");
    let td2 = document.createElement("th");
    let td3 = document.createElement("th");
    let td4 = document.createElement("th");
    let td5 = document.createElement("th");
    let td6 = document.createElement("th");

    td1.className = "hTable"
    td2.className = "hTable"
    td3.className = "hTable"
    td4.className = "hTable"
    td5.className = "hTable"
    td6.className = "hTable"

    td1.innerHTML = "Index"
    td2.innerHTML = "Chunks"
    td3.innerHTML = "Content"
    td4.innerHTML = "Tokens"
    td5.innerHTML = "Tokens per Chunk"


    tr.append(td1);
    tr.append(td2);
    tr.append(td3);
    tr.append(td4);
    tr.append(td5);
    tr.append(td6);

    indexTable.append(tr)


    dotAiState.indexes.map(row => {
       //console.log("row", row)


        const cost = row.name==='cache' ? "(est $" + ((parseInt(row.tokenTotal)/1000) * 0.0001).toFixed(2) + ")" : "";



        tr = document.createElement("tr");
        tr.style.borderBottom = "1px solid silver"
        td1 = document.createElement("td");
        td2 = document.createElement("td");
        td3 = document.createElement("td");
        td4 = document.createElement("td");
        td5 = document.createElement("td");
        td6 = document.createElement("td");
        td1.innerHTML = row.name;
        td2.innerHTML = row.fragments;
        td3.innerHTML = row.contents;
        td4.innerHTML = `${row.tokenTotal} ${cost}`;
        td4.style.whiteSpace="nowrap"
        td5.innerHTML = row.tokensPerChunk;
        td6.innerHTML = `<a href="#" onclick="doDeleteIndex('${row.name}')">delete</a>`
        tr.append(td1);
        tr.append(td2);
        tr.append(td3);
        tr.append(td4);
        tr.append(td5);
        tr.append(td6);
        indexTable.append(tr)
    })


    tr = document.createElement("tr");
    td1 = document.createElement("td");
    td1.style.textAlign="right";
    td1.colSpan="100";
    td1.innerHTML=`<a href="#" onclick="reinitializeDatabase()">delete all</a>`;
    tr.append(td1);
    indexTable.append(tr)





}


window.addEventListener('load', function () {
    refreshIndexes()
        .then(() => {
            writeIndexesToDropdowns();
            writeIndexManagementTable();
        });

    refreshConfigs().then(() => {
        writeConfigTable();
        writeModelToDropdown();
        if(dotAiState.config["apiKey"]!="*****"){
            document.getElementById("openAIKeyWarn").style.display="block";
        }
    });
    showResultTables();
});


const tab1 = () => {
    refreshIndexes()
        .then(() => {
            writeIndexesToDropdowns();
            writeIndexManagementTable();
        });

}
const tab2 = () => {
    refreshIndexes()
        .then(() => {
            writeIndexesToDropdowns();
            writeIndexManagementTable();
        });


};

const tab3 = () => {

    refreshConfigs().then(() => {
        writeConfigTable();
    });


};


const showResultTables = () => {
    const searching = document.getElementById("searchResponseType").checked;
    if (searching) {
        document.getElementById("answerChat").style.display = "none";
        document.getElementById("semanticSearchResults").style.display = "block";
    } else {
        const prompt = "Current Prompt: \n\n" + dotAiState.config["com.dotcms.ai.completion.text.prompt"];
        document.getElementById("answerChat").placeholder = prompt.replaceAll('\\n', '\n').replaceAll("\\\"", "\"")
        document.getElementById("answerChat").style.display = "block";
        document.getElementById("semanticSearchResults").style.display = "none";
    }

}


const doSearchChatJson = async (callback) => {

    const formDataRaw = new FormData(document.getElementById("chatForm"))
    const formData = Object.fromEntries(Array.from(formDataRaw.keys()).map(key => [key, formDataRaw.getAll(key).length > 1 ? formDataRaw.getAll(key) : formDataRaw.get(key)]))

    const prompt = document.getElementById("searchQuery").value;
    formData.prompt=prompt;


    const responseType = formData.responseType
    delete formData.responseType;
    if (formData.prompt == undefined || formData.prompt.trim().length == 0) {
        alert("please enter a query/prompt");
        return;
    }

    document.getElementById("submitChat").style.display = "none";
    document.getElementById("loaderChat").style.display = "block";


    if (responseType === "search") {
        doSearch(formData)

    } else if (responseType === "json") {
        return doJsonResponse(formData);
    } else {
        return doChatResponse(formData);
    }
}

const doDeleteIndex = async (indexName) => {
    if (!confirm("Are you sure you want to delete " + indexName + "?")) {
        return;
    }
    let formData = {};
    formData.indexName = indexName;
    //console.log("formData", formData)
    const response = await fetch('/api/v1/ai/embeddings', {
        method: "DELETE", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    })
        .then(response => response.json())
        .then(data => {
            refreshIndexes()
                .then(() => {
                    writeIndexesToDropdowns();
                    writeIndexManagementTable();
                });
        });
}

const doBuildIndex = async () => {

    const formDataRaw = new FormData(document.getElementById("createUpdateIndex"))
    const formData = Object.fromEntries(Array.from(formDataRaw.keys()).map(key => [key, formDataRaw.getAll(key).length > 1 ? formDataRaw.getAll(key) : formDataRaw.get(key)]))

    if (formData.indexName === null || formData.indexName.trim().length == 0) {
        alert("Index Name is required");
        return;
    }

    if (formData.query === null || formData.query.trim().length == 0) {
        alert("Query is required");
        return;
    }


    //console.log("formData", formData)
    const response = await fetch('/api/v1/ai/embeddings', {
        method: "POST", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    }).then(response => response.json())
        .then(data => {
            document.getElementById("buildResponse").innerHTML = `Building index ${data.indexName} with ${data.totalToEmbed} to embed`
            setTimeout(clearIndexMessage, 5000);

        });
}


const clearIndexMessage = async () => {
    document.getElementById("buildResponse").innerHTML = "";
    refreshIndexes()
        .then(() => {
            writeIndexesToDropdowns();
            writeIndexManagementTable();
        });


}

const doJsonResponse = async (formData) => {

    const response = await fetch('/api/v1/ai/completions', {
        method: "POST", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    });
    response.json().then(json => {
        //console.log("json", json)
        document.getElementById("answerChat").value = json.openAiResponse.choices[0].message.content + "\n\n------\nJSON Response\n------\n" + JSON.stringify(json, null, 2);
    });
    resetLoader();

}


const doChatResponse = async (formData) => {

    const stream = document.getElementById("streamingResponseType").checked;

    let parsedLines = [];
    //console.log(JSON.stringify(formData));
    formData.stream = true;
    let line="";
    let lines =[];
    try {
        const response = await fetch('/api/v1/ai/completions', {
            method: "POST", body: JSON.stringify(formData), headers: {
                "Content-Type": "application/json"
            }
        });


        document.getElementById("answerChat").value="";
        // Read the response as a stream of data
        const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();
        if (!reader) return;


        while (true) {
            const { value, done } = await reader.read();
            if (done) {
                console.log("got a done:" + done);
                break;
            }
            //console.log(value);
            lines = (line + value).split('\ndata: ');
            for (line of lines) {

                line = line.replace(/^data: /, '').trim();
                if (line.length === 0) continue; // ignore empty message
                if (line.startsWith(':')) continue; // ignore sse comment message

                if (line === '[DONE]') {
                    break;
                }
                try {
                    const json = JSON.parse(line);
                    line="";
                    const value = json.choices[0].delta.content;
                    if(value === undefined){
                        continue;
                    }
                    document.getElementById("answerChat").value +=value;
                }
                catch (e){
                    // line is half sent, will append to the next value
                    console.log("line:" + line);
                }


            };

        }
    } catch(e) {

        console.log("got an error:", e);
        console.log("line:" + line);
        console.log("lines:" + lines);
    }
    resetLoader();


};


const doSearch = async (formData) => {

    //console.log("formData", formData)
    const semanticSearchResults = document.getElementById("semanticSearchResults");
    semanticSearchResults.innerHTML = "";


    const table = document.createElement("table");
    table.className="aiSearchResultsTable";
    semanticSearchResults.appendChild(table);

    const truncateString = (str,num) =>{
        if (str.length <= num) {
            return str
        }
        return str.slice(0, num) + '...'


    }



    fetch("/api/v1/ai/search", {
        method: "POST", body: JSON.stringify(formData), headers: {
            "Content-Type": "application/json"
        }
    })
        .then(response => response.json())
        .then(data => {

            let tr = document.createElement("tr");
            tr.style.fontWeight = "bold";
            tr.style.textAlign = "left"
            let td1 = document.createElement("th");
            let td2 = document.createElement("th");
            let td3 = document.createElement("th");
            let td4 = document.createElement("th");

            td1.className = "hTable"
            td2.className = "hTable"
            td3.className = "hTable"
            td4.className = "hTable"

            td1.innerHTML = "Title"
            td2.innerHTML = "Matches"
            td3.innerHTML = "Distance"
            td4.innerHTML = "Top Match"

            tr.append(td1);
            tr.append(td2);
            tr.append(td3);
            tr.append(td4);

            table.append(tr)


            data.dotCMSResults.map(row => {
                //console.log("row", row)
                tr = document.createElement("tr");
                td1 = document.createElement("td");
                td2 = document.createElement("td");
                td3 = document.createElement("td");
                td4 = document.createElement("td");

                td1.innerHTML = `<a href="/dotAdmin/#/c/content/${row.inode}" target="_top">${row.title}</a>`;
                td2.innerHTML = row.matches.length;
                td3.innerHTML = parseFloat(row.matches[0].distance).toFixed(2);
                td4.innerHTML = truncateString(row.matches[0].extractedText, 200);

                tr.append(td1);
                tr.append(td2);
                tr.append(td3);
                tr.append(td4);
                table.append(tr)


            })
            resetLoader()
        })
};

const resetLoader = () => {

    document.getElementById("submitChat").style.display = "";
    document.getElementById("loaderChat").style.display = "none";


}
