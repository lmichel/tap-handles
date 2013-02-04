function initFunctions () {
	this.initMVC = function() {
		var resultPaneModel      = new $.ResultPaneModel();
		resultPaneView           = new $.ResultPaneView();
		new $.ResultPaneController(resultPaneModel, resultPaneView);

		var tapModel       = new $.TapModel();
		tapView            = new $.TapView();
		new $.TapController(tapModel, tapView);

		var cartModel       = new $.CartModel();
		cartView            = new $.CartView();
		new $.CartControler(cartModel, cartView);

		var nodeFilterModel       = new $.NodeFilterModel();
		nodeFilterView            = new $.NodeFilterView();
		new $.NodeFilterController(nodeFilterModel, nodeFilterView);
	};

	this.initLayout = function() {
		$.alerts.overlayOpacity = 0.5;
		$.alerts.overlayColor = '#000';
		/*
		 * layout plugin, requires JQuery 1.7 or higher
		 * Split the bottom div in 3 splitters divs.
		 */		
		layoutPane = $('#accesspane').layout();
	};

	this.initNodeAccess = function() {
		Processing.show("Fetching available nodes");
		$(".logo").attr("class", "logourbana");
		$.ajax({
			async: false,
			type: 'GET',
			dataType: 'json',
			url: "availablenodes",
			error: function() {
				Processing.hide();
				Modalinfo.error("availablenodes failure") ;
				},
			success: function(data) {
				Processing.hide();
				sessionID = data.sessionID;
				for( var i=0 ; i<data.nodes.length ; i++) {
					nodeList[nodeList.length] = {
							  id   :  data.nodes[i].key
							, text : data.nodes[i].key+ ' [' + data.nodes[i].description + ']'
							, ivoid: data.nodes[i].ivoid 
							, url: data.nodes[i].url
							, description: data.nodes[i].description
							, extra: "<br>" +data.nodes[i].url + "<br>" +data.nodes[i].ivoid };
				}
				$('input#node_selector').jsonSuggest(
						{data: nodeList
							, minCharacters: 0
							, onSelect: function(data){
								var key = $('#node_selector').val().split(' ')[0];
								resultPaneView.fireNewNodeEvent(key);
								$('#node_selector').val(key);
								}
						});
				setTimeout('$(".logourbana").attr("class", "logo")', 2000);
			}
		});


		$("input#node_selector").keypress(function(event) {
			if (event.which == '13') {
				resultPaneView.fireNewNodeEvent($('#node_selector').val());
			}
		});
		/*
		 * Name resolver buton activation
		 */
		$(".kw_filter").keyup(function(event) {
			var val = $(this).val();
			tapView.fireFilterColumns(val);
			$('.kw_filter').val(val);

		});
		/*
		 * Change the table displayed in the KW lists
		 */
		$('.table_filter').change(function() {
			tapView.fireChangeTable(this.value);
		});
	};

	this.initDataTree = function() {
		dataTree = $("div#treedisp").jstree({
			"json_data"   : {"data" : [ {  "attr"     : { "id"   : "rootid", "title": "Dummy node: Select one first with the node selector on the page top." },
				"data"        : { "title"   : "Tap Nodes" }}]}  , 
				"plugins"     : [ "themes", "json_data", "dnd", "crrm"],
				"dnd"         : {"drop_target" : "#resultpane,#taptab,#showquerymeta",
					"drop_finish" : function (data) {
						var parent = data.r;
						var id = data.o.attr("id");
						var streepath ;
						if( id == null || (streepath = data.o.attr("id").split(';')).length < 3 ) {
							Modalinfo.info("Meta data only available for tables: ("  +  streepath + ")", 'User Input Error');
						}
						else {
							var treePath = {nodekey: streepath[0], schema: streepath[1], table: streepath[2]};
							while(parent.length != 0  ) {
								resultPaneView.fireSetTreePath(treePath);	
								if(parent.is('#resultpane') ) {
									setTitlePath(treePath);
									resultPaneView.fireTreeNodeEvent(treePath);	
									return;
								}
								else if(parent.attr('id') == "showquerymeta" ) {
									resultPaneView.fireShowMetaNode(treePath);	
									return;
								}

								else if(  parent.attr('id') == "taptab") {
									tapView.fireTreeNodeEvent(treePath);	
									return;
								}
								parent = parent.parent();
							}
						}
					}
				},
				// Node sorting by DnD blocked
				"crrm" : {"move" : {"check_move" : function (m) {return false; }}
				}
		}); // end of jstree

		dataTree.bind("dblclick.jstree", function (e, data) {
			var node = $(e.target).closest("li");
			var id = node[0].id; //id of the selected node					
			var treePath = id.split(';');
			if( treePath.length == 1 ) {
				var nm = $(e.target).closest("a")[0].id;
				if( nm != "" ) {
					nodeFilterView.fireOpenSelectorWindow(nm);
				}
			} else if( treePath.length < 3 ) {
				Modalinfo.info("Query can only be applied on one data category or one data class: ("  +  treePath + ")", 'User Input Error');
			} else {
				var fTreePath = {nodekey: treePath[0], schema: treePath[1], table: treePath[2]};
				resultPaneView.fireSetTreePath(fTreePath);	
				setTitlePath(fTreePath);
				resultPaneView.fireTreeNodeEvent(fTreePath);	
			}
		});
		rootUrl = "http://" + window.location.hostname +  (location.port?":"+location.port:"") + window.location.pathname;
		/*
		 * Connect the URL passed as parameter
		 */
		var defaultUrl  =  (RegExp('url=' + '(.+?)(&|$)').exec(location.search)||[,null])[1];
		if( defaultUrl != null ) {
			resultPaneView.fireNewNodeEvent(unescape(defaultUrl));
		}
	}

	this.initQueryForm = function() {
		/*
		 * Activate submit buttons
		 */
		$('#submitquery').click(function() {
			resultPaneView.fireSubmitQueryEvent();
		});
		$("#qlimit").keyup(function(event) {
			if( $("#qlimit").val() == '' || $("#qlimit").val().match(/^[0-9]+$/) ) {
				tapView.fireUpdateQueryEvent();			
			} else {
				Modalinfo.info('The result limit must be a positive integer value' , "User Input Error");
				$("#qlimit").val(100);
				tapView.fireUpdateQueryEvent();			
				return false;
			}

		});

		/*********************************************************************************************
		 * Query form setup
		 */
		$("#taptab").tabs();
		$("#taptab").tabs({
			unselect : true
		});
		$("#tapconstraintlist").droppable({
			drop: function(event, ui){
				tapView.fireAttributeEvent(ui.draggable);		
			}
		});
		$("#tapselectlist").droppable({
			drop: function(event, ui){
				tapView.fireSelectEvent(ui.draggable);		
			}
		});
		$("#taporderby").droppable({
			drop: function(event, ui){
				tapView.fireOrderByEventEvent(ui.draggable);		
			}
		});
		$("#tapalpha").droppable({
			drop: function(event, ui){
				tapView.fireAlphaEvent(ui.draggable);		
			}
		});
		$("#tapdelta").droppable({
			drop: function(event, ui){
				tapView.fireDeltaEvent(ui.draggable);		
			}
		});
		/*
		 * Coordinates input
		 */
		$("#tapwhere input").keypress(function(event) {
			if (event.which == '13') {
				tapView.fireInputCoordEvent();
			}
		});
		/*
		 * Name resolver buton activation
		 */
		$(".sesame").click(function() {
			var inputfield = $(this).parents('div').find(".coordinputvalue");
			Processing.show("Waiting on SESAME response");
			$.getJSON("sesame", {object: inputfield.val() }, function(data) {
				Processing.hide();
				if( Processing.jsonError(data, "Sesame failure") ) {
					return;
				} else {
					inputfield.val(data.alpha + ' ' + data.delta);
				}
			});
		});
		/*
		 * This callback can be changed changed at everytime: do not use the "onclick" HTML  
		 * attribute which is not overriden by JQuery "click" callback
		 */
		$('#showquerymeta').click(function(){Modalinfo.info("No meta data available yet", 'Application not Ready');});
		tapView.fireRefreshJobList();
	}
	this.initSamp = function() {
		WebSamp_mVc.init("TAPHandle"
				, "http://saada.u-strasbg.fr/taphandle/images/tap64.png"
				, "Universal TAP service browser");
	}
};


var whenReady = new initFunctions ();