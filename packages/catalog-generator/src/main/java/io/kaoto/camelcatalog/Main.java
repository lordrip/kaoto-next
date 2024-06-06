package io.kaoto.camelcatalog;

public class Main {

    public static void main(String[] args) {
        KaotoCamelCatalogBuilder builder = new KaotoCamelCatalogBuilder();
        var kaotoCamelCatalogMojo = builder.withCamelCatalogVersion("4.6.0")
                .withKameletsVersion("4.6.0")
                .withCamelKCRDsVersion("2.3.1")
                .build();

        kaotoCamelCatalogMojo.generate();
    }
}
