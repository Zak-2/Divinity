package divinity.gui.alt.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public final class AuthTokenResponse {
    @Expose
    @SerializedName("access_token")
    public String access_token;
    @Expose
    @SerializedName("refresh_token")
    public String refresh_token;
}