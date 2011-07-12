dojo.provide("dwb.ui.ModuleGrid");

dojo.require("dojox.grid.EnhancedGrid");

// Wrap default enhanced grid to fix issues relating to row selection being
// maintained between filters, http://bugs.dojotoolkit.org/ticket/7304
dojo.declare("dwb.ui.ModuleGrid", dojox.grid.EnhancedGrid, {
	// Temporary storage for selected modules when grid is 
	// being re-rendered. We have to manually clear and re-select
	// modules because selection is based on row index rather than 
	// item. See http://bugs.dojotoolkit.org/ticket/7304
	previousSelection: null,
	lastResultSet: null,
	_start: null,
	
	constructor: function() {
		this.previousSelection = [];
		this.lastResultSet = [];
	},
	
	startup: function() {
		this.inherited(arguments);
		
		// On user scroll event, push out which rows are now visible. 
		this.connect(this.scroller, "scroll", dojo.hitch(this, function () {
			this.onVisibleRowsChange(this.scroller.firstVisibleRow, this.scroller.lastVisibleRow, this.rowCount);
		}));
	},
	
	// Need to have a reference to the last set of fetch'ed items
	_onFetchAllComplete: function(items, req){
		var item, idx;
		// List of modules not present in this result set 
		// but previously selected.
		var remainingSelection = [];

		// Loop through previous module selection, look up 
		// each module in current result set. If item is present,
		// select row. Otherwise, store reference.
		for (var i = 0; i < this.previousSelection.length; i++) {
			item = this.previousSelection[i];

			// Check if item is present in result set,
			// manually toggle row selection if found.
			idx = dojo.indexOf(items, item);
			if (idx !== -1) {
				this.rowSelectCell.toggleRow(idx, true);
			} else {
				remainingSelection.push(item);
			}
		}

		// Retain reference to selected, but not rendered, modules.
		this.previousSelection = remainingSelection;
		// Store reference to full result set
		this.lastResultSet = items;
	},
	
	_onFetchComplete : function () {
        this.inherited(arguments);
		
        // Run a secondary fetch to return all results that match the new query.
        // We need a reference to the entire result set when selecting/deselecting
        // by item.
        this.store.fetch({
            query: this.query,
            queryOptions: this.queryOptions,
            onComplete: dojo.hitch(this, "_onFetchAllComplete")
        });
	},
	
	// When module results are filtered, we must maintain reference to selected items
	// that may or may not be contained within new filtered results.
	filter: function (query) {
		// Merge stored and currently selected modules. Handle
		// lookup for modules which aren't even rendered yet.
		this.previousSelection = this.previousSelection.concat(this._getSelectedItems());

		// Manually clear module selection, we will re-select correct
		// modules after rendering. 
		if (this.previousSelection.length > 0) {
			this.indirectSelector.toggleAllSelection(false);
		}
		
		this.inherited(arguments);
	},
	
	// Find selected items, even those which haven't been rendered. 
	// Using grid.getItem(i) only works for items who've been rendered. 
	_getSelectedItems : function () {
		var selectedItems = [];
		var selectedIndices = this.selection.selected;

		for (var i = 0, l = selectedIndices.length; i < l; i++) { 
			if (selectedIndices[i]) {
				// Use reference to full result set to pull item
				selectedItems.push(this.lastResultSet[i]); 
			}
		}

		return selectedItems;
	},
		
	// Return all row items that have been selected, whether they are 
	// currently rendered or hidden due to a filter. 
	getAllSelectedItems : function () {
		var selection = this.previousSelection.concat(this._getSelectedItems());
		return selection;
	},
	
	addItemSelection : function (item) {
		// If row item is currently visible, toggle true. 
		var idx = dojo.indexOf(this.lastResultSet, item);
		if (idx !== -1) {
			this.rowSelectCell.toggleRow(idx, true);	
		// Otherwise, store reference
		} else {
			this.previousSelection.push(item);	
		}
		
		// Index of the module if shown
		return idx;
	},
	
	// Remove selected item from module grid, this may be a currently 
	// show row or in a previous selection
	removeItemSelection : function (item) {
		var idx = this.getItemIndex(item);
		if (idx !== -1) {
			this.rowSelectCell.toggleRow(idx, false);	
		} else {
			this.previousSelection.splice(dojo.indexOf(this.previousSelection, item), 1);	
		}
		
		// Force selection change event 
		this.onSelectionChanged();
	},
	
	// Given a module item, scroll to this row if available
	scrollToItem : function (item) {
		var idx = dojo.indexOf(this.lastResultSet, item);
		if (idx !== -1) {
			this.scrollToRow(idx);	
			this.onSelectionChanged();
		}
	},
	
	// Event fired when the visible row indices are modified 
	onVisibleRowsChange : function (startRowIndex, finalRowIndex, totalRowCount) {
	}
});

// Need to manually set this reference or grid won't render correctly.
dwb.ui.ModuleGrid.markupFactory = function(props, node, ctor, cellFunc){
	return dojox.grid._Grid.markupFactory(props, node, ctor, 
					dojo.partial(dojox.grid.DataGrid.cell_markupFactory, cellFunc));
};
