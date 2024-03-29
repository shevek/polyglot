/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class IndexedUniverse<IndexedItem extends Indexed> {

    /** A map from LRItem.index to LRItem, which allows us to emulate Set&lt;LRItem&gt; and also to find LRItem(CstAlternativeModel, n+1). */
    private final Class<? extends Indexed> itemType;
    protected final List<IndexedItem> itemList;

    public IndexedUniverse(@Nonnull Class<? extends Indexed> itemType, @Nonnull List<IndexedItem> itemList) {
        this.itemType = Preconditions.checkNotNull(itemType, "ItemType was null.");
        this.itemList = Preconditions.checkNotNull(itemList, "ItemList was null.");
    }

    public IndexedUniverse(@Nonnull Class<? extends Indexed> itemType) {
        this(itemType, new ArrayList<IndexedItem>());
    }

    @Nonnull
    public Class<? extends Indexed> getItemType() {
        return itemType;
    }

    @Nonnull
    public List<? extends IndexedItem> getItems() {
        return itemList;
    }

    @Nonnull
    public IndexedItem getItemByIndex(@Nonnegative int index) {
        // There may be nulls in the universe, but we should never retrieve one.
        return Preconditions.checkNotNull(itemList.get(index), "Unexpected null in universe.");
    }

    protected void addItemAtIndex(@Nonnull IndexedItem item) {
        int index = item.getIndex();
        for (int i = itemList.size() - 1; i < index; i++)
            itemList.add(null);
        if (itemList.get(index) != null)
            throw new IllegalStateException("Universe already has an item at " + index);
        itemList.set(index, item);
    }

    @Nonnegative
    public int size() {
        return itemList.size();
    }

    @Nonnull
    public String toString(@Nonnull IntCollection indices) {
        if (indices.isEmpty())
            return "{}";
        StringBuilder buf = new StringBuilder("{");
        for (IntIterator it = indices.iterator(); it.hasNext(); /**/) {
            buf.append(' ').append(getItemByIndex(it.nextInt()));
        }
        return buf.append(" }").toString();
    }
}
