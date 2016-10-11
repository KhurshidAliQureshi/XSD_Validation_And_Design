var validator = require('xsd-schema-validator');

var fs = require('fs'),
xml2js = require('xml2js');
 
var parser = new xml2js.Parser();
var parseString = require('xml2js').parseString;

var express = require('express');
var multer  = require('multer');
var storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, './uploads')
  },
  filename: function (req, file, cb) {
    cb(null, file.originalname)
  }
});

var	app = express();
var http = require('http');
var server = http.createServer(app);
var upload = multer({ storage: storage }).any();


var xsdCheck = false;
var xsdFileName = "";

var _getAllFilesFromFolder = function(dir) {

    var results = [];

    fs.readdirSync(dir).forEach(function(file) {

        file = dir+'/'+file;
        var stat = fs.statSync(file);

        if (stat && stat.isDirectory()) {
            results = results.concat(_getAllFilesFromFolder(file))
        } 
		else {
			results.push(file);
		}
    });

	var finalArray = [];
	for(var i = 0; i < results.length; i++){
	var res = results[i].split("//");
	finalArray.push(res[1])
	};
	
	for (var j = 0; j<finalArray.length; j++){
		if(finalArray[j] === xsdFileName){
			xsdCheck = true;
		}
	};
};

app.post('/checkXML', function(req,res){
	
	upload(req, res, function (err) {
    if (err) {
      console.log('Upload Error');
      return;
    }
	
	var xmlStr = fs.readFileSync(__dirname + '/uploads/' + req.files[0].originalname);
	var data = xmlStr.toString();
	var finalData;
	
	
	parseString(data, function (err, result) {
		finalData = result.SensorCatalogue;
	});
	
	if(finalData.$.hasOwnProperty("xsi:noNamespaceSchemaLocation")){
		console.log('The value of "noNamespaceSchemaLocation is" '+ finalData.$['xsi:noNamespaceSchemaLocation']);
		xsdFileName = finalData.$['xsi:noNamespaceSchemaLocation'];
	}
	else if(finalData.$.hasOwnProperty("xsi:schemaLocation")){
		console.log('The value of "schemaLocation is" '+ finalData.$['xsi:schemaLocation']);
		xsdFileName = finalData.$['xsi:schemaLocation'];
	}
	else{
		console.log('Status 404');
		res.writeHead(404, {"Content-Type": "text/plain"});
        res.write('Status 404');
        res.end();
	}
	
	
	_getAllFilesFromFolder(__dirname + "/schemas/");
	
	
	if(xsdCheck == true){
		validator.validateXML(data, __dirname + '/schemas/' + xsdFileName, function(err, result) {
			if (err) {
				console.log("An Error was found in xml");
				console.log(err);
				res.writeHead(200, {"Content-Type": "text/plain"});
				res.write(err);
				res.end();
				process.exit(1);
			}
		console.log("The name of XSD = " + xsdFileName);
		console.log("The xml file is valid "+ result.valid);
		
		res.writeHead(200, {"Content-Type": "text/plain"});
        res.write("The xml file is valid against "+ xsdFileName);
        res.end();
		});
	
	}
	else if(xsdCheck == false){
		console.log('Status 404');
		res.writeHead(404, {"Content-Type": "text/plain"});
        res.write('Status 404 No XSD Found!!!');
        res.end();
	}
  });
});

app.get('/', function(req, res){
	res.sendFile(__dirname + '/index.html');
});

server.listen(8080, function(){
	console.log('Started on port 8080...');
});


