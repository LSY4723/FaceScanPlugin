var exec = require('cordova/exec');
var facePlugin = function(){};  

facePlugin.prototype.faceScan = function(agrs,success, error) {
    exec(success, error, "FacePlugin", "faceScan",agrs);
};
var FACEPLUGIN = new facePlugin();
module.exports = FACEPLUGIN; 