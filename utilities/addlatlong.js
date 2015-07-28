var nodeCouchDB = require("node-couchdb");
var fs = require('fs');
var request = require("request")

var couch = new nodeCouchDB("admin:admin@localhost", 5984);
var cId = 0;

var fixmap = {
	"CHSEE-N-D-LOUVIGNIES" : "Chaussee-Notre-Dame-Louvignies",
	"BELLEVAUX-LIGNEUVILL" : "BELLEVAUX-LIGNEUVILLE",
	"LASNE-CH-ST-LAMBERT" : "Lasne-Chapelle-Saint-Lambert",
	"BEVEREN-A-D-IJZER" : "Beveren",
    "MONTIGNIES-ST-CHRIST":"Montignies-Saint-Christophe",
    "ORSMAAL-GUSSENHOVEN": "Orsmaal",
    "RAVELS POPPEL": "POPPEL",
    "ZOERLE-PERWIJS": "ZOERLE-PARWIJS",
    "WASMES-AUDEMEZ-BRIFF":"Wasmes",
    "STREE-LEZ-HUY":"STREE",
    "ST-PIETERS-KAP":"sint-pieters-kapelle",
    "ST-MARIA-OUD":"Sint-Maria-Oudenhove",
    "ST-GEORGES-S-MEUSE":"Saint-Georges-sur-Meuse",
    "SPIERE-HELKYN":"SPIERE-HELKIJN",
    "SINT-REMY-GEEST":"Geldenaken",
    "SINT-BLAZIUS-BOEKEL": "SINT-BLASIUS-BOEKEL",
    "SAINT-REMY-GEEST": "Geldenaken"
};

couch.get("ejustice", "_design/views/_view/NoLatlon", {reduce: false}, function (err, resData) {
    if (err)
      console.error(err);
    else {
      docs = resData.data.rows;
      console.log(resData)
      updateDoc();
    }
});

function updateDoc() {
  console.log(cId);
  if (cId < docs.length) {
    var id = docs[cId++].id;
    if (id.indexOf('_design') == 0) {
      updateDoc();
    } else { 
      couch.get("ejustice", id, function (err, resData) {
        if (err)
            console.error(err);
        else {
          fixDoc(resData.data);
        }
      });
    }
  }
}

function fixDoc(doc) {
  doc.adres.plaats = doc.adres.plaats.trim();
  
  console.log(typeof fixmap[doc.adres.plaats]);
  
  if (typeof fixmap[doc.adres.plaats] !== "undefined") {
    doc.adres.plaats=fixmap[doc.adres.plaats];
  }
  
  getAddrType1(doc);
}

function goodAddr(latlon, doc, type) {
  if (latlon.lat != 0) {
    doc.adres.latlon = latlon;
    couch.update("ejustice", doc, function(err) {if (err) console.log(err);});
    console.log("Found it!", latlon);
    updateDoc();
  } else {
    type(doc); 
  }
}

function getAddrType1(doc) {
  var adres = doc.adres;
  var addr = adres.orgineel;
  
  getLatLong(addr, goodAddr, doc, getAddrType2);
}

function getAddrType2(doc) {
  var adres = doc.adres;
  var addr = adres.straat + " " + adres.nr + ", " + adres.postcode + " " + adres.plaats;

  getLatLong(addr, goodAddr, doc, getAddrType3);
}

function getAddrType3(doc) {
  var adres = doc.adres;
  var addr = adres.straat + " " + adres.nr + ", " + adres.plaats;

  getLatLong(addr, goodAddr, doc, getAddrType4);
}

function getAddrType4(doc) {
  var adres = doc.adres;
  var addr = adres.plaats;
  
  getLatLong(addr, goodAddr, doc, getAddrType5);
}

function getAddrType5(doc) {
  console.log("Giving up latlon search for " + doc._id + " :(");
  updateDoc();
}

function getLatLong(addr, callback, doc, type) {
  var result = {lat: 0, lon:0};
  console.log("searching for " + addr);
  
  request(
    {
      url: "http://nominatim/search.php?format=json&q=" + encodeURIComponent(addr),
      json: true
    },
    function (error, response, body) {
      if (!error && response.statusCode === 200) {
        if (body.length > 0) {
          var data = body[0];
          result.lat = data.lat;
          result.lon = data.lon;
        }
      }
      
      callback(result, doc, type);
    }
  );
}