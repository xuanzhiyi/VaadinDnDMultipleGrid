package com.xxxx;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.grid.DropLocation;
import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.components.grid.GridDragSource;
import com.vaadin.ui.components.grid.GridDropEvent;
import com.vaadin.ui.components.grid.GridDropTarget;

public class MultiGridRowDragger<T> implements Serializable {

   
    /**
     * Set of items currently being dragged.
     */
    private List<T> draggedItems;
    private int shiftedDropIndex;
    private GridDragSource<T> gridDragSource;

    @SafeVarargs
    public MultiGridRowDragger(Grid<T>... grids) {

        for (Grid<T> grid : grids) {
            GridDragSource<T> gridDragSource1 = new GridDragSource<>(grid);
            GridDropTarget<T> gridDropTarget1 = new GridDropTarget<>(grid,
                    DropMode.BETWEEN);
            gridDropTarget1.setDropAllowedOnRowsWhenSorted(false);

            gridDragSource1.addGridDragStartListener(event -> {
                draggedItems = event.getDraggedItems();
                gridDragSource = gridDragSource1;
            });
            gridDropTarget1.addGridDropListener(this::handleDrop);
        }

    }

    protected void handleDrop(GridDropEvent<T> event) {
        // there is a case that the drop happened from some other grid than the
        // source one
        if (draggedItems == null) {
            return;
        }

        shiftedDropIndex = -1;
        handleSourceGridDrop(event, draggedItems);

        int index = calculateDropIndex(event);

        handleTargetGridDrop(event, index, draggedItems);

        draggedItems = null;
    }

    private void handleSourceGridDrop(GridDropEvent<T> event,
            final Collection<T> droppedItems) {
        Grid<T> source = gridDragSource.getGrid();

        ListDataProvider<T> listDataProvider = (ListDataProvider<T>) source
                .getDataProvider();

        // use the existing data source to keep filters and sort orders etc. in
        // place.
        Collection<T> sourceItems = listDataProvider.getItems();

        // if reordering the same grid and dropping on top of one of the dragged
        // rows, need to calculate the new drop index before removing the items
        // if (gridDragSource.getGrid() == gridDropTarget.getGrid()
        // && event.getDropTargetRow().isPresent()
        // && draggedItems.contains(event.getDropTargetRow().get())) {
        // List<T> sourceItemsList = (List<T>) sourceItems;
        // shiftedDropIndex = sourceItemsList
        // .indexOf(event.getDropTargetRow().get());
        // shiftedDropIndex -= draggedItems.stream().filter(
        // item -> sourceItemsList.indexOf(item) < shiftedDropIndex)
        // .count();
        // }

        sourceItems.removeAll(droppedItems);
        listDataProvider.refreshAll();
    }

    private void handleTargetGridDrop(GridDropEvent<T> event, final int index,
            Collection<T> droppedItems) {
        Grid<T> target = event.getComponent();

        ListDataProvider<T> listDataProvider = (ListDataProvider<T>) target
                .getDataProvider();
        // update the existing to keep filters etc.
        List<T> targetItems = (List<T>) listDataProvider.getItems();

        if (index != Integer.MAX_VALUE) {
            targetItems.addAll(index, droppedItems);
        } else {
            targetItems.addAll(droppedItems);
        }
        // instead of using setItems or creating a new data provider,
        // refresh the existing one to keep filters etc. in place
        listDataProvider.refreshAll();

        // if dropped to the end of the grid, the grid should scroll there so
        // that the dropped row is visible, but that is just recommended in
        // documentation and left for the users to take into use
    }

    private int calculateDropIndex(GridDropEvent<T> event) {

        // if the source and target grids are the same, then the index has been
        // calculated before removing the items. In this case the drop location
        // is always above, since the items will be starting from that point on
        if (shiftedDropIndex != -1) {
            return shiftedDropIndex;
        }

        ListDataProvider<T> targetDataProvider = (ListDataProvider<T>) event
                .getComponent().getDataProvider();
        List<T> items = (List<T>) targetDataProvider.getItems();
        int index = items.size();

        Optional<T> dropTargetRow = event.getDropTargetRow();
        if (dropTargetRow.isPresent()) {
            index = items.indexOf(dropTargetRow.get())
                    + (event.getDropLocation() == DropLocation.BELOW ? 1 : 0);
        }

        return index;
    }

}
