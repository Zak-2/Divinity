package divinity.gui.alt.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public final class XblXstsResponse {
    @Expose
    @SerializedName("Token")
    public String Token;
    @Expose
    @SerializedName("DisplayClaims")
    public DisplayClaims DisplayClaims;

    public static class DisplayClaims {
        @Expose
        @SerializedName("xui")
        public Claim[] xui;

        public static class Claim {
            @Expose
            @SerializedName("uhs")
            public String uhs;
        }
    }
}