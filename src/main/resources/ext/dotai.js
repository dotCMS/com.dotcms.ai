const changeTabs = async () => {
    var ele = document.getElementsByName('tab-group');
    for (i = 0; i < ele.length; i++) {
        document.getElementById('content-' + (i + 1)).style.display = ele[i].checked ? "block" : "none";
    }
    if(ele[3].checked){
        loadConfigs()
    }
};

const dotAiState ={} ;


const getText = async (callback) => {

    const prompt = document.getElementById("promptChat").value
    document.getElementById("submitChat").style.display = "none";
    document.getElementById("loaderChat").style.display = "block";
    document.getElementById("answerChat").innerText = "";

    const temperature = document.getElementById("temperatureChat").value;
    const streaming   = document.getElementById("streamingResponseType").checked;
    const indexName   = document.getElementById("indexNameChat").value;

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
            document.getElementById("answerChat").innerText = json.openAiResponse.choices[0].message.content
                + "\n\n------\nJSON Response\n------\n" + JSON.stringify(json, null, 2);
        });
        document.getElementById("loaderChat").style.display = "none";
        document.getElementById("submitChat").style.display = "";
        return;


    }


    const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();

    document.getElementById("loaderChat").style.display = "none";
    document.getElementById("submitChat").style.display = "";

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
            document.getElementById("answerChat").innerText += json.choices[0].delta.content;
        });
        if (dataDone) break;
    }
};





const refreshIndexes = async () => {
    dotAiState.indexes=[];
    fetch("/api/v1/ai/embeddings/indexCount")
        .then(response => response.json())
        .then(options => {
            for (const [key, value] of Object.entries(options.indexCount)) {
                let entry = {}
                entry.name=key;
                entry.contents=value.contents;
                entry.fragments=value.fragments;
                dotAiState.indexes.push(entry);
            }
        })
        .then(()=>{
            writeIndexesToDropdowns()
        })


};


const writeIndexesToDropdowns = async () => {
    const indexName = document.getElementById("indexNameChat");
    const indexToSearch = document.getElementById("indexToSearch");

    for (i=0;dotAiState.indexes.length;i++) {
        const newOption = document.createElement("option");
        newOption.value = dotAiState.indexes[i].name;
        newOption.text = `${dotAiState.indexes[i].name}   - (content:${dotAiState.indexes[i].contents}, fragments:${dotAiState.indexes[i].fragments})`

        indexToSearch.appendChild(newOption.cloneNode(true));

        indexName.appendChild(newOption);

    }

};


const getTypesAndFields = async () => {
    const contentTypes = [];

    fetch("/api/v1/contenttype?orderby=modDate&direction=DESC&per_page=40")
        .then(response => response.json())
        .then(options => {

            for (i=0;i<options.entity.length;i++) {
                let type = options.entity[i];
                let entry={};
                entry

            }
        })
        .then(()=>{

        })


};








const loadConfigs = async () => {

    const configTable = document.getElementById("configTable")


    configTable.innerHTML = "";

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
    refreshIndexes();
});


const tab4  =() => {

    loadConfigs();
};


const doSearch = async () => {
    const semanticSearchResults = document.getElementById("semanticSearchResults")
    const formDataRaw = new FormData(document.getElementById("searchForm"))

    const formData = Object.fromEntries(
        Array.from(formDataRaw.keys()).map(key => [
            key, formDataRaw.getAll(key).length > 1 ?
                formDataRaw.getAll(key) : formDataRaw.get(key)
        ])
    )

    console.log("formData", formData)

    semanticSearchResults.innerHTML = "";
    const headerDiv = document.createElement("h2");
    headerDiv.innerHTML = "Results";
    semanticSearchResults.appendChild(headerDiv);

    const table = document.createElement("table");
    semanticSearchResults.appendChild(table);


    fetch("/api/v1/ai/search", {
        method: "POST",
        body: JSON.stringify(formData),
        headers: {
            "Content-Type": "application/json"
        }
    })
        .then(response => response.json())
        .then(data => {

            let tr = document.createElement("tr");
            tr.style.fontWeight="bold";
            tr.style.textAlign="left"
            let td1 = document.createElement("th");
            let td2 = document.createElement("th");
            let td3 = document.createElement("th");
            let td4 = document.createElement("th");

            td1.className="hTable"
            td2.className="hTable"
            td3.className="hTable"
            td4.className="hTable"

            td1.innerHTML="Title"
            td2.innerHTML="Matches"
            td3.innerHTML="Distance"
            td4.innerHTML="Top Match"

            tr.append(td1);
            tr.append(td2);
            tr.append(td3);
            tr.append(td4);

            table.append(tr)


            data.dotCMSResults.map(row =>{
                console.log("row", row)
                tr = document.createElement("tr");
                td1 = document.createElement("td");
                td2 = document.createElement("td");
                td3 = document.createElement("td");
                td4 = document.createElement("td");

                td1.innerHTML=`<a href="/dotAdmin/#/c/content/${row.inode}" target="_top">${row.title}</a>`;
                td2.innerHTML= row.matches.length ;
                td3.innerHTML= parseFloat(row.matches[0].distance).toFixed(2) ;
                td4.innerHTML=row.matches[0].extractedText;

                tr.append(td1);
                tr.append(td2);
                tr.append(td3);
                tr.append(td4);
                table.append(tr)



            })

        })
};
