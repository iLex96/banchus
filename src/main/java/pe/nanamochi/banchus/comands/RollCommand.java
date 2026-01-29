package pe.nanamochi.banchus.comands;

import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.utils.ParsingUtils;

@Component
@Command(
    name = "!roll",
    privileges = 0,
    documentation = "Roll a random number between 0 and a given number.")
public class RollCommand extends BaseCommand {

  private static final Logger logger = LoggerFactory.getLogger(RollCommand.class);

  @Override
  String processCommand(String trigger, String[] args) {
    logger.info("Processing command !roll");
    int max = 100;
    if (args.length != 0) {
        Integer result = ParsingUtils.parseIntSafe(args[0]);
        if (result != null) {
            max = result;
        }
    }
    return String.valueOf(ThreadLocalRandom.current().nextInt(0, max));
  }
}
