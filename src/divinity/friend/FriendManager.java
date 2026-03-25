package divinity.friend;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import divinity.ClientManager;
import divinity.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FriendManager implements Core {

    private final List<Friend> friends;

    public FriendManager() {
        friends = new ArrayList<>();
    }

    @Override
    public void initialize() {
        loadFriends();
    }

    @Override
    public void shutdown() {
        saveFriends();
    }

    public List<Friend> getFriends() {
        return friends;
    }

    public void addFriend(Friend friend) {
        friends.add(friend);
    }

    public void addFriend(String name) {
        friends.add(new Friend(name));
    }

    public boolean isFriend(String name) {
        return friends.stream().anyMatch(friend -> friend.getName().equalsIgnoreCase(name));
    }

    public void remove(String name) {
        friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
    }

    public char getNameColour(final EntityLivingBase e) {
        String formatted = e.getDisplayName().getFormattedText();
        int idx = formatted.indexOf('§');
        if (idx != -1 && idx + 1 < formatted.length()) {
            return formatted.charAt(idx + 1);
        }
        return '\0';
    }

    public boolean isSameTeamByName(final EntityLivingBase e) {
        char mine = getNameColour(Minecraft.getMinecraft().thePlayer);
        char their = getNameColour(e);
        return mine != '\0' && mine == their;
    }

    public void saveFriends() {
        String directoryPath = ClientManager.getInstance().getCLIENT_DIR() + "/";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directoryPath + "friends.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(friends, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFriends() {
        String directoryPath = ClientManager.getInstance().getCLIENT_DIR() + "/";
        File file = new File(directoryPath + "friends.json");
        if (!file.exists()) {
            System.out.println("No friends.json at " + file.getAbsolutePath());
            return;
        }
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            Type friendListType = new TypeToken<List<Friend>>() {
            }.getType();
            List<Friend> loadedFriends = gson.fromJson(reader, friendListType);
            if (loadedFriends != null) {
                friends.clear();
                friends.addAll(loadedFriends);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
