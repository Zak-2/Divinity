package divinity.notifications;

import java.util.ArrayList;

public class NotificationManager {
    private final ArrayList<Notification> notificationsToRender = new ArrayList<>();

    private final NotificationRender nr;

    public NotificationManager() {
        this.nr = new NotificationRender();
    }

    public void send(Notification notif) {
        this.notificationsToRender.add(notif);
    }

    public void delete(int index) {
        this.notificationsToRender.remove(index);
    }

    public void render() {
        this.nr.onRender();
    }

    public ArrayList<Notification> getNotifications() {
        return this.notificationsToRender;
    }

    public void addNotification(String title, String message, long time) {
        getNotifications().add(new Notification(title, message, time));
    }
}