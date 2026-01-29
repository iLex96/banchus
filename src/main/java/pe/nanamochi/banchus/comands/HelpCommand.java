package pe.nanamochi.banchus.comands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Command(name = "!help", privileges = 0, documentation = "Displays a list of available commands.")
public class HelpCommand extends BaseCommand {

  private static final Logger logger = LoggerFactory.getLogger(HelpCommand.class);
  @Autowired private ApplicationContext context;

  @Override
  public String processCommand(String trigger, String[] args) {
    logger.info("Processing command !help");
    StringBuilder output = new StringBuilder();
    context
        .getBeansWithAnnotation(Command.class)
        .values()
        .forEach(
            processor -> {
              BaseCommand commandProcessor = (BaseCommand) processor;
              Command commandAnnotation = commandProcessor.getClass().getAnnotation(Command.class);
              output
                  .append(commandAnnotation.name())
                  .append(" - ")
                  .append(commandAnnotation.documentation())
                  .append("\n");
            });
    return output.toString();
  }
}
