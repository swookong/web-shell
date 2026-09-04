function WebSocketClient() {
    this._gen = 0;
    this._connection = null;
}

WebSocketClient.prototype.getWebSocketUrl = function () {
    let protocol = 'ws://';
    if (window.location.protocol === 'https:') {
        protocol = 'wss://';
    }
    return protocol + window.location.host + '/shell';
};

WebSocketClient.prototype.connect = function (params) {
    if (this._connection) {
        try { this._connection.close(); } catch (e) {}
        this._connection = null;
    }

    var self = this;
    var gen = ++this._gen;

    if (!window.WebSocket) {
        params.onError('WebSocket Not Supported');
        return;
    }

    var ws = new WebSocket(this.getWebSocketUrl());
    this._connection = ws;

    ws.onopen = function () {
        if (gen === self._gen) {
            params.onConnect();
        }
    };

    ws.onmessage = function (evt) {
        if (gen === self._gen) {
            params.onData(evt.data.toString());
        }
    };

    ws.onclose = function (evt) {
        if (gen === self._gen) {
            self._connection = null;
            params.onClose();
        }
    };
};

WebSocketClient.prototype.send = function (params) {
    if (this._connection && this._connection.readyState === WebSocket.OPEN) {
        this._connection.send(JSON.stringify(params));
    }
};

WebSocketClient.prototype.sendClientData = function (data) {
    if (this._connection && this._connection.readyState === WebSocket.OPEN) {
        this._connection.send(JSON.stringify({"operate": "command", "command": data}));
    }
};

WebSocketClient.prototype.disconnect = function () {
    if (this._connection) {
        this._gen++;
        try { this._connection.close(); } catch (e) {}
        this._connection = null;
    }
};