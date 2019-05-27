package de.edirom.meigarage.mei;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import pl.psnc.dl.ege.component.Customization;
import pl.psnc.dl.ege.exception.CustomizationException;
import pl.psnc.dl.ege.exception.EGEException;
import pl.psnc.dl.ege.types.CustomizationSetting;
import pl.psnc.dl.ege.types.CustomizationSourceInputType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MEICustomization implements Customization, ErrorHandler {


    private static final Logger LOGGER = Logger.getLogger(MEICustomization.class);

    private static final CustomizationSetting cs;

    static {
        List<CustomizationSourceInputType> sources = new ArrayList<CustomizationSourceInputType>();
        List<CustomizationSourceInputType> customizations = new ArrayList<CustomizationSourceInputType>();
        List<String> outputFormats = new ArrayList<String>();

        sources.add(new CustomizationSourceInputType("mei401", "MEI v4.0.1", CustomizationSourceInputType.TYPE_SERVER_FILE, "sources/mei-source.xml"));
        sources.add(new CustomizationSourceInputType("mei300", "MEI v3.0.0", CustomizationSourceInputType.TYPE_SERVER_FILE, "sources/mei-source.xml"));
        sources.add(new CustomizationSourceInputType("mei211", "MEI v2.1.1", CustomizationSourceInputType.TYPE_SERVER_FILE, "sources/mei-source.xml"));
        sources.add(new CustomizationSourceInputType("mei200", "MEI v2.0.0", CustomizationSourceInputType.TYPE_SERVER_FILE, "sources/mei-source.xml"));
        sources.add(new CustomizationSourceInputType("mei-local", "Local Source", CustomizationSourceInputType.TYPE_CLIENT_FILE));

        customizations.add(new CustomizationSourceInputType("c-mei-all", "MEI All", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-all.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-all-any", "MEI All anyStart", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-all_anyStart.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-cmn", "MEI CMN", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-CMN.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-mensural", "MEI Mensural", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-Mensural.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-neumes", "MEI Neumes", CustomizationSourceInputType.TYPE_SERVER_FILE, "customizations/mei-Neumes.xml"));
        customizations.add(new CustomizationSourceInputType("c-mei-local", "Local Customization", CustomizationSourceInputType.TYPE_CLIENT_FILE));

        outputFormats.add("RelaxNG");
        outputFormats.add("Compiled ODD");

        cs = new CustomizationSetting("mei", sources, customizations, outputFormats);
    }

    public void error(SAXParseException exception) throws SAXException {
        LOGGER.info("Error: " + exception.getMessage());
    }


    public void fatalError(SAXParseException exception) throws SAXException {
        LOGGER.info("Fatal Error: " + exception.getMessage());
        throw exception;
    }


    public void warning(SAXParseException exception) throws SAXException {
        LOGGER.info("Warning: " + exception.getMessage());
    }

    public void customize(InputStream inputData, CustomizationSetting customizationSetting) throws IOException, CustomizationException, EGEException {
        LOGGER.trace("MEICustomization.customize()");
    }

    public List<CustomizationSetting> getSupportedCustomizationSettings() {
        List<CustomizationSetting> css = new ArrayList<CustomizationSetting>();
        css.add(cs);
        return css;
    }
}
