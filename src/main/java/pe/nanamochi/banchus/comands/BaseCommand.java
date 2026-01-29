package pe.nanamochi.banchus.comands;

public abstract class BaseCommand {
  protected boolean shouldExecute(String trigger, int userPrivileges) {
    return trigger.startsWith(this.getClass().getAnnotation(Command.class).name())
        && (userPrivileges & this.getClass().getAnnotation(Command.class).privileges()) == 0;
  }

  abstract String processCommand(String trigger, String[] args);
}
