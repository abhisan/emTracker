var PORT=8082;
var MQTT_PORT = 1884;
var MATT_ENDPOINT = "104.200.17.97";
var express = require('express');
var app = express();
var server = require('http').Server(app);
var io = require('socket.io')(server);
var bodyParser = require('body-parser');
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());
var router = express.Router();
var routes = {};
var mqtt = require('mqtt')
var geolib = require('geolib');
var client = mqtt.connect({ port: MQTT_PORT, host: MATT_ENDPOINT, keepalive: 10000});
var points = [];
var rest = require('restler');
var EM_SERVER = "http://104.200.17.97:3000/api/routes/"; //4413a850-7d9b-11e5-bb2f-fb559c85c423
io.on('connection', function (socket) {
	console.log("connection request");
	socket.emit('ack', {});
	socket.on("register_tracker", function(data){
		console.log("register_tracker " + JSON.stringify(data));
		getRoute(data.routeId, function(result){
			var  route = result.responseData[0];
			routes[route.vehicleRouteId] = route;
			socket.join(route.vehicleRouteId);
		})
	});
	socket.on("subscribe_location", function(data){
		console.log("subscribe_location " + JSON.stringify(data));
		socket.join(data.routeId);
	});
	socket.on("update_location", function(data){
		console.log("update_location "+ JSON.stringify(data));
		updateClientsMap(data);
		stop = mapCurrentLocationToStop(data);
		if(stop){
			console.log("Matched Stop " + stop.stopName);
			sendPushyNotificationToClient(data.routeId, stop.stopName);
		}
	}); 
	socket.on('disconnect', function() {
	});
});
function updateClientsMap(data){
	io.to(data.routeId).emit("location_updated",  data);
}
function sendPushyNotificationToClient(topic, message){
	var message = {"NOTIFICATION":message};
	client.publish(topic, JSON.stringify(message));
}
function mapCurrentLocationToStop(data) {
	console.log("mapCurrentLocationToStop:" + data.routeId)
	route = getRouteByRouteId(data.routeId);
	for (var index in route.routeStops) {
		var stop = route.routeStops[index];
		var isPointInCircle = geolib.isPointInCircle(
			{latitude: data.lat, longitude: data.lng},
			{latitude: stop.latLng['lat'], longitude: stop.latLng['lng']},40);
		console.log("stop.visited" + stop.visited);
		if ((stop.visited == undefined || stop.visited == false) && isPointInCircle) {
			stop.visited = true;
			return stop;
		}
	}
	return undefined;
}
function getRouteByRouteId(routeId){
	return routes[routeId];
}
function getRoute(routeId, callBack){
	rest.get(EM_SERVER+routeId).on('complete', function(result) {
		if (result instanceof Error) {
			console.log('Error:', result.message);
		} else {
			callBack(result);
		}
	});
}
app.use('/api', router);
console.log("Starting server on: "+PORT)
server.listen(PORT);