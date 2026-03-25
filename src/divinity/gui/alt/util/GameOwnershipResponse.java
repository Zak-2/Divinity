package divinity.gui.alt.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public final class GameOwnershipResponse {
    @Expose
    @SerializedName("items")
    private Item[] items;

    public boolean hasGameOwnership() {
        boolean hasProduct = false;
        boolean hasGame = false;

        for (final Item item : items) {
            if (item.name.equals("product_minecraft")) {
                hasProduct = true;
            } else if (item.name.equals("game_minecraft")) {
                hasGame = true;
            }
        }

        return hasProduct && hasGame;
    }

    private static class Item {
        @Expose
        @SerializedName("name")
        private String name;
    }
}