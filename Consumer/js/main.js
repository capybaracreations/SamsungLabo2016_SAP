
var SAAgent = null;
var SASocket = null;
var CHANNELID = 104;
var ProviderAppName = "GearPlayerProvider";

var volume = 100;
var playing = true;

function log(log_string) {
	console.log(log_string);
}

function onerror(err) {
	console.log("err [" + err + "]");
}

var agentCallback = {
	onconnect : function(socket) {
		SASocket = socket;
		log("Connection established");
		SASocket.setSocketStatusListener(function(reason){
			log("Connection lost, Reason : [" + reason + "]");
			disconnect();
		});
		SASocket.setDataReceiveListener(onreceive);
	},
	onerror : onerror
};

var peerAgentFindCallback = {
	onpeeragentfound : function(peerAgent) {
		try {
			if (peerAgent.appName == ProviderAppName) {
				SAAgent.setServiceConnectionListener(agentCallback);
				SAAgent.requestServiceConnection(peerAgent);
			} else {
				log("Bad application : " + peerAgent.appName);
			}
		} catch(err) {
			log("exception [" + err.name + "] msg[" + err.message + "]");
		}
	},
	onerror : onerror
}

function onsuccess(agents) {
	try {
		if (agents.length > 0) {
			SAAgent = agents[0];

			SAAgent.setPeerAgentFindListener(peerAgentFindCallback);
			SAAgent.findPeerAgents();
		} else {
			log("Not found SAAgent!!");
		}
	} catch(err) {
		log("exception [" + err.name + "] msg[" + err.message + "]");
	}
}

function onreceive(channelId, data) {
	log(data);
}

function fetch(action) {
	try {
		SASocket.sendData(CHANNELID, action);
	} catch(err) {
		log("exception [" + err.name + "] msg[" + err.message + "]");
	}
}

function exit() {
	fetch("exit");
	tizen.application.getCurrentApplication().exit();
	disconnect();
}

function prev() {
	fetch("prevbutton");
}

function play() {
	if (playing) {
		fetch("pause");
		document.getElementById("playbutton").style.backgroundImage =   "url(/images/play.png)";
		playing = false;
	} else {
		fetch("play");
		document.getElementById("playbutton").style.backgroundImage =   "url(/images/pause.png)";
		playing = true;
	}
}

function next() {
	fetch("nextbutton");
}

function changeVolume() {
	if (volume == 0) {
		fetch("volume33");
		document.getElementById("volumebutton").style.backgroundImage = "url(/images/volume33.png)";
		volume = 33;
	}
	else if (volume == 33) {
		fetch("volume66");
		document.getElementById("volumebutton").style.backgroundImage = "url(/images/volume66.png)";
		volume = 66;
	}
	else if (volume == 66) {
		fetch("volume100");
		document.getElementById("volumebutton").style.backgroundImage = "url(/images/volume100.png)";
		volume = 100;
	}
	else if (volume == 100) {
		fetch("volume0");
		document.getElementById("volumebutton").style.backgroundImage = "url(/images/volume0.png)";
		volume = 0;
	}
}

function disconnect() {
	try {
		if (SASocket != null) {
			SASocket.close();
			SASocket = null;
			log("closeConnection");
		}
	} catch(err) {
		log("exception [" + err.name + "] msg[" + err.message + "]");
	}
}

function connect() {
	if (SASocket) {
		log('Already connected!');
        return false;
    }
	try {
		webapis.sa.requestSAAgent(onsuccess, function (err) {
			log("err [" + err.name + "] msg[" + err.message + "]");
		});
	} catch(err) {
		log("exception [" + err.name + "] msg[" + err.message + "]");
	}
}

window.onload = function () {
    document.addEventListener('tizenhwkey', function(e) {
        if(e.keyName == "back")
            tizen.application.getCurrentApplication().exit();
    });
    
    connect();
};