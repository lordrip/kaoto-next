package io.kaoto.camelcatalog;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.kaoto.camelcatalog.beans.ConfigBean;
import io.kaoto.camelcatalog.commands.GenerateCommand;
import io.kaoto.camelcatalog.generator.Util;
import io.kaoto.camelcatalog.model.CatalogCliArgument;
import io.kaoto.camelcatalog.model.CatalogRuntime;

public class Main {
        private static Options options = new Options();

        public static void main(String[] args) {
                Option outputOption = Option.builder().argName("outputDir").option("o").longOpt("output")
                                .desc("Output directory. It will be cleaned before generating the catalogs").hasArg()
                                .required()
                                .build();
                Option kameletsVersionOption = Option.builder().argName("kameletsVersion").option("k")
                                .longOpt("kamelets")
                                .desc("Kamelets catalog version").hasArg().required().build();
                Option camelMainVersionOption = Option.builder().argName("version").option("m").longOpt("main")
                                .desc("Camel Main version")
                                .hasArg().build();
                Option camelQuarkusVersionOption = Option.builder().argName("version").option("q").longOpt("quarkus")
                                .desc("Camel Extensions for Quarkus version").hasArg().build();
                Option camelSpringbootVersionOption = Option.builder().argName("version").option("s")
                                .longOpt("springboot")
                                .desc("Camel SpringBoot version").hasArg().build();
                Option verboseOption = Option.builder().argName("v").option("v").longOpt("verbose")
                                .desc("Be more verbose")
                                .build();

                options.addOption(outputOption);
                options.addOption(kameletsVersionOption);
                options.addOption(camelMainVersionOption);
                options.addOption(camelQuarkusVersionOption);
                options.addOption(camelSpringbootVersionOption);
                options.addOption(verboseOption);

                ConfigBean configBean = new ConfigBean();
                CommandLineParser parser = new DefaultParser();

                CommandLine cmd = null;
                try {
                        cmd = parser.parse(options, args);
                        configBean.setOutputFolder(Util.getNormalizedFolder(cmd.getOptionValue(outputOption.getOpt())));
                        configBean.setKameletsVersion(cmd.getOptionValue(kameletsVersionOption.getOpt()));

                        addRuntimeVersions(configBean, cmd, camelMainVersionOption, CatalogRuntime.Main);
                        addRuntimeVersions(configBean, cmd, camelQuarkusVersionOption, CatalogRuntime.Quarkus);
                        addRuntimeVersions(configBean, cmd, camelSpringbootVersionOption, CatalogRuntime.SpringBoot);
                } catch (ParseException e) {
                        System.out.println("Missing required options");
                        printHelpAndExit();
                }

                GenerateCommand generateCommand = new GenerateCommand(configBean);
                generateCommand.run();
        }

        private static void printHelpAndExit() {
                printHelp();
                System.exit(1);
        }

        private static void printHelp() {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("catalog-generator", options);
        }

        private static void addRuntimeVersions(ConfigBean configBean, CommandLine cmd, Option option,
                        CatalogRuntime runtime) {
                String[] versions = cmd.getOptionValues(option.getOpt());
                if (versions != null) {
                        for (String version : versions) {
                                configBean.addCatalogVersion(new CatalogCliArgument(runtime, version));
                        }
                }
        }
}
