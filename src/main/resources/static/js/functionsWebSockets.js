// Konfiguracja adresu WebSocket (pobranie z hosta)
var wsUri = "ws://" + location.host + "/websocketEndPointJSON";
var websocket;

/**
 * Inicjalizacja połączenia WebSocket
 */
function initWebSocket() {
    websocket = new WebSocket(wsUri);

    // Obsługa odebranych wiadomości
websocket.onmessage = function(evt) {
    var data = evt.data;

    if (data.startsWith("USERS:")) {
        var count = data.split(":")[1];
        document.querySelector('.users-count span').textContent = count;
    }
    else if (data.startsWith("AUCTIONS:")) {
        var count = data.split(":")[1];
        document.querySelector('.all-auctions span').textContent = count;
    }
};

    websocket.onopen = function(evt) {
        console.log("Połączenie WebSocket otwarte");
    };

    websocket.onclose = function(evt) {
        console.log("Połączenie WebSocket zamknięte");
    };

    websocket.onerror = function(evt) {
        console.error("Błąd WebSocket: " + evt.data);
    };
}

/**
 * Funkcja do wysyłania wiadomości do serwera
 */
function dosend(message) {
    if (websocket.readyState === WebSocket.OPEN) {
        websocket.send(message);
    } else {
        console.error("WebSocket nie jest otwarty. Stan: " + websocket.readyState);
    }
}

// Inicjalizacja połączenia po załadowaniu strony
window.addEventListener("load", initWebSocket, false);