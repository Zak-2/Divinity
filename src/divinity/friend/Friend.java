package divinity.friend;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Friend {

    private final String name;

    public Friend(String name) {
        this.name = name;
    }
}
