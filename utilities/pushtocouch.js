var nodeCouchDB = require("node-couchdb");
var fs = require('fs');

var couch = new nodeCouchDB("admin:admin@localhost", 5984);
var filename = process.argv[2];

fs.readFile(filename, 'utf8', function (err, data) {
  if (err) {
    return console.log(err);
  } else {
    var items = JSON.parse(data);
    
    items.forEach(function (item, index, array) {
      couch.insert("ejustice", item, function (err, resData) {
        if (err) return console.error(err);
      });
    });
  }
});



