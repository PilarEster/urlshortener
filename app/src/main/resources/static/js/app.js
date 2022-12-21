document
    .getElementById('shortener')
    .addEventListener('submit', getData);

document
    .getElementById('ranking')
    .addEventListener('submit', getRanking);

document
    .getElementById('ranking-users')
    .addEventListener('submit', getUsers);

function getData(event) {
    event.preventDefault();
    getURL(document.getElementsByName('url').item(0).value,
        document.getElementsByName('qr').item(0).checked);
}

function getURL(url, qr){
    let encodedBody = new URLSearchParams();
    encodedBody.append('url', url);
    encodedBody.append('qr', qr ? "true" : "false");

    const options = {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: encodedBody

    };

    fetch('http://localhost:8080/api/link', options)
        .then(response => {
            if(!response.ok) {
                throw Error(response.status)
            }
            return response.json()
        })
        .then(response => {
            if (response.properties.qr) {
                document.getElementById('result').innerHTML =
                    `<div class='alert alert-success lead'>
                    <a target='_blank' href="${response.url}">${response.url}</a>
                    <a target='_blank' href="${response.properties.qr}">${response.properties.qr}</a>
                    </div>`;
            }
            else {
                document.getElementById('result').innerHTML =
                    `<div class='alert alert-success lead'>
                    <a target='_blank' href="${response.url}">${response.url}</a>
                    </div>`;
            }
        })
        .catch(() =>
            document.getElementById('result').innerHTML =
                `<div class='alert alert-danger lead'>ERROR</div>`
        );
}

function getRanking(){
    event.preventDefault();
    fetch('http://localhost:8080/api/link')
        .then(response => {
            if(!response.ok) {
               throw Error(response.status)
            }
            return response.json()
        })
        .then(response => {
            response.list.forEach(e => {
                document.getElementById('list').innerHTML += `<li>${e.hash}     ${e.sum}</li>`
            })
        })
        .catch(() =>
            document.getElementById('result-list').innerHTML =
                `<div class='alert alert-danger lead'>ERROR</div>`
        );
}

function getUsers(){
    event.preventDefault();
    fetch('http://localhost:8080/api/link/{id}')
        .then(response => {
            if(!response.ok) {
               throw Error(response.status)
            }
            return response.json()
        })
        .then(response => {
            response.list.forEach(e => {
                document.getElementById('users').innerHTML += `<li>${e.ip}     ${e.sum}</li>`
            })
        })
        .catch(() =>
            document.getElementById('result-list-users').innerHTML =
                `<div class='alert alert-danger lead'>ERROR</div>`
        );
}