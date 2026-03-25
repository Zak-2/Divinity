package divinity.module;

import com.google.gson.*;
import divinity.ClientManager;
import divinity.Core;
import divinity.module.impl.combat.*;
import divinity.module.impl.movement.*;
import divinity.module.impl.player.*;
import divinity.module.impl.render.*;
import divinity.module.impl.render.Menu;
import divinity.module.impl.world.*;
import divinity.module.impl.world.hypixel.AutoHypixel;
import divinity.module.impl.world.hypixel.MurderMystery;
import divinity.module.impl.world.lag.Backtrack;
import divinity.module.impl.world.lag.PingSpoof;
import divinity.module.property.Property;
import divinity.module.property.impl.*;
import divinity.utils.font.FontRenderer;
import lombok.Getter;

import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ModuleManager implements Core {
    private final List<Module> modules = new ArrayList<>();

    public HUD hud;
    public Menu menu;
    public HitEffects hitEffects;
    public BlockOverlay blockOverlay;
    public LocalRender localRender;
    public Atmosphere atmosphere;
    public Crosshair crosshair;
    public TargetHUD targetHUD;
    public ESP esp;
    public ItemAnimations itemAnimations;
    public Sprint sprint;
    public NoSlowDown noSlowDown;
    public Stealer stealer;
    public Manager manager;
    public AutoTool autoTool;
    public NoFall noFall;
    public Aura aura;
    public Velocity velocity;
    public AntiFireball antiFireball;
    public AutoHypixel autoHypixel;
    public MurderMystery murderMystery;
    public FastBreak fastBreak;
    public FastPlace fastPlace;
    public MiddleClickFriend middleClickFriend;
    public AutoHead autoHead;
    public Scaffold scaffold;
    public MoveFix moveFix;
    public Backtrack backtrack;
    public Breaker breaker;
    public AutoPot autoPot;
    public Speed speed;
    public ProximityChat proximityChat;
    public SprintReset sprintReset;
    public Defender defender;
    public KeepSprint keepSprint;
    public PingSpoof pingSpoof;
    public AutoClutch autoClutch;

    @Override
    public void initialize() {
        addModule(hud = new HUD("HUD", new String[]{"hud", "overlay"}, Category.RENDER));
        addModule(menu = new Menu("Menu", new String[]{"clickgui", "clickable", "interface", "menu"}, Category.RENDER));
        addModule(hitEffects = new HitEffects("Hit Effects", new String[]{"hiteffects"}, Category.RENDER));
        addModule(blockOverlay = new BlockOverlay("Block Overlay", new String[]{"blockoverlay"}, Category.RENDER));
        addModule(localRender = new LocalRender("Local Render", new String[]{"localrender"}, Category.RENDER));
        addModule(atmosphere = new Atmosphere("Atmosphere", new String[]{"worldtime", "time", "world", "atmosphere"}, Category.RENDER));
        addModule(crosshair = new Crosshair("Crosshair", new String[]{"crosshair"}, Category.RENDER));
        addModule(targetHUD = new TargetHUD("TargetHUD", new String[]{"thud", "targethud", "targetinfo"}, Category.RENDER));
        addModule(esp = new ESP("ESP", new String[]{"esp", "playeresp", "chestesp"}, Category.RENDER));
        addModule(itemAnimations = new ItemAnimations("Item Animations", new String[]{"animations", "itemanimations"}, Category.RENDER));

        addModule(sprint = new Sprint("Sprint", new String[]{"sprint"}, Category.MOVEMENT));
        addModule(speed = new Speed("Speed", new String[]{"speed", "bhop"}, Category.MOVEMENT));
        addModule(moveFix = new MoveFix("Move Fix", new String[]{"movefix", "movecorrection"}, Category.MOVEMENT));
        addModule(sprintReset = new SprintReset("Sprint Reset", new String[]{"sprintreset", "wtap", "stap"}, Category.MOVEMENT));
        addModule(autoClutch = new AutoClutch("Auto Clutch", new String[]{"antivoid", "blockclutch", "autoclutch"}, Category.MOVEMENT));

        addModule(noSlowDown = new NoSlowDown("No Slowdown", new String[]{"noslow", "noslowdown"}, Category.PLAYER));
        addModule(stealer = new Stealer("Stealer", new String[]{"cheststealer", "stealer"}, Category.PLAYER));
        addModule(manager = new Manager("Manager", new String[]{"manager", "invmanager", "invcleaner"}, Category.PLAYER));
        addModule(autoTool = new AutoTool("Auto Tool", new String[]{"autotool"}, Category.PLAYER));
        addModule(noFall = new NoFall("No Fall", new String[]{"nofall"}, Category.PLAYER));

        addModule(aura = new Aura("Kill Aura", new String[]{"aura", "killaura"}, Category.COMBAT));
        addModule(velocity = new Velocity("Velocity", new String[]{"velocity", "noknockback"}, Category.COMBAT));
        addModule(antiFireball = new AntiFireball("Anti Fireball", new String[]{"antifireball", "antiprojectile"}, Category.COMBAT));
        addModule(autoHead = new AutoHead("Auto Head", new String[]{"autohead", "autoeat"}, Category.COMBAT));
        addModule(autoPot = new AutoPot("Auto Pot", new String[]{"autopotion", "autopot"}, Category.COMBAT));
        addModule(keepSprint = new KeepSprint("Keep Sprint", new String[]{"keepsprint"}, Category.COMBAT));

        addModule(autoHypixel = new AutoHypixel("Auto Hypixel", new String[]{"autohypixel", "autoplay"}, Category.WORLD));
        addModule(murderMystery = new MurderMystery("Murder Mystery", new String[]{"mm", "murdermystery"}, Category.WORLD));
        addModule(fastBreak = new FastBreak("Fast Break", new String[]{"fastbreak", "instabreak"}, Category.WORLD));
        addModule(fastPlace = new FastPlace("Fast Place", new String[]{"fastplace", "instaplace"}, Category.WORLD));
        addModule(middleClickFriend = new MiddleClickFriend("MCF", new String[]{"friends", "team", "mcf"}, Category.WORLD));
        addModule(scaffold = new Scaffold("Scaffold", new String[]{"scaf", "scaffold", "blockfly"}, Category.WORLD));

        addModule(backtrack = new Backtrack("Backtrack", new String[]{"backtrack"}, Category.WORLD));
        addModule(pingSpoof = new PingSpoof("Ping Spoof", new String[]{"pingspoof"}, Category.WORLD));
        addModule(breaker = new Breaker("Breaker", new String[]{"breaker", "nuker", "fucker"}, Category.WORLD));
        addModule(defender = new Defender("Defender", new String[]{"defender", "antinuker", "unfucker"}, Category.WORLD));

        addModule(proximityChat = new ProximityChat("Proximity Chat", new String[]{"voice", "proximitychat", "vc"}, Category.WORLD));
        try {
            loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        saveProperties();
    }

    public Module getWithAlias(String name) {
        return modules.stream().filter(module -> module.getName().equalsIgnoreCase(name) ||
                Arrays.stream(module.getAliases()).anyMatch(alias -> alias.equalsIgnoreCase(name))).findFirst().orElse(null);
    }

    public Module get(Object key) {
        return modules.stream().filter(module -> module.getClass() == key).findFirst().orElse(null);
    }

    private void addModule(Module module) {
        modules.add(module);
    }

    public List<Module> getModules(Category category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    public void saveProperties() {
        save("configs", "lastConfig");
    }

    public void save(String path, String name) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject configObject = new JsonObject();
        for (Module module : getModules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("state", module.isState());
            moduleObject.addProperty("keybind", module.getKeyBind());
            for (Property property : module.getProperties()) {
                JsonObject propertyObject = new JsonObject();
                if (property instanceof BooleanProperty) {
                    propertyObject.addProperty("value", ((BooleanProperty) property).getValue());
                } else if (property instanceof StringProperty) {
                    propertyObject.addProperty("value", ((StringProperty) property).getValue());
                } else if (property instanceof NumberProperty) {
                    propertyObject.addProperty("value", ((NumberProperty<? extends Number>) property).getValue());
                } else if (property instanceof ColorProperty) {
                    propertyObject.addProperty("value", ((ColorProperty) property).getValue().getRGB());
                    propertyObject.addProperty("hue", ((ColorProperty) property).getHue());
                    propertyObject.addProperty("sat", ((ColorProperty) property).getSaturation());
                    propertyObject.addProperty("bright", ((ColorProperty) property).getBrightness());
                    propertyObject.addProperty("alpha", ((ColorProperty) property).getAlpha());
                } else if (property instanceof ModeProperty) {
                    propertyObject.addProperty("value", ((ModeProperty) property).getValue());
                } else if (property instanceof MultiSelectProperty) {
                    JsonArray selectedArray = new JsonArray();
                    for (String selected : ((MultiSelectProperty) property).getSelected()) {
                        selectedArray.add(new JsonPrimitive(selected));
                    }
                    propertyObject.add("selected", selectedArray);
                }
                moduleObject.add(property.getName(), propertyObject);
            }
            configObject.add(module.getName(), moduleObject);
        }
        Path configPath = ClientManager.getInstance().getCLIENT_DIR().resolve(path).resolve(name + ".json");

        try {
            Files.createDirectories(configPath.getParent());
            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                gson.toJson(configObject, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadProperties() {
        load("configs", "lastConfig");
    }

    public void load(String path, String name) {
        Gson gson = new Gson();
        Path configPath = ClientManager.getInstance().getCLIENT_DIR().resolve(path).resolve(name + ".json");

        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                JsonObject configObject = gson.fromJson(reader, JsonObject.class);
                for (Module module : getModules()) {
                    JsonObject moduleObject = configObject.getAsJsonObject(module.getName());
                    if (moduleObject == null) continue;
                    if (moduleObject.has("state")) {
                        boolean moduleState = moduleObject.get("state").getAsBoolean();
                        if (module.isState() != moduleState) {
                            module.setState(moduleState);
                        }
                    }
                    module.setKeyBind(moduleObject.get("keybind").getAsInt());
                    for (Property property : module.getProperties()) {
                        JsonElement propertyElement = moduleObject.get(property.getName());
                        if (propertyElement != null && propertyElement.isJsonObject()) {
                            JsonObject propertyObject = propertyElement.getAsJsonObject();
                            if (property instanceof BooleanProperty) {
                                property.setValue(propertyObject.get("value").getAsBoolean());
                            } else if (property instanceof StringProperty) {
                                property.setValue(propertyObject.get("value").getAsString());
                            } else if (property instanceof NumberProperty) {
                                property.setValue(propertyObject.get("value").getAsDouble());
                            } else if (property instanceof ColorProperty) {
                                ColorProperty colorProperty = (ColorProperty) property;
                                colorProperty.setRGB(propertyObject.get("value").getAsInt());
                                colorProperty.setHue(propertyObject.get("hue").getAsFloat());
                                colorProperty.setSaturation(propertyObject.get("sat").getAsFloat());
                                colorProperty.setBrightness(propertyObject.get("bright").getAsFloat());

                                Color currentColor = colorProperty.getValue();
                                int alpha = propertyObject.get("alpha").getAsInt() == 0 ? 255 : propertyObject.get("alpha").getAsInt();
                                Color newColor = FontRenderer.reAlpha(currentColor, alpha);
                                colorProperty.setValue(newColor);
                            } else if (property instanceof ModeProperty) {
                                property.setValue(propertyObject.get("value").getAsString());
                            } else if (property instanceof MultiSelectProperty) {
                                ((MultiSelectProperty) property).getSelected().clear();
                                JsonArray selectedArray = propertyObject.getAsJsonArray("selected");
                                for (JsonElement selectedElement : selectedArray) {
                                    ((MultiSelectProperty) property).activate(selectedElement.getAsString());
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
