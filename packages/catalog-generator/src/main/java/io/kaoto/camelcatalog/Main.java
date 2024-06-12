package io.kaoto.camelcatalog;

import io.kaoto.camelcatalog.commands.GenerateCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

@QuarkusMain
@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = { GenerateCommand.class })
public class Main implements Runnable, QuarkusApplication {
    @Inject
    CommandLine.IFactory factory;

    @Override
    public void run() {
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).execute(args);
    }

    public static void main(String[] args) {
        Quarkus.run(Main.class, args);
    }
}
