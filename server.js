//Server	
var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server);
var sendNotification = function sendNotification(data){
	//TODO
}
var saveLocation = function (data){
	//TODO
}
var updateClients = function updateClients(data){
	
	io.to(data.trackerId).emit("location_updated",  data);
}
io.on('connection', function (socket) {
	console.log("connection request");
	socket.emit('ack', {});
	socket.on("register_tracker", function(data){
		console.log("register_tracker " + JSON.stringify(data));
		socket.join(data.trackerId);
	});
	socket.on("subscribe_location", function(data){
		console.log("subscribe_location " + JSON.stringify(data));
		socket.join(data.trackerId);
	});
	socket.on("update_location", function(data){
		console.log("location_updated "+ JSON.stringify(data));
		updateClients(data);
		sendNotification(data);
		saveLocation(data);
	}); 
});

server.listen(80);