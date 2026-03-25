package divinity.notifications;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Notification {
    private final long startTime;

    private final long duration;

    private String title;

    private String description;

    public Notification(String title, String description) {
        this.description = description;
        this.title = title;
        this.startTime = System.currentTimeMillis();
        this.duration = 3000L;
    }

    public Notification(String title, String description, long duration) {
        this.description = description;
        this.title = title;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public boolean isFinished() {
        return (System.currentTimeMillis() - this.startTime > this.duration);
    }

    public long getLifeTime() {
        return System.currentTimeMillis() - this.startTime;
    }
}