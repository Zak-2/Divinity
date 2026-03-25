package divinity.event.impl.minecraft;

import divinity.event.base.Event;

// This is basically GameTickEvent, but it doesn't allow inputs when inputs wouldn't normally be allowed
// (I.E) (Won't allow inputs, if in a GUI, Chat Open, ETC)
public class HandleInputEvent extends Event {
}
