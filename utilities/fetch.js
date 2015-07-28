var jsdom = require("jsdom");
var fs = require('fs');

//var nodeCouchDB = require("node-couchdb");
//var couch = new nodeCouchDB("admin:admin@localhost", 5984);

var cdate = new Date(2014, 8, 18); // Start datum

function getData(selectedDate, rowId) {
  console.log(selectedDate, rowId);
  
  jsdom.env(
    "http://www.ejustice.just.fgov.be/cgi_tsv/tsv_l_1.pl?sql=pd='" + selectedDate + "' and (akte='c01' or akte='c02')&fromtab=TSV_TMP&row_id=" + rowId,
    [],
    function (errors, window) {
      console.log(errors);
      var rows = window.document.getElementsByTagName("tr");
      
      if (window.document.body.innerHTML.indexOf("Ende der Liste") == -1) {
        var result_items = [];
        
        for (var i=0; i<rows.length; i++){
          try {
            var row = rows[i];
            var items = row.children[1].childNodes;
            
            var offset  = items.length > 12 ? 2 : 0;
            
            var naam    = items[1].textContent.trim();
            var otype   = items[2].textContent.trim();
            var btwnr   = items[6].textContent.trim();
            var adres   = items[4].textContent.trim();
            var rubriek = items[8 + offset].textContent.trim();
            var ref     = items[10 + offset].textContent.trim().split(" / ");
            var datum   = ref[0];
            var dagref  = ref[1];
            
            var adresData  = adres.match("([A-Z\- ']+)[, A-Z]*([0-9]*).* ([0-9]+) ([A-Z\- ']+)");   //.match("([A-Z\- ']+[, A-Z]*[0-9]*).* ([0-9]+ [A-Z\- ']+)");
            
            var adresProper = {
              straat   : adresData[1].trim(),
              nr       : adresData[2],
              postcode : adresData[3],
              plaats   : adresData[4],
              orgineel : adres
            };
                        
            var type = (rubriek.indexOf("FIN")+rubriek.indexOf("EINDE")) != -2 ? "einde" : "oprichting";
            
            var doc = {
              datum   : datum,
              rubriek : type,
              naam    : naam,
              adres   : adresProper,
              otype   : otype,
              btwnr   : btwnr,
              ref     : dagref
            };
            
            //couch.insert("ejustice", doc, function (err, resData) {
            //    if (err) return console.error(err);
            //});
            
            console.log(parseInt(rowId) + i);
            console.log(doc);
            
            result_items.push(doc);
          } catch (e) {
            console.log("error :(", e);
          }
        }
        
        fs.writeFile("./data/" + selectedDate + "_" + rowId + ".json", JSON.stringify(result_items), function (err) { if (err) console.log(err); } );
      }
      
      var nextRowId = rowId;
      try {
        nextRowId = window.document.getElementsByName("next_row_id")[0].value;
      } catch (e) {
        console.log("slechte html, waarschijnlijk een feestdag of weekend ofzo");
      }
      
      if (nextRowId != rowId) {
        getData(selectedDate, nextRowId);
      } else {
        setTimeout(volgendeDag, 100);
      }

    }
  );
  
}

var year  = cdate.getFullYear();
var month = cdate.getMonth() + 1;
var day   = cdate.getDate();
var selectedDate = "" + year + "-" + month + "-" + day;

function volgendeDag() {
  cdate.setDate(cdate.getDate() + 1);
  
  var year  = cdate.getFullYear();
  var month = cdate.getMonth() + 1;
  var day   = cdate.getDate();
  var selectedDate = "" + year + "-" + month + "-" + day;
   
  getData(selectedDate, 1);
}

volgendeDag();
//getData(selectedDate, 91);
