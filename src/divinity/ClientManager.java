package divinity;

import divinity.command.CommandManager;
import divinity.gui.alt.AltManager;
import divinity.event.base.Event;
import divinity.event.base.dispatcher.EventDispatcher;
import divinity.friend.FriendManager;
import divinity.gui.click.Clickable;
import divinity.handler.BlinkHandler;
import divinity.handler.PlayerTrackerHandler;
import divinity.module.ModuleManager;
import divinity.module.impl.render.element.GuiEditElement;
import divinity.notifications.NotificationManager;
import divinity.utils.player.rotation.RotationHandler;
import divinity.utils.shaders.ShaderMenu;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class ClientManager implements Core {
    private static final ClientManager INSTANCE = new ClientManager();
    public final String name = "Divinity", version = "1.0.0";
    public final BuildType buildType = BuildType.DEVELOPMENT;
    private final Path CLIENT_DIR = Paths.get(System.getenv("APPDATA"), "divinity");
    private final EventDispatcher<Event> eventDispatcher = new EventDispatcher<>();
    private final CommandManager commandManager = new CommandManager();
    private final Clickable clickable = new Clickable();
    private final FriendManager friendManager = new FriendManager();
    private final BlinkHandler blinkHandler = new BlinkHandler();
    private final RotationHandler rotationHandler = new RotationHandler();
    public ModuleManager moduleManager;
    private AltManager altManager;
    public int delta;
    public long lastFrame;
    public ShaderMenu shaderMenu;
    public boolean isMcChat = true;
    public boolean welcomed = false;
    private NotificationManager notificationManager;
    private GuiEditElement guiEditElement;

    //TEMP
    @Setter
    private Color mainColor = new Color(121, 105, 229);
    @Setter
    private Color secondaryColor = new Color(73, 63, 137);

    public static ClientManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void initialize() {
        moduleManager.initialize();
        commandManager.initialize();
        clickable.initialize();
        eventDispatcher.register(new PlayerTrackerHandler());
        eventDispatcher.register(blinkHandler);
        eventDispatcher.register(rotationHandler);

        if (!ImageIO.getImageReadersByFormatName("JPEG").hasNext()) {
            System.err.println("JPEG support not available!");
        } else {
            System.out.println("JPEG texture support enabled for cutscenes");
        }

        notificationManager = new NotificationManager();
        guiEditElement = new GuiEditElement();
        altManager = new AltManager();


    }

    public AltManager getAltManager() {
        return altManager;
    }

    @Override
    public void shutdown() {
        moduleManager.shutdown();
        friendManager.shutdown();
    }
}
