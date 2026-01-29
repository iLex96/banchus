package pe.nanamochi.banchus.comands;

import java.util.Arrays;
import java.util.List;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

@Component
public class CommandProcessor {

  private final List<? extends BaseCommand> commands;

  public CommandProcessor(List<? extends BaseCommand> beans) {
    this.commands =
        beans.stream()
            .filter(bean -> AnnotationUtils.findAnnotation(bean.getClass(), Command.class) != null)
            .map(BaseCommand.class::cast)
            .toList();
  }

  public String handle(String message, int userPrivileges) {
    BaseCommand command =
        this.commands.stream()
            .filter(processor -> processor.shouldExecute(message, userPrivileges))
            .findFirst()
            .orElse(null);
    if (command == null) return null;

    String[] parts = message.split(" ");
    String trigger = parts[0];
    String[] args = Arrays.copyOfRange(parts, 1, parts.length);

    return command.processCommand(trigger, args);
  }
}
