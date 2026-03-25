package divinity.command;

public interface Command {

    boolean run(String[] args);

    String usage();

}