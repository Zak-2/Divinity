package divinity.gui.alt.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public final class ProfileResponse {
    @Expose
    @SerializedName("id")
    public String id;
    @Expose
    @SerializedName("name")
    public String name;
}